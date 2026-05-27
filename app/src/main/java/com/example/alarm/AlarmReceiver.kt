package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.Alarm
import com.example.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmReceiver", "Received broadcast: action=$action")

        if (action == ACTION_DISMISS) {
            AlarmRingingManager.instance.dismiss(context)
            cancelNotification(context)
            return
        }

        if (action == ACTION_SNOOZE) {
            AlarmRingingManager.instance.snooze(context)
            cancelNotification(context)
            return
        }

        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        Log.d("AlarmReceiver", "Alarm triggered for ID: $alarmId")
        if (alarmId == -1L) return

        if (alarmId < 0) {
            // This is a snooze alarm, construct a temporary alarm and ring
            val snoozeAlarm = Alarm(
                id = alarmId,
                hour = 0,
                minute = 0,
                label = "贪睡提醒",
                isEnabled = true,
                repeatDays = "",
                ringtoneId = "zen_bell",
                isVibrate = true
            )
            triggerRingingAndNotification(context, snoozeAlarm)
        } else {
            // Regular database alarm
            CoroutineScope(Dispatchers.IO).launch {
                val db = AlarmDatabase.getDatabase(context)
                val alarm = db.alarmDao.getAlarmById(alarmId)
                if (alarm != null && alarm.isEnabled) {
                    triggerRingingAndNotification(context, alarm)
                }
            }
        }
    }

    private fun triggerRingingAndNotification(context: Context, alarm: Alarm) {
        // Start playing ringtone and vibrating on main thread
        CoroutineScope(Dispatchers.Main).launch {
            AlarmRingingManager.instance.startRinging(context, alarm)
        }

        // Show Heads-Up Notification with options to Snooze or Dismiss
        showNotification(context, alarm)
    }

    private fun showNotification(context: Context, alarm: Alarm) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Setup channel on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "闹钟提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "闹钟在响铃时显示的横幅通知"
                enableVibration(true)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // App launch pending intent
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action intent
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action intent
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // System standard alarm icon
            .setContentTitle("闹钟响铃中...")
            .setContentText(alarm.label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(appPendingIntent)
            .setFullScreenIntent(appPendingIntent, true) // Show full-screen if device is locked
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "关闭",
                dismissPendingIntent
            )
            .addAction(
                android.R.drawable.ic_popup_sync,
                "稍后提醒 (5分钟)",
                snoozePendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "alarm_ringing_channel"
        const val NOTIFICATION_ID = 4004
        const val ACTION_DISMISS = "com.example.alarm.DISMISS"
        const val ACTION_SNOOZE = "com.example.alarm.SNOOZE"
    }
}
