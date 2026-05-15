package com.scanfolio.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonUtils {
    private val gson = Gson()

    fun toJson(obj: Any): String = gson.toJson(obj)

    inline fun <reified T> fromJson(json: String): T? =
        try { gson.fromJson(json, T::class.java) } catch (_: Exception) { null }

    fun mapToJson(map: Map<String, String>): String = gson.toJson(map)

    fun jsonToMap(json: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return try { gson.fromJson(json, type) } catch (_: Exception) { emptyMap() }
    }
}
