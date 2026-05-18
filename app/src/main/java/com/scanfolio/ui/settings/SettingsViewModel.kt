package com.scanfolio.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.MomentumApp
import com.scanfolio.data.api.IndexQuote
import com.scanfolio.data.db.entity.*
import com.scanfolio.util.ExportImportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as MomentumApp
    val settingsRepo = app.settingsRepository
    val exportImportManager = ExportImportManager(
        app, app.stockRepository, app.tradeRepository, settingsRepo
    )

    val marketIndexRepo = app.marketIndexRepository
    val api = app.stockApiClient

    private val _indexSearchQuery = MutableStateFlow("")
    val indexSearchQuery: StateFlow<String> = _indexSearchQuery.asStateFlow()

    private val _indexSearchResult = MutableStateFlow<IndexQuote?>(null)
    val indexSearchResult: StateFlow<IndexQuote?> = _indexSearchResult.asStateFlow()

    private val _isSearchingIndex = MutableStateFlow(false)
    val isSearchingIndex: StateFlow<Boolean> = _isSearchingIndex.asStateFlow()

    private val _indexSearchError = MutableStateFlow<String?>(null)
    val indexSearchError: StateFlow<String?> = _indexSearchError.asStateFlow()

    fun updateIndexQuery(query: String) {
        if (query.length <= 6 && query.all { it.isDigit() }) {
            _indexSearchQuery.value = query
            if (query.length == 6) {
                searchIndex(query)
            } else {
                _indexSearchResult.value = null
                _indexSearchError.value = null
            }
        }
    }

    private fun searchIndex(code: String) {
        viewModelScope.launch {
            _isSearchingIndex.value = true
            _indexSearchError.value = null
            _indexSearchResult.value = null
            try {
                val quote = withContext(Dispatchers.IO) { api.fetchIndexQuote(code) }
                if (quote != null) {
                    _indexSearchResult.value = quote
                } else {
                    _indexSearchError.value = "未找到该指数代码：$code"
                }
            } catch (e: Exception) {
                _indexSearchError.value = "查询失败: ${e.message ?: "网络错误"}"
            } finally {
                _isSearchingIndex.value = false
            }
        }
    }

    fun addIndexFromSearch(quote: IndexQuote) {
        viewModelScope.launch {
            val defId = marketIndexRepo.addDefinition(
                MarketIndexDefinitionEntity(name = quote.name, code = quote.code)
            )
            withContext(Dispatchers.IO) {
                val kline = api.fetchIndexKline(quote.code, 60)
                for (k in kline) {
                    val cal = java.util.Calendar.getInstance()
                    val parts = k.day.split("-")
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                    marketIndexRepo.addRecord(
                        MarketIndexDailyRecordEntity(
                            indexId = defId,
                            date = cal.timeInMillis,
                            closeValue = k.close,
                            changePercent = (k.close - k.open) / k.open * 100
                        )
                    )
                }
            }
            _indexSearchQuery.value = ""
            _indexSearchResult.value = null
        }
    }

    val marketIndices: StateFlow<List<MarketIndexDefinitionEntity>> = marketIndexRepo.getAllDefinitions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _marketRecords = MutableStateFlow<Map<Long, List<MarketIndexDailyRecordEntity>>>(emptyMap())
    fun getMarketRecords(indexId: Long) = _marketRecords.map { it[indexId] ?: emptyList() }

    fun loadMarketRecords(indexId: Long) {
        viewModelScope.launch {
            marketIndexRepo.getRecordsByIndex(indexId).collect { records ->
                _marketRecords.value = _marketRecords.value + (indexId to records)
            }
        }
    }

    val columns: StateFlow<List<ColumnDefinitionEntity>> = settingsRepo.getAllColumns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val strategies: StateFlow<List<StrategyTypeEntity>> = settingsRepo.getStrategies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exportJson = MutableStateFlow<String?>(null)
    val exportJson: StateFlow<String?> = _exportJson.asStateFlow()

    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult.asStateFlow()

    fun exportData() {
        viewModelScope.launch {
            _exportJson.value = exportImportManager.exportToJson()
        }
    }

    fun importData(json: String) {
        viewModelScope.launch {
            val success = exportImportManager.importFromJson(json)
            _importResult.value = if (success) "导入成功" else "导入失败"
        }
    }

    fun clearExportResult() { _exportJson.value = null }
    fun clearImportResult() { _importResult.value = null }

    fun addCustomColumn(name: String, type: String) {
        viewModelScope.launch {
            settingsRepo.addColumn(
                ColumnDefinitionEntity(name = name, columnType = type, sortOrder = columns.value.size)
            )
        }
    }
}
