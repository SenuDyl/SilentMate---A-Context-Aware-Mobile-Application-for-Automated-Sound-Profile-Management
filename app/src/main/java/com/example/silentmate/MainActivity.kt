package com.example.silentmate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.silentmate.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val homeFragment = HomeFragment()
    private val sensorFragment = SensorFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
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

        binding.addEventFab.setOnClickListener {
            val intent = Intent(this, AddEventActivity::class.java)
            startActivity(intent)
        }

        // Disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}