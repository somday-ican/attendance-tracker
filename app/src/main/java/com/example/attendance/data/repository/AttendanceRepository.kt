package com.example.attendance.data.repository

import com.example.attendance.data.local.AttendanceDao
import com.example.attendance.data.local.entities.AttendanceRecord
import com.example.attendance.data.local.entities.LocationEvent
import com.example.attendance.data.settings.SettingsDataStore
import com.example.attendance.data.settings.SettingsDataStore.CompanyLocation
import com.example.attendance.domain.AttendanceRules
import com.example.attendance.domain.WorkdayChecker
import com.example.attendance.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class TodayAttendanceState {
    NOT_ARRIVED,
    INSIDE,
    LEFT,
    PENDING
}

class AttendanceRepository(
    val attendanceDao: AttendanceDao,
    private val settingsDataStore: SettingsDataStore,
    private val workdayChecker: WorkdayChecker = WorkdayChecker()
) {
    val companyLocationFlow: Flow<CompanyLocation> = settingsDataStore.companyLocationFlow

    val allRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllRecords()

    val allEvents: Flow<List<LocationEvent>> = attendanceDao.getAllEvents()

    suspend fun getTodayRecord(date: String): AttendanceRecord? {
        return attendanceDao.getRecordByDate(date)
    }

    suspend fun getTodayLatestEvent(): LocationEvent? {
        return attendanceDao.getLatestEventOnDate(DateTimeUtils.getTodayStartMillis(), DateTimeUtils.getTodayEndMillis())
    }

    suspend fun getTodayAttendanceStateOnce(): TodayAttendanceState {
        val today = DateTimeUtils.getTodayDate()
        val record = getTodayRecord(today)
        return deriveState(record, getTodayLatestEvent())
    }

    fun observeTodayAttendanceState(): Flow<TodayAttendanceState> {
        val today = DateTimeUtils.getTodayDate()
        val start = DateTimeUtils.getTodayStartMillis()
        val end = DateTimeUtils.getTodayEndMillis()
        return combine(
            attendanceDao.observeRecordByDate(today),
            attendanceDao.observeLatestEventOnDate(start, end)
        ) { record, latestEvent ->
            deriveState(record, latestEvent)
        }.distinctUntilChanged()
    }

    fun observeTodayRecord(): Flow<AttendanceRecord?> {
        return attendanceDao.observeRecordByDate(DateTimeUtils.getTodayDate())
    }

    fun observeTodayLatestEvent(): Flow<LocationEvent?> {
        return attendanceDao.observeLatestEventOnDate(
            DateTimeUtils.getTodayStartMillis(), DateTimeUtils.getTodayEndMillis()
        )
    }

    fun observeIsWorkday(): Flow<Boolean> {
        return observeTodayRecord().map { record ->
            record?.let { workdayChecker.isWorkday(it.date) } ?: workdayChecker.isWorkday(DateTimeUtils.getTodayDate())
        }.distinctUntilChanged()
    }

    private fun deriveState(record: AttendanceRecord?, latestEvent: LocationEvent?): TodayAttendanceState {
        if (record == null || record.arriveTime == null) return TodayAttendanceState.NOT_ARRIVED
        if (record.status == "PENDING") return TodayAttendanceState.PENDING
        return when (latestEvent?.type) {
            "ENTER" -> TodayAttendanceState.INSIDE
            "EXIT" -> TodayAttendanceState.LEFT
            else -> {
                if (record.leaveTime == null) TodayAttendanceState.INSIDE else TodayAttendanceState.LEFT
            }
        }
    }

    suspend fun handleEnter(source: String) {
        val now = System.currentTimeMillis()
        val today = DateTimeUtils.getTodayDate()
        val isWorkday = workdayChecker.isWorkday(now)
        val status = AttendanceRules.determineStatus(isWorkday)

        attendanceDao.insertEvent(LocationEvent(type = "ENTER", timestamp = now, handled = true))

        val existing = attendanceDao.getRecordByDate(today)
        if (existing == null) {
            attendanceDao.insertRecord(
                AttendanceRecord(
                    date = today,
                    arriveTime = now,
                    source = source,
                    status = status,
                    isWorkday = isWorkday
                )
            )
        }
    }

    suspend fun handleExit() {
        val now = System.currentTimeMillis()
        val today = DateTimeUtils.getTodayDate()

        attendanceDao.insertEvent(LocationEvent(type = "EXIT", timestamp = now, handled = true))

        val existing = attendanceDao.getRecordByDate(today)
        if (existing != null) {
            attendanceDao.updateRecord(existing.copy(leaveTime = now))
        }
    }

    suspend fun simulateArrive(date: String) {
        val now = System.currentTimeMillis()

        attendanceDao.insertEvent(LocationEvent(type = "ENTER", timestamp = now, handled = true))

        val existing = attendanceDao.getRecordByDate(date)
        if (existing == null) {
            val isWorkday = workdayChecker.isWorkday(now)
            val status = AttendanceRules.determineStatus(isWorkday)
            attendanceDao.insertRecord(
                AttendanceRecord(
                    date = date,
                    arriveTime = now,
                    source = "MANUAL",
                    status = status,
                    isWorkday = isWorkday
                )
            )
        }
    }

    suspend fun simulateLeave(date: String) {
        val now = System.currentTimeMillis()

        attendanceDao.insertEvent(LocationEvent(type = "EXIT", timestamp = now, handled = true))

        val existing = attendanceDao.getRecordByDate(date)
        if (existing != null) {
            attendanceDao.updateRecord(existing.copy(leaveTime = now))
        }
    }

    suspend fun confirmPendingRecord(date: String) {
        val existing = attendanceDao.getRecordByDate(date)
        if (existing != null && existing.status == "PENDING") {
            attendanceDao.updateRecord(existing.copy(status = "CONFIRMED"))
        }
    }
}