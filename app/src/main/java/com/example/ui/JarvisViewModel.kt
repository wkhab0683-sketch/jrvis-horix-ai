package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AgentAction
import com.example.ai.JarvisAgent
import com.example.data.ai.ChatPersona
import com.example.data.ai.ChatbotModel
import com.example.data.ai.GenerativeMediaResult
import com.example.data.ai.GroundedResponse
import com.example.data.ai.GroundingMode
import com.example.data.ai.JarvisAiHub
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthState
import com.example.data.firebase.AiCreationRecord
import com.example.data.firebase.FirestoreChatRecord
import com.example.data.firebase.FirestoreManager
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
    STUDIO,
    CHATBOT,
    SYSTEM,
    SYNC
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = JarvisDatabase.getDatabase(application, viewModelScope)
    private val dao = database.jarvisDao()
    val systemControl = SystemControlManager(application)
    private val agent = JarvisAgent(application, dao, systemControl)
    val authManager = AuthManager(application)
    val firestoreManager = FirestoreManager(application)
    val aiHub = JarvisAiHub(application)

    lateinit var speechManager: JarvisSpeechManager
        private set

    val authState: StateFlow<AuthState> = authManager.authState

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

    // Multi-Turn Chatbot State
    private val _chatMessages = MutableStateFlow<List<FirestoreChatRecord>>(emptyList())
    val chatMessages: StateFlow<List<FirestoreChatRecord>> = _chatMessages.asStateFlow()

    private val _currentChatbotModel = MutableStateFlow(ChatbotModel.BALANCED)
    val currentChatbotModel: StateFlow<ChatbotModel> = _currentChatbotModel.asStateFlow()

    private val _currentGroundingMode = MutableStateFlow(GroundingMode.NONE)
    val currentGroundingMode: StateFlow<GroundingMode> = _currentGroundingMode.asStateFlow()

    private val _currentPersona = MutableStateFlow(ChatPersona.JARVIS_CORE)
    val currentPersona: StateFlow<ChatPersona> = _currentPersona.asStateFlow()

    // Generative Media Studio State
    private val _creations = MutableStateFlow<List<AiCreationRecord>>(emptyList())
    val creations: StateFlow<List<AiCreationRecord>> = _creations.asStateFlow()

    private val _latestGeneration = MutableStateFlow<GenerativeMediaResult?>(null)
    val latestGeneration: StateFlow<GenerativeMediaResult?> = _latestGeneration.asStateFlow()

    private val _transcriptionResult = MutableStateFlow<String?>(null)
    val transcriptionResult: StateFlow<String?> = _transcriptionResult.asStateFlow()

    init {
        speechManager = JarvisSpeechManager(application) { recognized ->
            handleUserCommand(recognized)
        }

        // Observe Auth & sync user profile
        viewModelScope.launch {
            authManager.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    firestoreManager.syncUserProfile(state.user)
                    launch {
                        firestoreManager.observeUserCreations(state.user.uid).collect { list ->
                            _creations.value = list
                        }
                    }
                    launch {
                        firestoreManager.observeUserChatMessages(state.user.uid).collect { list ->
                            if (list.isNotEmpty()) {
                                _chatMessages.value = list
                            }
                        }
                    }
                }
            }
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

    fun signInWithGoogle() {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle()
            if (result.isSuccess) {
                systemControl.vibrateJarvisConfirmation()
                speechManager.speak("Identity recognized. Welcome, ${result.getOrNull()?.displayName ?: "sir"}. Neural link synchronized with Cloud Firestore.")
            } else {
                speechManager.speak("Authorization denied or canceled.")
            }
        }
    }

    fun signOut() {
        authManager.signOut()
        speechManager.speak("Signed out of secure session, sir.")
    }

    fun handleUserCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "ANALYZING TRANSMISSION..."
            try {
                // Try live conversational model or standard agent
                val result = agent.processQuery(command)
                _statusMessage.value = "DIRECTIVE EXECUTED // RESPONSE GENERATED"
                speechManager.speak(result.replyText)

                val actionDesc = result.actions.firstOrNull()?.let { action ->
                    when (action) {
                        is AgentAction.ScheduleCreated -> {
                            // Sync to Cloud Firestore if logged in
                            authManager.currentUserId?.let { uid ->
                                firestoreManager.saveScheduleToCloud(uid, action.item)
                            }
                            "SCHEDULE: ${action.item.title}"
                        }
                        is AgentAction.FlashlightToggled -> "ILLUMINATION: ${if (action.enabled) "ACTIVE" else "OFF"}"
                        is AgentAction.VolumeAdjusted -> "AUDIO: ${action.percent}%"
                        is AgentAction.ProtocolTriggered -> "PROTOCOL: ${action.title}"
                        is AgentAction.DiagnosticsReported -> "DIAGNOSTICS REFRESHED"
                        else -> null
                    }
                }
                _lastActionExecuted.value = actionDesc
            } catch (e: Exception) {
                val errorMsg = "Apologies, sir. Communications array encountered an anomaly: ${e.message}"
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
            val id = dao.insertScheduleItem(item)
            val savedItem = item.copy(id = id)

            // Save to Cloud Firestore
            authManager.currentUserId?.let { uid ->
                firestoreManager.saveScheduleToCloud(uid, savedItem)
            }

            systemControl.vibrateJarvisConfirmation()
            speechManager.speak("Directive recorded, sir. Added ${savedItem.title} to your daily agenda.")
        }
    }

    fun toggleScheduleCompleted(item: ScheduleItem) {
        viewModelScope.launch {
            val updatedState = !item.isCompleted
            dao.setScheduleItemCompleted(item.id, updatedState)
            authManager.currentUserId?.let { uid ->
                firestoreManager.saveScheduleToCloud(uid, item.copy(isCompleted = updatedState))
            }
            systemControl.vibratePulse()
            if (updatedState) {
                speechManager.speak("Objective completed: ${item.title}. Splendid execution, sir.")
            }
        }
    }

    fun deleteScheduleItem(item: ScheduleItem) {
        viewModelScope.launch {
            dao.deleteScheduleItem(item)
            authManager.currentUserId?.let { uid ->
                firestoreManager.deleteScheduleFromCloud(uid, item.id.toString())
            }
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

    // Multi-Turn Chatbot
    fun setChatbotModel(model: ChatbotModel) {
        _currentChatbotModel.value = model
    }

    fun setGroundingMode(mode: GroundingMode) {
        _currentGroundingMode.value = mode
    }

    fun setPersona(persona: ChatPersona) {
        _currentPersona.value = persona
    }

    fun sendChatbotMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isProcessing.value = true
            val userMsg = FirestoreChatRecord(
                userId = authManager.currentUserId ?: "local",
                role = "user",
                text = text,
                modelUsed = _currentChatbotModel.value.modelId,
                groundingType = _currentGroundingMode.value.name,
                timestamp = System.currentTimeMillis()
            )
            _chatMessages.value = _chatMessages.value + userMsg

            authManager.currentUserId?.let { uid ->
                firestoreManager.saveChatMessage(uid, userMsg)
            }

            // Build history pairs
            val history = _chatMessages.value.dropLast(1).map { it.role to it.text }

            val response: GroundedResponse = aiHub.sendChatMessage(
                history = history,
                userMessage = text,
                model = _currentChatbotModel.value,
                grounding = _currentGroundingMode.value,
                persona = _currentPersona.value
            )

            val fullReply = if (response.searchSources.isNotEmpty()) {
                "${response.replyText}\n\n🔍 Grounded Sources:\n" + response.searchSources.joinToString("\n") { "• $it" }
            } else {
                response.replyText
            }

            val modelMsg = FirestoreChatRecord(
                userId = authManager.currentUserId ?: "local",
                role = "model",
                text = fullReply,
                modelUsed = _currentChatbotModel.value.modelId,
                groundingType = _currentGroundingMode.value.name,
                timestamp = System.currentTimeMillis()
            )
            _chatMessages.value = _chatMessages.value + modelMsg

            authManager.currentUserId?.let { uid ->
                firestoreManager.saveChatMessage(uid, modelMsg)
            }

            speechManager.speak(response.replyText)
            _isProcessing.value = false
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = emptyList()
        speechManager.speak("Chat telemetry buffers flushed, sir.")
    }

    // Generative Media Studio
    fun generateMusic(prompt: String, isProTrack: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "SYNTHESIZING ACOUSTIC HARMONICS..."
            val result = aiHub.generateMusic(prompt, isProTrack)
            _latestGeneration.value = result
            if (result.success && result.mediaData != null) {
                val record = AiCreationRecord(
                    userId = authManager.currentUserId ?: "local",
                    type = "MUSIC",
                    model = if (isProTrack) "lyria-3-pro-preview" else "lyria-3-clip-preview",
                    prompt = prompt,
                    outputData = result.mediaData,
                    status = "COMPLETED"
                )
                _creations.value = listOf(record) + _creations.value
                authManager.currentUserId?.let { uid ->
                    firestoreManager.saveAiCreation(uid, record)
                }
                systemControl.vibrateJarvisConfirmation()
                speechManager.speak("Acoustic composition synthesis complete, sir.")
            } else {
                speechManager.speak("Music synthesis could not be completed: ${result.message}")
            }
            _isProcessing.value = false
            _statusMessage.value = "CORE SYSTEMS NOMINAL // STANDBY"
        }
    }

    fun generateOrEditImage(prompt: String, sourceBitmap: Bitmap?, aspectRatio: String = "1:1") {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "RENDERING HOLOGRAPHIC VISUAL ARRAY..."
            val result = aiHub.createOrEditImage(prompt, sourceBitmap, aspectRatio)
            _latestGeneration.value = result
            if (result.success && result.mediaData != null) {
                val record = AiCreationRecord(
                    userId = authManager.currentUserId ?: "local",
                    type = "IMAGE",
                    model = "gemini-3.1-flash-image-preview",
                    prompt = prompt,
                    outputData = result.mediaData,
                    status = "COMPLETED"
                )
                _creations.value = listOf(record) + _creations.value
                authManager.currentUserId?.let { uid ->
                    firestoreManager.saveAiCreation(uid, record)
                }
                systemControl.vibrateJarvisConfirmation()
                speechManager.speak("Visual rendering generated via Gemini 3.1 Flash Image, sir.")
            } else {
                speechManager.speak("Image rendering failed: ${result.message}")
            }
            _isProcessing.value = false
            _statusMessage.value = "CORE SYSTEMS NOMINAL // STANDBY"
        }
    }

    fun generateVideo(prompt: String, sourceBitmap: Bitmap?, aspectRatio: String = "16:9") {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "INITIALIZING VEO 3 VIDEO GENERATION MATRIX..."
            val result = aiHub.generateVideo(prompt, sourceBitmap, aspectRatio)
            _latestGeneration.value = result
            if (result.success) {
                val record = AiCreationRecord(
                    userId = authManager.currentUserId ?: "local",
                    type = "VIDEO",
                    model = "veo-3.1-fast-generate-preview",
                    prompt = prompt,
                    outputData = result.mediaData ?: "Job: ${System.currentTimeMillis()}",
                    status = "INITIALIZED"
                )
                _creations.value = listOf(record) + _creations.value
                authManager.currentUserId?.let { uid ->
                    firestoreManager.saveAiCreation(uid, record)
                }
                systemControl.vibrateJarvisConfirmation()
                speechManager.speak("Veo 3 video pipeline engaged with ${aspectRatio} aspect ratio, sir.")
            } else {
                speechManager.speak("Veo video generation failed: ${result.message}")
            }
            _isProcessing.value = false
            _statusMessage.value = "CORE SYSTEMS NOMINAL // STANDBY"
        }
    }

    fun transcribeAudioData(audioBase64: String, mimeType: String = "audio/wav") {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "TRANSCRIBING ACOUSTIC TELEMETRY..."
            val text = aiHub.transcribeAudio(audioBase64, mimeType)
            _transcriptionResult.value = text
            val record = AiCreationRecord(
                userId = authManager.currentUserId ?: "local",
                type = "TRANSCRIPTION",
                model = "gemini-3.5-transcribe",
                prompt = "Microphone Audio Transmission",
                outputData = text,
                status = "COMPLETED"
            )
            _creations.value = listOf(record) + _creations.value
            authManager.currentUserId?.let { uid ->
                firestoreManager.saveAiCreation(uid, record)
            }
            speechManager.speak("Transcription complete: $text")
            _isProcessing.value = false
            _statusMessage.value = "CORE SYSTEMS NOMINAL // STANDBY"
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
                "MORNING_BRIEF" -> handleUserCommand("Jarvis, execute morning briefing protocol")
                "DEEP_FOCUS" -> handleUserCommand("Jarvis, activate deep focus mode")
                "NIGHT_WATCH" -> handleUserCommand("Jarvis, initiate night watch protocol")
                "CLEAN_SLATE" -> handleUserCommand("Jarvis, protocol clean slate")
                else -> handleUserCommand("Jarvis, trigger ${protocol.title}")
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
        aiHub.setCustomApiKey(key)
        systemControl.vibrateJarvisConfirmation()
        speechManager.speak("Neural uplink reconfigured with custom credentials, sir.")
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
