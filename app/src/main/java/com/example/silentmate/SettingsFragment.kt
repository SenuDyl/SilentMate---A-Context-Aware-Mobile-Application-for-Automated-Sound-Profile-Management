package com.example.silentmate

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationsSwitch: SwitchCompat
    private lateinit var locationSwitch: SwitchCompat
    private lateinit var dndOverrideSwitch: SwitchCompat
    private lateinit var sensorAccessSwitch: SwitchCompat
    private lateinit var performanceModeSwitch: SwitchCompat

    // Permission launchers
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        locationSwitch.isChecked = allGranted
        sharedPreferences.edit().putBoolean("location_permission", allGranted).apply()

        if (allGranted) {
            Toast.makeText(requireContext(), "Location permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationsSwitch.isChecked = isGranted
        sharedPreferences.edit().putBoolean("notifications_enabled", isGranted).apply()

        if (isGranted) {
            Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

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

        // Apply purple color to all switches
        applySwitchColors()

        // Set up back button click listener
        val backButton = view.findViewById<ImageView>(R.id.backButton)
        backButton?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        }

        // Load current permission states
        loadPermissionStates()

        // Set up switch listeners
        setupSwitchListeners()

        return view
    }

    private fun applySwitchColors() {
        val purpleColor = Color.parseColor("#5B5380")
        val lightPurple = Color.parseColor("#B8A8D8")

        // Create ColorStateLists for thumb
        val thumbStates = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val thumbColors = intArrayOf(
            purpleColor,  // Purple when ON
            Color.WHITE   // White when OFF
        )
        val thumbColorStateList = ColorStateList(thumbStates, thumbColors)

        // Create ColorStateLists for track
        val trackColors = intArrayOf(
            lightPurple,     // Light purple when ON
            Color.LTGRAY     // Gray when OFF
        )
        val trackColorStateList = ColorStateList(thumbStates, trackColors)

        // Apply to all switches
        applySwitchColorStateList(notificationsSwitch, thumbColorStateList, trackColorStateList)
        applySwitchColorStateList(locationSwitch, thumbColorStateList, trackColorStateList)
        applySwitchColorStateList(dndOverrideSwitch, thumbColorStateList, trackColorStateList)
        applySwitchColorStateList(sensorAccessSwitch, thumbColorStateList, trackColorStateList)
        applySwitchColorStateList(performanceModeSwitch, thumbColorStateList, trackColorStateList)
    }

    private fun applySwitchColorStateList(
        switch: SwitchCompat,
        thumbColors: ColorStateList,
        trackColors: ColorStateList
    ) {
        switch.thumbTintList = thumbColors
        switch.trackTintList = trackColors
    }

    private fun loadPermissionStates() {
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPerm = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            notificationsSwitch.isChecked = hasNotificationPerm
        } else {
            notificationsSwitch.isChecked = true // Auto-granted on older versions
        }

        // Check location permission
        val hasFineLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        locationSwitch.isChecked = hasFineLocation && hasCoarseLocation

        // Check DND access
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val hasDndAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
        dndOverrideSwitch.isChecked = hasDndAccess

        // Sensor access (always available as system sensors)
        sensorAccessSwitch.isChecked = true

        // Performance mode from preferences
        performanceModeSwitch.isChecked = sharedPreferences.getBoolean("performance_mode", false)
    }

    private fun setupSwitchListeners() {
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestNotificationPermission()
            } else {
                sharedPreferences.edit().putBoolean("notifications_enabled", false).apply()
                Toast.makeText(requireContext(), "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }

        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestLocationPermission()
            } else {
                sharedPreferences.edit().putBoolean("location_permission", false).apply()
                Toast.makeText(requireContext(),
                    "Location disabled. Go to App Settings to revoke permission.",
                    Toast.LENGTH_LONG).show()
            }
        }

        dndOverrideSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestDndAccess()
            } else {
                sharedPreferences.edit().putBoolean("dnd_override", false).apply()
                Toast.makeText(requireContext(),
                    "DND Override disabled. Go to App Settings to revoke access.",
                    Toast.LENGTH_LONG).show()
            }
        }

        sensorAccessSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("sensor_access", isChecked).apply()

            if (isChecked) {
                Toast.makeText(requireContext(), "Sensor access enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Sensor access disabled", Toast.LENGTH_SHORT).show()
            }
        }

        performanceModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("performance_mode", isChecked).apply()

            val mode = if (isChecked) "Performance Mode" else "Normal Mode"
            val description = if (isChecked) {
                "Reduced sensor frequency for better battery life"
            } else {
                "Standard sensor frequency for real-time detection"
            }

            Toast.makeText(requireContext(), "$mode: $description", Toast.LENGTH_LONG).show()
            Log.d("SettingsFragment", "Performance mode changed: $mode - $description")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                sharedPreferences.edit().putBoolean("notifications_enabled", true).apply()
                Toast.makeText(requireContext(), "Notifications already enabled", Toast.LENGTH_SHORT).show()
            }
        } else {
            sharedPreferences.edit().putBoolean("notifications_enabled", true).apply()
            Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationPermission() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation || !hasCoarseLocation) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            sharedPreferences.edit().putBoolean("location_permission", true).apply()
            Toast.makeText(requireContext(), "Location already enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestDndAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
                Toast.makeText(
                    requireContext(),
                    "Please grant Do Not Disturb access to Silent Mate",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                sharedPreferences.edit().putBoolean("dnd_override", true).apply()
                Toast.makeText(requireContext(), "DND access already granted", Toast.LENGTH_SHORT).show()
            }
        } else {
            sharedPreferences.edit().putBoolean("dnd_override", true).apply()
            Toast.makeText(requireContext(), "DND Override enabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission states when returning to the fragment
        loadPermissionStates()
    }
}