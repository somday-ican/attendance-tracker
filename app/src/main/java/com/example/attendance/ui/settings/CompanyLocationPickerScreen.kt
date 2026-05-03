package com.example.attendance.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.attendance.location.AMapLocationClientWrapper
import com.example.attendance.location.PoiSearchManager
import com.example.attendance.location.ReverseGeocoder
import com.example.attendance.ui.map.AMapView
import com.example.attendance.ui.map.MapPickerState
import com.example.attendance.ui.map.PoiSearchBox
import com.example.attendance.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyLocationPickerScreen(
    viewModel: CompanyLocationPickerViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val state = remember { MapPickerState() }
    val searchManager = remember { PoiSearchManager(context) }
    var requestPermissions by remember { mutableStateOf(!PermissionUtils.hasLocationPermissions(context)) }
    val geocoder = remember { ReverseGeocoder(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val allGranted = granted.values.all { it }
        if (allGranted) {
            startLocation(context, state)
        }
    }

    LaunchedEffect(requestPermissions) {
        if (requestPermissions && !PermissionUtils.hasLocationPermissions(context)) {
            permissionLauncher.launch(PermissionUtils.LOCATION_PERMISSIONS)
        } else if (PermissionUtils.hasLocationPermissions(context)) {
            startLocation(context, state)
        }
        requestPermissions = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("选择公司位置") },
            navigationIcon = {
                Button(onClick = onNavigateBack) {
                    Text("返回")
                }
            }
        )

        PoiSearchBox(
            searchManager = searchManager,
            onPoiSelected = { poi ->
                state.selectedLatitude = poi.latitude
                state.selectedLongitude = poi.longitude
                state.selectedAddress = "${poi.name} ${poi.address}"
                state.hasMarker = true
                state.moveToTrigger += 1
                state.isResolvingAddress = false
                state.addressResolveFailed = false
            }
        )

        Box(modifier = Modifier.weight(1f)) {
            AMapView(
                state = state,
                onMapClicked = { lat, lng ->
                    state.isResolvingAddress = true
                    state.addressResolveFailed = false
                    geocoder.resolve(
                        latitude = lat,
                        longitude = lng,
                        onResult = { address ->
                            state.selectedAddress = address
                            state.isResolvingAddress = false
                        },
                        onError = {
                            state.selectedAddress = "地址解析失败"
                            state.isResolvingAddress = false
                            state.addressResolveFailed = true
                        }
                    )
                }
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("纬度: ${"%.6f".format(state.selectedLatitude)}")
                    Text("经度: ${"%.6f".format(state.selectedLongitude)}")
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (state.isResolvingAddress) "正在解析地址..." else "地址: ${state.selectedAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.addressResolveFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.geofenceRadius.toInt().toString(),
                    onValueChange = { value -> value.toIntOrNull()?.let { v -> state.geofenceRadius = v.toFloat() } },
                    label = { Text("围栏半径 (米)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.saveLocation(
                            latitude = state.selectedLatitude,
                            longitude = state.selectedLongitude,
                            radius = state.geofenceRadius,
                            address = state.selectedAddress
                        ) {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("确认保存")
                }
            }
        }
    }
}

private fun startLocation(context: android.content.Context, state: MapPickerState) {
    val locationWrapper = AMapLocationClientWrapper(context)
    locationWrapper.startOnceLocation(
        onSuccess = { location ->
            state.selectedLatitude = location.latitude
            state.selectedLongitude = location.longitude
            state.hasMarker = true
            state.selectedAddress = "已定位到当前位置"
        },
        onFail = { }
    )
}