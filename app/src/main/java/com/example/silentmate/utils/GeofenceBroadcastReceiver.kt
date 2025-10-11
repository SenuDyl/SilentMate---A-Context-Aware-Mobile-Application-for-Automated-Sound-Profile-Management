package com.example.silentmate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null) {
            if (geofencingEvent.hasError()) return
        }

        if (geofencingEvent != null) {
            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                val requestId = geofencingEvent.triggeringGeofences?.get(0)?.requestId
                Log.d("Geofence", "Entered geofence for event id: $requestId")

                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                // For demo: set to vibrate. Later, fetch event action from DB
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
        }
    }
}
