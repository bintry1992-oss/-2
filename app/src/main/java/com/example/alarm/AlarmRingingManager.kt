package com.example.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.Alarm
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmRingingManager private constructor() {
    private val _ringingAlarm = MutableStateFlow<Alarm?>(null)
    val ringingAlarm = _ringingAlarm.asStateFlow()

    private var audioPlayer: AlarmAudioPlayer? = null
    private var vibrator: Vibrator? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startRinging(context: Context, alarm: Alarm) {
        _ringingAlarm.value = alarm

        if (audioPlayer == null) {
            audioPlayer = AlarmAudioPlayer(context.applicationContext)
        }
        audioPlayer?.startPlaying(alarm.ringtoneId)

        if (alarm.isVibrate) {
            startVibration(context)
        }

        // Launch the MainActivity to show the ringing UI instantly if device allows it
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            Log.e("AlarmRingingManager", "Failed to launch main activity", e)
        }
    }

    private fun startVibration(context: Context) {
        try {
            val vibratorService = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator = vibratorService
            vibrator?.let {
                val pattern = longArrayOf(0, 500, 300, 500) // Vibrate, pause, vibrate...
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmRingingManager", "Failed to start vibration", e)
        }
    }

    fun dismiss(context: Context) {
        val alarm = _ringingAlarm.value ?: return
        Log.d("AlarmRingingManager", "Dismissing alarm ${alarm.id}")
        stopEffects()
        _ringingAlarm.value = null

        scope.launch {
            if (alarm.id > 0) { // Do not persist temporary snooze alarms (id <= 0)
                val db = AlarmDatabase.getDatabase(context)
                val scheduler = AlarmSchedulerImpl(context)
                val repo = AlarmRepository(db.alarmDao, scheduler)

                if (!alarm.isRepeating()) {
                    repo.updateAlarm(alarm.copy(isEnabled = false))
                } else {
                    // Update and schedule the next recurring instant
                    repo.updateAlarm(alarm)
                }
            }
        }
    }

    fun snooze(context: Context) {
        val alarm = _ringingAlarm.value ?: return
        Log.d("AlarmRingingManager", "Snoozing alarm ${alarm.id}")
        stopEffects()
        _ringingAlarm.value = null

        scope.launch {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 5)
            }

            val db = AlarmDatabase.getDatabase(context)
            val scheduler = AlarmSchedulerImpl(context)
            val repo = AlarmRepository(db.alarmDao, scheduler)

            // Direct schedule in the background with temporary ID
            val snoozeAlarm = Alarm(
                id = -alarm.id, // Negate ID so it's temporary and won't clash with database alarms
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE),
                label = "贪睡 (" + alarm.label + ")",
                isEnabled = true,
                repeatDays = "",
                ringtoneId = alarm.ringtoneId,
                isVibrate = alarm.isVibrate
            )
            scheduler.schedule(snoozeAlarm)
        }
    }

    private fun stopEffects() {
        audioPlayer?.stopPlaying()
        vibrator?.cancel()
        vibrator = null
    }

    companion object {
        val instance by lazy { AlarmRingingManager() }
    }
}
