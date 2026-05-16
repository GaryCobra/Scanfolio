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
    editTrade: TradeRecordEntity? = null,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val strategies by viewModel.settingsRepo.getStrategies()
        .collectAsState(initial = emptyList())
    val allStrategies = remember(strategies) {
        if (strategies.isEmpty()) listOf(StrategyTypeEntity(name = "爆量")) else strategies
    }

    var buyTime by remember(editTrade) { mutableStateOf(editTrade?.let { DateUtils.formatTimestamp(it.buyTime) } ?: "") }
    var buyPrice by remember(editTrade) { mutableStateOf(editTrade?.buyPrice?.let { "%.2f".format(it) } ?: "") }
    var sellTime by remember(editTrade) { mutableStateOf(editTrade?.sellTime?.let { DateUtils.formatTimestamp(it) } ?: "") }
    var profitRatio by remember(editTrade) { mutableStateOf(editTrade?.profitRatio?.let { "%.2f".format(it) } ?: "") }
    var profitAmount by remember(editTrade) { mutableStateOf(editTrade?.profitAmount?.let { "%.2f".format(it) } ?: "") }
    var strategyName by remember(editTrade) { mutableStateOf(editTrade?.strategyName ?: allStrategies.first().name) }
    var isSuccess by remember(editTrade) { mutableStateOf(editTrade?.isSuccess ?: true) }
    var strategyExpanded by remember(editTrade) { mutableStateOf(false) }
    var isVirtual by remember(editTrade) { mutableStateOf(editTrade?.isVirtual ?: false) }
    var virtualCapital by remember(editTrade) { mutableStateOf(editTrade?.virtualCapital?.let { "%.2f".format(it) } ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    if (editTrade != null) "编辑交易记录" else "添加交易记录",
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("模拟盘", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = isVirtual, onCheckedChange = { isVirtual = it })
                    Spacer(modifier = Modifier.weight(1f))
                    Text(if (isVirtual) "模拟" else "实盘", color = if (isVirtual) MaterialTheme.colorScheme.primary else com.scanfolio.ui.theme.UpRed)
                }
                if (isVirtual) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = virtualCapital,
                        onValueChange = { virtualCapital = it },
                        label = { Text("虚拟资金总额") },
                        placeholder = { Text("如：1000000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

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

                            val entity = TradeRecordEntity(
                                id = editTrade?.id ?: 0,
                                stockRecordId = stockId,
                                buyTime = buyTimeMs,
                                buyPrice = buyPrice.toDoubleOrNull() ?: 0.0,
                                sellTime = sellTimeMs,
                                holdingDuration = duration,
                                profitRatio = profitRatio.toDoubleOrNull(),
                                profitAmount = profitAmount.toDoubleOrNull(),
                                strategyName = strategyName,
                                isSuccess = isSuccess,
                                isVirtual = isVirtual,
                                virtualCapital = virtualCapital.toDoubleOrNull()
                            )
                            if (editTrade != null) {
                                viewModel.updateTrade(entity)
                            } else {
                                viewModel.tradeRepo.insert(entity)
                            }
                            onDismiss()
                        }
                    }) { Text("保存") }
                }
            }
        }
    }
}
