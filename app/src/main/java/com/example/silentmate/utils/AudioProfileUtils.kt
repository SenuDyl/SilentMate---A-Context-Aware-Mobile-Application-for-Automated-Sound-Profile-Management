package com.example.silentmate.utils

import android.content.Context
import android.media.AudioManager
import android.app.NotificationManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.example.silentmate.model.Action
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AudioProfileUtils {

    fun applyAudioMode(context: Context, mode: Action) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (mode.toString().uppercase()) {
            "SILENT" -> {
                // if DND management needed (API 23+), user must grant access
                if (requiresDnd()) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        !nm.isNotificationPolicyAccessGranted) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "Please grant 'Do Not Disturb' access.", Toast.LENGTH_LONG).show()
                        }
                        am.ringerMode = AudioManager.RINGER_MODE_SILENT
                        return
                    }
                }
                am.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
            "VIBRATE" -> am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            "NORMAL" -> am.ringerMode = AudioManager.RINGER_MODE_NORMAL
            else -> am.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
    }

    private fun requiresDnd(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
}
