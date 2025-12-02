package com.example.silentmate.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.example.silentmate.model.Event
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    companion object {
        const val GEOFENCE_PENDING_INTENT_ACTION = "com.example.silentmate.GEOFENCE_TRANSITION"
    }

    private fun getGeofencePendingIntent(eventId: Long): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = GEOFENCE_PENDING_INTENT_ACTION
            putExtra("eventId", eventId)
        }
        return PendingIntent.getBroadcast(
            context,
            eventId.toInt(), // requestCode per event
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun registerGeofence(event: Event, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        // Convert LocalTime to milliseconds since the epoch
        val endTimeMillis =
            LocalDateTime.now().with(event.endTime).atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli()
        val durationMillis = endTimeMillis - System.currentTimeMillis().coerceAtLeast(0L)

        // Check for location permissions
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val geofence = Geofence.Builder()
                .setRequestId(event.id.toString())
                .setCircularRegion(event.location.latitude, event.location.longitude, 100f)
                .setExpirationDuration(durationMillis)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(0) // don't trigger immediately
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(request, getGeofencePendingIntent(event.id.toLong()))
                .addOnSuccessListener { onSuccess?.invoke() }
                .addOnFailureListener { e -> onFailure?.invoke(Exception(e)) }
        } else {
            // Permissions are not granted, request them
//            ActivityCompat.requestPermissions(activity,
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
//                REQUEST_LOCATION_PERMISSION_CODE)
        }
    }

    fun removeGeofence(eventId: Long, onComplete: (() -> Unit)? = null) {
        geofencingClient.removeGeofences(listOf(eventId.toString()))
            .addOnCompleteListener { onComplete?.invoke() }
    }

}
