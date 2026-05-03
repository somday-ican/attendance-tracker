package com.example.attendance.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.attendance.data.local.AppDatabase
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
            val dao = AppDatabase.getInstance(context.applicationContext).attendanceDao()
            val record = dao.getRecordByDate(today)
            if (record != null && record.status == "PENDING") {
                dao.updateRecord(record.copy(status = "CONFIRMED"))
            }
        }
    }

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
}