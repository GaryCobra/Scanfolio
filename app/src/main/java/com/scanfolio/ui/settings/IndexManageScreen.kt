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
import com.scanfolio.data.api.IndexQuote
import com.scanfolio.data.db.entity.MarketIndexDefinitionEntity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexManageScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val indices by viewModel.marketIndices.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val query by viewModel.indexSearchQuery.collectAsState()
    val searchResult by viewModel.indexSearchResult.collectAsState()
    val isSearching by viewModel.isSearchingIndex.collectAsState()
    val searchError by viewModel.indexSearchError.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("大盘指数管理") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加指数")
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
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.updateIndexQuery(it) },
                    label = { Text("搜索指数代码") },
                    placeholder = { Text("输入6位指数代码") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { viewModel.updateIndexQuery("") }) { Icon(Icons.Default.Close, contentDescription = "清除") } }
                    } else null
                )
            }

            if (isSearching) {
                item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }

            if (searchResult != null) {
                item { SearchResultCard(searchResult!!, viewModel) }
            }

            if (searchError != null) {
                item { Text(searchError!!, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.error) }
            }

            if (indices.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无大盘指数，搜索代码或点击右上角 + 添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(indices, key = { it.id }) { index ->
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
                                Text(index.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                if (index.code.isNotBlank()) {
                                    Text(index.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            TextButton(onClick = {
                                navController.navigate("index_records/${index.id}/${index.name}")
                            }) {
                                Text("录入数据")
                            }
                            IconButton(onClick = {
                                GlobalScope.launch {
                                    viewModel.marketIndexRepo.deleteDefinition(index)
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加大盘指数") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("指数名称") },
                        placeholder = { Text("如：上证指数") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = code, onValueChange = { code = it },
                        label = { Text("指数代码（可选）") },
                        placeholder = { Text("如：000001") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    GlobalScope.launch {
                        viewModel.marketIndexRepo.addDefinition(
                            MarketIndexDefinitionEntity(name = name, code = code)
                        )
                    }
                    showAddDialog = false
                }, enabled = name.isNotBlank()) {
                    Text("添加")
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun SearchResultCard(quote: IndexQuote, viewModel: SettingsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(quote.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(quote.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text("当前: ${"%.2f".format(quote.currentValue)}", style = MaterialTheme.typography.bodyMedium)
                Text("涨跌: ${"%.2f%%".format(quote.changePercent)}", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { viewModel.addIndexFromSearch(quote) }) {
                Text("添加")
            }
        }
    }
}
