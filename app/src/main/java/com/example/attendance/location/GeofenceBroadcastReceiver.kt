package com.example.attendance.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.local.entities.AttendanceRecord
import com.example.attendance.data.local.entities.LocationEvent
import com.example.attendance.domain.AttendanceRules
import com.example.attendance.domain.WorkdayChecker
import com.example.attendance.notification.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) return

        val transitionType = geofencingEvent.geofenceTransition
        val now = System.currentTimeMillis()
        val today = dateFormat.format(Date(now))

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getInstance(context.applicationContext).attendanceDao()
            val workdayChecker = WorkdayChecker()
            val isWorkday = workdayChecker.isWorkday(now)
            val status = AttendanceRules.determineStatus(isWorkday)

            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    dao.insertEvent(
                        LocationEvent(type = "ENTER", timestamp = now, handled = true)
                    )
                    val existing = dao.getRecordByDate(today)
                    if (existing != null) {
                        dao.updateRecord(
                            existing.copy(arriveTime = now, status = status, isWorkday = isWorkday)
                        )
                    } else {
                        dao.insertRecord(
                            AttendanceRecord(
                                date = today,
                                arriveTime = now,
                                source = "AUTO",
                                status = status,
                                isWorkday = isWorkday
                            )
                        )
                    }

                    if (!isWorkday) {
                        val notificationHelper = NotificationHelper(context.applicationContext)
                        notificationHelper.createChannel()
                        notificationHelper.showNonWorkdayConfirmation()
                    }
                }

                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    dao.insertEvent(
                        LocationEvent(type = "EXIT", timestamp = now, handled = true)
                    )
                    val existing = dao.getRecordByDate(today)
                    if (existing != null) {
                        dao.updateRecord(existing.copy(leaveTime = now))
                    }
                }
            }
        }
    }

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
}