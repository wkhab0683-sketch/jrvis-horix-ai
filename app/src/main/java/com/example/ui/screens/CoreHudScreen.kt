package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.JarvisLog
import com.example.speech.SpeechRecognitionState
import com.example.ui.JarvisViewModel
import com.example.ui.components.ArcReactorCanvas
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.theme.JarvisBackgroundDark
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceVariantDark
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CoreHudScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val speechManager = viewModel.speechManager

    val isListening by speechManager.isListening.collectAsState()
    val isSpeaking by speechManager.isSpeaking.collectAsState()
    val soundRms by speechManager.soundLevelRms.collectAsState()
    val recognizedTranscript by speechManager.recognizedTranscript.collectAsState()
    val recognitionState by speechManager.recognitionState.collectAsState()
    val lastSpeechError by speechManager.lastSpeechError.collectAsState()
    val isOnDevice by speechManager.isOnDeviceRecognition.collectAsState()
    val isServiceAvailable by speechManager.isServiceAvailable.collectAsState()

    val isProcessing by viewModel.isProcessing.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val lastAction by viewModel.lastActionExecuted.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.startVoiceListening()
        }
    }

    // Fallback system SpeechRecognizer Intent launcher
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull()?.trim() ?: ""
            if (text.isNotEmpty()) {
                speechManager.handleExternalVoiceResult(text)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackgroundDark)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // System Status Header Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(JarvisSurfaceDark)
                .border(1.dp, JarvisCardBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isListening -> JarvisCyan
                                    isSpeaking -> JarvisGold
                                    isProcessing -> JarvisGreen
                                    recognitionState == SpeechRecognitionState.ERROR -> JarvisRed
                                    else -> JarvisCyan.copy(alpha = 0.5f)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "SPEECH RECOGNIZER ENGAGED // LISTENING"
                        else if (isSpeaking) "VOCAL SYNTHESIZER BROADCASTING"
                        else if (isProcessing) "ANALYZING DIRECTIVE TRANSMISSION"
                        else statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisCyan
                    )
                }

                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = JarvisCyan,
                        strokeWidth = 2.dp
                    )
                } else if (isOnDevice) {
                    Text(
                        text = "ON-DEVICE STT",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisGreen,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Arc Reactor Core Visualization with Tap to Listen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            ArcReactorCanvas(
                isListening = isListening,
                isSpeaking = isSpeaking,
                isProcessing = isProcessing,
                soundRms = soundRms,
                onClick = {
                    if (isListening) {
                        viewModel.stopVoiceListening()
                    } else if (isSpeaking) {
                        viewModel.stopSpeaking()
                    } else {
                        if (hasAudioPermission) {
                            viewModel.startVoiceListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            )
        }

        // Real-Time Audio Frequency Waveform Visualizer
        AudioWaveformVisualizer(
            isListening = isListening,
            isSpeaking = isSpeaking,
            soundRms = soundRms,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )

        // Real-Time Live Transcript Preview Card
        AnimatedVisibility(visible = isListening || recognizedTranscript.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1B2E))
                    .border(
                        1.dp,
                        if (isListening) JarvisCyan else JarvisCardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isListening) JarvisCyan else JarvisGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isListening) "SPEECH-TO-TEXT STREAMING..." else "RECOGNIZED VOICE INPUT:",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isListening) JarvisCyan else JarvisTextSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = if (recognizedTranscript.isNotBlank()) "\"$recognizedTranscript\""
                            else "Speak directive clearly into microphone, sir...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Speech Recognizer Error / Diagnostic Banner
        lastSpeechError?.let { errorMsg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E1313))
                    .border(1.dp, JarvisRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = JarvisRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF8A80),
                            fontSize = 10.sp
                        )
                    }

                    // Retry & System Dialog buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "RETRY",
                            color = JarvisCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(JarvisSurfaceDark)
                                .clickable { viewModel.startVoiceListening() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "SYS MIC",
                            color = JarvisGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(JarvisSurfaceDark)
                                .clickable {
                                    try {
                                        systemSpeechLauncher.launch(speechManager.createSystemRecognizerIntent())
                                    } catch (_: Exception) {}
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Action Confirmation Badge
        lastAction?.let { action ->
            Box(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF00382B))
                    .border(1.dp, JarvisGreen, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "EXECUTED: $action",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisGreen
                )
            }
        }

        // Primary Voice Interaction Control Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main SpeechRecognizer trigger button
            Button(
                onClick = {
                    if (isListening) {
                        viewModel.stopVoiceListening()
                    } else {
                        if (hasAudioPermission) {
                            viewModel.startVoiceListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("voice_listen_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) JarvisRed else JarvisCyan
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isListening) "STOP LISTENING" else "ENGAGE VOICE",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // System Speech Recognizer fallback button (Intent-based)
            OutlinedButton(
                onClick = {
                    try {
                        systemSpeechLauncher.launch(speechManager.createSystemRecognizerIntent())
                    } catch (_: Exception) {}
                },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("system_speech_intent_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "System Speech Dialog",
                    tint = JarvisCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isSpeaking) {
                Button(
                    onClick = { viewModel.stopSpeaking() },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("stop_speaking_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisGold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Silence Voice",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SILENCE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Command Voice Presets (Tap to Execute / Speak Directive)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickDirectives = listOf(
                "Morning Briefing",
                "Deep Focus Mode",
                "Toggle Flashlight",
                "Diagnostics Scan",
                "Volume 100%",
                "Schedule 3 PM Meeting",
                "Mute Audio"
            )
            quickDirectives.forEach { directive ->
                SuggestionChip(
                    onClick = { viewModel.handleUserCommand("Jarvis, $directive") },
                    label = { Text(directive, fontSize = 11.sp, color = JarvisCyan) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = JarvisSurfaceDark
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = JarvisCardBorder
                    ),
                    modifier = Modifier.testTag("quick_directive_${directive.replace(" ", "_")}")
                )
            }
        }

        // Text Directive Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("directive_input_field"),
                placeholder = {
                    Text(
                        "Type or speak directive for Jarvis...",
                        color = JarvisTextSecondary,
                        fontSize = 13.sp
                    )
                },
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisCardBorder,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary,
                    cursorColor = JarvisCyan
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.handleUserCommand(textInput)
                            textInput = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.handleUserCommand(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCyan)
                    .testTag("send_directive_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Directive",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Terminal Log Header
        Text(
            text = "// TRANSMISSION TERMINAL LOG",
            style = MaterialTheme.typography.labelSmall,
            color = JarvisTextSecondary,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(vertical = 2.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(JarvisSurfaceDark)
                .border(1.dp, JarvisCardBorder, RoundedCornerShape(8.dp))
                .padding(8.dp),
            reverseLayout = false,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (logs.isEmpty()) {
                item {
                    Text(
                        text = "System standing by for telemetry input, sir...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisTextSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                items(logs) { log ->
                    TerminalLogCard(log)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
fun TerminalLogCard(log: JarvisLog) {
    val isUser = log.sender == "USER"
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { timeFormat.format(Date(log.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_card_${log.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) Color(0xFF131D2D) else JarvisSurfaceVariantDark
        ),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isUser) JarvisCardBorder else JarvisCyan.copy(alpha = 0.3f)
            )
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUser) "OPERATOR // TRANSMISSION" else "J.A.R.V.I.S. // RESPONSE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) JarvisGold else JarvisCyan,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisTextPrimary,
                fontFamily = if (isUser) FontFamily.Default else FontFamily.Monospace
            )

            log.actionTag?.let { tag ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ACTION: $tag",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisGreen,
                    fontSize = 9.sp
                )
            }
        }
    }
}
