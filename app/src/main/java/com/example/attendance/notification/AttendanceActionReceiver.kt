package com.example.attendance.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.data.settings.SettingsDataStore
import com.example.attendance.domain.WorkdayChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationHelper.ACTION_CONFIRM) return

        val today = dateFormat.format(Date())

        CoroutineScope(Dispatchers.IO).launch {
            val appContext = context.applicationContext
            val database = AppDatabase.getInstance(appContext)
            val dataStore = SettingsDataStore(appContext)
            val repository = AttendanceRepository(database.attendanceDao(), dataStore, WorkdayChecker())
            repository.confirmPendingRecord(today)
        }
    }

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
}