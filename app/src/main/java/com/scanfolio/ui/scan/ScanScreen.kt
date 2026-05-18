package com.scanfolio.ui.scan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.scanfolio.ui.theme.DownGreen
import com.scanfolio.ui.theme.UpRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    navController: NavController,
    viewModel: ScanViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val error by viewModel.error.collectAsState()
    val addedMessage by viewModel.addedMessage.collectAsState()
    val strategies by viewModel.strategies.collectAsState()

    var selectedStrategy by remember { mutableStateOf<String?>(null) }
    var showStrategyPicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(addedMessage) {
        addedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAddedMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加自选", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateQuery(it) },
                label = { Text("输入6位股票代码") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如: 600036") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isSearching -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("正在查询...")
                }

                error != null -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            error!!,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                searchResult != null -> {
                    val quote = searchResult!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = quote.code,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = quote.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "%.2f".format(quote.currentPrice),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val changeColor = when {
                                    quote.changePercent > 0 -> UpRed
                                    quote.changePercent < 0 -> DownGreen
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (quote.changePercent >= 0) "+%.2f%%".format(quote.changePercent) else "%.2f%%".format(quote.changePercent),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = changeColor
                                    )
                                    Text(
                                        text = if (quote.changeAmount >= 0) "+%.2f".format(quote.changeAmount) else "%.2f".format(quote.changeAmount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = changeColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedButton(
                                onClick = { showStrategyPicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedStrategy ?: "选择分组（可选）")
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.addStock(selectedStrategy) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("加入自选")
                            }
                        }
                    }
                }

                else -> {
                    Icon(
                        Icons.Default.TravelExplore,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "输入股票代码查询并加入自选",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showStrategyPicker) {
        AlertDialog(
            onDismissRequest = { showStrategyPicker = false },
            title = { Text("选择分组") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            selectedStrategy = null
                            showStrategyPicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("不分组")
                    }
                    if (strategies.isEmpty()) {
                        Text("暂无分组，请先在设置中创建战法", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    strategies.forEach { strategy ->
                        TextButton(
                            onClick = {
                                selectedStrategy = strategy.name
                                showStrategyPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strategy.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStrategyPicker = false }) { Text("取消") }
            }
        )
    }
}
