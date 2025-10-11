package com.example.silentmate

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.silentmate.model.Event
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest

object GeofenceHelper {

    fun addGeofence(context: Context, client: GeofencingClient, event: Event) {
        val geofence = Geofence.Builder()
            .setRequestId(event.id.toString())
            .setCircularRegion(
                event.location.latitude,
                event.location.longitude,
                100f
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        client.addGeofences(request, intent)
            .addOnSuccessListener { Log.d("Geofence", "Geofence added for ${event.title}") }
            .addOnFailureListener { e -> Log.e("Geofence", "Failed to add geofence", e) }
    }
}
