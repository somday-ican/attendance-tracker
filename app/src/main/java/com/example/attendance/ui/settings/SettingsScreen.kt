package com.example.attendance.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.attendance.R
import com.example.attendance.ui.map.AMapView
import com.example.attendance.ui.map.MapPickerState
import com.example.attendance.ui.map.PoiSearchBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPicker: () -> Unit
) {
    val companyLocation by viewModel.companyLocation.collectAsState()

    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf("") }
    var radiusText by rememberSaveable { mutableStateOf("150") }
    var radiusError by remember { mutableStateOf("") }
    var enableWeekendReminder by remember { mutableStateOf(true) }
    var saveInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(companyLocation) {
        latitudeText = companyLocation.latitude?.toString() ?: ""
        longitudeText = companyLocation.longitude?.toString() ?: ""
        addressText = companyLocation.address ?: ""
        radiusText = companyLocation.geofenceRadiusMeters.toString()
        radiusError = ""
        enableWeekendReminder = companyLocation.enableWeekendReminder
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "设置公司位置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Company Location Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "公司位置",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (addressText.isNotBlank()) {
                    Text(
                        text = addressText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "暂未设置公司位置",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (latitudeText.isNotBlank() && longitudeText.isNotBlank()) {
                    Text(
                        text = "${latitudeText}, ${longitudeText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Map / Search Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "地图选点",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onNavigateToPicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("打开地图选点")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Manual Input Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "手动输入",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = latitudeText,
                    onValueChange = { latitudeText = it },
                    label = { Text("纬度") },
                    placeholder = { Text("例如 39.9042") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = longitudeText,
                    onValueChange = { longitudeText = it },
                    label = { Text("经度") },
                    placeholder = { Text("例如 116.4074") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addressText,
                    onValueChange = { addressText = it },
                    label = { Text("公司地址") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Radius Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "围栏半径",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d+\\.?\\d*$|^\\d*\\.?\\d+$"))) {
                            radiusText = value
                            radiusError = ""
                        }
                    },
                    label = { Text("米") },
                    placeholder = { Text("150") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = radiusError.isNotEmpty(),
                    supportingText = if (radiusError.isNotEmpty()) {{ Text(radiusError) }} else null
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Reminder Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "提醒设置",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = enableWeekendReminder,
                        onCheckedChange = { enableWeekendReminder = it }
                    )
                    Text(text = "周末提醒打卡")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Save Button ──
        Button(
            onClick = {
                val radiusValue = radiusText.toFloatOrNull()
                if (radiusValue == null || radiusValue <= 0) {
                    radiusError = "半径必须大于 0"
                    return@Button
                }
                if (radiusValue < 10 || radiusValue > 5000) {
                    radiusError = "半径应在 10 到 5000 米之间"
                    return@Button
                }
                saveInProgress = true
                viewModel.saveCompanyLocation(
                    latitude = latitudeText,
                    longitude = longitudeText,
                    address = addressText,
                    geofenceRadiusMeters = radiusText,
                    enableWeekendReminder = enableWeekendReminder,
                    onSaved = {
                        saveInProgress = false
                        onNavigateBack()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saveInProgress
        ) {
            Text(if (saveInProgress) "保存中..." else "保存设置")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                viewModel.clearCompanyLocation()
                latitudeText = ""
                longitudeText = ""
                addressText = ""
                radiusText = "150"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("清除公司位置")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}