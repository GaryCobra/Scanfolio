package com.scanfolio.util

import android.content.Context
import com.google.gson.Gson
import com.scanfolio.data.db.entity.*
import com.scanfolio.data.repository.*
import kotlinx.coroutines.flow.first

data class ExportData(
    val version: Int = 1,
    val stockRecords: List<StockRecordEntity>,
    val tradeRecords: List<TradeRecordEntity>,
    val columnDefinitions: List<ColumnDefinitionEntity>,
    val strategyTypes: List<StrategyTypeEntity>,
    val exportTime: Long = System.currentTimeMillis()
)

class ExportImportManager(
    private val context: Context,
    private val stockRepository: StockRepository,
    private val tradeRepository: TradeRepository,
    private val settingsRepository: SettingsRepository
) {
    private val gson = Gson()

    suspend fun exportToJson(): String {
        val stocks = stockRepository.getAll().let { flow ->
            val list = mutableListOf<StockRecordEntity>()
            flow.collect { list.addAll(it) }
            list
        }
        val data = ExportData(
            stockRecords = stocks,
            tradeRecords = tradeRepository.getAll().let { flow ->
                val list = mutableListOf<TradeRecordEntity>()
                flow.collect { list.addAll(it) }
                list
            },
            columnDefinitions = settingsRepository.getAllColumns().let { flow ->
                val list = mutableListOf<ColumnDefinitionEntity>()
                flow.collect { list.addAll(it) }
                list
            },
            strategyTypes = settingsRepository.getStrategies().let { flow ->
                val list = mutableListOf<StrategyTypeEntity>()
                flow.collect { list.addAll(it) }
                list
            }
        )
        return gson.toJson(data)
    }

    suspend fun importFromJson(json: String): Boolean = try {
        val data = gson.fromJson(json, ExportData::class.java) ?: return false
        data.columnDefinitions.forEach { settingsRepository.addColumn(it) }
        data.strategyTypes.forEach { settingsRepository.addStrategy(it.name) }
        data.stockRecords.forEach { stockRepository.insert(it) }
        data.tradeRecords.forEach { tradeRepository.insert(it) }
        true
    } catch (_: Exception) { false }
}
