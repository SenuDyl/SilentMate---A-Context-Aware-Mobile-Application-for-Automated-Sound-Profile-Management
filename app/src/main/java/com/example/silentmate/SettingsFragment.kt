package com.example.silentmate

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationsSwitch: SwitchCompat
    private lateinit var locationSwitch: SwitchCompat
    private lateinit var dndOverrideSwitch: SwitchCompat
    private lateinit var sensorAccessSwitch: SwitchCompat
    private lateinit var performanceModeSwitch: SwitchCompat

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        
        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
        
        // Initialize switches
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch)
        locationSwitch = view.findViewById(R.id.locationSwitch)
        dndOverrideSwitch = view.findViewById(R.id.dndOverrideSwitch)
        sensorAccessSwitch = view.findViewById(R.id.sensorAccessSwitch)
        performanceModeSwitch = view.findViewById(R.id.performanceModeSwitch)
        
        // Set up back button click listener
        val backButton = view.findViewById<ImageView>(R.id.backButton)
        backButton?.setOnClickListener {
            // Navigate back to home fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        }
        
        // Load saved preferences
        loadPreferences()
        
        // Set up switch listeners
        setupSwitchListeners()
        
        return view
    }

    private fun loadPreferences() {
        notificationsSwitch.isChecked = sharedPreferences.getBoolean("notifications_enabled", true)
        locationSwitch.isChecked = sharedPreferences.getBoolean("location_permission", true)
        dndOverrideSwitch.isChecked = sharedPreferences.getBoolean("dnd_override", true)
        sensorAccessSwitch.isChecked = sharedPreferences.getBoolean("sensor_access", true)
        performanceModeSwitch.isChecked = sharedPreferences.getBoolean("performance_mode", true)
    }

    private fun setupSwitchListeners() {
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
            // Here you can add logic to enable/disable notifications
        }

        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("location_permission", isChecked).apply()
            // Here you can add logic to request/revoke location permission
        }

        dndOverrideSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("dnd_override", isChecked).apply()
            // Here you can add logic to handle DND override
        }

        sensorAccessSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("sensor_access", isChecked).apply()
            // Here you can add logic to handle sensor access
        }

        performanceModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("performance_mode", isChecked).apply()
            
            // Show performance mode info
            val mode = if (isChecked) "Performance Mode" else "Normal Mode"
            val description = if (isChecked) {
                "Reduced sensor frequency for better battery life"
            } else {
                "Standard sensor frequency for real-time detection"
            }
            
            // You can show a toast or update UI to inform user about the change
            Log.d("SettingsFragment", "Performance mode changed: $mode - $description")
        }
    }
}