package com.example.attendance.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendance.BuildConfig
import com.example.attendance.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToRecords: () -> Unit
) {
    val screenState by viewModel.screenState.collectAsState()
    val companyLocation by viewModel.companyLocation.collectAsState()
    val autoState by viewModel.autoDetectState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val todayFormatted = rememberTodayFormatted()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Top Header ──
        HeaderSection(
            todayFormatted = todayFormatted,
            isWorkday = screenState.isWorkday,
            autoEnabled = autoState.enabled
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Main Status Card ──
        MainStatusCard(
            stateText = screenState.todayStateText,
            duration = screenState.todayDuration,
            record = screenState.record
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Timeline Card ──
        TimelineCard(record = screenState.record)

        Spacer(modifier = Modifier.height(16.dp))

        // ── Auto Detect Card ──
        AutoDetectCard(
            autoState = autoState,
            onStart = { viewModel.onStartAutoDetect(context) },
            onStop = { viewModel.onStopAutoDetect(context) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Company Location Card ──
        CompanyLocationCard(
            companyLocation = companyLocation,
            autoState = autoState
        )

        // ── Navigation Buttons ──
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            ) {
                Text("设置")
            }
            Button(
                onClick = onNavigateToRecords,
                modifier = Modifier.weight(1f)
            ) {
                Text("打卡记录")
            }
        }

        // ── Developer Debug Section ──
        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "开发者测试",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.simulateArrive() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("模拟进入", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.simulateLeave() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("模拟离开", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val event = screenState.debugLatestEvent
                    Text(
                        text = "最近事件: ${event?.let { "${it.type} ${DateTimeUtils.formatTimestamp(it.timestamp)}" } ?: "无"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "公司经纬度: ${"%.4f, %.4f".format(companyLocation.latitude ?: 0.0, companyLocation.longitude ?: 0.0)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Helper: today formatted string ──
@Composable
private fun rememberTodayFormatted(): String {
    val now = remember { Date() }
    val dateFormat = remember { SimpleDateFormat("M月d日", Locale.CHINESE) }
    val dayNames = remember { arrayOf("日", "一", "二", "三", "四", "五", "六") }
    val cal = remember { Calendar.getInstance().apply { time = now } }
    val dayOfWeek = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
    return "${dateFormat.format(now)} 周$dayOfWeek"
}

// ── Header ──
@Composable
private fun HeaderSection(
    todayFormatted: String,
    isWorkday: Boolean,
    autoEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "今天",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$todayFormatted · ${if (isWorkday) "工作日" else "休息日"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (autoEnabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = if (autoEnabled) "已开启" else "未开启",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (autoEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Main Status Card ──
@Composable
private fun MainStatusCard(
    stateText: String,
    duration: String,
    record: com.example.attendance.data.local.entities.AttendanceRecord?
) {
    val (bgColor, statusColor) = when (stateText) {
        "已到公司" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "已离开公司" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "非工作日到达，等待确认" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        else -> Color(0xFFF5F5F5) to Color(0xFF757575)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stateText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusDescription(stateText),
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            if (duration.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "今日工时 $duration",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun statusDescription(stateText: String): String {
    return when (stateText) {
        "已到公司" -> "你已到达公司范围"
        "已离开公司" -> "今日已离开公司"
        "非工作日到达，等待确认" -> "非工作日打卡待确认"
        else -> "尚未打卡"
    }
}

// ── Timeline Card ──
@Composable
private fun TimelineCard(
    record: com.example.attendance.data.local.entities.AttendanceRecord?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "今日打卡",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimelineItem(
                    label = "上班",
                    value = record?.arriveTime?.let { DateTimeUtils.formatTimestamp(it) } ?: "--:--"
                )
                TimelineDivider()
                TimelineItem(
                    label = "下班",
                    value = record?.leaveTime?.let { DateTimeUtils.formatTimestamp(it) } ?: "--:--"
                )
                TimelineDivider()
                TimelineItem(
                    label = "工时",
                    value = if (record?.arriveTime != null && record?.leaveTime != null)
                        DateTimeUtils.formatDuration(record.arriveTime, record.leaveTime)
                    else if (record?.arriveTime != null) "进行中"
                    else "--:--"
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TimelineDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

// ── Auto Detect Card ──
@Composable
private fun AutoDetectCard(
    autoState: com.example.attendance.data.settings.SettingsDataStore.AutoDetectState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "自动检测",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (autoState.enabled) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (autoState.enabled) "已开启" else "未开启",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (autoState.enabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (autoState.lastCheckTime != null) {
                InfoRow("最近检测", DateTimeUtils.formatTimestamp(autoState.lastCheckTime))
            }
            if (autoState.lastDistanceToCompany != null) {
                InfoRow("最近距离", "${autoState.lastDistanceToCompany.toInt()} 米")
            }
            InfoRow("当前判断", autoState.lastStatus)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = if (autoState.enabled) onStop else onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (autoState.enabled) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (autoState.enabled) "关闭自动检测" else "开启自动检测")
            }
        }
    }
}

// ── Company Location Card ──
@Composable
private fun CompanyLocationCard(
    companyLocation: com.example.attendance.data.settings.SettingsDataStore.CompanyLocation,
    autoState: com.example.attendance.data.settings.SettingsDataStore.AutoDetectState
) {
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
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (companyLocation.address != null) {
                Text(
                    text = companyLocation.address,
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
            Text(
                text = "围栏半径 ${companyLocation.geofenceRadiusMeters.toInt()} 米",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (autoState.lastDistanceToCompany != null) {
                Text(
                    text = "距离当前 ${autoState.lastDistanceToCompany.toInt()} 米",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}