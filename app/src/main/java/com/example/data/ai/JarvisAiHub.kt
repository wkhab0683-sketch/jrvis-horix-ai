package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

enum class ChatbotModel(val modelId: String, val displayName: String, val description: String) {
    BALANCED("gemini-3.5-flash", "Gemini 3.5 Flash", "General tasks & high efficiency"),
    COMPLEX("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Deep reasoning, analysis & complex tasks"),
    SPEED("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash-Lite", "Ultra-fast low-latency responses")
}

enum class GroundingMode {
    NONE,
    GOOGLE_SEARCH,
    GOOGLE_MAPS
}

enum class ChatPersona(val title: String, val instruction: String) {
    JARVIS_CORE("J.A.R.V.I.S. Core", "You are J.A.R.V.I.S., the hyper-intelligent Stark Industries AI. Polite, dignified British cadence, addressing the user as Sir or Ma'am. Highly capable, efficient, and precise."),
    STARK_ENGINEER("Lead Engineer", "You are the Stark Industries Chief Systems Engineer AI. Highly technical, brilliant with physics, software architecture, robotics, and hardware troubleshooting."),
    TACTICAL_DEFENSE("Tactical Defense", "You are the Stark Perimeter Defense & Strategic Security AI. Direct, vigilant, providing risk assessments, threat analyses, and defense protocol optimizations."),
    EXECUTIVE_BUTLER("Executive Butler", "You are the classic Stark personal butler. Impeccably courteous, discreet, managing logistics, itineraries, schedules, and daily affairs.")
}

data class GroundedResponse(
    val replyText: String,
    val searchSources: List<String> = emptyList(),
    val groundingChunks: List<String> = emptyList()
)

data class GenerativeMediaResult(
    val success: Boolean,
    val mediaData: String? = null, // base64 or URL
    val mimeType: String = "",
    val message: String = ""
)

