package com.example.silentmate

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        val debugInfo = sensorManager.getDebugInfo()
        batteryModeText.text = debugInfo
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
            updateStatusDisplay(position, profile)
        }
    }

    private fun updateStatusDisplay(position: DevicePosition, profile: AudioProfile) {
        val positionText = when (position) {
            DevicePosition.UPSIDE_DOWN -> "Upside Down 🔽"
            DevicePosition.IN_POCKET -> "In Pocket 👖"
            DevicePosition.IN_HAND -> "In Hand 🤚"
            DevicePosition.UNKNOWN -> "On Desk 📱"
        }

        val profileText = when (profile) {
            AudioProfile.SILENT -> "Silent Mode 🔇"
            AudioProfile.VIBRATION -> "Vibration Mode 📳"
            AudioProfile.GENERAL -> "General Mode 🔊"
        }

        currentPositionText.text = positionText
        currentProfileText.text = profileText

        val notificationMessage = "$positionText detected - $profileText"
        Toast.makeText(requireContext(), notificationMessage, Toast.LENGTH_SHORT).show()

        updateSwitchStatesFromPosition(position)
    }

    private fun updateSwitchStatesFromPosition(position: DevicePosition) {
        isUpdatingFromSensor = true

        upsideDownSwitch.isChecked = false
        inPocketSwitch.isChecked = false
        inHandSwitch.isChecked = false

        when (position) {
            DevicePosition.UPSIDE_DOWN -> upsideDownSwitch.isChecked = true
            DevicePosition.IN_POCKET -> inPocketSwitch.isChecked = true
            DevicePosition.IN_HAND -> inHandSwitch.isChecked = true
            DevicePosition.UNKNOWN -> {}
        }

        view?.postDelayed({
            isUpdatingFromSensor = false
        }, 100)
    }

    private fun loadPreferences() {
        val upsideDownEnabled = sharedPreferences.getBoolean("upside_down_enabled", true)
        val inPocketEnabled = sharedPreferences.getBoolean("in_pocket_enabled", true)
        val inHandEnabled = sharedPreferences.getBoolean("in_hand_enabled", true)

        sensorManager.setFeatureEnabled(DevicePosition.UPSIDE_DOWN, upsideDownEnabled)
        sensorManager.setFeatureEnabled(DevicePosition.IN_POCKET, inPocketEnabled)
        sensorManager.setFeatureEnabled(DevicePosition.IN_HAND, inHandEnabled)
    }

    private fun setupSwitchListeners() {
        upsideDownSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSensor) {
                sharedPreferences.edit().putBoolean("upside_down_enabled", isChecked).apply()
                sensorManager.setFeatureEnabled(DevicePosition.UPSIDE_DOWN, isChecked)
                val message = if (isChecked) "Upside Down detection enabled 🔽" else "Upside Down detection disabled"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        inPocketSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSensor) {
                sharedPreferences.edit().putBoolean("in_pocket_enabled", isChecked).apply()
                sensorManager.setFeatureEnabled(DevicePosition.IN_POCKET, isChecked)
                val message = if (isChecked) "In Pocket detection enabled 👖" else "In Pocket detection disabled"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        inHandSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromSensor) {
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