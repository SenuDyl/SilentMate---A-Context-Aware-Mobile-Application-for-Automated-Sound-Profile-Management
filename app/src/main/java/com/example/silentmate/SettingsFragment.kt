package com.example.silentmate

import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

const val KEY_EVENT_AUDIO_ENABLED = "event_audio_switch"

class SettingsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationsSwitch: SwitchCompat
    private lateinit var locationSwitch: SwitchCompat
    private lateinit var dndOverrideSwitch: SwitchCompat
    private lateinit var sensorAccessSwitch: SwitchCompat
    private lateinit var performanceModeSwitch: SwitchCompat
    private lateinit var darkModeSwitch: SwitchCompat
    private lateinit var eventAudioSwitch: SwitchCompat

    // Flags to prevent listeners from firing during programmatic changes
    private var isUpdatingProgrammatically = false

    // Permission launchers
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        updateSwitchWithoutListener(locationSwitch, allGranted)
        sharedPreferences.edit().putBoolean("location_permission", allGranted).apply()

//        if (allGranted) {
//            Toast.makeText(requireContext(), "Location permission granted", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
//        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updateSwitchWithoutListener(notificationsSwitch, isGranted)
        sharedPreferences.edit().putBoolean("notifications_enabled", isGranted).apply()

//        if (isGranted) {
//            Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show()
//        }
    }

    @SuppressLint("MissingInflatedId")
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
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch)
        eventAudioSwitch = view.findViewById(R.id.eventAudioSwitch)

        // Apply your app's color scheme to all switches
        applySwitchColors()

        // Set up back button click listener
        val backButton = view.findViewById<ImageView>(R.id.backButton)
        backButton?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        }

        // Set up switch listeners FIRST
        setupSwitchListeners()

        // Set up the User Guide TextView click listener
        val userGuideTextView: TextView = view.findViewById(R.id.userGuideText)
        userGuideTextView.setOnClickListener {
            onUserGuideClicked(it)
        }

        return view
    }

    private fun applySwitchColors() {
        // Use your app's color scheme: Purple and Pink
        val purpleColor = Color.parseColor("#7766C6")  // Purple
        val pinkColor = Color.parseColor("#F9B0C3")    // Pink
        val lightPurple = Color.parseColor("#B8A8E6")  // Light Purple
        val lightPink = Color.parseColor("#FDE0E9")    // Light Pink

        // Create ColorStateLists for thumb (the circle)
        val thumbStates = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val thumbColors = intArrayOf(
            pinkColor,    // Pink when ON
            purpleColor   // Purple when OFF
        )
        val thumbColorStateList = ColorStateList(thumbStates, thumbColors)

        // Create ColorStateLists for track (the background)
        val trackColors = intArrayOf(
            lightPink,    // Light pink when ON
            lightPurple   // Light purple when OFF
        )
        val trackColorStateList = ColorStateList(thumbStates, trackColors)

        // Apply to all switches
        applySwitchColorStateList(notificationsSwitch, thumbColorStateList, trackColorStateList)
        applySwitchColorStateList(locationSwitch, thumbColorStateList, trackColorStateList)
        applySwitchColorStateList(dndOverrideSwitch, thumbColorStateList, trackColorStateList)
        applySwitchColorStateList(sensorAccessSwitch, thumbColorStateList, trackColorStateList)
        applySwitchColorStateList(performanceModeSwitch, thumbColorStateList, trackColorStateList)
        applySwitchColorStateList(darkModeSwitch, thumbColorStateList, trackColorStateList)
        applySwitchColorStateList(eventAudioSwitch, thumbColorStateList, trackColorStateList)
    }

    private fun applySwitchColorStateList(
        switch: SwitchCompat,
        thumbColors: ColorStateList,
        trackColors: ColorStateList
    ) {
        switch.thumbTintList = thumbColors
        switch.trackTintList = trackColors
    }

    // Helper function to update switch state without triggering listener
    private fun updateSwitchWithoutListener(switch: SwitchCompat, checked: Boolean) {
        isUpdatingProgrammatically = true
        switch.isChecked = checked
        isUpdatingProgrammatically = false
    }

    private fun loadPermissionStates() {
        // Use helper function to prevent triggering listeners
        isUpdatingProgrammatically = true

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

        // Dark mode from preferences
        darkModeSwitch.isChecked = sharedPreferences.getBoolean("dark_mode", false)

        isUpdatingProgrammatically = false

        eventAudioSwitch.isChecked = sharedPreferences.getBoolean(KEY_EVENT_AUDIO_ENABLED, true) // default ON

    }

    private fun setupSwitchListeners() {
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingProgrammatically) return@setOnCheckedChangeListener

            if (isChecked) {
                requestNotificationPermission()
            } else {
                sharedPreferences.edit().putBoolean("notifications_enabled", false).apply()
//                Toast.makeText(requireContext(), "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }

        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingProgrammatically) return@setOnCheckedChangeListener

            if (isChecked) {
                requestLocationPermission()
            } else {
                sharedPreferences.edit().putBoolean("location_permission", false).apply()
//                Toast.makeText(requireContext(),
//                    "Location disabled. Go to App Settings to revoke permission.",
//                    Toast.LENGTH_LONG).show()
            }
        }

        dndOverrideSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingProgrammatically) return@setOnCheckedChangeListener

            if (isChecked) {
                requestDndAccess()
            } else {
                sharedPreferences.edit().putBoolean("dnd_override", false).apply()
//                Toast.makeText(requireContext(),
//                    "DND Override disabled. Go to App Settings to revoke access.",
//                    Toast.LENGTH_LONG).show()
            }
        }

        sensorAccessSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingProgrammatically) return@setOnCheckedChangeListener

            sharedPreferences.edit().putBoolean("sensor_access", isChecked).apply()

