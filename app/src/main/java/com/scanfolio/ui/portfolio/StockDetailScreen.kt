package com.scanfolio.ui.portfolio

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.scanfolio.ScanfolioApp
import com.scanfolio.data.db.entity.*
import com.scanfolio.ui.theme.DownGreen
import com.scanfolio.ui.theme.UpRed
import com.scanfolio.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StockDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp
    val stockRepo = app.stockRepository
    val tradeRepo = app.tradeRepository
    val settingsRepo = app.settingsRepository

    private val _stock = MutableStateFlow<StockRecordEntity?>(null)
    val stock: StateFlow<StockRecordEntity?> = _stock.asStateFlow()

    val trades = MutableStateFlow<List<TradeRecordEntity>>(emptyList())
    val columns = settingsRepo.getEnabledColumns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadStock(stockId: Long) {
        viewModelScope.launch {
            _stock.value = stockRepo.getById(stockId)
            tradeRepo.getByStockId(stockId).collect { trades.value = it }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    stockId: Long,
    navController: NavController,
    viewModel: StockDetailViewModel = viewModel()
) {
    LaunchedEffect(stockId) { viewModel.loadStock(stockId) }

    val stock by viewModel.stock.collectAsState()
    val trades by viewModel.trades.collectAsState()
    val columns by viewModel.columns.collectAsState()
    var showFormDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stock?.let { "${it.code} ${it.name}" } ?: "股票详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showFormDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加交易")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            stock?.let { s ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "截图数据",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            columns.forEach { col ->
                                val value = s.dataColumns[col.name] ?: "--"
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        col.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = valueColor(col.name, value)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "交易记录",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            if (trades.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "暂无交易记录，点击右上角 + 添加",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(trades, key = { it.id }) { trade ->
                    TradeListItem(trade = trade)
                }
            }
        }
    }

    if (showFormDialog) {
        TradeFormSheet(
            stockId = stockId,
            viewModel = viewModel,
            onDismiss = { showFormDialog = false }
        )
    }
}

@Composable
private fun TradeListItem(trade: TradeRecordEntity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("买入: ${DateUtils.formatTimestamp(trade.buyTime)}", style = MaterialTheme.typography.bodyMedium)
                    Text("价格: ¥${"%.2f".format(trade.buyPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(if (trade.isVirtual) "模拟" else "实盘", fontWeight = FontWeight.Medium) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (trade.isVirtual) MaterialTheme.colorScheme.primaryContainer else UpRed.copy(alpha = 0.1f),
                            labelColor = if (trade.isVirtual) MaterialTheme.colorScheme.onPrimaryContainer else UpRed
                        )
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(if (trade.isSuccess) "成功" else "失败", fontWeight = FontWeight.Medium) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (trade.isSuccess) UpRed.copy(alpha = 0.1f) else DownGreen.copy(alpha = 0.1f),
                            labelColor = if (trade.isSuccess) UpRed else DownGreen
                        )
                    )
                }
            }

            trade.sellTime?.let { sellTime ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("卖出: ${DateUtils.formatTimestamp(sellTime)}", style = MaterialTheme.typography.bodySmall)
                    trade.holdingDuration?.let {
                        Text("持仓: ${DateUtils.formatDuration(it)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    trade.profitRatio?.let {
                        Text("获利: ${"%.2f".format(it)}%", color = if (it >= 0) UpRed else DownGreen, fontWeight = FontWeight.Bold)
                    }
                    trade.profitAmount?.let {
                        Text("金额: ¥${"%.2f".format(it)}", color = if (it >= 0) UpRed else DownGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("战法: ${trade.strategyName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun valueColor(columnName: String, value: String): Color {
    val isChangeColumn = columnName.contains("涨跌") || columnName.contains("涨幅") ||
            columnName.contains("获利") || columnName == "涨跌幅"
    if (!isChangeColumn) return Color.Unspecified
    val num = value.replace("%", "").toDoubleOrNull() ?: return Color.Unspecified
    return if (num > 0) UpRed else if (num < 0) DownGreen else Color.Unspecified
}
