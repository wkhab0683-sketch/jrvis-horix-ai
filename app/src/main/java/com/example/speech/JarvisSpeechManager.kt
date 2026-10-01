package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SpeechRecognitionState {
    IDLE,
    INITIALIZING,
    READY,
    LISTENING,
    PROCESSING,
    ERROR
}

class JarvisSpeechManager(
    private val context: Context,
    private val onVoiceResult: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _recognitionState = MutableStateFlow(SpeechRecognitionState.IDLE)
    val recognitionState: StateFlow<SpeechRecognitionState> = _recognitionState.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _soundLevelRms = MutableStateFlow(0f)
    val soundLevelRms: StateFlow<Float> = _soundLevelRms.asStateFlow()

    private val _recognizedTranscript = MutableStateFlow("")
    val recognizedTranscript: StateFlow<String> = _recognizedTranscript.asStateFlow()

    private val _voiceEnabled = MutableStateFlow(true)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()

    private val _lastSpeechError = MutableStateFlow<String?>(null)
    val lastSpeechError: StateFlow<String?> = _lastSpeechError.asStateFlow()

    private val _isServiceAvailable = MutableStateFlow(true)
    val isServiceAvailable: StateFlow<Boolean> = _isServiceAvailable.asStateFlow()

    private val _isOnDeviceRecognition = MutableStateFlow(false)
    val isOnDeviceRecognition: StateFlow<Boolean> = _isOnDeviceRecognition.asStateFlow()

    init {
        initTts()
        mainHandler.post {
            initSpeechRecognizer()
        }
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.UK)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.US)
            }
            textToSpeech?.setPitch(0.92f)
            textToSpeech?.setSpeechRate(1.05f)
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
            isTtsReady = true
        }
    }

    private fun initSpeechRecognizer() {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        _isServiceAvailable.value = available

        if (!available) {
            _lastSpeechError.value = "Speech recognition service not detected on this system image. Fallback intents and text directives remain active."
            return
        }

        try {
            speechRecognizer?.destroy()

            // On Android 12+, prefer on-device recognizer for low latency if supported
            val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                _isOnDeviceRecognition.value = true
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                _isOnDeviceRecognition.value = false
                SpeechRecognizer.createSpeechRecognizer(context)
            }

            recognizer.setRecognitionListener(createRecognitionListener())
            speechRecognizer = recognizer
        } catch (e: Exception) {
            _isServiceAvailable.value = false
            _lastSpeechError.value = "Failed to initialize SpeechRecognizer: ${e.message}"
            speechRecognizer = null
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _recognitionState.value = SpeechRecognitionState.READY
                _isListening.value = true
                _lastSpeechError.value = null
            }

            override fun onBeginningOfSpeech() {
                _recognitionState.value = SpeechRecognitionState.LISTENING
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB typically ranges from -2 to 10+, normalize to 0..15 range
                val normalized = (rmsdB + 2f).coerceIn(0f, 15f)
                _soundLevelRms.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _recognitionState.value = SpeechRecognitionState.PROCESSING
                _isListening.value = false
                _soundLevelRms.value = 0f
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _soundLevelRms.value = 0f
                _recognitionState.value = SpeechRecognitionState.ERROR

                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                    SpeechRecognizer.ERROR_CLIENT -> "Client speech recognition error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "RECORD_AUDIO permission required."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error during speech processing."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout during recognition."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly, sir."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Reinitializing..."
                    SpeechRecognizer.ERROR_SERVER -> "Recognition server error."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No vocal directive detected."
                    else -> "Speech error code $error."
                }
                _lastSpeechError.value = errorMessage

                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                    // Reinitialize to clear stuck client states
                    mainHandler.postDelayed({ initSpeechRecognizer() }, 500)
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _soundLevelRms.value = 0f
                _recognitionState.value = SpeechRecognitionState.IDLE

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull()?.trim() ?: ""

                if (recognized.isNotEmpty()) {
                    _recognizedTranscript.value = recognized
                    _lastSpeechError.value = null
                    onVoiceResult(recognized)
                } else {
                    _lastSpeechError.value = "No vocal text extracted, sir."
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim() ?: ""
                if (partial.isNotEmpty()) {
                    _recognizedTranscript.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        stopSpeaking()
        _lastSpeechError.value = null

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initSpeechRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command: e.g. 'Schedule meeting at 3 PM', 'Turn on flashlight', 'Morning briefing'")
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                _isListening.value = true
                _recognitionState.value = SpeechRecognitionState.INITIALIZING
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _isListening.value = false
                _recognitionState.value = SpeechRecognitionState.ERROR
                _lastSpeechError.value = "Unable to start speech recognizer: ${e.message}"
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            _isListening.value = false
            _soundLevelRms.value = 0f
            _recognitionState.value = SpeechRecognitionState.IDLE
        }
    }

    fun cancelListening() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            _isListening.value = false
            _soundLevelRms.value = 0f
            _recognitionState.value = SpeechRecognitionState.IDLE
        }
    }

    fun createSystemRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Issue voice directive for JARVIS...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }

    fun handleExternalVoiceResult(transcript: String) {
        val clean = transcript.trim()
        if (clean.isNotEmpty()) {
            _recognizedTranscript.value = clean
            _lastSpeechError.value = null
            onVoiceResult(clean)
        }
    }

    fun speak(text: String) {
        if (!_voiceEnabled.value || !isTtsReady || text.isBlank()) return
        stopSpeaking()
        val cleanSpeech = text
            .replace(Regex("\\[ACTION:[^\\]]+\\]"), "")
            .replace(Regex("[*#_`~]"), "")
            .trim()

        if (cleanSpeech.isNotEmpty()) {
            val utteranceId = "JARVIS_${System.currentTimeMillis()}"
            textToSpeech?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun toggleVoiceFeedback() {
        val newState = !_voiceEnabled.value
        _voiceEnabled.value = newState
        if (!newState) {
            stopSpeaking()
        }
    }

    fun destroy() {
        mainHandler.post {
            stopListening()
            stopSpeaking()
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            textToSpeech?.shutdown()
            textToSpeech = null
        }
    }
}
