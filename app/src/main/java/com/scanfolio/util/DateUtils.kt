package com.scanfolio.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatTimestamp(millis: Long): String = dateFormat.format(Date(millis))

    fun formatDateOnly(millis: Long): String = dateOnlyFormat.format(Date(millis))

    fun parseTimestamp(text: String): Long? {
        return try { dateFormat.parse(text)?.time } catch (_: Exception) { null }
    }

    fun formatDuration(seconds: Long): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        return if (days > 0) "${days}天${hours}小时" else "${hours}小时"
    }
}
