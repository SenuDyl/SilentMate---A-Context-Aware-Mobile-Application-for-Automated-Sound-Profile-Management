package com.example.silentmate

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
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

    private var isUpdatingFromSensor = false
    private val handler = Handler(Looper.getMainLooper())

    private val debugRunnable = object : Runnable {
        override fun run() {
            updateDebugInfo()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_sensor, container, false)

        sharedPreferences = requireContext().getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
        sensorManager = SilentMateSensorManager(requireContext())

        initializeViews(view)
        applySwitchColors()

        val backButton = view.findViewById<ImageView>(R.id.backButton)
        backButton?.setOnClickListener {
            sensorManager.stopListening()
            handler.removeCallbacks(debugRunnable)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        }

        loadPreferences()
        setupSwitchListeners()
        startSensorMonitoring()
        handler.post(debugRunnable)

        return view
    }

    private fun initializeViews(view: View) {
        upsideDownSwitch = view.findViewById(R.id.upsideDownSwitch)
        inPocketSwitch = view.findViewById(R.id.inPocketSwitch)
        inHandSwitch = view.findViewById(R.id.inHandSwitch)
        currentPositionText = view.findViewById(R.id.currentPositionText)
        currentProfileText = view.findViewById(R.id.currentProfileText)
        batteryModeText = view.findViewById(R.id.batteryModeText)
    }

    private fun updateDebugInfo() {
        // Get sensor data from sensor manager (Z-axis, proximity, speed)
        val debugInfo = sensorManager.getDebugInfo()

        // Check if any detection is enabled
        val anyDetectionEnabled = upsideDownSwitch.isChecked ||
                inPocketSwitch.isChecked ||
                inHandSwitch.isChecked

        // Update battery mode text with sensor data
        batteryModeText.text = debugInfo

        // Update position display in real-time
        val currentPosition = sensorManager.getCurrentPosition()
        val currentProfile = sensorManager.getCurrentAudioProfile()
        updatePositionDisplay(currentPosition, currentProfile, silent = true)
    }

    private fun applySwitchColors() {
        val purpleColor = ContextCompat.getColor(requireContext(), R.color.purple)
        val pinkColor = ContextCompat.getColor(requireContext(), R.color.pink)
        val lightPurple = Color.parseColor("#B8A8E6")
        val lightPink = Color.parseColor("#FDE0E9")

        val thumbStates = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val thumbColors = intArrayOf(pinkColor, purpleColor)
        val thumbColorStateList = ColorStateList(thumbStates, thumbColors)

        val trackColors = intArrayOf(lightPink, lightPurple)
        val trackColorStateList = ColorStateList(thumbStates, trackColors)

        applySwitchColorStateList(upsideDownSwitch, thumbColorStateList, trackColorStateList)
        applySwitchColorStateList(inPocketSwitch, thumbColorStateList, trackColorStateList)
        applySwitchColorStateList(inHandSwitch, thumbColorStateList, trackColorStateList)
    }

    private fun applySwitchColorStateList(
        switch: SwitchCompat,
        thumbColors: ColorStateList,
        trackColors: ColorStateList
    ) {
        switch.thumbTintList = thumbColors
        switch.trackTintList = trackColors
    }

    private fun startSensorMonitoring() {
        sensorManager.startListening { position, profile ->
            // Only update if detection is enabled for this position
            val isDetectionEnabled = when (position) {
                DevicePosition.UPSIDE_DOWN -> upsideDownSwitch.isChecked
                DevicePosition.IN_POCKET -> inPocketSwitch.isChecked
                DevicePosition.IN_HAND -> inHandSwitch.isChecked
                DevicePosition.UNKNOWN -> false
            }

            if (isDetectionEnabled) {
                updateStatusDisplay(position, profile)
            }
        }
    }

    private fun updatePositionDisplay(position: DevicePosition, profile: AudioProfile, silent: Boolean = false) {
        val positionText = when (position) {
            DevicePosition.UPSIDE_DOWN -> "📱 On Desk (Face Up)"
            DevicePosition.IN_POCKET -> "👖 In Pocket"
            DevicePosition.IN_HAND -> "🤚 In Hand"
            DevicePosition.UNKNOWN -> "Detection not enabled"
        }

        val profileText = when (profile) {
            AudioProfile.SILENT -> "Silent Mode 🔇"
            AudioProfile.VIBRATION -> "Vibration Mode 📳"
            AudioProfile.GENERAL -> "General Mode 🔊"
        }

        currentPositionText.text = positionText
        currentProfileText.text = profileText
    }

    private fun updateStatusDisplay(position: DevicePosition, profile: AudioProfile) {
        // Update the position and profile displays
        updatePositionDisplay(position, profile)

        val notificationMessage = when (position) {
            DevicePosition.UPSIDE_DOWN -> "On Desk 📱 detected - Silent Mode 🔇"
            DevicePosition.IN_POCKET -> "In Pocket 👖 detected - Vibration Mode 📳"
            DevicePosition.IN_HAND -> "In Hand 🤚 detected - General Mode 🔊"
            DevicePosition.UNKNOWN -> "Unknown position detected"
        }
        Toast.makeText(requireContext(), notificationMessage, Toast.LENGTH_SHORT).show()

        // Do NOT automatically update switch states from sensor detection
        // Switches should only be toggled manually by the user
    }

    private fun vibrateDevice() {
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(200)
        }
    }

    private fun loadPreferences() {
        val upsideDownEnabled = sharedPreferences.getBoolean("upside_down_enabled", true)
        val inPocketEnabled = sharedPreferences.getBoolean("in_pocket_enabled", true)
        val inHandEnabled = sharedPreferences.getBoolean("in_hand_enabled", true)

        // Set switch states without triggering listeners
        isUpdatingFromSensor = true
        upsideDownSwitch.isChecked = upsideDownEnabled
        inPocketSwitch.isChecked = inPocketEnabled
        inHandSwitch.isChecked = inHandEnabled
        isUpdatingFromSensor = false

        sensorManager.setFeatureEnabled(DevicePosition.UPSIDE_DOWN, upsideDownEnabled)
        sensorManager.setFeatureEnabled(DevicePosition.IN_POCKET, inPocketEnabled)
        sensorManager.setFeatureEnabled(DevicePosition.IN_HAND, inHandEnabled)
    }

    private fun setupSwitchListeners() {
        upsideDownSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSensor) {
                // Vibrate when manually toggled
                vibrateDevice()

                sharedPreferences.edit().putBoolean("upside_down_enabled", isChecked).apply()
                sensorManager.setFeatureEnabled(DevicePosition.UPSIDE_DOWN, isChecked)
                val message = if (isChecked) "On Desk detection enabled 📱" else "On Desk detection disabled"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        inPocketSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSensor) {
                // Vibrate when manually toggled
                vibrateDevice()

                sharedPreferences.edit().putBoolean("in_pocket_enabled", isChecked).apply()
                sensorManager.setFeatureEnabled(DevicePosition.IN_POCKET, isChecked)
                val message = if (isChecked) "In Pocket detection enabled 👖" else "In Pocket detection disabled"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        inHandSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSensor) {
                // Vibrate when manually toggled
                vibrateDevice()

                sharedPreferences.edit().putBoolean("in_hand_enabled", isChecked).apply()
                sensorManager.setFeatureEnabled(DevicePosition.IN_HAND, isChecked)
                val message = if (isChecked) "In Hand detection enabled 🤚" else "In Hand detection disabled"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        val performanceModeEnabled = sharedPreferences.getBoolean("performance_mode", false)
        sensorManager.setPerformanceMode(performanceModeEnabled)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(debugRunnable)
        sensorManager.stopListening()
    }
}