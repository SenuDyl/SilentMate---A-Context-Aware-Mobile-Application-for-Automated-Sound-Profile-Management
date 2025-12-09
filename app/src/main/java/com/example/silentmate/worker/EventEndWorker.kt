package com.example.silentmate.worker

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.silentmate.KEY_EVENT_AUDIO_ENABLED
import com.example.silentmate.R
import com.example.silentmate.database.EventDatabaseHelper
import com.example.silentmate.model.Action
import com.example.silentmate.model.Recurrence
import com.example.silentmate.utils.AudioProfileUtils
import com.example.silentmate.utils.computeNextOccurrence

class EventEndWorker(appContext: Context, params: WorkerParameters)
    : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val eventId = inputData.getLong("eventId", -1)
            if (eventId == -1L) return Result.failure()

            val prefs = applicationContext.getSharedPreferences("event_prefs", Context.MODE_PRIVATE)
            val applied = prefs.getBoolean("event_${eventId}_applied", false)

            val sharedPrefs = applicationContext.getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
            // Skip events if the option is disabled
            val isEventAudioEnabled = sharedPrefs.getBoolean(KEY_EVENT_AUDIO_ENABLED, true)
            Log.e("EventEndWorker", "isEventAudioEnabled, ${isEventAudioEnabled}")

            if (!isEventAudioEnabled) {
                Log.e("EventEndWorker", "Event-based audio switching option disabled, skipping")
                return Result.success() // skip work safely
            }

            if (applied) {
                // Only revert audio if we changed it
                AudioProfileUtils.applyAudioMode(applicationContext, Action.NORMAL)
                sendEndNotification()
            } else {
                // No action taken at start → ignore
                Log.d("EventEndWorker", "End worker skipped. Audio was never changed.")
            }

            // ---- RESCHEDULE NEXT OCCURRENCE ------------------------------------------
            val db = EventDatabaseHelper(applicationContext)
            val event = db.getEventById(eventId.toInt())

            if (event != null && event.recurrence != Recurrence.ONCE) {
                val next = computeNextOccurrence(event)

                if (next != null) {
                    WorkManagerScheduler.scheduleEvent(
                        applicationContext,
                        event.id.toLong(),
                        next.startMillis,
                        next.endMillis
                    )
                    Log.d("RecurrenceTest", "Next occurrence scheduled at: ${next.startMillis}")
                }
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
