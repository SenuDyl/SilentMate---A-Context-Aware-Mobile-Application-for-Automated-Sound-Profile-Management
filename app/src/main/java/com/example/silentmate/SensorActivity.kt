package com.example.silentmate

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SensorActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var audioManager: AudioManager
    
    private var accelerometer: Sensor? = null
    private var proximity: Sensor? = null
    
    private lateinit var upsideDownSwitch: SwitchCompat
    private lateinit var inPocketSwitch: SwitchCompat
    private lateinit var inHandSwitch: SwitchCompat
    private lateinit var backButton: ImageView
    
    private var isUpsideDownEnabled = true
    private var isInPocketEnabled = true
    private var isInHandEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)
        
        // Initialize views
        upsideDownSwitch = findViewById(R.id.upsideDownSwitch)
        inPocketSwitch = findViewById(R.id.inPocketSwitch)
        inHandSwitch = findViewById(R.id.inHandSwitch)
        backButton = findViewById(R.id.backButton)
        
        // Initialize sensor and audio managers
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Get sensors
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        
        // Set up switch listeners
        upsideDownSwitch.setOnCheckedChangeListener { _, isChecked ->
            isUpsideDownEnabled = isChecked
        }
        
        inPocketSwitch.setOnCheckedChangeListener { _, isChecked ->
            isInPocketEnabled = isChecked
        }
        
        inHandSwitch.setOnCheckedChangeListener { _, isChecked ->
            isInHandEnabled = isChecked
        }
        
        // Set up back button
        backButton.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Register sensor listeners
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        proximity?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister sensor listeners to save battery
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                
                // Check if phone is upside down (z-axis negative and significant)
                if (z < -9.0f && isUpsideDownEnabled) {
                    setAudioProfile(AudioManager.RINGER_MODE_SILENT)
                }
                // Check if phone is in normal orientation and being used
                else if (Math.abs(x) < 3.0f && Math.abs(y) < 3.0f && z > 9.0f && isInHandEnabled) {
                    setAudioProfile(AudioManager.RINGER_MODE_NORMAL)
                }
            }
            
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                // If proximity sensor is covered (phone likely in pocket)
                if (distance == 0f && isInPocketEnabled) {
                    setAudioProfile(AudioManager.RINGER_MODE_VIBRATE)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
    
    private fun setAudioProfile(mode: Int) {
        if (audioManager.ringerMode != mode) {
            audioManager.ringerMode = mode
            val profileName = when (mode) {
                AudioManager.RINGER_MODE_SILENT -> "Silent"
                AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                AudioManager.RINGER_MODE_NORMAL -> "Normal"
                else -> "Unknown"
            }
            Toast.makeText(this, "Audio profile changed to: $profileName", Toast.LENGTH_SHORT).show()
        }
    }
}