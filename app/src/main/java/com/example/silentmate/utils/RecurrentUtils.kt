package com.example.silentmate.utils

import com.example.silentmate.model.Event
import com.example.silentmate.model.Recurrence
import java.time.LocalDateTime
import java.time.ZoneId

data class NextOccurrence(val startMillis: Long, val endMillis: Long)

fun computeNextOccurrence(event: Event): NextOccurrence? {
    val zone = ZoneId.systemDefault()
    val now = LocalDateTime.now()

    // For testing purposes
    val TEST_MODE = false
    if (TEST_MODE) {
        val now = System.currentTimeMillis()
        return NextOccurrence(
            startMillis = now + 15_000L,
            endMillis   = now + 30_000L
        )
    }

    var nextDateTimeStart = LocalDateTime.of(event.date, event.startTime)
    var nextDateTimeEnd = LocalDateTime.of(event.date, event.endTime)

    // Move base start/end to future dates depending on recurrence type
    when (event.recurrence) {
        Recurrence.DAILY -> {
            if (nextDateTimeStart.isBefore(now)) {
                nextDateTimeStart = nextDateTimeStart.plusDays(1)
                nextDateTimeEnd = nextDateTimeEnd.plusDays(1)
            }
        }

        Recurrence.WEEKLY -> {
            nextDateTimeStart = nextDateTimeStart.plusWeeks(1)
            nextDateTimeEnd = nextDateTimeEnd.plusWeeks(1)
        }

        Recurrence.MONTHLY -> {
            nextDateTimeStart = nextDateTimeStart.plusMonths(1)
            nextDateTimeEnd = nextDateTimeEnd.plusMonths(1)
        }

        Recurrence.ONCE -> return null
    }

    return NextOccurrence(
        startMillis = nextDateTimeStart.atZone(zone).toInstant().toEpochMilli(),
        endMillis = nextDateTimeEnd.atZone(zone).toInstant().toEpochMilli()
    )
}

