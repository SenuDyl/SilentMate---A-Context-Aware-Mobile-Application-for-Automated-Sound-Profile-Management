package com.example.silentmate.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.example.silentmate.database.EventDatabaseHelper
import com.example.silentmate.utils.AudioProfileUtils

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val geofencingEvent = intent?.let { GeofencingEvent.fromIntent(it) }
        if (geofencingEvent != null) {
            if (geofencingEvent.hasError()) {
                Log.e("GeofenceReceiver", "Geofence error: ${geofencingEvent.errorCode}")
                return
            }
        }

        val transition = geofencingEvent?.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            triggeringGeofences?.forEach { g ->
                val id = g.requestId.toLongOrNull()
                id?.let {
                    // Load event details to know mode and then apply audio
                    val db = EventDatabaseHelper(context)
                    val ev = db.getEventById(it.toInt())
                    if (ev != null) {
                        AudioProfileUtils.applyAudioMode(context, ev.action)
                    }
                    // remove geofence after it triggered
                    val gm = GeofenceManager(context)
                    gm.removeGeofence(it)
                }
            }
        }
    }
}
