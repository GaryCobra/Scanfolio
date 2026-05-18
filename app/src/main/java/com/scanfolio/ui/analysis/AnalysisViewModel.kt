package com.scanfolio.ui.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.MomentumApp
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

data class MarketComparisonData(
    val tradeDate: Long,
    val stockChange: Double?,
    val marketChange: Double?,
    val kdjStatus: String?
)

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MomentumApp
    private val stockRepo = app.stockRepository
    private val tradeRepo = app.tradeRepository
    private val settingsRepo = app.settingsRepository
    private val marketRepo = app.marketIndexRepository

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _virtualFilter = MutableStateFlow(-1) // -1=all, 0=real, 1=virtual
    val virtualFilter: StateFlow<Int> = _virtualFilter.asStateFlow()

    private val _kdjFilter = MutableStateFlow(-1) // -1=all, 0=golden, 1=death
    val kdjFilter: StateFlow<Int> = _kdjFilter.asStateFlow()

    private val _showMarketComparison = MutableStateFlow(false)
    val showMarketComparison: StateFlow<Boolean> = _showMarketComparison.asStateFlow()

    val columns: StateFlow<List<ColumnDefinitionEntity>> = settingsRepo.getEnabledColumns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val marketIndices: StateFlow<List<MarketIndexDefinitionEntity>> = marketRepo.getAllDefinitions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMarketIndex = MutableStateFlow<Long?>(null)
    val selectedMarketIndex: StateFlow<Long?> = _selectedMarketIndex.asStateFlow()

    private val _marketComparisons = MutableStateFlow<List<MarketComparisonData>>(emptyList())
    val marketComparisons: StateFlow<List<MarketComparisonData>> = _marketComparisons.asStateFlow()

    private val _analysisData = MutableStateFlow<List<AnalysisData>>(emptyList())
    val analysisData: StateFlow<List<AnalysisData>> = _analysisData.asStateFlow()

    init { loadAnalysisData() }

    fun selectTab(index: Int) { _selectedTab.value = index }
    fun setVirtualFilter(filter: Int) { _virtualFilter.value = filter; loadAnalysisData() }
    fun setKdjFilter(filter: Int) { _kdjFilter.value = filter; loadAnalysisData() }
    fun toggleMarketComparison() { _showMarketComparison.value = !_showMarketComparison.value }
    fun selectMarketIndex(id: Long?) { _selectedMarketIndex.value = id; loadMarketComparisons() }

    private fun loadAnalysisData() {
        viewModelScope.launch {
            var allTrades = tradeRepo.getAll().first()
            val allStocks = stockRepo.getAll().first()
            val stockMap = allStocks.associateBy { it.id }

            when (_virtualFilter.value) {
                0 -> allTrades = allTrades.filter { !it.isVirtual }
                1 -> allTrades = allTrades.filter { it.isVirtual }
            }

            when (_kdjFilter.value) {
                0 -> { /* golden cross - handled after market data loaded */ }
                1 -> { /* death cross - handled after market data loaded */ }
            }

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

    private fun loadMarketComparisons() {
        viewModelScope.launch {
            val indexId = _selectedMarketIndex.value ?: return@launch
            val allTrades = tradeRepo.getAll().first()
            val allStocks = stockRepo.getAll().first()
            val stockMap = allStocks.associateBy { it.id }

            val comparisons = allTrades.mapNotNull { trade ->
                val buyDate = normalizeToDay(trade.buyTime)
                val record = marketRepo.getRecordByIndexAndDate(indexId, buyDate)
                val stockChangeStr = stockMap[trade.stockRecordId]
                    ?.dataColumns?.get("涨跌幅")
                MarketComparisonData(
                    tradeDate = buyDate,
                    stockChange = stockChangeStr?.toDoubleOrNull(),
                    marketChange = record?.changePercent,
                    kdjStatus = record?.kdjStatus
                )
            }
            _marketComparisons.value = comparisons
        }
    }

    private fun normalizeToDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
