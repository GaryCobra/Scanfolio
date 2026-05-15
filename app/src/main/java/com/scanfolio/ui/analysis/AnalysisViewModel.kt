package com.scanfolio.ui.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.data.db.entity.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AnalysisData(
    val columnName: String,
    val successAvg: Double?,
    val failAvg: Double?,
    val successValues: List<Double>,
    val failValues: List<Double>
)

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp
    private val stockRepo = app.stockRepository
    private val tradeRepo = app.tradeRepository
    private val settingsRepo = app.settingsRepository

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val columns: StateFlow<List<ColumnDefinitionEntity>> = settingsRepo.getEnabledColumns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _analysisData = MutableStateFlow<List<AnalysisData>>(emptyList())
    val analysisData: StateFlow<List<AnalysisData>> = _analysisData.asStateFlow()

    init { loadAnalysisData() }

    fun selectTab(index: Int) { _selectedTab.value = index }

    private fun loadAnalysisData() {
        viewModelScope.launch {
            val allTrades = tradeRepo.getAll().first()
            val allStocks = stockRepo.getAll().first()
            val stockMap = allStocks.associateBy { it.id }

            val successTrades = allTrades.filter { it.isSuccess }
            val failTrades = allTrades.filter { !it.isSuccess }

            val analysis = columns.value.map { col ->
                val successValues = successTrades.mapNotNull { trade ->
                    stockMap[trade.stockRecordId]
                        ?.dataColumns?.get(col.name)?.toDoubleOrNull()
                }
                val failValues = failTrades.mapNotNull { trade ->
                    stockMap[trade.stockRecordId]
                        ?.dataColumns?.get(col.name)?.toDoubleOrNull()
                }
                AnalysisData(
                    columnName = col.name,
                    successAvg = successValues.average().takeIf { successValues.isNotEmpty() },
                    failAvg = failValues.average().takeIf { failValues.isNotEmpty() },
                    successValues = successValues,
                    failValues = failValues
                )
            }
            _analysisData.value = analysis
        }
    }
}
