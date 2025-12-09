package com.example.silentmate.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.silentmate.model.Event
import com.example.silentmate.model.Location
import com.example.silentmate.utils.Converters

class EventDatabaseHelper(context: Context):
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "silentmate.db"
        private const val DATABASE_VERSION = 4

        private const val TABLE_EVENTS = "events"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_START_TIME = "startTime"
        private const val COLUMN_END_TIME = "endTime"
        private const val COLUMN_RECURRENCE = "recurrence"
        private const val COLUMN_LOCATION = "location"
        private const val COLUMN_ACTION = "action"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_EVENTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT,
                $COLUMN_DATE TEXT,
                $COLUMN_START_TIME TEXT,
                $COLUMN_END_TIME TEXT,
                $COLUMN_RECURRENCE TEXT,
                $COLUMN_LOCATION TEXT,
                $COLUMN_ACTION TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        onCreate(db)
    }

    fun insertEvent(event: Event): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, event.title)
            put(COLUMN_DATE, Converters.localDateToString(event.date))
            put(COLUMN_START_TIME, Converters.localTimeToString(event.startTime))
            put(COLUMN_END_TIME, Converters.localTimeToString(event.endTime))
            put(COLUMN_RECURRENCE, Converters.recurrenceToString(event.recurrence))
            put(COLUMN_LOCATION, Converters.locationToString(event.location.displayName, event.location.latitude, event.location.longitude))
            put(COLUMN_ACTION, Converters.actionToString(event.action))
        }
        return db.insert(TABLE_EVENTS, null, values)
    }

    fun getAllEvents(): List<Event> {
        val events = mutableListOf<Event>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_EVENTS", null)
        if (cursor.moveToFirst()) {
            do {
                val locationStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION))
                val (displayName, lat, lon) = Converters.stringToLocation(locationStr)
                val event = Event(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                    date = Converters.stringToLocalDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))),
                    startTime = Converters.stringToLocalTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_TIME))),
                    endTime = Converters.stringToLocalTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_END_TIME))),
                    recurrence = Converters.stringToRecurrence(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECURRENCE))),
                    location = Location(displayName, lat, lon),
                    action = Converters.stringToAction(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTION)))
                )
                events.add(event)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return events
    }

    fun deleteEvent(eventId: Int): Boolean {
        val db = writableDatabase
        val deletedRows = db.delete(
            TABLE_EVENTS,
            "$COLUMN_ID = ?",
            arrayOf(eventId.toString())
        )
        return deletedRows > 0
    }

    fun updateEvent(event: Event): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, event.title)
            put(COLUMN_DATE, Converters.localDateToString(event.date))
            put(COLUMN_START_TIME, Converters.localTimeToString(event.startTime))
            put(COLUMN_END_TIME, Converters.localTimeToString(event.endTime))
            put(COLUMN_RECURRENCE, Converters.recurrenceToString(event.recurrence))
            put(COLUMN_LOCATION, Converters.locationToString(event.location.displayName, event.location.latitude, event.location.longitude))
            put(COLUMN_ACTION, Converters.actionToString(event.action))
        }
        val updatedRows = db.update(
            TABLE_EVENTS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(event.id.toString())
        )
        return updatedRows > 0
    }

    fun getEventById(eventId: Int): Event? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null, // null means all columns
            "$COLUMN_ID = ?",
            arrayOf(eventId.toString()),
            null,
            null,
            null
        )

        var event: Event? = null
        if (cursor.moveToFirst()) {
            val locationStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION))
            val (displayName, lat, lon) = Converters.stringToLocation(locationStr)
            event = Event(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                date = Converters.stringToLocalDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))),
                startTime = Converters.stringToLocalTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_TIME))),
                endTime = Converters.stringToLocalTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_END_TIME))),
                recurrence = Converters.stringToRecurrence(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECURRENCE))),
                location = Location(displayName, lat, lon),
                action = Converters.stringToAction(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTION)))
            )
        }
        cursor.close()
        return event
    }


}