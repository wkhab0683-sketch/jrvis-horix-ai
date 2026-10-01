package com.example.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class DeviceTelemetry(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val chargingSource: String = "Battery",
    val batteryTempCelsius: Float = 25f,
    val availableRamMb: Long = 0,
    val totalRamMb: Long = 0,
    val availableStorageGb: Float = 0f,
    val totalStorageGb: Float = 0f,
    val networkType: String = "Wi-Fi",
    val deviceModel: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val isFlashlightOn: Boolean = false,
    val volumePercent: Int = 50,
    val ringerMode: String = "Normal"
)

class SystemControlManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val _telemetry = MutableStateFlow(DeviceTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private var torchCameraId: String? = null
    private var isTorchActive: Boolean = false

    init {
        initTorch()
        refreshTelemetry()
    }

    private fun initTorch() {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    return
                }
            }
            if (torchCameraId == null && (cameraManager?.cameraIdList?.isNotEmpty() == true)) {
                torchCameraId = cameraManager.cameraIdList[0]
            }
        } catch (_: Exception) {
            torchCameraId = null
        }
    }

    fun toggleFlashlight(): Boolean {
        val target = !isTorchActive
        return setFlashlight(target)
    }

    fun setFlashlight(enable: Boolean): Boolean {
        return try {
            val camId = torchCameraId
            if (camId != null && cameraManager != null) {
                cameraManager.setTorchMode(camId, enable)
                isTorchActive = enable
                _telemetry.value = _telemetry.value.copy(isFlashlightOn = enable)
                vibratePulse()
                true
            } else {
                isTorchActive = enable
                _telemetry.value = _telemetry.value.copy(isFlashlightOn = enable)
                false
            }
        } catch (_: Exception) {
            isTorchActive = enable
            _telemetry.value = _telemetry.value.copy(isFlashlightOn = enable)
            false
        }
    }

    fun setVolumePercent(percent: Int): Int {
        val clamped = percent.coerceIn(0, 100)
        audioManager?.let { am ->
            try {
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (clamped * max / 100).coerceIn(0, max)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                _telemetry.value = _telemetry.value.copy(volumePercent = clamped)
                vibratePulse()
            } catch (_: Exception) {}
        }
        return clamped
    }

    fun muteVolume() {
        setVolumePercent(0)
    }

    fun maxVolume() {
        setVolumePercent(100)
    }

    fun vibratePulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (_: Exception) {}
    }

    fun vibrateJarvisConfirmation() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 50, 40, 70)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }

    fun refreshTelemetry(): DeviceTelemetry {
        // Battery
        var batPercent = 100
        var isCharging = false
        var chargeSource = "Battery"
        var tempC = 26.0f

        try {
            val batteryStatus: Intent? = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            batteryStatus?.let { intent ->
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batPercent = ((level.toFloat() / scale.toFloat()) * 100).toInt()
                }
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                chargeSource = when (chargePlug) {
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Adapter"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
                    else -> if (isCharging) "Charging" else "Discharging"
                }
                val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250)
                tempC = rawTemp / 10f
            }
        } catch (_: Exception) {}

        // Memory
        var availRam = 0L
        var totalRam = 0L
        try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            availRam = memInfo.availMem / (1024 * 1024)
            totalRam = memInfo.totalMem / (1024 * 1024)
        } catch (_: Exception) {}

        // Storage
        var availStorage = 0f
        var totalStorage = 0f
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            totalStorage = (totalBlocks * blockSize) / (1024f * 1024f * 1024f)
            availStorage = (availableBlocks * blockSize) / (1024f * 1024f * 1024f)
        } catch (_: Exception) {}

        // Network
        var netType = "Offline"
        try {
            val network = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(network)
            netType = when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi (High Bandwidth)"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular LTE/5G"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet Arc Uplink"
                network != null -> "Connected"
                else -> "Offline / Local Array"
            }
        } catch (_: Exception) {}

        // Volume
        var volPercent = 50
        var ringer = "Normal"
        try {
            audioManager?.let { am ->
                val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (maxVol > 0) {
                    volPercent = ((currentVol.toFloat() / maxVol.toFloat()) * 100).toInt()
                }
                ringer = when (am.ringerMode) {
                    AudioManager.RINGER_MODE_SILENT -> "Silent"
                    AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                    else -> "Normal"
                }
            }
        } catch (_: Exception) {}

        val newTelemetry = DeviceTelemetry(
            batteryPercent = batPercent,
            isCharging = isCharging,
            chargingSource = chargeSource,
            batteryTempCelsius = tempC,
            availableRamMb = availRam,
            totalRamMb = totalRam,
            availableStorageGb = availStorage,
            totalStorageGb = totalStorage,
            networkType = netType,
            isFlashlightOn = isTorchActive,
            volumePercent = volPercent,
            ringerMode = ringer
        )
        _telemetry.value = newTelemetry
        return newTelemetry
    }

    // Settings Quick Launchers
    fun openSettings(action: String) {
        try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    fun openWifiSettings() = openSettings(Settings.ACTION_WIFI_SETTINGS)
    fun openBluetoothSettings() = openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
    fun openDisplaySettings() = openSettings(Settings.ACTION_DISPLAY_SETTINGS)
    fun openSoundSettings() = openSettings(Settings.ACTION_SOUND_SETTINGS)
    fun openBatterySettings() = openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS)
    fun openDateSettings() = openSettings(Settings.ACTION_DATE_SETTINGS)
}
