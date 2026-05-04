package com.example.attendance.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    fun formatTimestamp(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatDuration(startMillis: Long, endMillis: Long): String {
        val diff = endMillis - startMillis
        if (diff < 0) return "0 分钟"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = minutes / 60
        val remainMinutes = minutes % 60
        return if (hours > 0) {
            "${hours} 小时 ${remainMinutes} 分钟"
        } else {
            "${remainMinutes} 分钟"
        }
    }

    fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getTodayEndMillis(): Long {
        return getTodayStartMillis() + 24 * 60 * 60 * 1000L - 1
    }
}