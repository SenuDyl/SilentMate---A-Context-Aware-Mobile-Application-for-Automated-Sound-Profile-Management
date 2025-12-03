package com.example.silentmate.worker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.silentmate.R
import com.google.android.gms.location.LocationServices
import com.example.silentmate.database.EventDatabaseHelper
import com.example.silentmate.geofence.GeofenceManager
import com.example.silentmate.model.Event
import com.example.silentmate.model.Recurrence
import com.example.silentmate.utils.AudioProfileUtils
import kotlinx.coroutines.tasks.await
import com.example.silentmate.utils.computeNextOccurrence
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.*

class EventStartWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_EVENT_ID = "eventId"
    }

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val eventId = inputData.getLong(KEY_EVENT_ID, -1L)
        if (eventId == -1L) return Result.failure()

        val db = EventDatabaseHelper(applicationContext)
        val event: Event = db.getEventById(eventId.toInt()) ?: return Result.failure()

        // Try one-shot location check using fused location provider
        val fusedClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        return try {
            // getCurrentLocation is preferred for one-shot (requires proper permissions)
            val loc: Location? = try {
                fusedClient.lastLocation.await() // fast fallback; may be null
            } catch (e: Exception) {
                null
            }

            val userLocation: Location? = loc ?: fusedClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, null
            ).await()

            if (userLocation != null && insideGeofence(userLocation.latitude, userLocation.longitude, event)) {
                // user is at event location -> switch now
                AudioProfileUtils.applyAudioMode(applicationContext, event.action)

                // mark that this event actually applied a profile
                val prefs = applicationContext.getSharedPreferences("event_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("event_${event.id}_applied", true).apply()

                sendNotification(event)
                Result.success()
            } else {
                // Not at event location: register geofence that expires at event.endTime
                val geofenceManager = GeofenceManager(applicationContext)
                geofenceManager.registerGeofence(event,
                    onSuccess = { Log.d("EventStartWorker", "Geofence registered for event ${event.id}") },
                    onFailure = { ex -> Log.e("EventStartWorker", "Geofence register failed: ${ex.message}") }
                )

                // Did NOT apply audio
                val prefs = applicationContext.getSharedPreferences("event_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("event_${event.id}_applied", false).apply()

                Result.success()
            }
        } catch (e: Exception) {
            Log.e("EventStartWorker", "location error ${e.message}")
            Result.failure()
        }
    }

    // Check if location permissions are granted
    private fun hasLocationPermissions(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED || coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun insideGeofence(lat: Double, lng: Double, event: Event): Boolean {
        val d = distanceMeters(lat, lng, event.location.latitude, event.location.longitude)
        return d <= 100f
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun sendNotification(event: Event) {
        val channelId = "event_notifications"
        val channelName = "Event Notifications"

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Event Started")
            .setContentText("Location verified. Your phone is now set to ${event.action.toString().lowercase()} mode.")
            .setSmallIcon(R.drawable.baseline_notifications_24) // replace with your icon
            .setAutoCancel(true)
            .build()

        notificationManager.notify(event.id.toInt(), notification)
    }
}
