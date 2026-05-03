package com.example.attendance.ui.map

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import kotlinx.coroutines.delay

@Composable
fun AMapView(
    state: MapPickerState,
    onMapClicked: (Double, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val aMapRef = remember { mutableStateOf<AMap?>(null) }
    val mapView = remember {
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    LaunchedEffect(Unit) {
        mapView.onCreate(null)
    }

    LaunchedEffect(Unit) {
        var aMap: AMap? = null
        while (aMap == null) {
            aMap = mapView.map
            if (aMap == null) delay(100)
        }
        aMapRef.value = aMap
        setupMap(aMap, state, onMapClicked)
    }

    LaunchedEffect(state.moveToTrigger) {
        val aMap = aMapRef.value ?: return@LaunchedEffect
        if (state.hasMarker) {
            val latLng = LatLng(state.selectedLatitude, state.selectedLongitude)
            aMap.clear()
            aMap.addMarker(MarkerOptions().position(latLng).title("公司位置"))
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { mapView },
        update = { }
    )
}

private fun setupMap(aMap: AMap, state: MapPickerState, onMapClicked: (Double, Double) -> Unit) {
    aMap.uiSettings.isZoomControlsEnabled = true
    aMap.uiSettings.isMyLocationButtonEnabled = false

    if (state.hasMarker) {
        val latLng = LatLng(state.selectedLatitude, state.selectedLongitude)
        aMap.clear()
        aMap.addMarker(MarkerOptions().position(latLng).title("公司位置"))
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
    } else {
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(39.9042, 116.4074), 12f
        ))
    }

    aMap.setOnMapClickListener { latLng ->
        state.selectedLatitude = latLng.latitude
        state.selectedLongitude = latLng.longitude
        state.hasMarker = true
        state.isMapReady = true
        aMap.clear()
        aMap.addMarker(MarkerOptions().position(latLng).title("公司位置"))
        onMapClicked(latLng.latitude, latLng.longitude)
    }

    state.isMapReady = true
}