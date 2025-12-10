package com.example.silentmate

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.silentmate.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.WindowInsetsController

class MainActivity : BaseActivity() {

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

        if (!seen && !darkModeSet) {
            lifecycleScope.launch {
                delay(100)
                if (!isFinishing && !isDestroyed) {
                    val tutorialDialog = TutorialDialogFragment()
                    tutorialDialog.isCancelable = false
                    tutorialDialog.show(supportFragmentManager, "tutorial_dialog")
                }
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commitNow()
    }
}