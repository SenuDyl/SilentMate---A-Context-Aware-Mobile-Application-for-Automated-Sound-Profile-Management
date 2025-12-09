package com.example.silentmate.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.silentmate.model.Action
import com.example.silentmate.model.Recurrence
import java.time.LocalDate
import java.time.LocalTime

object Converters {

    // Date conversion: LocalDate <-> String
    fun localDateToString(date: LocalDate) = date.toString() // e.g., "2025-10-11"

    @RequiresApi(Build.VERSION_CODES.O)
    fun stringToLocalDate(str: String) = LocalDate.parse(str) // parses "2025-10-11"

    // Time conversion
    fun localTimeToString(time: LocalTime) = time.toString()

    @RequiresApi(Build.VERSION_CODES.O)
    fun stringToLocalTime(str: String) = LocalTime.parse(str)

    // Enum conversion
    fun recurrenceToString(r: Recurrence) = r.name
    fun stringToRecurrence(str: String) = Recurrence.valueOf(str)

    fun actionToString(a: Action) = a.name
    fun stringToAction(str: String) = Action.valueOf(str)

    // Location conversion
    fun locationToString(displayName: String, locationLat: Double, locationLon: Double): String {
        // Use commas but ensure it’s consistent and readable
        return "$displayName,$locationLat,$locationLon"
    }

    fun stringToLocation(str: String): Triple<String, Double, Double> {
        val parts = str.split(",")

        // Handle missing or malformed values safely
        val name = parts.getOrNull(0) ?: ""
        val lat = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        val lon = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0

        return Triple(name, lat, lon)
    }

}
