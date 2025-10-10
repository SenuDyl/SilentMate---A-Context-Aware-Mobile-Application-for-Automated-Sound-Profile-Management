package com.example.silentmate

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment

class SensorFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sensorManager: SilentMateSensorManager
    private lateinit var upsideDownSwitch: SwitchCompat
    private lateinit var inPocketSwitch: SwitchCompat
    private lateinit var inHandSwitch: SwitchCompat
    
    private lateinit var currentPositionText: TextView
    private lateinit var currentProfileText: TextView
    private lateinit var batteryModeText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_sensor, container, false)
        
        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
        
        // Initialize sensor manager
        sensorManager = SilentMateSensorManager(requireContext())
        
        // Initialize UI elements
        initializeViews(view)
        
        // Set up back button click listener
        val backButton = view.findViewById<ImageView>(R.id.backButton)
        backButton?.setOnClickListener {
            sensorManager.stopListening()
            // Navigate back to home fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        }
        
        // Load saved preferences
        loadPreferences()
        
        // Set up switch listeners
        setupSwitchListeners()
        
        // Start sensor monitoring
        startSensorMonitoring()
        
        return view
    }

    private fun initializeViews(view: View) {
        upsideDownSwitch = view.findViewById(R.id.upsideDownSwitch)
        inPocketSwitch = view.findViewById(R.id.inPocketSwitch)
        inHandSwitch = view.findViewById(R.id.inHandSwitch)
        
        // Find status text views
        currentPositionText = view.findViewById(R.id.currentPositionText)
        currentProfileText = view.findViewById(R.id.currentProfileText)
        batteryModeText = view.findViewById(R.id.batteryModeText)
    }

    private fun startSensorMonitoring() {
        sensorManager.startListening { position, profile ->
            updateStatusDisplay(position, profile)
        }
    }

    private fun updateStatusDisplay(position: DevicePosition, profile: AudioProfile) {
        val positionText = when (position) {
            DevicePosition.UPSIDE_DOWN -> "Upside Down"
            DevicePosition.IN_POCKET -> "In Pocket"
            DevicePosition.IN_HAND -> "In Hand/Active"
            DevicePosition.UNKNOWN -> "Unknown"
        }
        
        val profileText = when (profile) {
            AudioProfile.SILENT -> "Silent Mode"
            AudioProfile.VIBRATION -> "Vibration Mode"
            AudioProfile.GENERAL -> "General Mode"
        }
        
        currentPositionText.text = positionText
        currentProfileText.text = profileText
        
        // Update battery mode display
        updateBatteryModeDisplay()
        
        // Update switch states to reflect current detection
        updateSwitchStates(position)
    }

    private fun updateSwitchStates(position: DevicePosition) {
        upsideDownSwitch.isChecked = position == DevicePosition.UPSIDE_DOWN
        inPocketSwitch.isChecked = position == DevicePosition.IN_POCKET
        inHandSwitch.isChecked = position == DevicePosition.IN_HAND
    }

    private fun updateBatteryModeDisplay() {
        val mode = if (sensorManager.isPerformanceModeEnabled()) "Performance Mode" else "Normal Mode"
        batteryModeText.text = mode
    }

    private fun loadPreferences() {
        upsideDownSwitch.isChecked = sharedPreferences.getBoolean("upside_down_enabled", true)
        inPocketSwitch.isChecked = sharedPreferences.getBoolean("in_pocket_enabled", true)
        inHandSwitch.isChecked = sharedPreferences.getBoolean("in_hand_enabled", true)
    }

    private fun setupSwitchListeners() {
        upsideDownSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("upside_down_enabled", isChecked).apply()
            // Update sensor manager settings
            sensorManager.setSensorSwitchingEnabled(isChecked || inPocketSwitch.isChecked || inHandSwitch.isChecked)
        }

        inPocketSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("in_pocket_enabled", isChecked).apply()
            sensorManager.setSensorSwitchingEnabled(isChecked || upsideDownSwitch.isChecked || inHandSwitch.isChecked)
        }

        inHandSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("in_hand_enabled", isChecked).apply()
            sensorManager.setSensorSwitchingEnabled(isChecked || upsideDownSwitch.isChecked || inPocketSwitch.isChecked)
        }
        
        // Update performance mode in sensor manager
        val performanceModeEnabled = sharedPreferences.getBoolean("performance_mode", false)
        sensorManager.setPerformanceMode(performanceModeEnabled)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.stopListening()
    }
}