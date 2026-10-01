package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.ai.ChatPersona
import com.example.data.ai.ChatbotModel
import com.example.data.ai.GroundingMode
import com.example.data.firebase.FirestoreChatRecord
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisBackgroundDark
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceVariantDark
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatbotConsoleScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.chatMessages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val currentModel by viewModel.currentChatbotModel.collectAsState()
    val currentGrounding by viewModel.currentGroundingMode.collectAsState()
    val currentPersona by viewModel.currentPersona.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackgroundDark)
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Top Model & Grounding Selectors Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GEMINI MULTI-TURN AI CHAT",
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisCyan
                )
                Text(
                    text = "GROUNDED INTELLIGENCE ARRAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSecondary
                )
            }

            IconButton(
                onClick = { viewModel.clearChatHistory() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ClearAll,
                    contentDescription = "Clear History",
                    tint = JarvisTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Model Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChatbotModel.values().forEach { model ->
                val isSelected = currentModel == model
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setChatbotModel(model) },
                    label = { Text(model.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (model == ChatbotModel.COMPLEX) JarvisGold else JarvisCyan,
                        selectedLabelColor = Color.Black,
                        containerColor = JarvisSurfaceDark,
                        labelColor = JarvisTextSecondary
                    ),
                    modifier = Modifier.testTag("model_chip_${model.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Grounding Mode Selector (Search vs Maps vs None)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = currentGrounding == GroundingMode.NONE,
                onClick = { viewModel.setGroundingMode(GroundingMode.NONE) },
                label = { Text("Standard", fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF192A42),
                    selectedLabelColor = JarvisCyan
                )
            )

            FilterChip(
                selected = currentGrounding == GroundingMode.GOOGLE_SEARCH,
                onClick = {
                    viewModel.setGroundingMode(
                        if (currentGrounding == GroundingMode.GOOGLE_SEARCH) GroundingMode.NONE else GroundingMode.GOOGLE_SEARCH
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(12.dp))
                },
                label = { Text("Google Search Grounding", fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = JarvisCyan,
                    selectedLabelColor = Color.Black
                ),
                modifier = Modifier.testTag("grounding_search_chip")
            )

            FilterChip(
                selected = currentGrounding == GroundingMode.GOOGLE_MAPS,
                onClick = {
                    viewModel.setGroundingMode(
                        if (currentGrounding == GroundingMode.GOOGLE_MAPS) GroundingMode.NONE else GroundingMode.GOOGLE_MAPS
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(12.dp))
                },
                label = { Text("Google Maps Grounding", fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = JarvisGreen,
                    selectedLabelColor = Color.Black
                ),
                modifier = Modifier.testTag("grounding_maps_chip")
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Persona Role Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChatPersona.values().forEach { persona ->
                val isSelected = currentPersona == persona
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setPersona(persona) },
                    label = { Text("Role: ${persona.title}", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1E3250),
                        selectedLabelColor = JarvisCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Thread
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(JarvisSurfaceDark)
                .border(1.dp, JarvisCardBorder, RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Initiate multi-turn conversation with J.A.R.V.I.S.",
                                color = JarvisTextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Powered by ${currentModel.displayName} with realtime Search and Maps grounding.",
                                color = JarvisTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            } else {
                items(messages) { msg ->
                    ChatBubbleCard(
                        msg = msg,
                        onSpeak = { viewModel.speechManager.speak(msg.text) },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("JARVIS Message", msg.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            if (isProcessing) {
                item {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = JarvisCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "J.A.R.V.I.S. is synthesizing grounded response...",
                            color = JarvisCyan,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Field Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                placeholder = {
                    Text(
                        "Transmit message or query to Jarvis...",
                        color = JarvisTextSecondary,
                        fontSize = 13.sp
                    )
                },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisCardBorder,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputPrompt.isNotBlank() && !isProcessing) {
                            viewModel.sendChatbotMessage(inputPrompt)
                            inputPrompt = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputPrompt.isNotBlank() && !isProcessing) {
                        viewModel.sendChatbotMessage(inputPrompt)
                        inputPrompt = ""
                    }
                },
                enabled = inputPrompt.isNotBlank() && !isProcessing,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (inputPrompt.isNotBlank()) JarvisCyan else JarvisCardBorder)
                    .testTag("send_chat_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ChatBubbleCard(
    msg: FirestoreChatRecord,
    onSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = msg.role == "user"
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(msg.timestamp) { timeFormat.format(Date(msg.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_bubble_${msg.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) Color(0xFF132236) else JarvisSurfaceVariantDark
        ),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isUser) JarvisCardBorder else JarvisCyan.copy(alpha = 0.35f)
            )
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isUser) JarvisGold else JarvisCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUser) "OPERATOR" else "J.A.R.V.I.S. (${msg.modelUsed})",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) JarvisGold else JarvisCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisTextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = JarvisTextSecondary, modifier = Modifier.size(14.dp))
                    }
                    if (!isUser) {
                        IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak", tint = JarvisCyan, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = msg.text,
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisTextPrimary,
                fontFamily = if (isUser) FontFamily.Default else FontFamily.Monospace,
                lineHeight = 20.sp
            )
        }
    }
}
