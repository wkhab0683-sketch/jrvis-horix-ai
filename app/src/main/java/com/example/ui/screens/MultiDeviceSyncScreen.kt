package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private const val PUBLISHED_APP_URL = "https://ais-pre-4eatacwszwsfzze2fdhkgi-38977320310.asia-southeast1.run.app"

@Composable
fun MultiDeviceSyncScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceEnabled by viewModel.speechManager.voiceEnabled.collectAsState()
    var customKeyInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackgroundDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            Column {
                Text(
                    text = "CROSS-DEVICE ACCESSIBILITY HUB",
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisCyan
                )
                Text(
                    text = "PUBLISHED MULTI-DEVICE ACCESSIBILITY ARRAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSecondary
                )
            }
        }

        // Section 1: Published Live Link Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("published_link_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1C2E)),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisCyan)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GLOBAL CLOUD ACCESS URL",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(JarvisGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE & READY",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisGreen,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Access your full J.A.R.V.I.S. interface, voice control, scheduling, and device telemetry from any mobile phone, tablet, laptop, or desktop browser via this published cloud endpoint:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisTextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Published Link Text Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF070E1A))
                            .border(1.dp, JarvisCardBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = PUBLISHED_APP_URL,
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisCyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("JARVIS Published URL", PUBLISHED_APP_URL)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "J.A.R.V.I.S. link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                viewModel.systemControl.vibrateJarvisConfirmation()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_published_link_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COPY LINK", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PUBLISHED_APP_URL)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_published_link_button"),
                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "Open",
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LAUNCH", color = JarvisCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section 2: Connected Ecosystem Devices Array
        item {
            Text(
                text = "// SYNCHRONIZED HARDWARE MATRIX",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisTextSecondary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DeviceSyncRow(
                    icon = Icons.Default.PhoneAndroid,
                    deviceName = "Primary Android Device",
                    deviceType = "Host System • Real-Time Hardware Sensor Array",
                    status = "CONNECTED (PRIMARY)",
                    statusColor = JarvisGreen
                )
                DeviceSyncRow(
                    icon = Icons.Default.TabletMac,
                    deviceName = "Stark Tactical Tablet",
                    deviceType = "Secondary HUD Terminal (Web / PWA)",
                    status = "STANDBY",
                    statusColor = JarvisCyan
                )
                DeviceSyncRow(
                    icon = Icons.Default.Computer,
                    deviceName = "Lab Workstation & Laptop",
                    deviceType = "Command Console Browser Link",
                    status = "ONLINE VIA WEB LINK",
                    statusColor = JarvisGold
                )
            }
        }

        // Section 3: Speech & Synthesis Configuration
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "// ACOUSTIC SPEECH & VOCAL FEEDBACK",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisTextSecondary
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_settings_card"),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisCardBorder)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Jarvis Voice Synthesis",
                                style = MaterialTheme.typography.bodyLarge,
                                color = JarvisTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = { viewModel.toggleVoiceFeedback() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyan,
                                checkedTrackColor = Color(0xFF003840),
                                uncheckedThumbColor = JarvisTextSecondary,
                                uncheckedTrackColor = JarvisSurfaceVariantDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Synthesizes responses using British vocal inflection with robotic cadence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.speechManager.speak("Systems operational, sir. Speech synthesis frequency is verified and optimal.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariantDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("test_voice_button")
                    ) {
                        Text("TEST VOCAL OUTPUT", color = JarvisCyan, fontSize = 11.sp)
                    }
                }
            }
        }

        // Section 4: Gemini AI Neural Uplink Configuration
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "// NEURAL UPLINK (GEMINI AI AGENT)",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisTextSecondary
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_key_card"),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisCardBorder)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = JarvisGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini Neural API Credential",
                            style = MaterialTheme.typography.bodyLarge,
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Configured via AI Studio Secrets Panel (or enter custom key below for local override):",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customKeyInput,
                        onValueChange = { customKeyInput = it },
                        label = { Text("Custom API Key Override (Optional)") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_gemini_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (customKeyInput.isNotBlank()) {
                                viewModel.setCustomApiKey(customKeyInput)
                                Toast.makeText(context, "API Key updated successfully, sir.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_api_key_button")
                    ) {
                        Text("SAVE UPLINK KEY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun DeviceSyncRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    deviceName: String,
    deviceType: String,
    status: String,
    statusColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(JarvisCardBorder)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF131F33)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = deviceType,
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}
