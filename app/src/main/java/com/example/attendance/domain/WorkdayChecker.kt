package com.example.attendance.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WorkdayChecker {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun isWorkday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
    }

    fun isWorkday(dateString: String): Boolean {
        val date = dateFormat.parse(dateString) ?: return true
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
    }
}