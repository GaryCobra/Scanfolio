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
import com.scanfolio.data.db.entity.MarketIndexDefinitionEntity
import com.scanfolio.util.DateUtils
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
        if (indices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无大盘指数，点击右上角 + 添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
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
