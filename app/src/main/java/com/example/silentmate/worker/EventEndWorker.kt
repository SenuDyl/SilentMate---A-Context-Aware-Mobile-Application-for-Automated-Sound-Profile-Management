package com.example.silentmate.worker

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.silentmate.R
import com.example.silentmate.model.Action
import com.example.silentmate.utils.AudioProfileUtils

class EventEndWorker(appContext: Context, params: WorkerParameters)
    : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val eventId = inputData.getLong("eventId", -1)
            if (eventId == -1L) return Result.failure()

            val prefs = applicationContext.getSharedPreferences("event_prefs", Context.MODE_PRIVATE)
            val applied = prefs.getBoolean("event_${eventId}_applied", false)

            if (applied) {
                // Only revert audio if we changed it
                AudioProfileUtils.applyAudioMode(applicationContext, Action.NORMAL)
                sendEndNotification()
            } else {
                // No action taken at start → ignore
                Log.d("EventEndWorker", "End worker skipped. Audio was never changed.")
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun sendEndNotification() {
        val channelId = "event_notifications"

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Event Ended")
            .setContentText("Your phone has been returned to normal ringer mode.")
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
