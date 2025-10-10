package com.example.silentmate

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log

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
    private val sharedPreferences = context.getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
    
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var proximity: Sensor? = null
    
    private var currentPosition = DevicePosition.UNKNOWN
    private var currentAudioProfile = AudioProfile.GENERAL
    
    private val handler = Handler(Looper.getMainLooper())
    private var positionUpdateCallback: ((DevicePosition, AudioProfile) -> Unit)? = null
    
    // Performance mode variables
    private var isPerformanceMode = false
    private var lastSensorCheckTime = 0L
    private var sensorCheckInterval = 1000L // 1 second in normal mode
    private var performanceModeInterval = 5000L // 5 seconds in performance mode
    
    // Battery optimization
    private var lastPositionChangeTime = 0L
    private var positionStableTime = 30000L // 30 seconds of stability before reducing checks
    
    // Sensor data
    private var lastAccelerometerData = floatArrayOf(0f, 0f, 0f)
    private var lastGyroscopeData = floatArrayOf(0f, 0f, 0f)
    private var isNearProximity = false
    
    // Thresholds
    private val UPSIDE_DOWN_THRESHOLD = -8f // Z-axis acceleration when upside down
    private val MOVEMENT_THRESHOLD = 0.5f // Gyroscope movement threshold
    private val STABLE_THRESHOLD = 0.1f // Stability threshold for position detection

    init {
        initializeSensors()
    }

    private fun initializeSensors() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        
        Log.d("SensorManager", "Sensors initialized - Accelerometer: ${accelerometer != null}, Gyroscope: ${gyroscope != null}, Proximity: ${proximity != null}")
    }

    fun startListening(callback: (DevicePosition, AudioProfile) -> Unit) {
        positionUpdateCallback = callback
        isPerformanceMode = sharedPreferences.getBoolean("performance_mode", false)
        
        // Set sensor delay based on performance mode
        val sensorDelay = if (isPerformanceMode) {
            SensorManager.SENSOR_DELAY_UI // Slower updates for battery saving
        } else {
            SensorManager.SENSOR_DELAY_NORMAL
        }
        
        // Register sensor listeners with appropriate delay
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
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        positionUpdateCallback = null
        Log.d("SensorManager", "Stopped listening to sensors")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val currentTime = System.currentTimeMillis()
            
            // Performance mode throttling - only process sensor data at intervals
            if (isPerformanceMode) {
                val timeSinceLastCheck = currentTime - lastSensorCheckTime
                val currentInterval = if (isPositionStable()) {
                    performanceModeInterval * 2 // Even slower when position is stable
                } else {
                    performanceModeInterval
                }
                
                if (timeSinceLastCheck < currentInterval) {
                    return // Skip this sensor update for battery saving
                }
                lastSensorCheckTime = currentTime
            }
            
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    lastAccelerometerData = it.values.clone()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    lastGyroscopeData = it.values.clone()
                }
                Sensor.TYPE_PROXIMITY -> {
                    isNearProximity = it.values[0] < it.sensor.maximumRange
                }
            }
            
            // Update position detection after collecting sensor data
            updateDevicePosition()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    private fun updateDevicePosition() {
        val newPosition = detectDevicePosition()
        
        if (newPosition != currentPosition) {
            currentPosition = newPosition
            lastPositionChangeTime = System.currentTimeMillis()
            val newAudioProfile = mapPositionToAudioProfile(newPosition)
            
            if (newAudioProfile != currentAudioProfile) {
                currentAudioProfile = newAudioProfile
                applyAudioProfile(newAudioProfile)
            }
            
            // Notify callback on main thread
            handler.post {
                positionUpdateCallback?.invoke(currentPosition, currentAudioProfile)
            }
            
            Log.d("SensorManager", "Position changed: $currentPosition -> $currentAudioProfile")
        }
    }

    private fun isPositionStable(): Boolean {
        val timeSinceLastChange = System.currentTimeMillis() - lastPositionChangeTime
        return timeSinceLastChange > positionStableTime
    }

    private fun detectDevicePosition(): DevicePosition {
        // Check if proximity sensor indicates pocket/bag
        if (isNearProximity) {
            return DevicePosition.IN_POCKET
        }
        
        // Check if device is upside down (negative Z acceleration)
        if (lastAccelerometerData[2] < UPSIDE_DOWN_THRESHOLD) {
            // Check if device is stable (not moving much)
            val isStable = isDeviceStable()
            if (isStable) {
                return DevicePosition.UPSIDE_DOWN
            }
        }
        
        // Check if device is being actively used (movement detected)
        if (isDeviceMoving()) {
            return DevicePosition.IN_HAND
        }
        
        return DevicePosition.UNKNOWN
    }

    private fun isDeviceStable(): Boolean {
        val gyroMagnitude = kotlin.math.sqrt(
            lastGyroscopeData[0] * lastGyroscopeData[0] +
            lastGyroscopeData[1] * lastGyroscopeData[1] +
            lastGyroscopeData[2] * lastGyroscopeData[2]
        )
        return gyroMagnitude < STABLE_THRESHOLD
    }

    private fun isDeviceMoving(): Boolean {
        val gyroMagnitude = kotlin.math.sqrt(
            lastGyroscopeData[0] * lastGyroscopeData[0] +
            lastGyroscopeData[1] * lastGyroscopeData[1] +
            lastGyroscopeData[2] * lastGyroscopeData[2]
        )
        return gyroMagnitude > MOVEMENT_THRESHOLD
    }

    private fun mapPositionToAudioProfile(position: DevicePosition): AudioProfile {
        return when (position) {
            DevicePosition.UPSIDE_DOWN -> AudioProfile.SILENT
            DevicePosition.IN_POCKET -> AudioProfile.VIBRATION
            DevicePosition.IN_HAND -> AudioProfile.GENERAL
            DevicePosition.UNKNOWN -> AudioProfile.GENERAL
        }
    }

    private fun applyAudioProfile(profile: AudioProfile) {
        // Check if sensor-based switching is enabled
        if (!isSensorSwitchingEnabled()) {
            return
        }

        when (profile) {
            AudioProfile.SILENT -> {
                // Set to silent mode
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                Log.d("SensorManager", "Applied SILENT profile")
            }
            AudioProfile.VIBRATION -> {
                // Set to vibration mode
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                Log.d("SensorManager", "Applied VIBRATION profile")
            }
            AudioProfile.GENERAL -> {
                // Set to normal mode
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                Log.d("SensorManager", "Applied GENERAL profile")
            }
        }
    }

    fun isSensorSwitchingEnabled(): Boolean {
        return sharedPreferences.getBoolean("sensor_switching_enabled", true)
    }

    fun setSensorSwitchingEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("sensor_switching_enabled", enabled).apply()
        if (!enabled) {
            // Reset to normal mode when disabled
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
    }

    fun getCurrentPosition(): DevicePosition = currentPosition
    fun getCurrentAudioProfile(): AudioProfile = currentAudioProfile

    fun setPerformanceMode(enabled: Boolean) {
        isPerformanceMode = enabled
        sharedPreferences.edit().putBoolean("performance_mode", enabled).apply()
        
        // Restart sensor listening with new performance settings
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

    fun cleanup() {
        stopListening()
    }
}
