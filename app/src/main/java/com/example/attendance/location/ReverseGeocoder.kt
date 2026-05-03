package com.example.attendance.location

import android.content.Context
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery

class ReverseGeocoder(private val context: Context) {
    private val geocodeSearch: GeocodeSearch by lazy { GeocodeSearch(context) }

    fun resolve(
        latitude: Double,
        longitude: Double,
        onResult: (String) -> Unit,
        onError: () -> Unit
    ) {
        try {
            val query = RegeocodeQuery(
                com.amap.api.maps.model.LatLng(latitude, longitude).let {
                    com.amap.api.services.core.LatLonPoint(it.latitude, it.longitude)
                },
                300f,
                GeocodeSearch.AMAP
            )
            geocodeSearch.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onRegeocodeSearched(result: com.amap.api.services.geocoder.RegeocodeResult?, rCode: Int) {
                    if (rCode == 1000 && result != null) {
                        val address = result.regeocodeAddress?.formatAddress
                        if (!address.isNullOrBlank()) {
                            onResult(address)
                        } else {
                            onError()
                        }
                    } else {
                        onError()
                    }
                }

                override fun onGeocodeSearched(result: com.amap.api.services.geocoder.GeocodeResult?, rCode: Int) {}
            })
            geocodeSearch.getFromLocationAsyn(query)
        } catch (e: Exception) {
            onError()
        }
    }
}