class JarvisAiHub(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var customApiKey: String? = null

    fun setCustomApiKey(key: String) {
        customApiKey = key.trim()
    }

    private fun getEffectiveApiKey(): String {
        val custom = customApiKey
        if (!custom.isNullOrBlank()) return custom
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    // 1. Multi-Turn Chatbot with Model Selection & Search/Maps Grounding
    suspend fun sendChatMessage(
        history: List<Pair<String, String>>, // role to text
        userMessage: String,
        model: ChatbotModel = ChatbotModel.BALANCED,
        grounding: GroundingMode = GroundingMode.NONE,
        persona: ChatPersona = ChatPersona.JARVIS_CORE
    ): GroundedResponse = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext GroundedResponse(
                replyText = "Apologies, sir. The Gemini Neural Uplink requires an API key in the AI Studio Secrets panel or local settings."
            )
        }

        val jsonBody = JSONObject()

        // System Instruction
        jsonBody.put("systemInstruction", JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", persona.instruction) })
            })
        })

        // Conversation History
        val contentsArray = JSONArray()
        history.forEach { (role, msg) ->
            contentsArray.put(JSONObject().apply {
                put("role", if (role == "user") "user" else "model")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", msg) })
                })
            })
        }
        // Current user prompt
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", userMessage) })
            })
        })
        jsonBody.put("contents", contentsArray)

        // Tools for Grounding (Google Search or Google Maps)
        if (grounding == GroundingMode.GOOGLE_SEARCH) {
            jsonBody.put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("googleSearch", JSONObject())
                })
            })
        } else if (grounding == GroundingMode.GOOGLE_MAPS) {
            jsonBody.put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("googleMaps", JSONObject())
                })
            })
        }

        jsonBody.put("generationConfig", JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 1200)
        })

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.modelId}:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GroundedResponse(
                    replyText = "Neural connection anomaly (HTTP ${response.code}): $respBody"
                )
            }

            val json = JSONObject(respBody)
            val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
            val contentParts = candidate?.optJSONObject("content")?.optJSONArray("parts")
            val text = contentParts?.optJSONObject(0)?.optString("text") ?: "No textual transmission received."

            // Extract grounding metadata if present
            val sources = mutableListOf<String>()
            val groundingMetadata = candidate?.optJSONObject("groundingMetadata")
            if (groundingMetadata != null) {
                val searchQueries = groundingMetadata.optJSONArray("webSearchQueries")
                if (searchQueries != null) {
                    for (i in 0 until searchQueries.length()) {
                        sources.add(searchQueries.optString(i))
                    }
                }
                val groundingChunks = groundingMetadata.optJSONArray("groundingChunks")
                if (groundingChunks != null) {
                    for (i in 0 until groundingChunks.length()) {
                        val web = groundingChunks.optJSONObject(i)?.optJSONObject("web")
                        val uri = web?.optString("uri")
                        val title = web?.optString("title")
                        if (!uri.isNullOrBlank()) {
                            sources.add("${title ?: "Source"}: $uri")
                        }
                    }
                }
            }

            GroundedResponse(replyText = text, searchSources = sources)
        } catch (e: Exception) {
            GroundedResponse(replyText = "Apologies, sir. Communications array encountered an anomaly: ${e.message}")
        }
    }

    // 2. Music Generation (lyria-3-clip-preview & lyria-3-pro-preview)
    suspend fun generateMusic(
        prompt: String,
        isProFullTrack: Boolean = false
    ): GenerativeMediaResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext GenerativeMediaResult(false, message = "Gemini API key is required.")
        }

        val model = if (isProFullTrack) "lyria-3-pro-preview" else "lyria-3-clip-preview"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply {
                    put("AUDIO")
                })
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GenerativeMediaResult(false, message = "Music synthesis error: HTTP ${response.code}")
            }

            val json = JSONObject(respBody)
            val parts = json.optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts")

            var audioData: String? = null
            var mime = "audio/mp3"

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.optJSONObject(i)
                    val inline = p?.optJSONObject("inlineData")
                    if (inline != null) {
                        audioData = inline.optString("data")
                        mime = inline.optString("mimeType", "audio/mp3")
                        break
                    }
                }
            }

            if (audioData != null) {
                GenerativeMediaResult(true, mediaData = audioData, mimeType = mime, message = "Music synthesis complete via $model")
            } else {
                GenerativeMediaResult(false, message = "No audio stream returned by $model.")
            }
        } catch (e: Exception) {
            GenerativeMediaResult(false, message = "Music synthesis exception: ${e.message}")
        }
    }

    // 3. Create & Edit Images (gemini-3.1-flash-image-preview)
    suspend fun createOrEditImage(
        prompt: String,
        sourceBitmap: Bitmap? = null,
        aspectRatio: String = "1:1"
    ): GenerativeMediaResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext GenerativeMediaResult(false, message = "Gemini API key is required.")
        }

        val model = "gemini-3.1-flash-image-preview"

        val partsArray = JSONArray()
        partsArray.put(JSONObject().apply { put("text", prompt) })

        if (sourceBitmap != null) {
            val stream = ByteArrayOutputStream()
            sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            partsArray.put(JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                })
            })
        }

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", partsArray)
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply {
                    put("TEXT")
                    put("IMAGE")
                })
                put("imageConfig", JSONObject().apply {
                    put("aspectRatio", aspectRatio)
                    put("imageSize", "1K")
                })
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GenerativeMediaResult(false, message = "Image generation error: HTTP ${response.code}")
            }

            val json = JSONObject(respBody)
            val parts = json.optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts")

            var imgData: String? = null
            var mime = "image/png"

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.optJSONObject(i)
                    val inline = p?.optJSONObject("inlineData")
                    if (inline != null) {
                        imgData = inline.optString("data")
                        mime = inline.optString("mimeType", "image/png")
                        break
                    }
                }
            }

            if (imgData != null) {
                GenerativeMediaResult(true, mediaData = imgData, mimeType = mime, message = "Image rendered successfully.")
            } else {
                GenerativeMediaResult(false, message = "Model responded without image payload.")
            }
        } catch (e: Exception) {
            GenerativeMediaResult(false, message = "Image generation exception: ${e.message}")
        }
    }

    // 4. Video Generation from Text or Image (veo-3.1-fast-generate-preview)
    suspend fun generateVideo(
        prompt: String,
        sourceBitmap: Bitmap? = null,
        aspectRatio: String = "16:9" // "16:9" or "9:16"
    ): GenerativeMediaResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext GenerativeMediaResult(false, message = "Gemini API key is required.")
        }

        val model = "veo-3.1-fast-generate-preview"
        val jsonBody = JSONObject().apply {
            put("prompt", prompt)
            put("config", JSONObject().apply {
                put("aspectRatio", aspectRatio)
                put("resolution", "1080p")
                put("numberOfVideos", 1)
            })

            if (sourceBitmap != null) {
                val stream = ByteArrayOutputStream()
                sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                put("image", JSONObject().apply {
                    put("imageBytes", base64Image)
                })
            }
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateVideos?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GenerativeMediaResult(false, message = "Veo 3 error: HTTP ${response.code}")
            }

            val json = JSONObject(respBody)
            val operationName = json.optString("name", "Veo Video Generation Job Queued")
            GenerativeMediaResult(true, mediaData = operationName, mimeType = "video/mp4", message = "Veo video generation initialized: $operationName")
        } catch (e: Exception) {
            GenerativeMediaResult(false, message = "Video generation exception: ${e.message}")
        }
    }

    // 5. Audio Transcription (gemini-3.5-transcribe)
    suspend fun transcribeAudio(
        audioBase64: String,
        mimeType: String = "audio/wav"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext "API key is required for transcription."
        }

        val model = "gemini-3.5-transcribe"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Please provide a complete and accurate transcription of this audio transmission.")
                        })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", mimeType)
                                put("data", audioBase64)
                            })
                        })
                    })
                })
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext "Transcription error: HTTP ${response.code}"
            }

            val json = JSONObject(respBody)
            val parts = json.optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts")
            parts?.optJSONObject(0)?.optString("text") ?: "No transcription text returned."
        } catch (e: Exception) {
            "Transcription anomaly: ${e.message}"
        }
    }

    // 6. Voice Conversations Live API (gemini-3.8-live)
    suspend fun liveVoiceChat(
        userTranscript: String,
        conversationContext: String = ""
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext "Gemini API key is required."
        }

        val model = "gemini-3.8-live"

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are J.A.R.V.I.S. interacting via real-time live vocal dialogue. Keep replies concise, natural, and conversational for high-bandwidth verbal speech.")
                    })
                })
            })
            put("contents", JSONArray().apply {
                if (conversationContext.isNotBlank()) {
                    put(JSONObject().apply {
                        put("role", "model")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", conversationContext) })
                        })
                    })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userTranscript) })
                    })
                })
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext "Live voice stream error: HTTP ${response.code}"
            }

            val json = JSONObject(respBody)
            val text = json.optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")

            text ?: "Live audio stream received."
        } catch (e: Exception) {
            "Live voice anomaly: ${e.message}"
        }
    }
}
