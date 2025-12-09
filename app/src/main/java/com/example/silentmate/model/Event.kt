package com.example.silentmate.model

import java.time.LocalDate
import java.time.LocalTime

data class Event(
    val id: Int = 0,
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val recurrence: Recurrence,
    val location: Location,
    val action: Action
)
