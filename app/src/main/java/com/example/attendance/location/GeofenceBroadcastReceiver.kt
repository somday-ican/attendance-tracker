package com.example.attendance.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.data.settings.SettingsDataStore
import com.example.attendance.domain.WorkdayChecker
import com.example.attendance.notification.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) return

        val transitionType = geofencingEvent.geofenceTransition

        CoroutineScope(Dispatchers.IO).launch {
            val appContext = context.applicationContext
            val database = AppDatabase.getInstance(appContext)
            val dataStore = SettingsDataStore(appContext)
            val repository = AttendanceRepository(database.attendanceDao(), dataStore, WorkdayChecker())

            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    repository.handleEnter("AUTO")
                    val today = repository.getTodayRecord(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()))
                    if (today != null && today.status == "PENDING") {
                        val notificationHelper = NotificationHelper(appContext)
                        notificationHelper.createChannel()
                        notificationHelper.showNonWorkdayConfirmation()
                    }
                }

                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    repository.handleExit()
                }
            }
        }
    }
}