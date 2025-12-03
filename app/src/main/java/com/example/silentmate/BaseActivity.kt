package com.example.silentmate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

/**
 * Base Activity that applies theme before any activity is created
 * All activities should extend this instead of AppCompatActivity
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme BEFORE super.onCreate()
        applyTheme()
        super.onCreate(savedInstanceState)
    }

    private fun applyTheme() {
        val sharedPreferences = getSharedPreferences("SilentMatePrefs", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)

        val desiredMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        // Only set if different from current mode to avoid unnecessary recreation
        if (AppCompatDelegate.getDefaultNightMode() != desiredMode) {
            AppCompatDelegate.setDefaultNightMode(desiredMode)
        }
    }
}