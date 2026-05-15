package com.scanfolio.ui.portfolio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.data.db.entity.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp
    private val stockRepo = app.stockRepository
    private val tradeRepo = app.tradeRepository
    private val settingsRepo = app.settingsRepository

    val stocks: StateFlow<List<StockRecordEntity>> = stockRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledColumns: StateFlow<List<ColumnDefinitionEntity>> = settingsRepo.getEnabledColumns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteStock(stock: StockRecordEntity) {
        viewModelScope.launch { stockRepo.delete(stock) }
    }
}
