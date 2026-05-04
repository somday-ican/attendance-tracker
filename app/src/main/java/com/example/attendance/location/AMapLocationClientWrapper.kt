package com.example.attendance.location

import android.content.Context
import android.util.Log
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener

class AMapLocationClientWrapper(private val context: Context) {
    private var locationClient: AMapLocationClient? = null

    fun startOnceLocation(
        onSuccess: (AMapLocation) -> Unit,
        onFail: (errorCode: Int, errorInfo: String) -> Unit
    ) {
        try {
            locationClient = AMapLocationClient(context.applicationContext)
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isOnceLocation = true
                isOnceLocationLatest = true
                isNeedAddress = false
                isMockEnable = false
                httpTimeOut = 10000
            }
            locationClient?.setLocationOption(option)
            locationClient?.setLocationListener(object : AMapLocationListener {
                override fun onLocationChanged(location: AMapLocation?) {
                    if (location != null && location.errorCode == 0) {
                        Log.d(TAG, "定位成功: ${location.latitude}, ${location.longitude}")
                        onSuccess(location)
                    } else {
                        val code = location?.errorCode ?: -1
                        val info = location?.errorInfo ?: "未知错误"
                        val detail = location?.locationDetail ?: ""
                        Log.e(TAG, "定位失败 - 错误码: $code, 错误信息: $info, 详情: $detail")
                        onFail(code, info)
                    }
                    destroy()
                }
            })
            locationClient?.startLocation()
        } catch (e: Exception) {
            Log.e(TAG, "定位初始化异常: ${e.message}", e)
            onFail(-1, "定位初始化异常: ${e.message}")
        }
    }

    fun destroy() {
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
    }

    companion object {
        private const val TAG = "AMapLocation"
    }
}