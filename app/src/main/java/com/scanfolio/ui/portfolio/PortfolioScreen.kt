package com.scanfolio.ui.portfolio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.scanfolio.data.db.entity.ColumnDefinitionEntity
import com.scanfolio.data.db.entity.StockRecordEntity
import com.scanfolio.ui.theme.DownGreen
import com.scanfolio.ui.theme.UpRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    navController: NavController,
    viewModel: PortfolioViewModel = viewModel()
) {
    val stocks by viewModel.stocks.collectAsState()
    val columns by viewModel.enabledColumns.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanfolio", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DashboardCard(stats = dashboardStats)

            SearchSortBar(
                query = searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                sortOption = sortOption,
                onSortChange = viewModel::setSortOption
            )

            if (stocks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "未找到匹配股票" else "暂无股票数据",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Text(
                                "请先截图扫描同花顺股票列表",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { navController.navigate("manual_stock_entry") }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("手动添加股票")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(stocks, key = { it.id }) { stock ->
                        StockListItem(
                            stock = stock,
                            columns = columns,
                            onClick = { navController.navigate("stock_detail/${stock.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockListItem(
    stock: StockRecordEntity,
    columns: List<ColumnDefinitionEntity>,
    onClick: () -> Unit
) {
    val dataMap = stock.dataColumns

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stock.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stock.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val previewColumns = columns.take(3)
            if (previewColumns.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    previewColumns.forEach { col ->
                        val value = dataMap[col.name] ?: "--"
                        Column {
                            Text(
                                text = col.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = value,
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
}

private fun valueColor(columnName: String, value: String): Color {
    val isChangeColumn = columnName.contains("涨跌") || columnName.contains("涨幅") ||
            columnName.contains("获利") || columnName == "涨跌幅" || columnName == "20日涨幅"
    if (!isChangeColumn) return Color.Unspecified
    val num = value.replace("%", "").toDoubleOrNull() ?: return Color.Unspecified
    return if (num > 0) UpRed else if (num < 0) DownGreen else Color.Unspecified
}
