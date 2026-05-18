package com.scanfolio.ui.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.MomentumApp
import com.scanfolio.data.api.StockFullData
import com.scanfolio.data.api.StockQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MomentumApp
    private val stockRepo = app.stockRepository
    private val api = app.stockApiClient
    private val indicatorCalc = app.indicatorCalculator

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResult = MutableStateFlow<StockQuote?>(null)
    val searchResult: StateFlow<StockQuote?> = _searchResult.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _addedMessage = MutableStateFlow<String?>(null)
    val addedMessage: StateFlow<String?> = _addedMessage.asStateFlow()

    val strategies = app.settingsRepository.getStrategies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(query: String) {
        if (query.length <= 6 && query.all { it.isDigit() }) {
            _searchQuery.value = query
            if (query.length == 6) {
                searchStock(query)
            } else {
                _searchResult.value = null
                _error.value = null
            }
        }
    }

    private fun searchStock(code: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _error.value = null
            _searchResult.value = null
            try {
                val quote = withContext(Dispatchers.IO) { api.fetchQuote(code) }
                if (quote != null) {
                    _searchResult.value = quote
                } else {
                    _error.value = "未找到该股票代码：$code"
                }
            } catch (e: Exception) {
                _error.value = "查询失败: ${e.message ?: "网络错误"}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addStock(strategyName: String? = null) {
        val quote = _searchResult.value ?: return
        viewModelScope.launch {
            try {
                val fullQuote = withContext(Dispatchers.IO) {
                    api.fetchFullQuote(quote.code) ?: quote
                }
                val kline = withContext(Dispatchers.IO) {
                    api.fetchKline(quote.code, 60)
                }
                val moneyFlow = withContext(Dispatchers.IO) {
                    api.fetchMoneyFlow(quote.code)
                }

                val dataColumns = api.buildDataColumns(fullQuote, kline, moneyFlow)
                val indicatorColumns = indicatorCalc.toDataColumns(kline)
                val allColumns = dataColumns + indicatorColumns

                stockRepo.mergeScreenshotData(
                    code = quote.code,
                    name = fullQuote.name,
                    newData = allColumns
                )

                _addedMessage.value = "${fullQuote.name} 已加入自选"
                _searchQuery.value = ""
                _searchResult.value = null
            } catch (e: Exception) {
                stockRepo.mergeScreenshotData(
                    code = quote.code,
                    name = quote.name,
                    newData = api.buildDataColumns(quote, emptyList(), com.scanfolio.data.api.MoneyFlow())
                )
                _addedMessage.value = "${quote.name} 已加入自选"
                _searchQuery.value = ""
                _searchResult.value = null
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearAddedMessage() { _addedMessage.value = null }

    fun updateStockStrategy(stockId: Long, strategyName: String) {
        viewModelScope.launch {
            val stock = stockRepo.getById(stockId) ?: return@launch
            stockRepo.update(stock.copy(strategyName = strategyName))
        }
    }
}
