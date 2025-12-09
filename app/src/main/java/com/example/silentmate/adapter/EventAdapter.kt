package com.example.silentmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.silentmate.R
import com.example.silentmate.model.Action
import com.example.silentmate.model.Event
import java.time.format.DateTimeFormatter

class EventAdapter(
    private val events: List<Event>,
    private val onEdit: (Event) -> Unit,
    private val onDelete: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventIcon: ImageView = itemView.findViewById(R.id.eventIcon)
        val eventTitle: TextView = itemView.findViewById(R.id.eventTitle)
        val eventDays: TextView = itemView.findViewById(R.id.eventDays)
        val eventTime: TextView = itemView.findViewById(R.id.eventTime)
        val eventLocation: TextView = itemView.findViewById(R.id.eventLocation)
        val eventRecurrence: TextView = itemView.findViewById(R.id.eventRecurrence)
        val editButton: Button = itemView.findViewById(R.id.editButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.event_item, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        // Title
        holder.eventTitle.text = event.title

        // Date display (format as "Mon, 11 Oct 2025")
        val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
        holder.eventDays.text = event.date.format(formatter)

        // Time (format LocalTime properly)
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
        val start = event.startTime.format(timeFormatter)
        val end = event.endTime.format(timeFormatter)
        holder.eventTime.text = "$start - $end"

        // Location
        holder.eventLocation.text = if (event.location.displayName.isNotEmpty())
            event.location.displayName
        else
            "No location"

        // Recurrence
        holder.eventRecurrence.text = event.recurrence.toString()

        // Icon based on action
        holder.eventIcon.setImageResource(
            when (event.action) {
                Action.NORMAL -> R.drawable.baseline_volume_up_24
                Action.VIBRATE -> R.drawable.baseline_vibration_24
                Action.SILENT -> R.drawable.baseline_volume_off_24
            }
        )

        // Edit/Delete button actions
        holder.editButton.setOnClickListener { onEdit(event) }
        holder.deleteButton.setOnClickListener { onDelete(event) }
    }


    override fun getItemCount(): Int = events.size


}
