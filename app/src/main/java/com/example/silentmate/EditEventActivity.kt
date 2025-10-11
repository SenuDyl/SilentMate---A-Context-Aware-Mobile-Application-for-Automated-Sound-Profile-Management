package com.example.silentmate

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.example.silentmate.database.EventDatabaseHelper
import com.example.silentmate.model.Action
import com.example.silentmate.model.Event
import com.example.silentmate.model.Location
import com.example.silentmate.model.Recurrence
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

class EditEventActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var dbHelper: EventDatabaseHelper

    private lateinit var titleEditText: EditText
    private lateinit var startTimeEditText: EditText
    private lateinit var endTimeEditText: EditText
    private lateinit var recurrenceGroup: RadioGroup
    private lateinit var oneTimeRadio: RadioButton
    private lateinit var weeklyRadio: RadioButton
    private lateinit var biWeeklyRadio: RadioButton
    private lateinit var monthlyRadio: RadioButton
    private lateinit var doneButton: ImageView
    private lateinit var cancelButton: ImageView
    private lateinit var backButton: ImageView
    private lateinit var startDateText: TextView

    private lateinit var soundOffButton: ImageButton
    private lateinit var vibrateButton: ImageButton
    private lateinit var soundOnButton: ImageButton

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    private var eventId: Int = -1
    private var startTime: LocalTime? = null
    private var endTime: LocalTime? = null
    private var selectedDate: LocalDate? = null
    private var recurrence = Recurrence.ONCE
    private var action = Action.NORMAL
    private var location = Location("", 0.0, 0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_event)

        dbHelper = EventDatabaseHelper(this)

        // Initialize Google Places
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.my_map_api_key))
        }

        eventId = intent.getIntExtra("event_id", -1)
        if (eventId == -1) {
            Toast.makeText(this, "Invalid event ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        bindViews()
        setupDatePicker()
        setupTimePickers()
        setupRecurrenceRadioGroup()
        setupActionButtons()

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setupPlaceSearch()

        loadEvent() // Load existing data

        doneButton.setOnClickListener { updateEvent() }
        cancelButton.setOnClickListener { finish() }
        backButton.setOnClickListener { finish() }
    }

    private fun bindViews() {
        titleEditText = findViewById(R.id.eventTitle)
        startTimeEditText = findViewById(R.id.startTime)
        endTimeEditText = findViewById(R.id.endTime)
        startDateText = findViewById(R.id.startDateText)

        recurrenceGroup = findViewById(R.id.recurrenceGroup)
        oneTimeRadio = findViewById(R.id.oneTime)
        weeklyRadio = findViewById(R.id.weekly)
        biWeeklyRadio = findViewById(R.id.biWeekly)
        monthlyRadio = findViewById(R.id.monthly)

        doneButton = findViewById(R.id.doneIcon)
        cancelButton = findViewById(R.id.closeIcon)
        backButton = findViewById(R.id.backIcon)

        soundOffButton = findViewById(R.id.soundOff)
        vibrateButton = findViewById(R.id.vibrate)
        soundOnButton = findViewById(R.id.soundOn)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        // Default to Sri Lanka
        val sriLanka = LatLng(7.8731, 80.7718)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(sriLanka, 7f))

        // Show event’s location if available
        if (location.displayName.isNotEmpty() && location.latitude != 0.0) {
            val eventLatLng = LatLng(location.latitude, location.longitude)
            googleMap?.addMarker(MarkerOptions().position(eventLatLng).title(location.displayName))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLatLng, 15f))
        }
    }

    private fun setupPlaceSearch() {
        val autocompleteFragment = supportFragmentManager
            .findFragmentById(R.id.place_autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val name = place.name ?: "Unnamed place"
                val latLng = place.latLng

                if (latLng != null) {
                    location = Location(name, latLng.latitude, latLng.longitude)
                    autocompleteFragment.setText(name)
                    googleMap?.clear()

                    val marker = MarkerOptions().position(latLng).title(name)
                    googleMap?.addMarker(marker)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                } else {
                    Toast.makeText(this@EditEventActivity, "No coordinates found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Log.e("PlaceError", "Error: $status")
            }
        })
    }

    private fun setupTimePickers() {
        startTimeEditText.setOnClickListener { pickTime(true) }
        endTimeEditText.setOnClickListener { pickTime(false) }
    }

    private fun pickTime(isStart: Boolean) {
        val now = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            val time = LocalTime.of(hour, minute)
            if (isStart) {
                startTime = time
                startTimeEditText.setText(time.toString())
            } else {
                endTime = time
                endTimeEditText.setText(time.toString())
            }
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()

        val pickDate = {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = android.app.DatePickerDialog(this, { _, y, m, d ->
                selectedDate = LocalDate.of(y, m + 1, d) // Month is 0-indexed
                startDateText.text = selectedDate.toString()
            }, year, month, day)

            datePicker.show()
        }

        startDateText.setOnClickListener { pickDate() }
        // If you have an icon next to the date text
        val startDateIcon: ImageView = findViewById(R.id.startDateIcon)
        startDateIcon.setOnClickListener { pickDate() }
    }


    private fun setupRecurrenceRadioGroup() {
        recurrenceGroup.setOnCheckedChangeListener { _, checkedId ->
            recurrence = when (checkedId) {
                R.id.oneTime -> Recurrence.ONCE
                R.id.weekly -> Recurrence.WEEKLY
                R.id.biWeekly -> Recurrence.BIWEEKLY
                R.id.monthly -> Recurrence.MONTHLY
                else -> Recurrence.ONCE
            }
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

    private fun loadEvent() {
        val event = dbHelper.getAllEvents().find { it.id == eventId }
        if (event != null) {
            titleEditText.setText(event.title)
            startTime = event.startTime
            endTime = event.endTime
            startTimeEditText.setText(event.startTime.toString())
            endTimeEditText.setText(event.endTime.toString())
            selectedDate = event.date
            startDateText.text = event.date.toString()
            recurrence = event.recurrence
            when (recurrence) {
                Recurrence.ONCE -> oneTimeRadio.isChecked = true
                Recurrence.WEEKLY -> weeklyRadio.isChecked = true
                Recurrence.BIWEEKLY -> biWeeklyRadio.isChecked = true
                Recurrence.MONTHLY -> monthlyRadio.isChecked = true
            }
            action = event.action
            location = event.location
            soundOffButton.alpha = 0.5f
            vibrateButton.alpha = 0.5f
            soundOnButton.alpha = 0.5f

            when (action) {
                Action.NORMAL -> soundOffButton.alpha = 1f
                Action.VIBRATE -> vibrateButton.alpha = 1f
                Action.SILENT -> soundOnButton.alpha = 1f
            }
            val autocompleteFragment =
                supportFragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as AutocompleteSupportFragment
            if (location.displayName.isNotEmpty()) {
                autocompleteFragment.setText(location.displayName)
            }

        }
    }

    private fun updateEvent() {
        val title = titleEditText.text.toString()
        if (title.isBlank() || startTime == null || endTime == null || selectedDate == null) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedEvent = Event(
            id = eventId,
            title = title,
            date = selectedDate!!,
            startTime = startTime!!,
            endTime = endTime!!,
            recurrence = recurrence,
            location = location,
            action = action
        )

        val success = dbHelper.updateEvent(updatedEvent)
        if (success) {
            Toast.makeText(this, "Event updated!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to update event", Toast.LENGTH_SHORT).show()
        }
    }

    // Required lifecycle methods for MapView
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
}
