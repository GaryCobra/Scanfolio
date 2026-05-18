package com.scanfolio.ui.pnl

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.scanfolio.ui.theme.DownGreen
import com.scanfolio.ui.theme.UpRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PnLDetailScreen(
    navController: NavController,
    viewModel: PnLViewModel = viewModel()
) {
    val stats by viewModel.pnlStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("盈亏统计") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val totalPnl = stats.totalRealizedPnl + stats.totalUnrealizedPnl
                    Text(
                        "总盈亏",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (totalPnl >= 0) "+¥%.2f".format(totalPnl) else "-¥%.2f".format(-totalPnl),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (totalPnl >= 0) UpRed else DownGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("胜率", "%.1f%%".format(stats.winRate * 100))
                        StatItem("交易次数", "${stats.totalTrades}")
                        StatItem("持仓浮盈", "¥%.2f".format(stats.totalUnrealizedPnl))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("平均盈利", "+%.2f%%".format(stats.avgWin))
                        StatItem("平均亏损", "-%.2f%%".format(stats.avgLoss))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (stats.monthlyPnL.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("月度盈亏", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        MonthlyBarChart(stats.monthlyPnL, Modifier.height(200.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (stats.profitCurve.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("收益曲线", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfitLineChart(stats.profitCurve, Modifier.height(200.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonthlyBarChart(monthlyPnL: List<MonthlyPnL>, modifier: Modifier) {
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                setFitBars(true)
                setScaleEnabled(false)
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                legend.isEnabled = false
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    valueFormatter = IndexAxisValueFormatter(
                        monthlyPnL.map {
                            val parts = it.month.split("-")
                            if (parts.size >= 2) "${parts[1]}月" else it.month
                        }
                    )
                }
                val entries = monthlyPnL.mapIndexed { index, item ->
                    BarEntry(index.toFloat(), item.pnl.toFloat())
                }
                val dataSet = BarDataSet(entries, "月度盈亏").apply {
                    colors = entries.map { entry ->
                        if (entry.y >= 0) Color.rgb(220, 38, 38) else Color.rgb(34, 197, 94)
                    }
                    valueTextSize = 10f
                }
                data = BarData(dataSet)
                invalidate()
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ProfitLineChart(curve: List<CurvePoint>, modifier: Modifier) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setScaleEnabled(true)
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                legend.isEnabled = false
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    labelCount = minOf(curve.size, 5)
                    valueFormatter = IndexAxisValueFormatter(
                        curve.map { it.date }.let {
                            if (it.size <= 5) it
                            else listOf(it.first(), it[it.size / 4], it[it.size / 2], it[3 * it.size / 4], it.last())
                        }
                    )
                }
                val entries = curve.mapIndexed { index, point ->
                    Entry(index.toFloat(), point.cumulativePnl.toFloat())
                }
                val dataSet = LineDataSet(entries, "收益").apply {
                    color = Color.rgb(220, 38, 38)
                    valueTextSize = 10f
                    setCircleColor(Color.rgb(220, 38, 38))
                    circleRadius = 3f
                    lineWidth = 2f
                    setDrawValues(false)
                }
                data = LineData(dataSet)
                invalidate()
            }
        },
        modifier = modifier
    )
}
