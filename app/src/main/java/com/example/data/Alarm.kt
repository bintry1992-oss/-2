package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "闹钟",
    val isEnabled: Boolean = true,
    val repeatDays: String = "", // Comma-separated: "1,2,3,4,5" (1=Mon, 7=Sun). Empty means once.
    val ringtoneId: String = "zen_bell", // zen_bell, digital_beep, gentle_breeze, deep_pulse, system_default
    val isVibrate: Boolean = true,
    val lastTriggeredTime: Long = 0L
) {
    fun getFormattedTime(): String {
        return String.format("%02d:%02d", hour, minute)
    }

    fun isRepeating(): Boolean = repeatDays.isNotEmpty()

    fun getRepeatDaysList(): List<Int> {
        if (repeatDays.isEmpty()) return emptyList()
        return repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}
