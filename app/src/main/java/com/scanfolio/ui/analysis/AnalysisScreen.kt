package com.scanfolio.ui.analysis

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scanfolio.ui.theme.DownGreen
import com.scanfolio.ui.theme.UpRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel = viewModel()) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val analysisData by viewModel.analysisData.collectAsState()
    val virtualFilter by viewModel.virtualFilter.collectAsState()
    val kdjFilter by viewModel.kdjFilter.collectAsState()
    val showMarket by viewModel.showMarketComparison.collectAsState()
    val marketIndices by viewModel.marketIndices.collectAsState()
    val selectedIndex by viewModel.selectedMarketIndex.collectAsState()
    val comparisons by viewModel.marketComparisons.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据分析") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { viewModel.selectTab(0) }) {
                    Text("成功分析", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { viewModel.selectTab(1) }) {
                    Text("失败分析", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { viewModel.selectTab(2) }) {
                    Text("对比分析", modifier = Modifier.padding(12.dp))
                }
            }

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = virtualFilter == -1,
                    onClick = { viewModel.setVirtualFilter(-1) },
                    label = { Text("全部") }
                )
                FilterChip(
                    selected = virtualFilter == 0,
                    onClick = { viewModel.setVirtualFilter(0) },
                    label = { Text("实盘") }
                )
                FilterChip(
                    selected = virtualFilter == 1,
                    onClick = { viewModel.setVirtualFilter(1) },
                    label = { Text("模拟盘") }
                )

                if (marketIndices.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = showMarket,
                        onClick = { viewModel.toggleMarketComparison() },
                        label = { Text("大盘对比") }
                    )
                }
            }

            if (showMarket && marketIndices.isNotEmpty()) {
                // Market index selector
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    marketIndices.forEach { index ->
                        FilterChip(
                            selected = selectedIndex == index.id,
                            onClick = { viewModel.selectMarketIndex(index.id) },
                            label = { Text(index.name) }
                        )
                    }
                }
            }

            if (analysisData.isEmpty() || analysisData.all { it.successValues.isEmpty() && it.failValues.isEmpty() }) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "暂无交易数据可分析\n请先添加交易记录并填写战法结果",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(analysisData) { data ->
                        AnalysisCard(data = data, mode = selectedTab)
                    }

                    if (showMarket && selectedIndex != null && comparisons.isNotEmpty()) {
                        item {
                            MarketComparisonCard(comparisons = comparisons, indexName = marketIndices.find { it.id == selectedIndex }?.name ?: "")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisCard(data: AnalysisData, mode: Int) {
    if (data.successValues.isEmpty() && data.failValues.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                data.columnName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            when (mode) {
                0 -> StatRow("成功组", data.successValues, data.successAvg, UpRed)
                1 -> StatRow("失败组", data.failValues, data.failAvg, DownGreen)
                2 -> {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        StatColumn("成功", data.successValues, data.successAvg, UpRed)
                        VerticalDivider(modifier = Modifier.height(80.dp))
                        StatColumn("失败", data.failValues, data.failAvg, DownGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketComparisonCard(comparisons: List<MarketComparisonData>, indexName: String) {
    val beatCount = comparisons.count { c ->
        c.stockChange != null && c.marketChange != null && c.stockChange > c.marketChange
    }
    val totalWithData = comparisons.count { it.stockChange != null && it.marketChange != null }
    val goldenCount = comparisons.count { it.kdjStatus == "金叉" }
    val deathCount = comparisons.count { it.kdjStatus == "死叉" }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("大盘对比 ($indexName)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$beatCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = UpRed)
                    Text("跑赢", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${comparisons.size - beatCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DownGreen)
                    Text("跑输", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$goldenCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("金叉日", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$deathCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("死叉日", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, values: List<Double>, avg: Double?, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, fontWeight = FontWeight.Bold, color = color)
        Text("样本数: ${values.size}", style = MaterialTheme.typography.bodySmall)
        avg?.let { Text("均值: ${"%.4f".format(it)}", style = MaterialTheme.typography.bodyMedium) }
        if (values.isNotEmpty()) {
            Text(
                "区间: ${"%.2f".format(values.minOrNull())} ~ ${"%.2f".format(values.maxOrNull())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, values: List<Double>, avg: Double?, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text("${values.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("样本", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        avg?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text("均值 ${"%.2f".format(it)}", style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}
