package com.scanfolio.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.scanfolio.data.db.entity.MarketIndexDailyRecordEntity
import com.scanfolio.ui.theme.DownGreen
import com.scanfolio.ui.theme.UpRed
import com.scanfolio.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexRecordEntryScreen(
    navController: NavController,
    indexId: Long,
    indexName: String,
    viewModel: SettingsViewModel = viewModel()
) {
    val records by viewModel.getMarketRecords(indexId).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<MarketIndexDailyRecordEntity?>(null) }

    LaunchedEffect(indexId) { viewModel.loadMarketRecords(indexId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$indexName - 每日记录") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加记录")
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
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无记录，点击右上角 + 添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(DateUtils.formatDateOnly(record.date), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("收盘: ${"%.2f".format(record.closeValue)}", style = MaterialTheme.typography.bodySmall)
                                    Text("涨跌: ${"%.2f".format(record.changePercent)}%", style = MaterialTheme.typography.bodySmall, color = if (record.changePercent >= 0) UpRed else DownGreen)
                                }
                                record.kdjStatus?.let {
                                    Text("KDJ: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = {
                                viewModel.marketIndexRepo.deleteRecord(record)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingRecord != null) {
        IndexRecordDialog(
            indexId = indexId,
            existingRecord = editingRecord,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false; editingRecord = null },
            onSaved = { showAddDialog = false; editingRecord = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IndexRecordDialog(
    indexId: Long,
    existingRecord: MarketIndexDailyRecordEntity?,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var date by remember { mutableStateOf(existingRecord?.let { DateUtils.formatDateOnly(it.date) } ?: "") }
    var closeValue by remember { mutableStateOf(existingRecord?.closeValue?.toString() ?: "") }
    var changePercent by remember { mutableStateOf(existingRecord?.changePercent?.toString() ?: "") }
    var kdjStatus by remember { mutableStateOf(existingRecord?.kdjStatus ?: "") }
    var kdjExpanded by remember { mutableStateOf(false) }
    val kdjOptions = listOf("", "金叉", "死叉")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingRecord != null) "编辑记录" else "添加记录") },
        text = {
            Column {
                OutlinedTextField(
                    value = date, onValueChange = { date = it },
                    label = { Text("日期") },
                    placeholder = { Text("yyyy-MM-dd") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = closeValue, onValueChange = { closeValue = it },
                    label = { Text("收盘价") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = changePercent, onValueChange = { changePercent = it },
                    label = { Text("涨跌幅 (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = kdjExpanded, onExpandedChange = { kdjExpanded = it }) {
                    OutlinedTextField(
                        value = kdjStatus, onValueChange = {},
                        readOnly = true, label = { Text("KDJ状态 (可选)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(kdjExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = kdjExpanded, onDismissRequest = { kdjExpanded = false }) {
                        kdjOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.ifBlank { "不记录" }) },
                                onClick = { kdjStatus = opt; kdjExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val dateMs = DateUtils.parseTimestamp(date + " 00:00") ?: return@Button
                val record = MarketIndexDailyRecordEntity(
                    id = existingRecord?.id ?: 0,
                    indexId = indexId,
                    date = dateMs,
                    closeValue = closeValue.toDoubleOrNull() ?: return@Button,
                    changePercent = changePercent.toDoubleOrNull() ?: return@Button,
                    kdjStatus = kdjStatus.ifBlank { null }
                )
                kotlinx.coroutines.GlobalScope.launch {
                    if (existingRecord != null) viewModel.marketIndexRepo.updateRecord(record)
                    else viewModel.marketIndexRepo.addRecord(record)
                }
                onSaved()
            }, enabled = date.isNotBlank() && closeValue.isNotBlank() && changePercent.isNotBlank()) {
                Text("保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
