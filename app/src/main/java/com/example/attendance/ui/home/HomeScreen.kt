package com.example.attendance.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.attendance.util.DateTimeUtils.formatTimestamp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToRecords: () -> Unit
) {
    val companyLocation by viewModel.companyLocation.collectAsState()
    val todayState by viewModel.todayState.collectAsState()
    val todayRecord by viewModel.todayRecord.collectAsState()
    val isWorkday by viewModel.isWorkday.collectAsState()
    val todayDuration by viewModel.todayDuration.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshToday()
    }

    val locationText = if (companyLocation.address != null) {
        "公司地址: ${companyLocation.address}\n围栏半径: ${companyLocation.geofenceRadiusMeters}米"
    } else {
        "未设置公司位置"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Attendance Tracker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "今日打卡状态",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isWorkday) "工作日" else "非工作日",
            style = MaterialTheme.typography.titleSmall,
            color = if (isWorkday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.padding(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = todayState,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (todayState) {
                        "已离开公司" -> MaterialTheme.colorScheme.tertiary
                        "已到公司" -> MaterialTheme.colorScheme.primary
                        "非工作日到达，等待确认" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                if (todayRecord != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    todayRecord?.arriveTime?.let {
                        Text(
                            text = "上班: ${formatTimestamp(it)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    todayRecord?.leaveTime?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "下班: ${formatTimestamp(it)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (todayDuration.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "今日时长: $todayDuration",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "公司位置",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = locationText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "提示：需要后台定位权限才能自动记录进出公司",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { viewModel.simulateArrive() }) {
                Text("模拟进入公司")
            }
            Button(onClick = { viewModel.simulateLeave() }) {
                Text("模拟离开公司")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToSettings) {
            Text(text = "设置公司位置")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onNavigateToRecords) {
            Text(text = "查看打卡记录")
        }
    }
}