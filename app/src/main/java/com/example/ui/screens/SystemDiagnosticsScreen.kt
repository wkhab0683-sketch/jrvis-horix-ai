package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system.DeviceTelemetry
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
import java.util.Locale

@Composable
fun SystemDiagnosticsScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackgroundDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            // Screen Header with Refresh Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SYSTEM CONTROL & TELEMETRY",
                        style = MaterialTheme.typography.titleMedium,
                        color = JarvisCyan
                    )
                    Text(
                        text = "REAL-TIME SENSOR & HARDWARE ARRAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisTextSecondary
                    )
                }

                Button(
                    onClick = { viewModel.refreshTelemetry() },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("refresh_telemetry_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan",
                        tint = JarvisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SWEEP", color = JarvisCyan, fontSize = 11.sp)
                }
            }
        }

        // Section 1: Direct Hardware Controls
        item {
            Text(
                text = "// HARDWARE MANIPULATION CONTROLS",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisTextSecondary
            )
        }

        // Flashlight (Torch) Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashlight_control_card"),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (telemetry.isFlashlightOn) JarvisCyan else JarvisCardBorder
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (telemetry.isFlashlightOn) JarvisCyan.copy(alpha = 0.2f) else Color(0xFF101B2E)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (telemetry.isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Flashlight",
                                tint = if (telemetry.isFlashlightOn) JarvisCyan else JarvisTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "High-Intensity Illumination",
                                style = MaterialTheme.typography.bodyLarge,
                                color = JarvisTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (telemetry.isFlashlightOn) "Torch Active (Camera Module)" else "Torch Standby",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (telemetry.isFlashlightOn) JarvisCyan else JarvisTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = telemetry.isFlashlightOn,
                        onCheckedChange = { viewModel.toggleFlashlight() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisCyan,
                            checkedTrackColor = Color(0xFF003840),
                            uncheckedThumbColor = JarvisTextSecondary,
                            uncheckedTrackColor = JarvisSurfaceVariantDark
                        ),
                        modifier = Modifier.testTag("flashlight_switch")
                    )
                }
            }
        }

        // Audio Volume Slider Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("volume_control_card"),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisCardBorder)
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
                                imageVector = if (telemetry.volumePercent == 0) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Acoustic Amplifier Output",
                                style = MaterialTheme.typography.bodyLarge,
                                color = JarvisTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "${telemetry.volumePercent}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisCyan
                        )
                    }

                    Slider(
                        value = telemetry.volumePercent.toFloat(),
                        onValueChange = { viewModel.setVolume(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyan,
                            activeTrackColor = JarvisCyan,
                            inactiveTrackColor = JarvisSurfaceVariantDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("volume_slider")
                    )

                    // Volume Quick Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "MUTE", 30 to "LOW", 70 to "MED", 100 to "MAX").forEach { (level, label) ->
                            OutlinedButton(
                                onClick = { viewModel.setVolume(level) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("volume_preset_$label"),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = JarvisCyan)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Real-time Telemetry Cards
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "// HARDWARE TELEMETRY & DIAGNOSTICS",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisTextSecondary
            )
        }

        // Battery & Power Core
        item {
            TelemetryMetricCard(
                icon = if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                title = "Arc Power Cells (Battery)",
                mainValue = "${telemetry.batteryPercent}%",
                subValue = "${telemetry.chargingSource} • ${String.format(Locale.US, "%.1f", telemetry.batteryTempCelsius)}°C",
                progress = telemetry.batteryPercent / 100f,
                accentColor = if (telemetry.batteryPercent > 20) JarvisCyan else JarvisRed
            )
        }

        // Memory RAM Card
        item {
            val ramPercent = if (telemetry.totalRamMb > 0) {
                ((telemetry.totalRamMb - telemetry.availableRamMb).toFloat() / telemetry.totalRamMb)
            } else 0.5f

            TelemetryMetricCard(
                icon = Icons.Default.Memory,
                title = "Volatile Memory Buffer (RAM)",
                mainValue = "${telemetry.availableRamMb} MB Free",
                subValue = "Total: ${telemetry.totalRamMb} MB (${(ramPercent * 100).toInt()}% utilized)",
                progress = ramPercent,
                accentColor = JarvisGold
            )
        }

        // Internal Storage Card
        item {
            val storagePercent = if (telemetry.totalStorageGb > 0) {
                ((telemetry.totalStorageGb - telemetry.availableStorageGb) / telemetry.totalStorageGb)
            } else 0.5f

            TelemetryMetricCard(
                icon = Icons.Default.SdStorage,
                title = "Solid State Core Storage",
                mainValue = "${String.format(Locale.US, "%.1f", telemetry.availableStorageGb)} GB Free",
                subValue = "Total: ${String.format(Locale.US, "%.1f", telemetry.totalStorageGb)} GB",
                progress = storagePercent,
                accentColor = JarvisGreen
            )
        }

        // Device Model & Network Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("device_specs_card"),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisCardBorder)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "HARDWARE PLATFORM IDENTIFICATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = telemetry.deviceModel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = JarvisCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${telemetry.androidVersion} • Uplink: ${telemetry.networkType}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisTextSecondary
                    )
                }
            }
        }

        // Section 3: Android System Settings Shortcuts
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "// ANDROID SYSTEM SUBSYSTEM SHORTCUTS",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisTextSecondary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SystemSettingShortcutButton(
                        label = "Wi-Fi Uplink",
                        icon = Icons.Default.Wifi,
                        onClick = { viewModel.systemControl.openWifiSettings() },
                        modifier = Modifier.weight(1f)
                    )
                    SystemSettingShortcutButton(
                        label = "Bluetooth Array",
                        icon = Icons.Default.Bluetooth,
                        onClick = { viewModel.systemControl.openBluetoothSettings() },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SystemSettingShortcutButton(
                        label = "Display Core",
                        icon = Icons.Default.BrightnessMedium,
                        onClick = { viewModel.systemControl.openDisplaySettings() },
                        modifier = Modifier.weight(1f)
                    )
                    SystemSettingShortcutButton(
                        label = "Acoustic System",
                        icon = Icons.AutoMirrored.Filled.VolumeDown,
                        onClick = { viewModel.systemControl.openSoundSettings() },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SystemSettingShortcutButton(
                        label = "Battery Saver",
                        icon = Icons.Default.BatteryChargingFull,
                        onClick = { viewModel.systemControl.openBatterySettings() },
                        modifier = Modifier.weight(1f)
                    )
                    SystemSettingShortcutButton(
                        label = "Temporal Clock",
                        icon = Icons.Default.DateRange,
                        onClick = { viewModel.systemControl.openDateSettings() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TelemetryMetricCard(
    icon: ImageVector,
    title: String,
    mainValue: String,
    subValue: String,
    progress: Float,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = mainValue,
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = JarvisSurfaceVariantDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = JarvisTextSecondary
            )
        }
    }
}

@Composable
fun SystemSettingShortcutButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(44.dp)
            .testTag("setting_btn_${label.replace(" ", "_")}"),
        colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceDark),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = JarvisCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = JarvisTextPrimary,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}
