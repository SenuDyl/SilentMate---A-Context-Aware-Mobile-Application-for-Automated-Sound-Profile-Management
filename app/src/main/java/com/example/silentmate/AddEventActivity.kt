package com.example.silentmate

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.GridLayout
import android.widget.TextView
import com.example.silentmate.database.EventDatabaseHelper
import com.example.silentmate.geofence.GeofenceManager
import com.example.silentmate.model.Action
import com.example.silentmate.model.Event
import com.example.silentmate.model.Location
import com.example.silentmate.model.Recurrence
import com.example.silentmate.worker.WorkManagerScheduler
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

class AddEventActivity : AppCompatActivity(), OnMapReadyCallback {

    // Database helper
    private lateinit var dbHelper: EventDatabaseHelper

    // Location client
    private lateinit var geofencingClient: GeofencingClient

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    // UI Elements
    private lateinit var titleEditText: EditText
    private lateinit var startTimeEditText: EditText
    private lateinit var endTimeEditText: EditText
    private lateinit var recurrenceGroup: GridLayout
    private lateinit var oneTimeButton: RadioButton
    private lateinit var weeklyButton: RadioButton
    private lateinit var dailyButton: RadioButton
    private lateinit var monthlyButton: RadioButton
    private lateinit var doneButton: ImageView
    private lateinit var cancelButton: ImageView
    private lateinit var backButton: ImageView

    private lateinit var startDateText: TextView
    private lateinit var startDateIcon: ImageView

    // Action buttons
    private lateinit var soundOffButton: ImageButton
    private lateinit var vibrateButton: ImageButton
    private lateinit var soundOnButton: ImageButton

    // Selected values
    private var startTime: LocalTime? = null
    private var endTime: LocalTime? = null
    private var selectedDate: LocalDate? = null
    private var recurrence = Recurrence.ONCE
    private var action = Action.NORMAL
    private var location = Location("", 0.0, 0.0)

    private val AUTOCOMPLETE_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_event)

