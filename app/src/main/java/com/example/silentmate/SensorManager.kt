package com.example.silentmate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.sqrt

enum class DevicePosition {
    UPSIDE_DOWN,
    IN_POCKET,
    IN_HAND,
    UNKNOWN
}

enum class AudioProfile {
    SILENT,
    VIBRATION,
    GENERAL
}

class SilentMateSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val sharedPreferences = context.getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)

    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var proximity: Sensor? = null

    private var currentPosition = DevicePosition.UNKNOWN
    private var currentAudioProfile = AudioProfile.GENERAL

    private val handler = Handler(Looper.getMainLooper())
    private var positionUpdateCallback: ((DevicePosition, AudioProfile) -> Unit)? = null

    private val featureEnabled = mutableMapOf(
        DevicePosition.UPSIDE_DOWN to true,
        DevicePosition.IN_POCKET to true,
        DevicePosition.IN_HAND to true
    )

    private var isPerformanceMode = false
    private var lastSensorCheckTime = 0L
    private var sensorCheckInterval = 500L
    private var performanceModeInterval = 2000L

    private var lastPositionChangeTime = 0L
    private var positionStableTime = 5000L

    private var lastAccelerometerData = floatArrayOf(0f, 0f, 0f)
    private var lastGyroscopeData = floatArrayOf(0f, 0f, 0f)
    private var proximityDistance = 100f

    private val UPSIDE_DOWN_THRESHOLD = -5f
    private val IN_HAND_MOVEMENT_THRESHOLD = 0.5f
    private val STABLE_THRESHOLD = 0.5f
    private val PROXIMITY_NEAR_THRESHOLD = 5f // Increased to 5cm for better detection
    private val DESK_STABLE_THRESHOLD = 0.1f

    companion object {
        private const val CHANNEL_ID = "SilentMateChannelV2"
        private const val NOTIFICATION_ID = 1001
    }

    init {
        initializeSensors()
        createNotificationChannel()
    }

    private fun initializeSensors() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        Log.d("SensorManager", "Sensors initialized - Accel: ${accelerometer != null}, Gyro: ${gyroscope != null}, Proximity: ${proximity != null}")

        // Log proximity sensor details
        proximity?.let {
            Log.d("SensorManager", "Proximity Sensor Info:")
            Log.d("SensorManager", "  Name: ${it.name}")
            Log.d("SensorManager", "  Vendor: ${it.vendor}")
            Log.d("SensorManager", "  Max Range: ${it.maximumRange}cm")
            Log.d("SensorManager", "  Resolution: ${it.resolution}cm")
            Log.d("SensorManager", "  Power: ${it.power}mA")
        } ?: run {
            Log.e("SensorManager", "⚠️ PROXIMITY SENSOR NOT FOUND! In-pocket detection will not work.")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                notificationManager.deleteNotificationChannel("SilentMateChannel")
            } catch (e: Exception) {
                Log.e("SensorManager", "Error deleting old channel", e)
            }

            val name = "Silent Mate Status"
            val descriptionText = "Notifications for device position and audio profile changes"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setBypassDnd(false)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("SensorManager", "Notification channel created: $CHANNEL_ID")
        }
    }

    fun startListening(callback: (DevicePosition, AudioProfile) -> Unit) {
        positionUpdateCallback = callback
        isPerformanceMode = sharedPreferences.getBoolean("performance_mode", false)

        val sensorDelay = if (isPerformanceMode) {
            SensorManager.SENSOR_DELAY_UI
        } else {
            SensorManager.SENSOR_DELAY_NORMAL
        }

        accelerometer?.let {
            sensorManager.registerListener(this, it, sensorDelay)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, sensorDelay)
        }
        proximity?.let {
            sensorManager.registerListener(this, it, sensorDelay)
        }

        lastSensorCheckTime = System.currentTimeMillis()
        lastPositionChangeTime = System.currentTimeMillis()

        Log.d("SensorManager", "Started listening to sensors - Performance mode: $isPerformanceMode")

        handler.postDelayed({
            detectAndUpdatePosition()
        }, 1000)
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        positionUpdateCallback = null
        Log.d("SensorManager", "Stopped listening to sensors")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    lastAccelerometerData = it.values.clone()
                    Log.d("SensorManager", "Accel: X=${it.values[0]}, Y=${it.values[1]}, Z=${it.values[2]}")
                }
                Sensor.TYPE_GYROSCOPE -> {
                    lastGyroscopeData = it.values.clone()
                }
                Sensor.TYPE_PROXIMITY -> {
                    proximityDistance = it.values[0]
                    val maxRange = it.sensor.maximumRange
                    Log.d("SensorManager", "⚠️ PROXIMITY UPDATE: ${proximityDistance}cm (max: ${maxRange}cm) - Sensor: ${it.sensor.name}")

                    // Immediately check if this triggers pocket detection
                    if (proximityDistance < PROXIMITY_NEAR_THRESHOLD) {
                        Log.d("SensorManager", "🔍 Proximity close enough for pocket detection!")
                    }
                }
            }

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSensorCheckTime > sensorCheckInterval) {
                lastSensorCheckTime = currentTime
                detectAndUpdatePosition()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("SensorManager", "Sensor accuracy changed: ${sensor?.name} = $accuracy")
    }

    private fun detectAndUpdatePosition() {
        val newPosition = detectDevicePosition()

        Log.d("SensorManager", "Detected Position: $newPosition (Current: $currentPosition)")

        if (newPosition != currentPosition) {
            currentPosition = newPosition
            lastPositionChangeTime = System.currentTimeMillis()
            val newAudioProfile = mapPositionToAudioProfile(newPosition)

            if (newAudioProfile != currentAudioProfile) {
                currentAudioProfile = newAudioProfile
                applyAudioProfile(newAudioProfile)
                sendStatusChangeNotification(newPosition, newAudioProfile)
            }

            handler.post {
                positionUpdateCallback?.invoke(currentPosition, currentAudioProfile)
            }

            Log.d("SensorManager", "Position changed: $currentPosition -> $currentAudioProfile")
        }
    }

    private fun detectDevicePosition(): DevicePosition {
        val x = lastAccelerometerData[0]
        val y = lastAccelerometerData[1]
        val z = lastAccelerometerData[2]
        val movement = getMovementMagnitude()

        Log.d("SensorManager", "Detection - Accel(X:$x, Y:$y, Z:$z), Proximity:$proximityDistance, Movement:$movement")

        // Priority 1: Check proximity sensor (in pocket) - This triggers VIBRATION mode
        if (featureEnabled[DevicePosition.IN_POCKET] == true) {
            // Method 1: Check proximity sensor - MUST be very close (under 5cm)
            val isProximityNear = proximityDistance < PROXIMITY_NEAR_THRESHOLD

            // Method 2: Check if phone is face down (screen facing down)
            // When face down, Z-axis should be negative (between -3 and -12)
            val isFaceDown = z < -3f && z > -12f

            // Require BOTH proximity AND face down for reliable pocket detection
            if (isProximityNear && isFaceDown) {
                Log.d("SensorManager", "✓ Detected: IN_POCKET (proximity=${proximityDistance}cm + face-down Z=$z)")
                return DevicePosition.IN_POCKET
            }

            // Fallback: If proximity is VERY close (under 2cm), assume pocket even without face-down
            if (proximityDistance < 2f) {
                Log.d("SensorManager", "✓ Detected: IN_POCKET (very close proximity=${proximityDistance}cm)")
                return DevicePosition.IN_POCKET
            }

            // Log why pocket detection didn't trigger
            if (isProximityNear && !isFaceDown) {
                Log.d("SensorManager", "⚠️ Proximity near (${proximityDistance}cm) but NOT face-down (Z=$z)")
            }
            if (!isProximityNear && isFaceDown) {
                Log.d("SensorManager", "⚠️ Face-down (Z=$z) but proximity too far (${proximityDistance}cm)")
            }
        }

        // Priority 2: Check if actively being used (IN_HAND with movement) - GENERAL mode
        if (featureEnabled[DevicePosition.IN_HAND] == true) {
            // Phone is face up (normal position) - Z between 5 and 12
            if (z > 5f && z < 12f) {
                if (movement > IN_HAND_MOVEMENT_THRESHOLD) {
                    Log.d("SensorManager", "✓ Detected: IN_HAND (active use, movement=$movement)")
                    return DevicePosition.IN_HAND
                } else if (movement > 0.05f) {
                    Log.d("SensorManager", "✓ Detected: IN_HAND (holding, movement=$movement)")
                    return DevicePosition.IN_HAND
                }
            }
        }

        // Priority 3: Check if upside down (face up, stable on desk) - SILENT mode
        if (featureEnabled[DevicePosition.UPSIDE_DOWN] == true) {
            // Phone is face up (Z between 7 and 12) and stable (low movement)
            if (z > 7f && z < 12f && movement < DESK_STABLE_THRESHOLD) {
                Log.d("SensorManager", "✓ Detected: UPSIDE_DOWN (face up on desk, Z=$z, movement=$movement)")
                return DevicePosition.UPSIDE_DOWN
            }
        }

        // Priority 4: Everything else (default state)
        Log.d("SensorManager", "Detected: UNKNOWN (default state)")
        return DevicePosition.UNKNOWN
    }

    private fun sendStatusChangeNotification(position: DevicePosition, profile: AudioProfile) {
        if (!sharedPreferences.getBoolean("notifications_enabled", true)) {
            return
        }

        val positionText = when (position) {
            DevicePosition.UPSIDE_DOWN -> "Upside Down"
            DevicePosition.IN_POCKET -> "In Pocket"
            DevicePosition.IN_HAND -> "In Hand"
            DevicePosition.UNKNOWN -> "On Desk"
        }

        val profileText = when (profile) {
            AudioProfile.SILENT -> "Silent Mode"
            AudioProfile.VIBRATION -> "Vibration Mode"
            AudioProfile.GENERAL -> "General Mode"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Silent Mate Status Changed")
            .setContentText("Position: $positionText | Profile: $profileText")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        when (position) {
            DevicePosition.IN_HAND -> {
                val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

                notificationBuilder
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSound(defaultSoundUri, android.media.AudioManager.STREAM_NOTIFICATION)
                    .setVibrate(longArrayOf(0, 300, 200, 300))
                    .setOnlyAlertOnce(false)

                Log.d("SensorManager", "IN_HAND notification: Sound + Vibration (General Mode) - URI: $defaultSoundUri")

                try {
                    val ringtone = android.media.RingtoneManager.getRingtone(context, defaultSoundUri)
                    ringtone?.play()
                } catch (e: Exception) {
                    Log.e("SensorManager", "Error playing ringtone", e)
                }
            }
            DevicePosition.IN_POCKET -> {
                notificationBuilder
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                    .setVibrate(longArrayOf(0, 800, 300, 800))
                    .setSound(null)
                    .setOnlyAlertOnce(false)

                Log.d("SensorManager", "IN_POCKET notification: Vibration only (Vibration Mode)")
            }
            DevicePosition.UPSIDE_DOWN, DevicePosition.UNKNOWN -> {
                notificationBuilder
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setDefaults(0)
                    .setVibrate(longArrayOf(0))
                    .setSound(null)
                    .setOnlyAlertOnce(true)

                Log.d("SensorManager", "ON_DESK notification: Silent (Silent Mode)")
            }
        }

        val notification = notificationBuilder.build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.e("SensorManager", "Notification permission not granted", e)
        }
    }

    private fun isPositionStable(): Boolean {
        val timeSinceLastChange = System.currentTimeMillis() - lastPositionChangeTime
        return timeSinceLastChange > positionStableTime
    }

    private fun isDeviceStable(): Boolean {
        val gyroMagnitude = getMovementMagnitude()
        return gyroMagnitude < STABLE_THRESHOLD
    }

    private fun isDeviceMoving(): Boolean {
        val gyroMagnitude = getMovementMagnitude()
        return gyroMagnitude > IN_HAND_MOVEMENT_THRESHOLD
    }

    private fun getMovementMagnitude(): Float {
        return sqrt(
            lastGyroscopeData[0] * lastGyroscopeData[0] +
                    lastGyroscopeData[1] * lastGyroscopeData[1] +
                    lastGyroscopeData[2] * lastGyroscopeData[2]
        )
    }

    private fun mapPositionToAudioProfile(position: DevicePosition): AudioProfile {
        return when (position) {
            DevicePosition.UPSIDE_DOWN -> AudioProfile.SILENT
            DevicePosition.IN_POCKET -> AudioProfile.VIBRATION
            DevicePosition.IN_HAND -> AudioProfile.GENERAL
            DevicePosition.UNKNOWN -> AudioProfile.SILENT
        }
    }

    private fun applyAudioProfile(profile: AudioProfile) {
        if (!isSensorSwitchingEnabled()) {
            Log.d("SensorManager", "Sensor switching disabled, skipping audio profile change")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Log.w("SensorManager", "DND permission not granted, cannot change audio mode")
                return
            }
        }

        try {
            val oldRingerMode = audioManager.ringerMode
            when (profile) {
                AudioProfile.SILENT -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    Log.d("SensorManager", "✓ Applied SILENT profile (was: $oldRingerMode)")
                }
                AudioProfile.VIBRATION -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    Log.d("SensorManager", "✓ Applied VIBRATION profile (was: $oldRingerMode)")
                }
                AudioProfile.GENERAL -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                    if (currentVolume == 0) {
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume / 2, 0)
                    }
                    Log.d("SensorManager", "✓ Applied GENERAL profile (was: $oldRingerMode, volume: $currentVolume/$maxVolume)")
                }
            }
        } catch (e: SecurityException) {
            Log.e("SensorManager", "SecurityException: Cannot modify audio settings", e)
        }
    }

    fun isSensorSwitchingEnabled(): Boolean {
        return sharedPreferences.getBoolean("sensor_switching_enabled", true)
    }

    fun setSensorSwitchingEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("sensor_switching_enabled", enabled).apply()
        if (!enabled) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
    }

    fun setFeatureEnabled(position: DevicePosition, enabled: Boolean) {
        featureEnabled[position] = enabled
        Log.d("SensorManager", "Feature ${position.name} set to: $enabled")
        handler.postDelayed({
            detectAndUpdatePosition()
        }, 100)
    }

    fun getCurrentPosition(): DevicePosition = currentPosition
    fun getCurrentAudioProfile(): AudioProfile = currentAudioProfile

    fun setPerformanceMode(enabled: Boolean) {
        isPerformanceMode = enabled
        sharedPreferences.edit().putBoolean("performance_mode", enabled).apply()

        if (positionUpdateCallback != null) {
            stopListening()
            startListening(positionUpdateCallback!!)
        }

        Log.d("SensorManager", "Performance mode set to: $enabled")
    }

    fun isPerformanceModeEnabled(): Boolean = isPerformanceMode

    fun getBatteryOptimizationInfo(): String {
        val stableTime = if (isPositionStable()) "Stable" else "Active"
        val mode = if (isPerformanceMode) "Performance" else "Normal"
        val interval = if (isPerformanceMode) performanceModeInterval else sensorCheckInterval

        return "Mode: $mode | Status: $stableTime | Interval: ${interval}ms"
    }

    fun getDebugInfo(): String {
        val x = lastAccelerometerData[0]
        val y = lastAccelerometerData[1]
        val z = lastAccelerometerData[2]
        val movement = getMovementMagnitude()

        return """
            Z: ${String.format("%.2f", z)} | Prox: ${String.format("%.1f", proximityDistance)}cm
            Move: ${String.format("%.2f", movement)} | Pos: $currentPosition
        """.trimIndent()
    }

    fun cleanup() {
        stopListening()
    }
}