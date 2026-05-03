package com.example.attendance.domain

object AttendanceRules {

    fun determineStatus(isWorkday: Boolean): String {
        return if (isWorkday) "CONFIRMED" else "PENDING"
    }

    fun isWorkdayToday(): Boolean {
        return WorkdayChecker().isWorkday(System.currentTimeMillis())
    }
}