package com.scanfolio.ui.portfolio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.MomentumApp
import com.scanfolio.data.db.entity.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MomentumApp
    private val stockRepo = app.stockRepository
    private val tradeRepo = app.tradeRepository
    private val settingsRepo = app.settingsRepository

    private val allStocks: StateFlow<List<StockRecordEntity>> = stockRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allTrades: StateFlow<List<TradeRecordEntity>> = tradeRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.LAST_SCANNED)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    val enabledColumns: StateFlow<List<ColumnDefinitionEntity>> = settingsRepo.getEnabledColumns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class StockGroup(
        val strategyName: String?,
        val stocks: List<StockRecordEntity>,
        val winRate: Double = 0.0,
        val totalPnl: Double = 0.0
    )

    val groupedStocks: StateFlow<List<StockGroup>> = combine(
        allStocks, allTrades, _searchQuery, _sortOption
    ) { stocks, trades, query, sort ->
        var filtered = stocks
        if (query.isNotBlank()) {
            val q = query.lowercase()
            filtered = stocks.filter {
                it.code.lowercase().contains(q) || it.name.lowercase().contains(q)
            }
        }
        when (sort) {
            SortOption.NAME_ASC -> filtered = filtered.sortedBy { it.name }
            SortOption.NAME_DESC -> filtered = filtered.sortedByDescending { it.name }
            SortOption.CODE -> filtered = filtered.sortedBy { it.code }
            SortOption.LAST_SCANNED -> filtered = filtered.sortedByDescending { it.lastScreenshot }
            SortOption.CHANGE_DESC -> filtered = filtered.sortedByDescending {
                it.dataColumns["涨跌幅"]?.replace("%", "")?.toDoubleOrNull() ?: Double.MIN_VALUE
            }
            SortOption.CHANGE_ASC -> filtered = filtered.sortedBy {
                it.dataColumns["涨跌幅"]?.replace("%", "")?.toDoubleOrNull() ?: Double.MAX_VALUE
            }
        }
        val grouped = filtered.groupBy { it.strategyName }
        grouped.map { (strategy, groupStocks) ->
            val groupTrades = trades.filter { t ->
                groupStocks.any { s -> s.id == t.stockRecordId }
            }
            val closed = groupTrades.filter { it.sellTime != null }
            val winRate = if (closed.isNotEmpty())
                closed.count { it.isSuccess }.toDouble() / closed.size else 0.0
            val totalPnl = closed.sumOf { it.profitAmount ?: 0.0 }
            StockGroup(
                strategyName = strategy,
                stocks = groupStocks,
                winRate = winRate,
                totalPnl = totalPnl
            )
        }.sortedBy { it.strategyName ?: "zzz" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stocks: StateFlow<List<StockRecordEntity>> = combine(
        allStocks, _searchQuery, _sortOption
    ) { stocks, query, sort ->
        var filtered = stocks
        if (query.isNotBlank()) {
            val q = query.lowercase()
            filtered = stocks.filter {
                it.code.lowercase().contains(q) || it.name.lowercase().contains(q)
            }
        }
        when (sort) {
            SortOption.NAME_ASC -> filtered.sortedBy { it.name }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.name }
            SortOption.CODE -> filtered.sortedBy { it.code }
            SortOption.LAST_SCANNED -> filtered.sortedByDescending { it.lastScreenshot }
            SortOption.CHANGE_DESC -> filtered.sortedByDescending {
                it.dataColumns["涨跌幅"]?.replace("%", "")?.toDoubleOrNull() ?: Double.MIN_VALUE
            }
            SortOption.CHANGE_ASC -> filtered.sortedBy {
                it.dataColumns["涨跌幅"]?.replace("%", "")?.toDoubleOrNull() ?: Double.MAX_VALUE
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats?> = combine(
        allTrades, allStocks
    ) { trades, stocks ->
        if (trades.isEmpty()) return@combine null
        val closed = trades.filter { it.sellTime != null }
        if (closed.isEmpty()) return@combine DashboardStats(0.0, 0.0, stocks.size, trades.size, 0.0, 0.0)
        val totalPnl = closed.sumOf { it.profitAmount ?: 0.0 }
        val wins = closed.count { it.isSuccess }
        val winRate = wins.toDouble() / closed.size
        val avgWin = closed.filter { it.isSuccess }.let {
            if (it.isEmpty()) 0.0 else it.sumOf { t -> t.profitRatio ?: 0.0 } / it.size
        }
        val avgLoss = closed.filter { !it.isSuccess }.let {
            if (it.isEmpty()) 0.0 else it.sumOf { t -> t.profitRatio ?: 0.0 } / it.size
        }
        DashboardStats(
            totalPnl = totalPnl,
            winRate = winRate,
            totalStocks = stocks.size,
            openPositions = trades.count { it.sellTime == null },
            avgWin = avgWin,
            avgLoss = kotlin.math.abs(avgLoss)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortOption(option: SortOption) { _sortOption.value = option }

    fun deleteStock(stock: StockRecordEntity) {
        viewModelScope.launch { stockRepo.delete(stock) }
    }

    fun addStockManually(code: String, name: String) {
        viewModelScope.launch {
            stockRepo.mergeScreenshotData(code, name, emptyMap())
        }
    }
}
