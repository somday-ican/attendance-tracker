package com.example.attendance.location

import android.content.Context
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch

class PoiSearchManager(private val context: Context) {

    data class PoiResultInfo(
        val name: String,
        val address: String,
        val latitude: Double,
        val longitude: Double
    )

    fun search(
        keyword: String,
        city: String = "",
        onResult: (List<PoiResultInfo>) -> Unit,
        onError: () -> Unit
    ) {
        try {
            val query = PoiSearch.Query(keyword, "", city).apply {
                pageSize = 20
                pageNum = 0
            }
            val search = PoiSearch(context, query)
            search.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                override fun onPoiSearched(result: PoiResult?, rCode: Int) {
                    if (rCode == 1000 && result != null) {
                        val pois = result.pois
                        if (pois != null && pois.isNotEmpty()) {
                            onResult(pois.map { it.toInfo() })
                        } else {
                            onResult(emptyList())
                        }
                    } else {
                        onError()
                    }
                }

                override fun onPoiItemSearched(result: com.amap.api.services.core.PoiItem?, rCode: Int) {}
            })
            search.searchPOIAsyn()
        } catch (e: Exception) {
            onError()
        }
    }

    private fun com.amap.api.services.core.PoiItem.toInfo(): PoiResultInfo {
        return PoiResultInfo(
            name = title ?: "",
            address = snippet ?: "",
            latitude = latLonPoint?.latitude ?: 0.0,
            longitude = latLonPoint?.longitude ?: 0.0
        )
    }
}