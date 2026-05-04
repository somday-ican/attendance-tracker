package com.example.attendance.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance.data.local.entities.AttendanceRecord
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.data.settings.SettingsDataStore.CompanyLocation
import com.example.attendance.domain.WorkdayChecker
import com.example.attendance.util.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: AttendanceRepository) : ViewModel() {

    val companyLocation: StateFlow<CompanyLocation> = repository.companyLocationFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CompanyLocation()
        )

    private val _todayState = MutableStateFlow("未到公司")
    val todayState: StateFlow<String> = _todayState

    private val _todayRecord = MutableStateFlow<AttendanceRecord?>(null)
    val todayRecord: StateFlow<AttendanceRecord?> = _todayRecord

    private val _isWorkday = MutableStateFlow(true)
    val isWorkday: StateFlow<Boolean> = _isWorkday

    private val _todayDuration = MutableStateFlow("")
    val todayDuration: StateFlow<String> = _todayDuration

    init {
        refreshToday()
    }

    fun refreshToday() {
        viewModelScope.launch {
            val today = DateTimeUtils.getTodayDate()
            val record = repository.getTodayRecord(today)
            val latestEvent = repository.getTodayLatestEvent()
            _todayRecord.value = record
            _isWorkday.value = WorkdayChecker().isWorkday(today)

            when {
                record == null || record.arriveTime == null -> {
                    _todayState.value = "未到公司"
                    _todayDuration.value = ""
                }
                record.status == "PENDING" -> {
                    _todayState.value = "非工作日到达，等待确认"
                    _todayDuration.value = "进行中"
                }
                record.leaveTime != null -> {
                    val isCurrentlyIn = latestEvent?.type == "ENTER"
                    _todayState.value = if (isCurrentlyIn) "已到公司" else "已离开公司"
                    _todayDuration.value = if (isCurrentlyIn) "进行中" else DateTimeUtils.formatDuration(record.arriveTime, record.leaveTime)
                }
                else -> {
                    _todayState.value = "已到公司"
                    _todayDuration.value = "进行中"
                }
            }
        }
    }

    val companyAddressSummary: StateFlow<String> = companyLocation
        .map { location ->
            if (location.address != null) {
                "公司地址: ${location.address}\n围栏半径: ${location.geofenceRadiusMeters}米"
            } else {
                "未设置公司位置"
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "未设置公司位置"
        )

    fun simulateArrive() {
        viewModelScope.launch {
            repository.simulateArrive(DateTimeUtils.getTodayDate())
            refreshToday()
        }
    }

    fun simulateLeave() {
        viewModelScope.launch {
            repository.simulateLeave(DateTimeUtils.getTodayDate())
            refreshToday()
        }
    }
}