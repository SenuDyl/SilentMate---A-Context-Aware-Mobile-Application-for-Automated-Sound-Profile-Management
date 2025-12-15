package com.example.silentmate

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.silentmate.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.app.AlertDialog
import android.util.Log

class MainActivity : BaseActivity(), TutorialDialogFragment.OnTutorialDismissListener {

    private lateinit var binding: ActivityMainBinding

    private val homeFragment = HomeFragment()
    private val sensorFragment = SensorFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme preference BEFORE super.onCreate()
        // IMPORTANT: Only apply on first creation, not on configuration changes
        if (savedInstanceState == null) {
            val sharedPreferences = getSharedPreferences("SilentMatePrefs", MODE_PRIVATE)
            val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)

            val desiredMode = if (isDarkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }

            // Only set if it's different from current mode
            if (AppCompatDelegate.getDefaultNightMode() != desiredMode) {
                AppCompatDelegate.setDefaultNightMode(desiredMode)
            }
        }

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color for entire app
        setupStatusBarColor()
        setupNavigationBar()

        // Check if a specific tab was requested
        val selectedTab = intent.getStringExtra("selected_tab")
        when (selectedTab) {
            "sensor" -> {
                replaceFragment(sensorFragment)
                binding.bottomNavigation.selectedItemId = R.id.nav_sensor
            }
            "settings" -> {
                replaceFragment(settingsFragment)
                binding.bottomNavigation.selectedItemId = R.id.nav_settings
            }
            else -> {
                // Show home fragment by default
                replaceFragment(homeFragment)
                binding.bottomNavigation.selectedItemId = R.id.nav_home
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(homeFragment)
                    true
                }
                R.id.nav_sensor -> {
                    replaceFragment(sensorFragment)
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(settingsFragment)
                    true
                }
                else -> false
            }
        }

        // Show tutorial only on first launch (not on theme changes)
        showTutorialIfNeeded()
//        requestPermissionsAfterTutorial()
    }

    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        val prefs = getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("location_permission", allGranted).apply()
        if (allGranted) {
            // Proceed to request DND access after location permission is granted
            requestDndAccess()
        }
    }

    // Notification permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val prefs = getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("notifications_enabled", isGranted).apply()
    }

    // Function to request DND access
    private fun requestDndAccess() {
        // Check if the device is running Android M (API level 23) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Check if notification policy access is granted
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                // Show a confirmation dialog before opening the settings
                AlertDialog.Builder(this)
                    .setTitle("Grant DND Access")
                    .setMessage("In order to override Do Not Disturb, you need to grant access to the DND settings. Would you like to go to the settings now?")
                    .setPositiveButton("Yes") { _, _ ->
                        // If user confirms, open the DND settings screen
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        // User cancels the action, just dismiss the dialog
                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()
            } else {
                // If access is already granted, save the setting
                val prefs = getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("dnd_override", true).apply()
                // After DND access, request notification permission
                requestNotificationPermission()
            }
        } else {
            // If the device version is lower than Android M, automatically save the preference
            val prefs = getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("dnd_override", true).apply()
            // After DND access, request notification permission
            requestNotificationPermission()
        }
    }

    // Function to request Notification permissions
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Request notification permission (Android 13+)
            val notifPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (notifPermission != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // If already granted, save the setting
                val prefs = getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("notifications_enabled", true).apply()
            }
        } else {
            // For earlier versions, assume permission is already granted
            val prefs = getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("notifications_enabled", true).apply()
        }
    }

    // Main function to request all permissions after the tutorial
    private fun requestPermissionsAfterTutorial() {
        // Request location permissions first
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation != PackageManager.PERMISSION_GRANTED || coarseLocation != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // If location permissions are already granted, save and proceed to DND access
            val prefs = getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("location_permission", true).apply()
            requestDndAccess()
        }
    }


    private fun setupStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Set status bar color to match app header (darkPurple #46467A)
            window.statusBarColor = ContextCompat.getColor(this, R.color.darkPurple)

            // Set light status bar icons (white icons for dark purple background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Clear the light status bar flag to show white icons
                var flags = window.decorView.systemUiVisibility
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                window.decorView.systemUiVisibility = flags
            }
        }
    }

    private fun setupNavigationBar() {
        // Set navigation bar color to white
        window.navigationBarColor = ContextCompat.getColor(this, R.color.darkPurple)

        // Make navigation bar icons dark (for light background)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 and above
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0 and above
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    override fun onResume() {
        super.onResume()
        // Reapply status bar color when activity resumes
        setupStatusBarColor()
    }

    private fun showTutorialIfNeeded() {
        // Only show tutorial once, even across theme changes
        val prefs = getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
        val seen = prefs.getBoolean("tutorial_seen", false)

        // Don't show tutorial if it's been seen OR if dark mode setting exists (app was used before)
        val darkModeSet = prefs.contains("dark_mode")
        Log.d("MainActivity", "tutorial_seen: ${prefs.getBoolean("tutorial_seen", false)}")
        Log.d("MainActivity", "dark_mode set: ${prefs.contains("dark_mode")}")

        if (!seen && !darkModeSet) {
            lifecycleScope.launch {
                delay(100)
                if (!isFinishing && !isDestroyed) {
                    val tutorialDialog = TutorialDialogFragment()
                    tutorialDialog.isCancelable = false
                    tutorialDialog.show(supportFragmentManager, "tutorial_dialog")

                    // After showing tutorial, request permissions
//                    tutorialDialog.lifecycleScope.launchWhenResumed {
//                        requestPermissionsAfterTutorial()
//                        prefs.edit().putBoolean("tutorial_seen", true).apply()
//                    }

                }
            }
        }
        // Request permissions regardless of tutorial state
//        lifecycleScope.launch {
//            delay(200) // slight delay to ensure UI is ready
//            requestPermissionsAfterTutorial()
//        }
    }

    // Implement the callback from the tutorial dialog
    override fun onTutorialDismissed() {
        // Once the tutorial is dismissed, request the permissions
        lifecycleScope.launch {
            delay(200) // Slight delay to ensure the UI is ready
            requestPermissionsAfterTutorial()
        }
    }


    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commitNow()
    }
}