//        val apiKey = ""
//        Places.initialize(applicationContext, apiKey)

        val autocompleteFragment = supportFragmentManager
            .findFragmentById(R.id.place_autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))

        // Handle user selection
        autocompleteFragment.setOnPlaceSelectedListener(object : com.google.android.libraries.places.widget.listener.PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val name = place.name ?: "Unnamed place"
                val latLng = place.latLng

                if (latLng != null) {
                    location = Location(name, latLng.latitude, latLng.longitude)
                    googleMap?.clear()
                    val marker = MarkerOptions().position(latLng).title(name)
                    googleMap?.addMarker(marker)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                } else {
                    Toast.makeText(this@AddEventActivity, "No coordinates available", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Log.e("PlaceError", "Error: $status")
            }
        })

        dbHelper = EventDatabaseHelper(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        bindViews()
        setupTimePickers()
        setupDatePicker()
        setupRecurrenceRadioGroup()
        setupActionButtons()
//        setupLocationSearch()

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        doneButton.setOnClickListener { saveEvent() }
        cancelButton.setOnClickListener { cancelEvent() }
        backButton.setOnClickListener { finish() }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        // Set initial position (optional)
        val sriLanka = LatLng(7.8731, 80.7718)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(sriLanka, 7f))
    }

    private fun bindViews() {
        titleEditText = findViewById(R.id.eventTitle)
        startTimeEditText = findViewById(R.id.startTime)
        endTimeEditText = findViewById(R.id.endTime)
        startDateText = findViewById(R.id.startDateText)
        startDateIcon = findViewById(R.id.startDateIcon)

        doneButton = findViewById(R.id.doneIcon)
        cancelButton = findViewById(R.id.closeIcon)
        backButton = findViewById(R.id.backIcon)

        soundOffButton = findViewById(R.id.soundOff)
        vibrateButton = findViewById(R.id.vibrate)
        soundOnButton = findViewById(R.id.soundOn)
    }

    private fun setupTimePickers() {
        startTimeEditText.setOnClickListener { pickTime(true) }
        endTimeEditText.setOnClickListener { pickTime(false) }
    }

    private fun pickTime(isStart: Boolean) {
        val now = Calendar.getInstance()
        TimePickerDialog(
            this,
            R.style.CustomTimePicker,
            { _, hour, minute ->
            val time = LocalTime.of(hour, minute)
            // Format the time as 12-hour format with AM/PM
            val formatter = DateTimeFormatter.ofPattern("hh:mm a") // 12-hour format with AM/PM
            val formattedTime = time.format(formatter)

            if (isStart) {
                startTime = time
                startTimeEditText.setText(formattedTime)
            } else {
                endTime = time
                endTimeEditText.setText(formattedTime)
            }
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()

        val pickDate = {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = android.app.DatePickerDialog(
                this,
                R.style.CustomTimePicker,
                { _, y, m, d ->
                selectedDate = LocalDate.of(y, m + 1, d) // Month is 0-indexed
                startDateText.text = selectedDate.toString()
            }, year, month, day)

            datePicker.show()
        }

        startDateText.setOnClickListener { pickDate() }
        startDateIcon.setOnClickListener { pickDate() }
    }

    private fun setupRecurrenceRadioGroup() {
        recurrenceGroup = findViewById(R.id.recurrenceGroup)

        // Find the individual RadioButton views by their ID
        oneTimeButton = findViewById(R.id.oneTime)
        weeklyButton = findViewById(R.id.weekly)
        dailyButton = findViewById(R.id.daily)
        monthlyButton = findViewById(R.id.monthly)

        // Set the listener for each RadioButton
        oneTimeButton.setOnClickListener {
            recurrence = Recurrence.ONCE
        }

        weeklyButton.setOnClickListener {
            recurrence = Recurrence.WEEKLY
        }

        dailyButton.setOnClickListener {
            recurrence = Recurrence.DAILY
        }

        monthlyButton.setOnClickListener {
            recurrence = Recurrence.MONTHLY
        }
    }

    private fun setupActionButtons() {
        fun selectAction(selected: Action) {
            action = selected
            soundOffButton.alpha = if (selected == Action.NORMAL) 1f else 0.5f
            vibrateButton.alpha = if (selected == Action.VIBRATE) 1f else 0.5f
            soundOnButton.alpha = if (selected == Action.SILENT) 1f else 0.5f
        }

        soundOffButton.setOnClickListener { selectAction(Action.NORMAL) }
        vibrateButton.setOnClickListener { selectAction(Action.VIBRATE) }
        soundOnButton.setOnClickListener { selectAction(Action.SILENT) }
    }

    private fun saveEvent() {
        val title = titleEditText.text.toString()
        if (title.isBlank() || startTime == null || endTime == null || selectedDate == null) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val event = Event(
            title = title,
            date = selectedDate!!,
            startTime = startTime!!,
            endTime = endTime!!,
            recurrence = recurrence,
            location = location,
            action = action
        )

        val id = dbHelper.insertEvent(event).toInt()
        // Calculate the event start time in milliseconds
        val eventStartTime = LocalDateTime.of(selectedDate, startTime!!) // Combine date and time
        val eventStartTimeMillis = eventStartTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Calculate the event start time in milliseconds
        val eventEndTime = LocalDateTime.of(selectedDate, endTime!!) // Combine date and time
        val eventEndTimeMillis = eventEndTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // After DB insert (get returned id)
        WorkManagerScheduler.scheduleEvent(applicationContext, id.toLong(), eventStartTimeMillis, eventEndTimeMillis)
        createGeofence(event.copy(id = id))
        Toast.makeText(this, "Event saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun cancelEvent() {
        finish() // simply closes the activity
    }

    private fun createGeofence(event: Event) {
        GeofenceManager(applicationContext).registerGeofence(event)
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }

}