package com.example.attendance.data.repository

import com.example.attendance.data.local.AttendanceDao
import com.example.attendance.data.local.entities.AttendanceRecord
import com.example.attendance.data.local.entities.LocationEvent
import com.example.attendance.data.settings.SettingsDataStore
import com.example.attendance.data.settings.SettingsDataStore.CompanyLocation
import com.example.attendance.domain.AttendanceRules
import com.example.attendance.domain.WorkdayChecker
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val settingsDataStore: SettingsDataStore,
    private val workdayChecker: WorkdayChecker = WorkdayChecker()
) {
    val companyLocationFlow: Flow<CompanyLocation> = settingsDataStore.companyLocationFlow

    val allRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllRecords()

    val allEvents: Flow<List<LocationEvent>> = attendanceDao.getAllEvents()

    suspend fun getTodayRecord(date: String): AttendanceRecord? {
        return attendanceDao.getRecordByDate(date)
    }

    suspend fun simulateArrive(date: String) {
        val now = System.currentTimeMillis()
        val isWorkday = workdayChecker.isWorkday(date)
        val status = AttendanceRules.determineStatus(isWorkday)
        val existing = attendanceDao.getRecordByDate(date)
        if (existing != null) {
            attendanceDao.updateRecord(existing.copy(arriveTime = now, status = status, isWorkday = isWorkday))
        } else {
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
        attendanceDao.insertEvent(
            LocationEvent(
                type = "ENTER",
                timestamp = now,
                handled = true
            )
        )
    }

    suspend fun simulateLeave(date: String) {
        val now = System.currentTimeMillis()
        val existing = attendanceDao.getRecordByDate(date)
        if (existing != null) {
            attendanceDao.updateRecord(existing.copy(leaveTime = now))
        }
        attendanceDao.insertEvent(
            LocationEvent(
                type = "EXIT",
                timestamp = now,
                handled = true
            )
        )
    }
}