package com.scanfolio.ui.pnl

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.MomentumApp
import com.scanfolio.data.db.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class PnLStats(
    val totalRealizedPnl: Double = 0.0,
    val totalUnrealizedPnl: Double = 0.0,
    val winRate: Double = 0.0,
    val totalTrades: Int = 0,
    val avgWin: Double = 0.0,
    val avgLoss: Double = 0.0,
    val largestWin: Double = 0.0,
    val largestLoss: Double = 0.0,
    val monthlyPnL: List<MonthlyPnL> = emptyList(),
    val profitCurve: List<CurvePoint> = emptyList()
)

data class MonthlyPnL(val month: String, val pnl: Double)
data class CurvePoint(val date: String, val cumulativePnl: Double)

class PnLViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MomentumApp
    private val tradeRepo = app.tradeRepository
    private val stockRepo = app.stockRepository
    private val api = app.stockApiClient

    private val _pnlStats = MutableStateFlow(PnLStats())
    val pnlStats: StateFlow<PnLStats> = _pnlStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPnL()
    }

    fun loadPnL() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val trades = tradeRepo.getAll().first()
                val stocks = stockRepo.getAll().first()

                val closedTrades = trades.filter { it.sellTime != null }
                val openTrades = trades.filter { it.sellTime == null }

                val totalRealized = closedTrades.sumOf { it.profitAmount ?: 0.0 }

                var totalUnrealized = 0.0
                for (trade in openTrades) {
                    val stock = stocks.find { it.id == trade.stockRecordId }
                    if (stock != null && trade.quantity != null) {
                        val quote = withContext(Dispatchers.IO) {
                            api.fetchQuote(stock.code)
                        }
                        if (quote != null) {
                            val unrealized = (quote.currentPrice - trade.buyPrice) * trade.quantity!!
                            totalUnrealized += unrealized
                        }
                    }
                }

                val wins = closedTrades.count { it.isSuccess }
                val winRate = if (closedTrades.isNotEmpty()) wins.toDouble() / closedTrades.size else 0.0

                val avgWin = closedTrades.filter { it.isSuccess }.let {
                    if (it.isEmpty()) 0.0 else it.sumOf { t -> t.profitRatio ?: 0.0 } / it.size
                }
                val avgLoss = closedTrades.filter { !it.isSuccess }.let {
                    if (it.isEmpty()) 0.0 else it.sumOf { t -> t.profitRatio ?: 0.0 } / it.size
                }

                val largestWin = closedTrades.maxOfOrNull { it.profitAmount ?: 0.0 } ?: 0.0
                val largestLoss = closedTrades.minOfOrNull { it.profitAmount ?: 0.0 } ?: 0.0

                val monthlyMap = mutableMapOf<String, Double>()
                val dateFormat = SimpleDateFormat("yyyy-MM", Locale.CHINA)
                for (trade in closedTrades) {
                    val month = dateFormat.format(Date(trade.buyTime))
                    monthlyMap[month] = (monthlyMap[month] ?: 0.0) + (trade.profitAmount ?: 0.0)
                }
                val monthlyPnL = monthlyMap.entries
                    .sortedBy { it.key }
                    .map { MonthlyPnL(it.key, it.value) }

                val sortedTrades = closedTrades.sortedBy { it.sellTime ?: it.buyTime }
                val curve = mutableListOf<CurvePoint>()
                var cumulative = 0.0
                val dayFormat = SimpleDateFormat("MM-dd", Locale.CHINA)
                for (trade in sortedTrades) {
                    cumulative += trade.profitAmount ?: 0.0
                    curve.add(CurvePoint(dayFormat.format(Date(trade.sellTime ?: trade.buyTime)), cumulative))
                }

                _pnlStats.value = PnLStats(
                    totalRealizedPnl = totalRealized,
                    totalUnrealizedPnl = totalUnrealized,
                    winRate = winRate,
                    totalTrades = closedTrades.size,
                    avgWin = avgWin,
                    avgLoss = kotlin.math.abs(avgLoss),
                    largestWin = largestWin,
                    largestLoss = kotlin.math.abs(largestLoss),
                    monthlyPnL = monthlyPnL,
                    profitCurve = curve
                )
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}
