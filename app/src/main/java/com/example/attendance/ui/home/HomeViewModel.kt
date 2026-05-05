package com.example.attendance.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance.data.local.entities.LocationEvent
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.data.repository.TodayAttendanceState
import com.example.attendance.data.settings.SettingsDataStore
import com.example.attendance.data.settings.SettingsDataStore.AutoDetectState
import com.example.attendance.data.settings.SettingsDataStore.CompanyLocation
import com.example.attendance.domain.WorkdayChecker
import com.example.attendance.location.AttendanceLocationService
import com.example.attendance.util.DateTimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeScreenState(
    val todayStateText: String = "未到公司",
    val todayDuration: String = "",
    val isWorkday: Boolean = true,
    val record: com.example.attendance.data.local.entities.AttendanceRecord? = null,
    val debugLatestEvent: LocationEvent? = null
)

class HomeViewModel(
    private val repository: AttendanceRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val companyLocation: StateFlow<CompanyLocation> = repository.companyLocationFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CompanyLocation()
        )

    val autoDetectState: StateFlow<AutoDetectState> = settingsDataStore.autoDetectStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AutoDetectState()
        )

    val screenState: StateFlow<HomeScreenState> = combine(
        repository.observeTodayAttendanceState(),
        repository.observeTodayRecord(),
        repository.observeTodayLatestEvent(),
        repository.observeIsWorkday()
    ) { state, record, event, isWorkday ->
        val (stateText, duration) = when (state) {
            TodayAttendanceState.NOT_ARRIVED -> "未到公司" to ""
            TodayAttendanceState.PENDING -> "非工作日到达，等待确认" to "进行中"
            TodayAttendanceState.INSIDE -> {
                if (record?.leaveTime != null) "已到公司" to "进行中"
                else "已到公司" to "进行中"
            }
            TodayAttendanceState.LEFT -> {
                val dur = if (record?.arriveTime != null && record?.leaveTime != null)
                    DateTimeUtils.formatDuration(record.arriveTime, record.leaveTime) else ""
                "已离开公司" to dur
            }
        }
        Log.d("HomeScreenState", "todayAttendanceState=$state, stateText=$stateText")
        HomeScreenState(
            todayStateText = stateText,
            todayDuration = duration,
            isWorkday = isWorkday,
            record = record,
            debugLatestEvent = event
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeScreenState()
    )

    fun onStartAutoDetect(context: android.content.Context) {
        viewModelScope.launch {
            settingsDataStore.setAutoDetectEnabled(true)
            Log.d("AutoDetectSwitch", "enabled=true, serviceStartRequested=true")
            AttendanceLocationService.start(context)
        }
    }

    fun onStopAutoDetect(context: android.content.Context) {
        viewModelScope.launch {
            settingsDataStore.setAutoDetectEnabled(false)
            Log.d("AutoDetectSwitch", "enabled=false, serviceStopRequested=true")
            AttendanceLocationService.stop(context)
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
        }
    }

    fun simulateLeave() {
        viewModelScope.launch {
            repository.simulateLeave(DateTimeUtils.getTodayDate())
        }
    }
}