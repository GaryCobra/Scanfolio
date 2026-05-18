package com.scanfolio

import android.app.Application
import com.scanfolio.data.api.StockApiClient
import com.scanfolio.data.db.AppDatabase
import com.scanfolio.data.indicator.TechnicalIndicatorCalculator
import com.scanfolio.data.repository.*

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
    val stockApiClient by lazy { StockApiClient(okhttp3.OkHttpClient()) }
    val indicatorCalculator by lazy { TechnicalIndicatorCalculator() }
}
