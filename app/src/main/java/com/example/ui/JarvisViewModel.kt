package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AgentAction
import com.example.ai.JarvisAgent
import com.example.data.local.JarvisDatabase
import com.example.data.local.JarvisLog
import com.example.data.local.JarvisProtocol
import com.example.data.local.ScheduleItem
import com.example.speech.JarvisSpeechManager
import com.example.system.DeviceTelemetry
import com.example.system.SystemControlManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class JarvisTab {
    CORE,
    AGENDA,
    SYSTEM,
    SYNC
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = JarvisDatabase.getDatabase(application, viewModelScope)
    private val dao = database.jarvisDao()
    val systemControl = SystemControlManager(application)
    private val agent = JarvisAgent(application, dao, systemControl)

    lateinit var speechManager: JarvisSpeechManager
        private set

    val scheduleItems: StateFlow<List<ScheduleItem>> = dao.getAllScheduleItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<JarvisLog>> = dao.getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val protocols: StateFlow<List<JarvisProtocol>> = dao.getAllProtocols()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val telemetry: StateFlow<DeviceTelemetry> = systemControl.telemetry

    private val _selectedTab = MutableStateFlow(JarvisTab.CORE)
    val selectedTab: StateFlow<JarvisTab> = _selectedTab.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _statusMessage = MutableStateFlow("CORE SYSTEMS NOMINAL // STANDBY")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _lastActionExecuted = MutableStateFlow<String?>(null)
    val lastActionExecuted: StateFlow<String?> = _lastActionExecuted.asStateFlow()

    init {
        speechManager = JarvisSpeechManager(application) { recognized ->
            handleUserCommand(recognized)
        }
    }

    fun selectTab(tab: JarvisTab) {
        _selectedTab.value = tab
    }

    fun startVoiceListening() {
        speechManager.startListening()
        _statusMessage.value = "AUDIO SENSORS ENGAGED // LISTENING..."
    }

    fun stopVoiceListening() {
        speechManager.stopListening()
        _statusMessage.value = "CORE SYSTEMS NOMINAL // STANDBY"
    }

    fun stopSpeaking() {
        speechManager.stopSpeaking()
    }

    fun toggleVoiceFeedback() {
        speechManager.toggleVoiceFeedback()
    }

    fun handleUserCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "ANALYZING TRANSMISSION..."
            try {
                val result = agent.processQuery(command)
                _statusMessage.value = "DIRECTIVE EXECUTED // RESPONSE GENERATED"
                speechManager.speak(result.replyText)

                // Summarize action executed
                val actionDesc = result.actions.firstOrNull()?.let { action ->
                    when (action) {
                        is AgentAction.ScheduleCreated -> "SCHEDULE: ${action.item.title}"
                        is AgentAction.FlashlightToggled -> "ILLUMINATION: ${if (action.enabled) "ACTIVE" else "OFF"}"
                        is AgentAction.VolumeAdjusted -> "AUDIO: ${action.percent}%"
                        is AgentAction.ProtocolTriggered -> "PROTOCOL: ${action.title}"
                        is AgentAction.DiagnosticsReported -> "DIAGNOSTICS REFRESHED"
                        else -> null
                    }
                }
                _lastActionExecuted.value = actionDesc
            } catch (e: Exception) {
                val errorMsg = "Apologies, sir. My communications array encountered an anomaly: ${e.message}"
                speechManager.speak(errorMsg)
                _statusMessage.value = "ANOMALY DETECTED // RETRYING"
            } finally {
                _isProcessing.value = false
                systemControl.refreshTelemetry()
            }
        }
    }

    // Schedule actions
    fun addScheduleItem(title: String, time: String, date: String, priority: String, category: String) {
        viewModelScope.launch {
            val item = ScheduleItem(
                title = title.ifBlank { "Operational Mission" },
                time = time.ifBlank { "12:00 PM" },
                date = date.ifBlank { "Today" },
                priority = priority,
                category = category
            )
            dao.insertScheduleItem(item)
            systemControl.vibrateJarvisConfirmation()
            speechManager.speak("Directive recorded, sir. Added ${item.title} to your daily agenda.")
        }
    }

    fun toggleScheduleCompleted(item: ScheduleItem) {
        viewModelScope.launch {
            dao.setScheduleItemCompleted(item.id, !item.isCompleted)
            systemControl.vibratePulse()
            if (!item.isCompleted) {
                speechManager.speak("Objective completed: ${item.title}. Splendid execution, sir.")
            }
        }
    }

    fun deleteScheduleItem(item: ScheduleItem) {
        viewModelScope.launch {
            dao.deleteScheduleItem(item)
            systemControl.vibratePulse()
        }
    }

    fun clearCompletedSchedules() {
        viewModelScope.launch {
            dao.clearCompletedSchedules()
            systemControl.vibratePulse()
            speechManager.speak("Completed agenda items cleared, sir.")
        }
    }

    // Hardware control
    fun toggleFlashlight() {
        val newState = systemControl.toggleFlashlight()
        speechManager.speak(if (newState) "Illumination engaged, sir." else "Flashlight deactivated.")
    }

    fun setVolume(percent: Int) {
        systemControl.setVolumePercent(percent)
        speechManager.speak("Volume set to $percent percent, sir.")
    }

    fun refreshTelemetry() {
        systemControl.refreshTelemetry()
        systemControl.vibratePulse()
    }

    fun executeProtocol(protocol: JarvisProtocol) {
        viewModelScope.launch {
            when (protocol.id) {
                "MORNING_BRIEF" -> {
                    handleUserCommand("Jarvis, execute morning briefing protocol")
                }
                "DEEP_FOCUS" -> {
                    handleUserCommand("Jarvis, activate deep focus mode")
                }
                "NIGHT_WATCH" -> {
                    handleUserCommand("Jarvis, initiate night watch protocol")
                }
                "CLEAN_SLATE" -> {
                    handleUserCommand("Jarvis, protocol clean slate")
                }
                else -> {
                    handleUserCommand("Jarvis, trigger ${protocol.title}")
                }
            }
        }
    }

    fun optimizeScheduleWithAi() {
        viewModelScope.launch {
            _isProcessing.value = true
            val prompt = "Jarvis, review my current agenda and optimize it. What is the single most critical high-priority task I should focus on right now?"
            handleUserCommand(prompt)
        }
    }

    fun setCustomApiKey(key: String) {
        agent.setCustomApiKey(key)
        systemControl.vibrateJarvisConfirmation()
        speechManager.speak("Neural uplink reconfigured with custom credentials, sir.")
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
