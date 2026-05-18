package com.scanfolio.ocr

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class BaiduOcrApi(private val apiKey: String, private val secretKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun recognizeText(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken() ?: return@withContext null
            val base64 = bitmapToBase64(bitmap)
            val body = FormBody.Builder()
                .add("image", base64)
                .build()
            val request = Request.Builder()
                .url("https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic?access_token=$token")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: return@withContext null)
            val wordsResult = json.optJSONArray("words_result") ?: return@withContext null
            if (wordsResult.length() == 0) return@withContext null
            val sb = StringBuilder()
            for (i in 0 until wordsResult.length()) {
                val words = wordsResult.getJSONObject(i).optString("words", "")
                if (sb.isNotEmpty()) sb.append("\n")
                sb.append(words)
            }
            sb.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun getAccessToken(): String? {
        val body = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_id", apiKey)
            .add("client_secret", secretKey)
            .build()
        val request = Request.Builder()
            .url("https://aip.baidubce.com/oauth/2.0/token")
            .post(body)
            .build()
        return try {
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: return null)
            json.optString("access_token", null)
        } catch (e: Exception) {
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
    }
}
