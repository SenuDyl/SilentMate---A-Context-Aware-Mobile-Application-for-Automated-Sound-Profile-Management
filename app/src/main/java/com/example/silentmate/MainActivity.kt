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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val homeFragment = HomeFragment()
    private val sensorFragment = SensorFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme preference BEFORE super.onCreate()
        val sharedPreferences = getSharedPreferences("SilentMatePrefs", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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
                binding.addEventFab.show()
            }
            "settings" -> {
                replaceFragment(settingsFragment)
                binding.bottomNavigation.selectedItemId = R.id.nav_settings
                binding.addEventFab.hide()
            }
            else -> {
                // Show home fragment by default
                replaceFragment(homeFragment)
                binding.bottomNavigation.selectedItemId = R.id.nav_home
                binding.addEventFab.show()
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(homeFragment)
                    binding.addEventFab.show()
                    true
                }
                R.id.nav_sensor -> {
                    replaceFragment(sensorFragment)
                    binding.addEventFab.show()
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(settingsFragment)
                    binding.addEventFab.hide()
                    true
                }
                else -> false
            }
        }

//        val intent = Intent(this, TestDatabaseActivity::class.java)
//        startActivity(intent)
//        finish()

        // Disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Show tutorial if it hasn't been seen yet
        showTutorialIfNeeded()
    }

    private fun showTutorialIfNeeded() {
        val prefs = getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
        val seen = prefs.getBoolean("tutorial_seen", false)
        if (!seen) {
            // Use lifecycleScope to ensure the activity is running
            lifecycleScope.launch {
                // Small delay to let the activity fully attach
                delay(100) // 100ms is usually enough
                if (!isFinishing && !isDestroyed) {
                    val tutorialDialog = TutorialDialogFragment()
                    tutorialDialog.isCancelable = false
                    tutorialDialog.show(supportFragmentManager, "tutorial_dialog")
                }
            }
        }
        binding.addEventFab.setOnClickListener {
            val intent = Intent(this, AddEventActivity::class.java)
            startActivity(intent)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commitNow()
    }
}