package com.example.attendance.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val arriveTime: Long? = null,
    val leaveTime: Long? = null,
    val isWorkday: Boolean = true,
    val source: String = "MANUAL",
    val status: String = "CONFIRMED",
    val address: String? = null,
    val note: String? = null
)