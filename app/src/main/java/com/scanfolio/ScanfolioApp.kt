package com.scanfolio

import android.app.Application
import com.scanfolio.data.db.AppDatabase
import com.scanfolio.data.repository.*
import com.scanfolio.ocr.*

class ScanfolioApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val stockRepository by lazy { StockRepository(database.stockRecordDao()) }
    val tradeRepository by lazy { TradeRepository(database.tradeRecordDao()) }
    val settingsRepository by lazy {
        SettingsRepository(database.columnDefinitionDao(), database.strategyTypeDao())
    }

    val ocrEngine by lazy { OcrEngine(this) }
    val tableAnalyzer by lazy { TableAnalyzer(ocrEngine) }
}
