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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.R
import com.example.data.auth.AuthState
import com.example.ui.JarvisViewModel
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

private const val PUBLISHED_APP_URL = "https://ais-pre-4eatacwszwsfzze2fdhkgi-38977320310.asia-southeast1.run.app"

@Composable
fun MultiDeviceSyncScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceEnabled by viewModel.speechManager.voiceEnabled.collectAsState()
    val authState by viewModel.authState.collectAsState()
    var customKeyInput by remember { mutableStateOf("") }
    val databaseId = remember { context.getString(R.string.firestore_database_id) }

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
                    text = "IDENTITY & CLOUD PERSISTENCE HUB",
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisCyan
                )
                Text(
                    text = "FIREBASE AUTH • FIRESTORE • MULTI-DEVICE",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSecondary
                )
            }
        }

        // Section 1: Firebase Authentication (Google Sign-In) & Firestore Persistence
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_persistence_card"),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (authState is AuthState.Authenticated) JarvisGreen else JarvisCyan
                    )
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
                                imageVector = if (authState is AuthState.Authenticated) Icons.Default.CloudDone else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (authState is AuthState.Authenticated) JarvisGreen else JarvisGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "STARK CLOUD DATABASE & AUTH",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (authState is AuthState.Authenticated) JarvisGreen else JarvisGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (authState is AuthState.Authenticated) JarvisGreen.copy(alpha = 0.2f)
                                    else Color(0xFF2C2210)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (authState is AuthState.Authenticated) "SECURE SYNC" else "LOCAL ONLY",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (authState is AuthState.Authenticated) JarvisGreen else JarvisGold,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (val state = authState) {
                        is AuthState.Authenticated -> {
                            val user = state.user
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(JarvisCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = JarvisCyan,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = user.displayName ?: "Authenticated Operator",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = JarvisTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = user.email ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = JarvisCyan
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Cloud Firestore Database: $databaseId",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { viewModel.signOut() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisRed.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sign_out_button")
                            ) {
                                Text("SIGN OUT OF STARK SESSION", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        is AuthState.Authenticating -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = JarvisCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Verifying Google Identity credentials...", color = JarvisCyan, fontSize = 12.sp)
                            }
                        }

                        else -> {
                            Text(
                                text = "Sign in with your Google Account to synchronize your schedules, AI creations (music, images, videos), and chat logs securely to Cloud Firestore.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisTextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { viewModel.signInWithGoogle() },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("sign_in_google_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SIGN IN WITH GOOGLE",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Global Published Link Card
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
                                text = "ACCESSIBLE ANYWHERE",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisGreen,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Access your full J.A.R.V.I.S. interface, voice control, scheduling, and generative AI matrix from any device via this published endpoint:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisTextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

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
                            fontSize = 11.sp
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
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Black, modifier = Modifier.size(16.dp))
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
                            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = "Open", tint = JarvisCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LAUNCH", color = JarvisCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section 3: Speech Synthesis
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
                            Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Jarvis Voice Synthesis", style = MaterialTheme.typography.bodyLarge, color = JarvisTextPrimary, fontWeight = FontWeight.SemiBold)
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
                }
            }
        }

        // Section 4: Gemini AI Neural Uplink Configuration
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
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = JarvisGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini Neural API Credential", style = MaterialTheme.typography.bodyLarge, color = JarvisTextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
