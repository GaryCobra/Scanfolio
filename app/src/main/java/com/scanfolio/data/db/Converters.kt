package com.scanfolio.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun mapToString(value: Map<String, String>): String =
        gson.toJson(value)

    @TypeConverter
    fun stringToMap(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return try { gson.fromJson(value, type) } catch (_: Exception) { emptyMap() }
    }
}
