package com.example.data

import com.example.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler
) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarmsFlow()

    suspend fun getAlarmById(id: Long): Alarm? = alarmDao.getAlarmById(id)

    suspend fun insertAlarm(alarm: Alarm): Long {
        val id = alarmDao.insertAlarm(alarm)
        val insertedAlarm = alarm.copy(id = id)
        if (insertedAlarm.isEnabled) {
            alarmScheduler.schedule(insertedAlarm)
        } else {
            alarmScheduler.cancel(insertedAlarm)
        }
        return id
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
        if (alarm.isEnabled) {
            alarmScheduler.schedule(alarm)
        } else {
            alarmScheduler.cancel(alarm)
        }
    }

    suspend fun toggleAlarm(alarm: Alarm) {
        val updated = alarm.copy(isEnabled = !alarm.isEnabled)
        alarmDao.updateAlarm(updated)
        if (updated.isEnabled) {
            alarmScheduler.schedule(updated)
        } else {
            alarmScheduler.cancel(updated)
        }
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
        alarmScheduler.cancel(alarm)
    }

    suspend fun rescheduleAllEnabledAlarms() {
        val enabledAlarms = alarmDao.getEnabledAlarms()
        for (alarm in enabledAlarms) {
            alarmScheduler.schedule(alarm)
        }
    }
}
