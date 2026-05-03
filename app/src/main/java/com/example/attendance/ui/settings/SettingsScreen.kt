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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.attendance.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPicker: () -> Unit
) {
    val companyLocation by viewModel.companyLocation.collectAsState()
    val scope = rememberCoroutineScope()
    
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf("") }
    var radiusText by remember { mutableStateOf("150") }
    var enableWeekendReminder by remember { mutableStateOf(true) }
    
    LaunchedEffect(companyLocation) {
        latitudeText = companyLocation.latitude?.toString() ?: ""
        longitudeText = companyLocation.longitude?.toString() ?: ""
        addressText = companyLocation.address ?: ""
        radiusText = companyLocation.geofenceRadiusMeters.toString()
        enableWeekendReminder = companyLocation.enableWeekendReminder
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "公司位置设置",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNavigateToPicker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("选择公司位置（地图选点）")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        TextField(
            value = latitudeText,
            onValueChange = { latitudeText = it },
            label = { Text("纬度 (例如: 39.9042)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = longitudeText,
            onValueChange = { longitudeText = it },
            label = { Text("经度 (例如: 116.4074)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = addressText,
            onValueChange = { addressText = it },
            label = { Text("公司地址") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = radiusText,
            onValueChange = { radiusText = it },
            label = { Text("围栏半径 (米)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "提醒设置",
                style = MaterialTheme.typography.titleMedium
            )
            
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        viewModel.saveCompanyLocation(
                            latitude = latitudeText,
                            longitude = longitudeText,
                            address = addressText,
                            geofenceRadiusMeters = radiusText,
                            enableWeekendReminder = enableWeekendReminder
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存设置")
            }
            
            Button(
                onClick = {
                    scope.launch {
                        viewModel.clearCompanyLocation()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清除设置")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回首页")
            }
        }
    }
}