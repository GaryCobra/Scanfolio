package com.scanfolio

import android.app.Application
import android.content.Context
import com.scanfolio.data.db.AppDatabase
import com.scanfolio.data.repository.*
import com.scanfolio.ocr.*

class MomentumApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val stockRepository by lazy { StockRepository(database.stockRecordDao()) }
    val tradeRepository by lazy { TradeRepository(database.tradeRecordDao()) }
    val settingsRepository by lazy {
        SettingsRepository(database.columnDefinitionDao(), database.strategyTypeDao())
    }
    val marketIndexRepository by lazy {
        MarketIndexRepository(
            database.marketIndexDefinitionDao(),
            database.marketIndexDailyRecordDao()
        )
    }

    val ocrEngine by lazy { OcrEngine(this) }
    val tableAnalyzer by lazy { TableAnalyzer(ocrEngine) }

    private val prefs by lazy {
        getSharedPreferences("scanfolio_prefs", Context.MODE_PRIVATE)
    }

    fun getBaiduOcrApiKey(): String? = prefs.getString("baidu_ocr_api_key", null)
    fun getBaiduOcrSecretKey(): String? = prefs.getString("baidu_ocr_secret_key", null)

    fun setBaiduOcrKeys(apiKey: String, secretKey: String) {
        prefs.edit()
            .putString("baidu_ocr_api_key", apiKey)
            .putString("baidu_ocr_secret_key", secretKey)
            .apply()
    }
}
