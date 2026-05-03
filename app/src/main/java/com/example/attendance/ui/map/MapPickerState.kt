package com.example.attendance.ui.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MapPickerState {
    var isMapReady by mutableStateOf(false)
    var selectedLatitude by mutableStateOf(39.9042)
    var selectedLongitude by mutableStateOf(116.4074)
    var geofenceRadius by mutableStateOf(150f)
    var selectedAddress by mutableStateOf("地图选点")
    var hasMarker by mutableStateOf(false)
    var isResolvingAddress by mutableStateOf(false)
    var addressResolveFailed by mutableStateOf(false)
    var moveToTrigger by mutableStateOf(0)
}