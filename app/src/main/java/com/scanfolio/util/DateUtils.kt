package com.scanfolio.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val dateOnlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun formatTimestamp(millis: Long): String {
        val dt = LocalDateTime.ofEpochSecond(millis / 1000, 0, ZoneId.systemDefault().rules.getOffset(java.time.Instant.ofEpochMilli(millis)))
        return dt.format(dateTimeFormatter)
    }

    fun formatDateOnly(millis: Long): String {
        val dt = LocalDateTime.ofEpochSecond(millis / 1000, 0, ZoneId.systemDefault().rules.getOffset(java.time.Instant.ofEpochMilli(millis)))
        return dt.format(dateOnlyFormatter)
    }

    fun parseTimestamp(text: String): Long? {
        return try {
            val dt = LocalDateTime.parse(text, dateTimeFormatter)
            dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) { null }
    }

    fun formatDuration(seconds: Long): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        return if (days > 0) "${days}天${hours}小时" else "${hours}小时"
    }
}