//            if (isChecked) {
//                Toast.makeText(requireContext(), "Sensor access enabled", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(requireContext(), "Sensor access disabled", Toast.LENGTH_SHORT).show()
//            }
        }

        performanceModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingProgrammatically) return@setOnCheckedChangeListener

            sharedPreferences.edit().putBoolean("performance_mode", isChecked).apply()

            val mode = if (isChecked) "Performance Mode" else "Normal Mode"
            val description = if (isChecked) {
                "Reduced sensor frequency for better battery life"
            } else {
                "Standard sensor frequency for real-time detection"
            }

//            Toast.makeText(requireContext(), "$mode: $description", Toast.LENGTH_LONG).show()
            Log.d("SettingsFragment", "Performance mode changed: $mode - $description")
        }

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingProgrammatically) return@setOnCheckedChangeListener

            // Save preference
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()

            // Get current night mode
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            val desiredMode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }

            // Only apply if mode is changing
            if (currentMode != desiredMode) {
                AppCompatDelegate.setDefaultNightMode(desiredMode)

                val message = if (isChecked) "Dark mode enabled" else "Light mode enabled"
//                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        eventAudioSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingProgrammatically) return@setOnCheckedChangeListener

            sharedPreferences.edit().putBoolean(KEY_EVENT_AUDIO_ENABLED, isChecked).apply()
            val message = if (isChecked) "Auto Audio Mode enabled" else "Auto Audio Mode disabled"
            Log.d("SettingsFragment", message)
//    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
//                Toast.makeText(requireContext(), "Notifications already enabled", Toast.LENGTH_SHORT).show()
            }
        } else {
            sharedPreferences.edit().putBoolean("notifications_enabled", true).apply()
//            Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show()
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
//            Toast.makeText(requireContext(), "Location already enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestDndAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
//                Toast.makeText(
//                    requireContext(),
//                    "Please grant Do Not Disturb access to Silent Mate",
//                    Toast.LENGTH_LONG
//                ).show()
            } else {
                sharedPreferences.edit().putBoolean("dnd_override", true).apply()
//                Toast.makeText(requireContext(), "DND access already granted", Toast.LENGTH_SHORT).show()
            }
        } else {
            sharedPreferences.edit().putBoolean("dnd_override", true).apply()
//            Toast.makeText(requireContext(), "DND Override enabled", Toast.LENGTH_SHORT).show()
        }
    }

    fun onUserGuideClicked(view: View) {
        // Create a new instance of the TutorialDialogFragment
        val tutorialDialog = TutorialDialogFragment()

        // Pass a flag to indicate this was launched from the settings
        val bundle = Bundle()
        bundle.putBoolean("isFromSettings", true) // Flag to indicate it's from Settings

        tutorialDialog.arguments = bundle
        tutorialDialog.isCancelable = false // Prevent dismissing by clicking outside
        tutorialDialog.show(parentFragmentManager, "tutorial_dialog")
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission states when returning to the fragment
        // The isUpdatingProgrammatically flag will prevent listeners from firing
        loadPermissionStates()
    }
}