package com.example.attendance.location

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener

class AMapLocationClientWrapper(private val context: Context) {
    private var locationClient: AMapLocationClient? = null

    fun startOnceLocation(onSuccess: (AMapLocation) -> Unit, onFail: (String) -> Unit) {
        try {
            locationClient = AMapLocationClient(context)
            val option = AMapLocationClientOption().apply {
                isOnceLocation = true
                isNeedAddress = false
            }
            locationClient?.setLocationOption(option)
            locationClient?.setLocationListener(object : AMapLocationListener {
                override fun onLocationChanged(location: AMapLocation?) {
                    if (location != null && location.errorCode == 0) {
                        onSuccess(location)
                    } else {
                        onFail(location?.errorInfo ?: "定位失败")
                    }
                    destroy()
                }
            })
            locationClient?.startLocation()
        } catch (e: Exception) {
            onFail("定位初始化失败: ${e.message}")
        }
    }

    fun destroy() {
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
    }
}