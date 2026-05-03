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

    init {
        refreshToday()
    }

    fun refreshToday() {
        viewModelScope.launch {
            val today = DateTimeUtils.getTodayDate()
            val record = repository.getTodayRecord(today)
            _todayRecord.value = record
            _isWorkday.value = WorkdayChecker().isWorkday(today)
            _todayState.value = when {
                record == null || record.arriveTime == null -> "未到公司"
                record.status == "PENDING" -> "非工作日到达，等待确认"
                record.leaveTime == null -> "已到公司"
                else -> "已离开公司"
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
            val today = DateTimeUtils.getTodayDate()
            repository.simulateArrive(today)
            refreshToday()
        }
    }

    fun simulateLeave() {
        viewModelScope.launch {
            val today = DateTimeUtils.getTodayDate()
            repository.simulateLeave(today)
            refreshToday()
        }
    }
}