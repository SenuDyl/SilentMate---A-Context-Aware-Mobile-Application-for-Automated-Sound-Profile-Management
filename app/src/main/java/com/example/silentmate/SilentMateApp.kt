package com.example.silentmate

import android.app.Application
import com.google.android.libraries.places.api.Places

class SilentMateApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val apiKey = getString(R.string.my_map_api_key)

        if (!Places.isInitialized()) {
            Places.initialize(this, apiKey)
        }
    }
}
