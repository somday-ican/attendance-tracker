package com.example.attendance.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.example.attendance.MainActivity
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.data.repository.TodayAttendanceState
import com.example.attendance.data.settings.SettingsDataStore
import com.example.attendance.domain.WorkdayChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AttendanceLocationService : Service() {

    private var locationClient: AMapLocationClient? = null
    private var lastInsideCompany: Boolean? = null
    private var companyLat: Double = 0.0
    private var companyLng: Double = 0.0
    private var companyRadius: Float = 150f
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "服务创建")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "服务启动: onStartCommand")
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(FG_TAG, "startForeground called")

        scope.launch {
            loadCompanyLocation()
            startLocationUpdates()
        }

        return START_STICKY
    }

    private suspend fun loadCompanyLocation() {
        val dataStore = SettingsDataStore(this)
        val location = dataStore.companyLocationFlow.first()
        if (location.latitude == null || location.longitude == null) {
            Log.e(TAG, "公司位置未设置，停止检测")
            stopSelf()
            return
        }
        companyLat = location.latitude
        companyLng = location.longitude
        companyRadius = location.geofenceRadiusMeters
        Log.d(TAG, "公司位置: $companyLat, $companyLng, 半径: ${companyRadius}m")
    }

    private fun startLocationUpdates() {
        try {
            locationClient = AMapLocationClient(this)
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                setInterval(60 * 1000L)
                isOnceLocation = false
                isNeedAddress = false
                isMockEnable = false
                httpTimeOut = 10000
            }
            locationClient?.setLocationOption(option)
            locationClient?.setLocationListener(object : AMapLocationListener {
                override fun onLocationChanged(location: AMapLocation?) {
                    if (location != null && location.errorCode == 0) {
                        handleLocationResult(location.latitude, location.longitude)
                    } else {
                        val code = location?.errorCode ?: -1
                        val info = location?.errorInfo ?: "未知错误"
                        val detail = location?.locationDetail ?: ""
                        Log.e(TAG, "定位失败 - 错误码: $code, 信息: $info, 详情: $detail")
                        scope.launch {
                            SettingsDataStore(this@AttendanceLocationService)
                                .updateAutoDetectResult(null, null, "定位失败($code)")
                        }
                    }
                }
            })
            locationClient?.startLocation()
            Log.d(TAG, "定位已启动，间隔60秒")
        } catch (e: Exception) {
            Log.e(TAG, "定位初始化异常: ${e.message}", e)
        }
    }

    private fun handleLocationResult(lat: Double, lng: Double) {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat, lng, companyLat, companyLng, results)
        val distance = results[0]
        val inside = distance <= companyRadius

        scope.launch {
            val dataStore = SettingsDataStore(this@AttendanceLocationService)
            val dao = AppDatabase.getInstance(this@AttendanceLocationService).attendanceDao()
            val repo = AttendanceRepository(dao, dataStore, WorkdayChecker())

            val todayState = repo.getTodayAttendanceStateOnce()
            Log.d(AutoDetectTag,
                "AutoDetect decision: " +
                "insideCompany=$inside, " +
                "lastInsideCompany=$lastInsideCompany, " +
                "todayAttendanceState=$todayState"
            )

            val action: String
            if (inside) {
                when (todayState) {
                    TodayAttendanceState.NOT_ARRIVED, TodayAttendanceState.LEFT -> {
                        repo.handleEnter("AUTO")
                        action = "handleEnter"
                        Log.d(AutoDetectTag, "action=$action — 在公司内且状态为$todayState, 触发进入打卡")
                    }
                    TodayAttendanceState.INSIDE, TodayAttendanceState.PENDING -> {
                        action = "skip_already_inside"
                        Log.d(AutoDetectTag, "action=$action — 已在公司内或等待确认中")
                    }
                }
            } else {
                when (todayState) {
                    TodayAttendanceState.INSIDE -> {
                        repo.handleExit()
                        action = "handleExit"
                        Log.d(AutoDetectTag, "action=$action — 已离开公司且之前在公司内, 触发离开打卡")
                    }
                    TodayAttendanceState.NOT_ARRIVED, TodayAttendanceState.LEFT, TodayAttendanceState.PENDING -> {
                        action = "skip_already_outside"
                        Log.d(AutoDetectTag, "action=$action — 不在公司内或不需操作")
                    }
                }
            }

            lastInsideCompany = inside
            dataStore.updateAutoDetectResult(inside, distance, if (inside) "公司内" else "公司外")
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "服务销毁")
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
        lastInsideCompany = null
        scope.launch {
            SettingsDataStore(this@AttendanceLocationService).setAutoDetectEnabled(false)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "自动打卡检测",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "用于自动记录进出公司"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        Log.d(FG_TAG, "notification channel created")
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Attendance Tracker")
            .setContentText("自动打卡检测运行中")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "AttendanceLocationService"
        private const val FG_TAG = "ForegroundService"
        private const val AutoDetectTag = "AutoDetect"
        private const val CHANNEL_ID = "attendance_location_service"
        private const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, AttendanceLocationService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "请求启动服务")
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AttendanceLocationService::class.java))
            Log.d(TAG, "请求停止服务")
        }
    }
}