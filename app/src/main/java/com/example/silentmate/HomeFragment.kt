package com.example.silentmate

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.silentmate.adapter.EventAdapter
import com.example.silentmate.geofence.GeofenceManager
import com.example.silentmate.model.Event
import com.example.silentmate.worker.WorkManagerScheduler
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.DayOfWeek
import java.time.LocalTime

class HomeFragment : Fragment() {

    private lateinit var fab: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var noEventsLayout: View
    private lateinit var adapter: EventAdapter

    private val eventsList = mutableListOf<Event>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("HomeFragment", "onCreateView called")
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fab = view.findViewById(R.id.addEventFab)
        recyclerView = view.findViewById(R.id.eventsRecyclerView)
        noEventsLayout = view.findViewById(R.id.noEventsLayout)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = EventAdapter(
            eventsList,
            onEdit = { event ->
                val intent = Intent(requireContext(), EditEventActivity::class.java)
                intent.putExtra("event_id", event.id)
                startActivity(intent)
            },
            onDelete = { event ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete \"${event.title}\"?")
                    .setPositiveButton("Yes") { _, _ ->
                        val dbHelper = com.example.silentmate.database.EventDatabaseHelper(requireContext())
                        val success = dbHelper.deleteEvent(event.id)
                        if (success) {
                            eventsList.remove(event)
                            adapter.notifyDataSetChanged()
                            WorkManagerScheduler.cancelEvent(requireContext(), event.id.toLong())
                            // Also remove any geofence
                            GeofenceManager(requireContext()).removeGeofence(event.id.toLong())

                            updateUI()
                        } else {
                            Toast.makeText(requireContext(), "Failed to delete event", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }

        )
        recyclerView.adapter = adapter

        fab.setOnClickListener {
            startActivity(Intent(requireContext(), AddEventActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadEvents()  // Reload events from DB
        updateUI()    // Update visibility based on list
    }

    private fun loadEvents() {
        eventsList.clear()

        // Dummy event for testing
//        val sampleEvent = Event(
//            id = 1,
//            title = "Office Meeting",
//            days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
//            startTime = LocalTime.of(9, 0),
//            endTime = LocalTime.of(10, 0),
//            location = com.example.silentmate.model.Location(6.9271, 79.8612),
//            recurrence = com.example.silentmate.model.Recurrence.WEEKLY,
//            action = com.example.silentmate.model.Action.NORMAL
//        )

        val dbHelper = com.example.silentmate.database.EventDatabaseHelper(requireContext())
        val allEvents = dbHelper.getAllEvents()  // Fetch all events from DB

        eventsList.addAll(allEvents)

        Log.d("HomeFragment", "Loaded ${eventsList.size} events")
        adapter.notifyDataSetChanged()
    }

    private fun updateUI() {
        if (eventsList.isEmpty()) {
            noEventsLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noEventsLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}
