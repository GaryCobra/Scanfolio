package com.scanfolio.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.MomentumApp
import com.scanfolio.data.db.entity.*
import com.scanfolio.util.ExportImportManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as MomentumApp
    val settingsRepo = app.settingsRepository
    val exportImportManager = ExportImportManager(
        app, app.stockRepository, app.tradeRepository, settingsRepo
    )

    val marketIndexRepo = app.marketIndexRepository

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

    val baiduApiKey = MutableStateFlow(app.getBaiduOcrApiKey() ?: "")
    val baiduSecretKey = MutableStateFlow(app.getBaiduOcrSecretKey() ?: "")

    fun updateBaiduOcrKeys(apiKey: String, secretKey: String) {
        baiduApiKey.value = apiKey
        baiduSecretKey.value = secretKey
        app.setBaiduOcrKeys(apiKey, secretKey)
    }

    fun addDefaultColumns() {
        val defaults = listOf(
            "最新价" to "number", "涨跌幅" to "percentage", "成交量" to "number",
            "20日涨幅" to "percentage", "DDE散户数量" to "number",
            "竞价涨幅" to "percentage", "竞价金额" to "number",
            "个股热度排名" to "number", "人气数值" to "number",
            "涨停次数(年)" to "number", "所属概念" to "text", "所属同花顺行业" to "text"
        )
        viewModelScope.launch {
            defaults.forEachIndexed { index, (name, type) ->
                settingsRepo.addColumn(
                    ColumnDefinitionEntity(name = name, columnType = type, sortOrder = index)
                )
            }
        }
    }
}
