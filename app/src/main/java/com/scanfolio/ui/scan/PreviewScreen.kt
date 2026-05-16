package com.scanfolio.ui.scan

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    navController: NavController,
    viewModel: ScanViewModel = viewModel(LocalContext.current as ComponentActivity)
) {
    val headers by viewModel.previewHeaders.collectAsState()
    val rows by viewModel.previewRows.collectAsState()
    val imported by viewModel.imported.collectAsState()
    val importedCount by viewModel.importedCount.collectAsState()

    if (imported) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("导入成功") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ))
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        modifier = Modifier.size(64.dp), tint = com.scanfolio.ui.theme.UpRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("共导入 $importedCount 只股票", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        navController.popBackStack("scan", inclusive = true)
                        navController.navigate("portfolio")
                    }) { Text("查看持仓") }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预览识别结果") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "取消")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) { Text("取消") }
                    Button(
                        onClick = { viewModel.confirmImport() },
                        modifier = Modifier.weight(1f),
                        enabled = rows.isNotEmpty()
                    ) { Text("确认导入 (${rows.size}只)") }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text("代码", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Text("名称", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                headers.forEach { header ->
                    if (header != "代码名称") {
                        Text(header, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.width(40.dp))
            }
            HorizontalDivider()

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(rows, key = { i, _ -> i }) { index, row ->
                    PreviewRowItem(
                        row = row,
                        headers = headers,
                        onEditCell = { columnKey, newValue ->
                            viewModel.updatePreviewCell(index, columnKey, newValue)
                        },
                        onDelete = { viewModel.removePreviewRow(index) }
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun PreviewRowItem(
    row: com.scanfolio.ocr.OcrRow,
    headers: List<String>,
    onEditCell: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editColumnKey by remember { mutableStateOf("") }
    var editColumnValue by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(row.stockCode, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
        Text(row.stockName, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f))
        headers.forEach { header ->
            if (header != "代码名称") {
                val value = row.data[header] ?: "--"
                TextButton(
                    onClick = {
                        editColumnKey = header
                        editColumnValue = value
                        showEditDialog = true
                    },
                    modifier = Modifier.weight(1.5f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (value == "--") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error)
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("编辑 $editColumnKey") },
            text = {
                OutlinedTextField(
                    value = editColumnValue,
                    onValueChange = { editColumnValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onEditCell(editColumnKey, editColumnValue)
                    showEditDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("取消") }
            }
        )
    }
}
