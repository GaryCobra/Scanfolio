package com.scanfolio.ui.portfolio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scanfolio.ui.theme.DownGreen
import com.scanfolio.ui.theme.UpRed

data class DashboardStats(
    val totalPnl: Double,
    val winRate: Double,
    val totalStocks: Int,
    val openPositions: Int,
    val avgWin: Double,
    val avgLoss: Double
)

@Composable
fun DashboardCard(stats: DashboardStats?) {
    if (stats == null) return

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("总盈亏", if (stats.totalPnl >= 0) "+¥%.2f".format(stats.totalPnl) else "-¥%.2f".format(-stats.totalPnl),
                    if (stats.totalPnl >= 0) UpRed else DownGreen)
                MetricItem("胜率", "%.1f%%".format(stats.winRate * 100), MaterialTheme.colorScheme.primary)
                MetricItem("持仓", "${stats.totalStocks}", MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("平均盈利", "+%.2f%%".format(stats.avgWin), UpRed)
                MetricItem("平均亏损", "-%.2f%%".format(-stats.avgLoss), DownGreen)
                MetricItem("在途", "${stats.openPositions}", MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
