package com.example.attendance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.attendance.data.local.entities.AttendanceRecord
import com.example.attendance.data.local.entities.LocationEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): AttendanceRecord?

    @Insert
    suspend fun insertEvent(event: LocationEvent): Long

    @Query("SELECT * FROM location_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<LocationEvent>>
}