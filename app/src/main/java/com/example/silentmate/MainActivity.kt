package com.example.silentmate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.silentmate.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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