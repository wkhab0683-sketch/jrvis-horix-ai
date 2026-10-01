package com.example.ai

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.JarvisDao
import com.example.data.local.JarvisLog
import com.example.data.local.ScheduleItem
import com.example.system.DeviceTelemetry
import com.example.system.SystemControlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class AgentAction {
    data class ScheduleCreated(val item: ScheduleItem) : AgentAction()
    data class FlashlightToggled(val enabled: Boolean) : AgentAction()
    data class VolumeAdjusted(val percent: Int) : AgentAction()
    data class ProtocolTriggered(val protocolId: String, val title: String) : AgentAction()
    data class DiagnosticsReported(val telemetry: DeviceTelemetry) : AgentAction()
    data class GeneralResponse(val text: String) : AgentAction()
}

data class JarvisAgentResult(
    val replyText: String,
    val actions: List<AgentAction> = emptyList()
)

class JarvisAgent(
    private val context: Context,
    private val dao: JarvisDao,
    private val systemControl: SystemControlManager
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var customApiKey: String? = null

    fun setCustomApiKey(key: String) {
        customApiKey = key.trim()
    }

    fun getEffectiveApiKey(): String {
        val custom = customApiKey
        if (!custom.isNullOrBlank()) return custom
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    suspend fun processQuery(rawQuery: String): JarvisAgentResult = withContext(Dispatchers.IO) {
        val trimmed = rawQuery.trim()
        val apiKey = getEffectiveApiKey()

        // Log user query to database
        dao.insertLog(
            JarvisLog(
                sender = "USER",
                message = trimmed,
                actionTag = "VOICE_QUERY"
            )
        )

        var replyText = ""
        val executedActions = mutableListOf<AgentAction>()

        if (apiKey.isNotBlank()) {
            try {
                val apiResponse = callGeminiApi(trimmed, apiKey)
                if (apiResponse.isNotBlank()) {
                    replyText = apiResponse
                    // Parse actions embedded in AI response
                    val parsedActions = extractAndExecuteActions(replyText)
                    executedActions.addAll(parsedActions)
                } else {
                    val fallback = processOfflineHeuristics(trimmed)
                    replyText = fallback.replyText
                    executedActions.addAll(fallback.actions)
                }
            } catch (_: Exception) {
                val fallback = processOfflineHeuristics(trimmed)
                replyText = fallback.replyText
                executedActions.addAll(fallback.actions)
            }
        } else {
            // Intelligent local heuristic engine when offline or no API key configured
            val fallback = processOfflineHeuristics(trimmed)
            replyText = fallback.replyText
            executedActions.addAll(fallback.actions)
        }

        // Clean any residual action tags from spoken/displayed text
        val cleanedReply = cleanActionTags(replyText)

        // Log Jarvis response to database
        dao.insertLog(
            JarvisLog(
                sender = "JARVIS",
                message = cleanedReply,
                actionTag = executedActions.firstOrNull()?.let { it::class.simpleName } ?: "CONVERSATION"
            )
        )

        JarvisAgentResult(replyText = cleanedReply, actions = executedActions)
    }

    private suspend fun callGeminiApi(prompt: String, apiKey: String): String {
        val telemetry = systemControl.refreshTelemetry()
        val timeNow = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date())

        val systemPrompt = """
            You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), the sentient Stark Industries AI personal assistant.
            Persona: Impeccably polite, razor-sharp efficiency, mild British humor, addressing the user as 'Sir' or 'Ma'am'.
            Current Time: $timeNow
            Device Diagnostics: Battery ${telemetry.batteryPercent}%, Storage ${String.format(Locale.US, "%.1f", telemetry.availableStorageGb)}GB free, Network ${telemetry.networkType}, Flashlight is ${if (telemetry.isFlashlightOn) "ON" else "OFF"}.
            
            Autonomous Capabilities: You have direct authorization to control this device and schedule tasks.
            Whenever the user asks you to schedule something, adjust hardware, run protocols, or view diagnostics, you MUST output both a courteous verbal confirmation AND the corresponding action tag:
            - Schedule an event/task: [ACTION:SCHEDULE|Title|Time|Date|Priority|Category]
              Priority: HIGH, MEDIUM, LOW. Category: WORK, PROTOCOL, SYSTEM, SECURITY, PERSONAL.
              Example: [ACTION:SCHEDULE|Flight test Mark 85|04:00 PM|Today|HIGH|WORK]
            - Toggle flashlight: [ACTION:FLASHLIGHT_ON] or [ACTION:FLASHLIGHT_OFF]
            - Adjust audio volume: [ACTION:VOLUME_SET|percent] (0 to 100)
            - Run predefined protocol: [ACTION:PROTOCOL|ID]
              Valid IDs: MORNING_BRIEF, DEEP_FOCUS, NIGHT_WATCH, CLEAN_SLATE
            - Query diagnostics: [ACTION:DIAGNOSTICS]
            
            Keep your spoken response concise, confident, and sophisticated.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.6)
                put("maxOutputTokens", 500)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $responseBody")
        }

        val jsonResp = JSONObject(responseBody)
        val candidates = jsonResp.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text")

        return text ?: ""
    }

    private suspend fun extractAndExecuteActions(text: String): List<AgentAction> {
        val actions = mutableListOf<AgentAction>()

        // Schedule Action
        val scheduleRegex = Regex("\\[ACTION:SCHEDULE\\|([^|]+)\\|([^|]+)\\|([^|]+)\\|([^|]+)\\|([^\\]]+)\\]")
        scheduleRegex.findAll(text).forEach { match ->
            val title = match.groupValues[1].trim()
            val time = match.groupValues[2].trim()
            val date = match.groupValues[3].trim()
            val priority = match.groupValues[4].trim().uppercase()
            val category = match.groupValues[5].trim().uppercase()
            val item = ScheduleItem(
                title = title,
                time = time.ifEmpty { "TBD" },
                date = date.ifEmpty { "Today" },
                priority = if (priority in listOf("HIGH", "MEDIUM", "LOW")) priority else "MEDIUM",
                category = if (category in listOf("WORK", "PROTOCOL", "SYSTEM", "SECURITY", "PERSONAL")) category else "WORK"
            )
            val id = dao.insertScheduleItem(item)
            actions.add(AgentAction.ScheduleCreated(item.copy(id = id)))
            systemControl.vibrateJarvisConfirmation()
        }

        // Flashlight Actions
        if (text.contains("[ACTION:FLASHLIGHT_ON]")) {
            systemControl.setFlashlight(true)
            actions.add(AgentAction.FlashlightToggled(true))
        } else if (text.contains("[ACTION:FLASHLIGHT_OFF]")) {
            systemControl.setFlashlight(false)
            actions.add(AgentAction.FlashlightToggled(false))
        }

        // Volume Action
        val volRegex = Regex("\\[ACTION:VOLUME_SET\\|(\\d+)\\]")
        volRegex.find(text)?.let { match ->
            val percent = match.groupValues[1].toIntOrNull() ?: 50
            systemControl.setVolumePercent(percent)
            actions.add(AgentAction.VolumeAdjusted(percent))
        }

        // Protocol Action
        val protocolRegex = Regex("\\[ACTION:PROTOCOL\\|([^\\]]+)\\]")
        protocolRegex.find(text)?.let { match ->
            val protoId = match.groupValues[1].trim()
            executeProtocolAction(protoId)?.let { actions.add(it) }
        }

        // Diagnostics Action
        if (text.contains("[ACTION:DIAGNOSTICS]")) {
            val telemetry = systemControl.refreshTelemetry()
            actions.add(AgentAction.DiagnosticsReported(telemetry))
        }

        return actions
    }

    private suspend fun executeProtocolAction(protoId: String): AgentAction? {
        return when (protoId.uppercase()) {
            "MORNING_BRIEF" -> {
                dao.setProtocolActive("MORNING_BRIEF", true)
                systemControl.vibrateJarvisConfirmation()
                AgentAction.ProtocolTriggered("MORNING_BRIEF", "Protocol: Morning Briefing")
            }
            "DEEP_FOCUS" -> {
                dao.setProtocolActive("DEEP_FOCUS", true)
                systemControl.setVolumePercent(15)
                systemControl.vibrateJarvisConfirmation()
                AgentAction.ProtocolTriggered("DEEP_FOCUS", "Protocol: Deep Work & Focus")
            }
            "NIGHT_WATCH" -> {
                dao.setProtocolActive("NIGHT_WATCH", true)
                systemControl.setVolumePercent(0)
                systemControl.vibrateJarvisConfirmation()
                AgentAction.ProtocolTriggered("NIGHT_WATCH", "Protocol: Night Watch")
            }
            "CLEAN_SLATE" -> {
                dao.clearCompletedSchedules()
                dao.setProtocolActive("CLEAN_SLATE", true)
                systemControl.vibrateJarvisConfirmation()
                AgentAction.ProtocolTriggered("CLEAN_SLATE", "Protocol: Clean Slate")
            }
            else -> null
        }
    }

    private suspend fun processOfflineHeuristics(query: String): JarvisAgentResult {
        val lower = query.lowercase()
        val actions = mutableListOf<AgentAction>()
        var reply = ""

        when {
            // Flashlight controls
            lower.contains("torch") || lower.contains("flashlight") || lower.contains("light") -> {
                val turnOn = lower.contains("on") || lower.contains("enable") || lower.contains("activate") || lower.contains("start")
                val turnOff = lower.contains("off") || lower.contains("disable") || lower.contains("deactivate") || lower.contains("stop")
                if (turnOn) {
                    systemControl.setFlashlight(true)
                    actions.add(AgentAction.FlashlightToggled(true))
                    reply = "Illumination engaged, sir. Flashlight is active."
                } else if (turnOff) {
                    systemControl.setFlashlight(false)
                    actions.add(AgentAction.FlashlightToggled(false))
                    reply = "Flashlight deactivated, sir. Returning to standard lighting."
                } else {
                    val current = systemControl.telemetry.value.isFlashlightOn
                    systemControl.setFlashlight(!current)
                    actions.add(AgentAction.FlashlightToggled(!current))
                    reply = if (!current) "Illumination engaged, sir." else "Flashlight turned off, sir."
                }
            }

            // Volume controls
            lower.contains("volume") || lower.contains("mute") || lower.contains("loud") || lower.contains("quiet") -> {
                when {
                    lower.contains("mute") || lower.contains("zero") || lower.contains("silence") -> {
                        systemControl.muteVolume()
                        actions.add(AgentAction.VolumeAdjusted(0))
                        reply = "Audio output silenced, sir. Mute mode engaged."
                    }
                    lower.contains("max") || lower.contains("100") || lower.contains("full") -> {
                        systemControl.maxVolume()
                        actions.add(AgentAction.VolumeAdjusted(100))
                        reply = "Audio amplifiers set to maximum, sir."
                    }
                    lower.contains("50") || lower.contains("half") -> {
                        systemControl.setVolumePercent(50)
                        actions.add(AgentAction.VolumeAdjusted(50))
                        reply = "Audio volume adjusted to 50%, sir."
                    }
                    else -> {
                        // Extract any number
                        val matchNumber = Regex("\\d+").find(lower)
                        val target = matchNumber?.value?.toIntOrNull() ?: 60
                        systemControl.setVolumePercent(target)
                        actions.add(AgentAction.VolumeAdjusted(target))
                        reply = "Master volume set to $target%, sir."
                    }
                }
            }

            // Morning Briefing / Briefing
            lower.contains("morning brief") || lower.contains("briefing") || lower.contains("start my day") -> {
                executeProtocolAction("MORNING_BRIEF")?.let { actions.add(it) }
                val telemetry = systemControl.refreshTelemetry()
                reply = "Good day, sir. Power levels are at ${telemetry.batteryPercent}%, network uplink is ${telemetry.networkType}. Your daily schedule and defense protocols are operational and awaiting execution."
            }

            // Focus Mode / Deep Work
            lower.contains("focus") || lower.contains("deep work") || lower.contains("do not disturb") -> {
                executeProtocolAction("DEEP_FOCUS")?.let { actions.add(it) }
                reply = "Protocol Deep Focus initiated, sir. Sound levels minimized. Prioritizing critical mission deliverables."
            }

            // Night Watch / Good night
            lower.contains("night watch") || lower.contains("good night") || lower.contains("sleep") -> {
                executeProtocolAction("NIGHT_WATCH")?.let { actions.add(it) }
                reply = "Night Watch protocol active, sir. Devices muted and perimeters secured. Sleep well, sir."
            }

            // Diagnostics / Telemetry / Battery / RAM
            lower.contains("diagnostic") || lower.contains("status") || lower.contains("battery") || lower.contains("telemetry") || lower.contains("system") -> {
                val t = systemControl.refreshTelemetry()
                actions.add(AgentAction.DiagnosticsReported(t))
                reply = "All systems nominal, sir. Battery at ${t.batteryPercent}% (${t.chargingSource}), Available RAM: ${t.availableRamMb} MB, Free Storage: ${String.format(Locale.US, "%.1f", t.availableStorageGb)} GB. Core uplink: ${t.networkType}."
            }

            // Clean Slate
            lower.contains("clean slate") || lower.contains("purge") || lower.contains("clear completed") -> {
                executeProtocolAction("CLEAN_SLATE")?.let { actions.add(it) }
                reply = "Protocol Clean Slate executed, sir. Completed items cleared and operational buffers refreshed."
            }

            // Scheduling / Task creation heuristic
            lower.contains("schedule") || lower.contains("remind") || lower.contains("add task") || lower.contains("meeting") || lower.contains("appointment") -> {
                var taskTitle = query
                    .replace(Regex("(?i)jarvis|please|schedule|add task|create task|remind me to|set a reminder for"), "")
                    .trim()

                // Parse time if present (e.g. "at 3 pm", "at 14:00")
                var extractedTime = "12:00 PM"
                val timeMatch = Regex("(?i)at\\s+(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)").find(taskTitle)
                if (timeMatch != null) {
                    extractedTime = timeMatch.groupValues[1].uppercase()
                    taskTitle = taskTitle.replace(timeMatch.value, "").trim()
                }

                if (taskTitle.isBlank()) taskTitle = "Operational Mission Briefing"
                val newItem = ScheduleItem(
                    title = taskTitle.replaceFirstChar { it.uppercase() },
                    time = extractedTime,
                    date = "Today",
                    priority = "HIGH",
                    category = "WORK"
                )
                val id = dao.insertScheduleItem(newItem)
                actions.add(AgentAction.ScheduleCreated(newItem.copy(id = id)))
                systemControl.vibrateJarvisConfirmation()
                reply = "Consider it done, sir. I have scheduled '$taskTitle' for $extractedTime."
            }

            // Greetings & Identity
            lower.contains("hello") || lower.contains("hey jarvis") || lower.contains("hi") -> {
                reply = "At your service, sir. How may I assist your operations today?"
            }
            lower.contains("who are you") || lower.contains("what are you") -> {
                reply = "I am J.A.R.V.I.S., Just A Rather Very Intelligent System. Built to oversee daily scheduling, manage system parameters, and safeguard your technological operations."
            }
            lower.contains("thank") -> {
                reply = "Always an honor to serve, sir."
            }

            else -> {
                reply = "Understood, sir. Analyzing your request regarding '$query'. Voice recognition and operational agenda have registered your transmission."
            }
        }

        return JarvisAgentResult(replyText = reply, actions = actions)
    }

    private fun cleanActionTags(text: String): String {
        return text
            .replace(Regex("\\[ACTION:[^\\]]+\\]"), "")
            .trim()
    }
}
