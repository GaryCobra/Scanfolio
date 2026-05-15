package com.scanfolio.ui.portfolio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.scanfolio.data.db.entity.TradeRecordEntity
import com.scanfolio.data.db.entity.StrategyTypeEntity
import com.scanfolio.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeFormSheet(
    stockId: Long,
    viewModel: StockDetailViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val strategies by viewModel.settingsRepo.getStrategies()
        .collectAsState(initial = emptyList())
    val allStrategies = remember(strategies) {
        if (strategies.isEmpty()) listOf(StrategyTypeEntity(name = "爆量")) else strategies
    }

    var buyTime by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var sellTime by remember { mutableStateOf("") }
    var profitRatio by remember { mutableStateOf("") }
    var profitAmount by remember { mutableStateOf("") }
    var strategyName by remember { mutableStateOf(allStrategies.first().name) }
    var isSuccess by remember { mutableStateOf(true) }
    var strategyExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "添加交易记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = buyTime,
                    onValueChange = { buyTime = it },
                    label = { Text("买入时间") },
                    placeholder = { Text("yyyy-MM-dd HH:mm") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = buyPrice,
                    onValueChange = { buyPrice = it },
                    label = { Text("买入价格") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sellTime,
                    onValueChange = { sellTime = it },
                    label = { Text("卖出时间 (可选)") },
                    placeholder = { Text("yyyy-MM-dd HH:mm") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = profitRatio,
                    onValueChange = { profitRatio = it },
                    label = { Text("获利比例 (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = profitAmount,
                    onValueChange = { profitAmount = it },
                    label = { Text("获利金额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = strategyExpanded,
                    onExpandedChange = { strategyExpanded = it }
                ) {
                    OutlinedTextField(
                        value = strategyName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("战法") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = strategyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = strategyExpanded,
                        onDismissRequest = { strategyExpanded = false }
                    ) {
                        allStrategies.forEach { strategy ->
                            DropdownMenuItem(
                                text = { Text(strategy.name) },
                                onClick = {
                                    strategyName = strategy.name
                                    strategyExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("战法成功", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = isSuccess, onCheckedChange = { isSuccess = it })
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        scope.launch {
                            val buyTimeMs = DateUtils.parseTimestamp(buyTime) ?: return@launch
                            val sellTimeMs = DateUtils.parseTimestamp(sellTime)
                            val duration = if (sellTimeMs != null) (sellTimeMs - buyTimeMs) / 1000 else null

                            viewModel.tradeRepo.insert(
                                TradeRecordEntity(
                                    stockRecordId = stockId,
                                    buyTime = buyTimeMs,
                                    buyPrice = buyPrice.toDoubleOrNull() ?: 0.0,
                                    sellTime = sellTimeMs,
                                    holdingDuration = duration,
                                    profitRatio = profitRatio.toDoubleOrNull(),
                                    profitAmount = profitAmount.toDoubleOrNull(),
                                    strategyName = strategyName,
                                    isSuccess = isSuccess
                                )
                            )
                            onDismiss()
                        }
                    }) { Text("保存") }
                }
            }
        }
    }
}
