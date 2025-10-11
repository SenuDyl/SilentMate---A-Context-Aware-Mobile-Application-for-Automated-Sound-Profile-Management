package com.example.silentmate

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.silentmate.database.EventDatabaseHelper
import com.example.silentmate.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class TestDatabaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No layout needed; we just test DB operations

        val dbHelper = EventDatabaseHelper(this)

        // 1️⃣ Create a dummy event
        val dummyEvent = Event(
            title = "Test Event",
            date = LocalDate.of(2025, 10, 11),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(9, 0),
            recurrence = Recurrence.WEEKLY,
            location = Location("University", 6.9271, 79.8612),
            action = Action.VIBRATE
        )

        // 2️⃣ Insert the dummy event
        val id = dbHelper.insertEvent(dummyEvent)
        Toast.makeText(this, "Inserted Event ID: $id", Toast.LENGTH_SHORT).show()
        Log.d("DB_TEST", "Inserted Event ID: $id")

        // 3️⃣ Retrieve all events
        val events = dbHelper.getAllEvents()
        events.forEach { Log.d("DB_TEST", it.toString()) }
    }
}
