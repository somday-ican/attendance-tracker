package com.example.attendance.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    fun registerGeofence(latitude: Double, longitude: Double, radiusMeters: Float) {
        Log.d(TAG, "开始注册Geofence: lat=$latitude, lng=$longitude, radius=${radiusMeters}m, requestId=$GEOFENCE_ID")

        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Geofence注册失败: 缺少 ACCESS_FINE_LOCATION 权限")
            onGeofenceResult?.invoke(false, "缺少 ACCESS_FINE_LOCATION 权限")
            return
        }

        geofencingClient.addGeofences(geofenceRequest, createPendingIntent())
            .addOnSuccessListener {
                Log.d(TAG, "Geofence注册成功: lat=$latitude, lng=$longitude, radius=${radiusMeters}m")
                onGeofenceResult?.invoke(true, "成功")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Geofence注册失败: ${e.javaClass.simpleName}: ${e.message}")
                val msg = when {
                    e.message?.contains("permission", ignoreCase = true) == true -> "缺少权限: ${e.message}"
                    e.message?.contains("google play", ignoreCase = true) == true -> "Google Play服务异常: ${e.message}"
                    e.message?.contains("battery", ignoreCase = true) == true -> "省电限制: ${e.message}"
                    else -> "${e.javaClass.simpleName}: ${e.message}"
                }
                onGeofenceResult?.invoke(false, msg)
            }
    }

    fun removeGeofence() {
        Log.d(TAG, "移除Geofence: requestId=$GEOFENCE_ID")
        geofencingClient.removeGeofences(listOf(GEOFENCE_ID))
            .addOnSuccessListener {
                Log.d(TAG, "Geofence移除成功")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Geofence移除失败: ${e.message}")
            }
    }

    var onGeofenceResult: ((success: Boolean, message: String) -> Unit)? = null

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val GEOFENCE_ID = "company_geofence"
        private const val TAG = "GeofenceManager"
    }
}