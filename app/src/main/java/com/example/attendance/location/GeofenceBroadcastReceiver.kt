package com.example.attendance.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.w(TAG, "收到空事件")
            return
        }
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofence事件错误: errorCode=${geofencingEvent.errorCode}")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val now = System.currentTimeMillis()
        val nowStr = dateFormat.format(Date(now))
        val transitionStr = when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            else -> "UNKNOWN($transitionType)"
        }
        Log.d(TAG, "收到Geofence事件: type=$transitionStr, time=$nowStr")

        CoroutineScope(Dispatchers.IO).launch {
            val appContext = context.applicationContext
            val database = AppDatabase.getInstance(appContext)
            val dataStore = SettingsDataStore(appContext)
            val repository = AttendanceRepository(database.attendanceDao(), dataStore, WorkdayChecker())

            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.d(TAG, "处理ENTER事件: 调用repository.handleEnter")
                    repository.handleEnter("AUTO")
                    val today = dateFormat.format(Date())
                    val record = repository.getTodayRecord(today)
                    if (record != null) {
                        Log.d(TAG, "ENTER处理完成: recordId=${record.id}, arriveTime=${record.arriveTime}, status=${record.status}")
                    } else {
                        Log.w(TAG, "ENTER处理完成: 未创建记录")
                    }
                    if (record != null && record.status == "PENDING") {
                        Log.d(TAG, "非工作日到达, 发送确认通知")
                        val notificationHelper = NotificationHelper(appContext)
                        notificationHelper.createChannel()
                        notificationHelper.showNonWorkdayConfirmation()
                    }
                }

                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.d(TAG, "处理EXIT事件: 调用repository.handleExit")
                    repository.handleExit()
                    val today = dateFormat.format(Date())
                    val record = repository.getTodayRecord(today)
                    Log.d(TAG, "EXIT处理完成: leaveTime=${record?.leaveTime}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }
}