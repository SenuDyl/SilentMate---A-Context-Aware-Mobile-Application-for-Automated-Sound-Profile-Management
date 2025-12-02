package com.example.silentmate.worker

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlin.math.max

object WorkManagerScheduler {

    // We'll give each event a unique work name "event_<id>"
    private fun workNameForEvent(eventId: Long) = "event_$eventId"

    fun scheduleEvent(context: Context, eventId: Long, startTimeMillis: Long, endTimeMillis: Long) {

        val now = System.currentTimeMillis()

        // Schedule start worker
        val startDelay = startTimeMillis - now
        enqueueWorker(context, eventId, max(startDelay, 0L))

        // Schedule end worker
        val endDelay = endTimeMillis - now
        Log.d("Scheduler", "now=$now start=$startTimeMillis end=$endTimeMillis")
        Log.d("Scheduler", "delays: startDelay=$startDelay endDelay=$endDelay")

        enqueueEndWorker(context, eventId, endDelay)
    }


    // For testing purposes
//    fun scheduleEvent(context: Context, eventId: Long, startTimeMillis: Long) {
//        // Force immediate execution for testing
//        enqueueWorker(context, eventId, 0)
//    }

    private fun enqueueWorker(context: Context, eventId: Long, delayMillis: Long) {
        val input = workDataOf(EventStartWorker.KEY_EVENT_ID to eventId)
        val builder = OneTimeWorkRequestBuilder<EventStartWorker>()
            .setInputData(input)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)

        val req = builder.build()
        // unique per event
        WorkManager.getInstance(context).enqueueUniqueWork(workNameForEvent(eventId), ExistingWorkPolicy.REPLACE, req)
    }

    private fun enqueueEndWorker(context: Context, eventId: Long, delay: Long) {
        val data = workDataOf("eventId" to eventId)

        val request = OneTimeWorkRequestBuilder<EventEndWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }


    fun cancelEvent(context: Context, eventId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workNameForEvent(eventId))
    }
}
