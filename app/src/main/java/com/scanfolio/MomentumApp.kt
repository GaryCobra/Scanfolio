package com.scanfolio

import android.app.Application
import com.scanfolio.data.api.StockApiClient
import com.scanfolio.data.db.AppDatabase
import com.scanfolio.data.db.entity.ColumnDefinitionEntity
import com.scanfolio.data.indicator.TechnicalIndicatorCalculator
import com.scanfolio.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            ensureFixedColumns()
        }
    }

    private suspend fun ensureFixedColumns() {
        val existing = settingsRepository.getAllColumnsSync()
        val existingNames = existing.map { it.name }.toSet()
        val toInsert = mutableListOf<ColumnDefinitionEntity>()
        var order = 0
        for ((name, type) in FIXED_COLUMNS) {
            if (name !in existingNames) {
                toInsert.add(ColumnDefinitionEntity(
                    name = name, columnType = type, sortOrder = order, isBuiltIn = true
                ))
            } else {
                val existingCol = existing.find { it.name == name }!!
                if (!existingCol.isBuiltIn) {
                    settingsRepository.markAsBuiltIn(existingCol.id)
                }
            }
            order++
        }
        if (toInsert.isNotEmpty()) {
            settingsRepository.addColumns(toInsert)
        }
    }

    companion object {
        val FIXED_COLUMNS = listOf(
            "最新价" to "number", "涨跌幅" to "percentage", "涨跌额" to "number",
            "今开" to "number", "最高" to "number", "最低" to "number", "昨收" to "number",
            "成交量" to "number", "成交额" to "number", "振幅" to "percentage",
            "换手率" to "percentage", "量比" to "number",
            "市盈率" to "number", "市净率" to "number",
            "总市值" to "number", "流通市值" to "number",
            "20日涨幅" to "percentage", "20日振幅" to "percentage",
            "连续上涨天数" to "number", "月涨跌幅" to "percentage", "涨停次数" to "number",
            "主力净流入" to "number",
            "所属行业" to "text", "所属概念" to "text"
        )
    }
}
