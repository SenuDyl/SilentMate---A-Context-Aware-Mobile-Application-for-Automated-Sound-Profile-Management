package com.example.silentmate

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class TutorialDialogFragment : DialogFragment() {

    private lateinit var tutorialTitle: TextView
    private lateinit var tutorialContent: TextView
    private lateinit var tutorialImage: ImageView
    private lateinit var dontShowAgainCheckBox: CheckBox
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private var isFromSettings: Boolean = false

    private var listener: OnTutorialDismissListener? = null

    // Define the callback interface
    interface OnTutorialDismissListener {
        fun onTutorialDismissed()
    }

    private var currentStepIndex = 0

    private val steps = listOf(
        TutorialStep(
            title = "Welcome to SilentMate",
            content = "1. This area shows that no schedules have been created yet.\n\n" +
                    "2. Use this button to create a new schedule.\n\n" +
                    "3. Use settings button to grant necessary permissions.",
            imageResId = R.drawable.tutorial1
        ),
        TutorialStep(
            title = "Creating a New Event",
            content = "1. Fill in event name, date, start/end time, location, and action (choose which audio profile to switch to during the event). \n\n" +
                    "2. Tap this to store the event. It will now appear in the Home screen event list.",
            imageResId = R.drawable.tutorial2
        ),
        TutorialStep(
            title = "Sensor-based Audio Profile Switching",
            content = "SilentMate can automatically switch your phone’s audio profile based on your device position. Users can enable or disable each option.",
            imageResId = R.drawable.tutorial3
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_tutorial, container, false)

        // Get the arguments passed from the parent fragment
        isFromSettings = arguments?.getBoolean("isFromSettings", false) ?: false

        tutorialTitle = view.findViewById(R.id.tutorialTitle)
        tutorialContent = view.findViewById(R.id.tutorialContent)
        tutorialImage = view.findViewById(R.id.tutorialImage)
        dontShowAgainCheckBox = view.findViewById(R.id.dontShowAgainCheckBox)
        prevButton = view.findViewById(R.id.prevButton)
        nextButton = view.findViewById(R.id.nextButton)

        prevButton.setOnClickListener { showPreviousStep() }
        nextButton.setOnClickListener { showNextStep() }
        // Conditionally hide the "Do not show again" checkbox if it's from Settings
        if (isFromSettings) {
            dontShowAgainCheckBox.visibility = View.GONE // Hide the checkbox when showing from Settings
        }

        updateStep() // show first step

        return view
    }

    private fun updateStep() {
        val step = steps[currentStepIndex]

        tutorialTitle.text = step.title
        tutorialContent.text = step.content
        tutorialImage.setImageResource(step.imageResId)

        prevButton.visibility = if (currentStepIndex == 0) View.INVISIBLE else View.VISIBLE
        nextButton.text = if (currentStepIndex == steps.lastIndex) "Finish" else "Next"
    }

    private fun showNextStep() {
        if (currentStepIndex < steps.lastIndex) {
            currentStepIndex++
            updateStep()
        } else {
            saveDontShowAgainPreferenceIfChecked()
            dismiss()
        }
    }

    private fun showPreviousStep() {
        if (currentStepIndex > 0) {
            currentStepIndex--
            updateStep()
        }
    }

    private fun saveDontShowAgainPreferenceIfChecked() {
        if (dontShowAgainCheckBox.isChecked) {
            val prefs = requireContext().getSharedPreferences("SilentMatePrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("tutorial_seen", true).apply()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.95).toInt()
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Check if the parent activity implements the listener
        if (context is OnTutorialDismissListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Notify the listener when the dialog is dismissed
        listener?.onTutorialDismissed()
    }
}
