# Scanfolio MVP + UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement UI redesign (腾讯理财通-inspired theme + layouts), scan preview flow, trade edit/delete, P&L dashboard, and search/sort for Scanfolio.

**Architecture:** Incremental changes to existing Compose screens and ViewModels. New composable components added alongside existing. Theme globally swapped via Theme.kt. Room/Repository layer unchanged except new ViewModel methods.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Room, AndroidViewModel, Coroutines/Flow

**Design doc:** `docs/superpowers/specs/2026-05-16-scanfolio-features-design.md`

---

### Task 1: Update Theme.kt (Color Palette + UpRed/DownGreen)

**Files:**
- Modify: `app/src/main/java/com/scanfolio/ui/theme/Theme.kt` (full file)

- [ ] **Step 1: Replace light color scheme with 腾讯理财通-inspired palette**

Replace the entire `LightColorScheme` block:

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3a7bd1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFd8e9fb),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF7380a9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFe8ecf4),
    onSecondaryContainer = Color(0xFF1C313A),
    tertiary = Color(0xFFa1a7d9),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFe8eaf6),
    onTertiaryContainer = Color(0xFF1a1b3a),
    surface = Color(0xFFf4f7fb),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF49454F),
    background = Color(0xFFf4f7fb),
    onBackground = Color(0xFF1C1B1F),
    error = Color(0xFFE53935),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFFE0E0E0),
)
```

- [ ] **Step 2: Replace dark color scheme**

```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF83b6ed),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFd8e9fb),
    secondary = Color(0xFF90A4AE),
    onSecondary = Color(0xFF1C313A),
    secondaryContainer = Color(0xFF37474F),
    onSecondaryContainer = Color(0xFFCFD8DC),
    tertiary = Color(0xFFc2a48a),
    onTertiary = Color(0xFF3a2a1a),
    tertiaryContainer = Color(0xFF544030),
    onTertiaryContainer = Color(0xFFe8d5c4),
    surface = Color(0xFF0d0d11),
    onSurface = Color(0xFFf6f0ea),
    surfaceVariant = Color(0xFF1a1a1e),
    onSurfaceVariant = Color(0xFFCAC4D0),
    background = Color(0xFF0d0d11),
    onBackground = Color(0xFFf6f0ea),
    error = Color(0xFFEF9A9A),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF333333),
)
```

- [ ] **Step 3: Add soft versions of UpRed/DownGreen for card backgrounds**

Add after existing `DownGreenLight`:

```kotlin
val UpRedSoft = Color(0xFFFCE4EC)
val DownGreenSoft = Color(0xFFE8F5E9)
```

- [ ] **Step 4: Verify build**

Run: `cd D:\workspace\Scanfolio; .\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/theme/Theme.kt
git commit -m "feat: update theme to 腾讯理财通-inspired blue-purple palette"
```

---

### Task 2: Create DashboardCard Component

**Files:**
- Create: `app/src/main/java/com/scanfolio/ui/portfolio/DashboardCard.kt`

- [ ] **Step 1: Create DashboardCard composable**

```kotlin
package com.scanfolio.ui.portfolio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/portfolio/DashboardCard.kt
git commit -m "feat: add DashboardCard component with P&L summary"
```

---

### Task 3: Create SearchSortBar Component

**Files:**
- Create: `app/src/main/java/com/scanfolio/ui/portfolio/SearchSortBar.kt`

- [ ] **Step 1: Create SearchSortBar composable**

```kotlin
package com.scanfolio.ui.portfolio

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class SortOption(val label: String) {
    NAME_ASC("名称 A→Z"),
    NAME_DESC("名称 Z→A"),
    CODE("代码"),
    LAST_SCANNED("最近扫描"),
    CHANGE_DESC("涨跌幅 ↓"),
    CHANGE_ASC("涨跌幅 ↑")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSortBar(
    query: String,
    onQueryChange: (String) -> Unit,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("搜索股票...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        Box {
            OutlinedButton(onClick = { sortExpanded = true }) {
                Icon(Icons.Default.Sort, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(sortOption.label, maxLines = 1)
            }
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = { onSortChange(option); sortExpanded = false },
                        leadingIcon = {
                            if (option == sortOption) Icon(Icons.Default.Check, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/portfolio/SearchSortBar.kt
git commit -m "feat: add SearchSortBar with search field and sort dropdown"
```

---

### Task 4: Update PortfolioViewModel with Search/Sort/Dashboard Logic

**Files:**
- Modify: `app/src/main/java/com/scanfolio/ui/portfolio/PortfolioViewModel.kt`

- [ ] **Step 1: Add search, sort, dashboard state and logic**

Replace entire file content:

```kotlin
package com.scanfolio.ui.portfolio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.data.db.entity.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp
    private val stockRepo = app.stockRepository
    private val tradeRepo = app.tradeRepository
    private val settingsRepo = app.settingsRepository

    private val allStocks: StateFlow<List<StockRecordEntity>> = stockRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allTrades: StateFlow<List<TradeRecordEntity>> = tradeRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.LAST_SCANNED)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    val enabledColumns: StateFlow<List<ColumnDefinitionEntity>> = settingsRepo.getEnabledColumns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stocks: StateFlow<List<StockRecordEntity>> = combine(
        allStocks, _searchQuery, _sortOption
    ) { stocks, query, sort ->
        var filtered = stocks
        if (query.isNotBlank()) {
            val q = query.lowercase()
            filtered = stocks.filter {
                it.code.lowercase().contains(q) || it.name.lowercase().contains(q)
            }
        }
        when (sort) {
            SortOption.NAME_ASC -> filtered.sortedBy { it.name }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.name }
            SortOption.CODE -> filtered.sortedBy { it.code }
            SortOption.LAST_SCANNED -> filtered.sortedByDescending { it.lastScreenshot }
            SortOption.CHANGE_DESC -> filtered.sortedByDescending {
                it.dataColumns["涨跌幅"]?.replace("%", "")?.toDoubleOrNull() ?: Double.MIN_VALUE
            }
            SortOption.CHANGE_ASC -> filtered.sortedBy {
                it.dataColumns["涨跌幅"]?.replace("%", "")?.toDoubleOrNull() ?: Double.MAX_VALUE
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats?> = allTrades.map { trades ->
        if (trades.isEmpty()) return@map null
        val closed = trades.filter { it.sellTime != null }
        if (closed.isEmpty()) return@map DashboardStats(0.0, 0.0, allStocks.value.size, trades.size, 0.0, 0.0)
        val totalPnl = closed.sumOf { it.profitAmount ?: 0.0 }
        val wins = closed.count { it.isSuccess }
        val winRate = wins.toDouble() / closed.size
        val avgWin = closed.filter { it.isSuccess }.let { if (it.isEmpty()) 0.0 else it.sumOf { t -> t.profitRatio ?: 0.0 } / it.size }
        val avgLoss = closed.filter { !it.isSuccess }.let { if (it.isEmpty()) 0.0 else it.sumOf { t -> t.profitRatio ?: 0.0 } / it.size }
        DashboardStats(
            totalPnl = totalPnl,
            winRate = winRate,
            totalStocks = allStocks.value.size,
            openPositions = trades.count { it.sellTime == null },
            avgWin = avgWin,
            avgLoss = avgLoss
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortOption(option: SortOption) { _sortOption.value = option }

    fun deleteStock(stock: StockRecordEntity) {
        viewModelScope.launch { stockRepo.delete(stock) }
    }

    fun addStockManually(code: String, name: String) {
        viewModelScope.launch {
            stockRepo.mergeScreenshotData(code, name, emptyMap())
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/portfolio/PortfolioViewModel.kt
git commit -m "feat: add search, sort, and dashboard stats to PortfolioViewModel"
```

---

### Task 5: Update PortfolioScreen with Dashboard + SearchSortBar

**Files:**
- Modify: `app/src/main/java/com/scanfolio/ui/portfolio/PortfolioScreen.kt`

- [ ] **Step 1: Add DashboardCard and SearchSortBar to PortfolioScreen**

Add imports at top:
```kotlin
import androidx.compose.foundation.lazy.rememberLazyListState
```

Replace the scaffold content block with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    navController: NavController,
    viewModel: PortfolioViewModel = viewModel()
) {
    val stocks by viewModel.stocks.collectAsState()
    val columns by viewModel.enabledColumns.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanfolio", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DashboardCard(stats = dashboardStats)

            SearchSortBar(
                query = searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                sortOption = sortOption,
                onSortChange = viewModel::setSortOption
            )

            if (stocks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "未找到匹配股票" else "暂无股票数据",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Text(
                                "请先截图扫描同花顺股票列表",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { navController.navigate("manual_stock_entry") }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("手动添加股票")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(stocks, key = { it.id }) { stock ->
                        StockListItem(
                            stock = stock,
                            columns = columns,
                            onClick = { navController.navigate("stock_detail/${stock.id}") }
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/portfolio/PortfolioScreen.kt
git commit -m "feat: add DashboardCard and SearchSortBar to PortfolioScreen"
```

---

### Task 6: Create ManualStockEntryScreen

**Files:**
- Create: `app/src/main/java/com/scanfolio/ui/portfolio/ManualStockEntryScreen.kt`
- Modify: `app/src/main/java/com/scanfolio/ui/navigation/AppNavigation.kt`

- [ ] **Step 1: Create ManualStockEntryScreen**

```kotlin
package com.scanfolio.ui.portfolio

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualStockEntryScreen(
    navController: NavController,
    viewModel: PortfolioViewModel = viewModel()
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("手动添加股票") },
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            if (saved) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            modifier = Modifier.size(64.dp), tint = com.scanfolio.ui.theme.UpRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("添加成功！", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { navController.popBackStack() }) { Text("返回持仓") }
                    }
                }
            } else {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.take(6) },
                    label = { Text("股票代码") },
                    placeholder = { Text("如：000001") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("股票名称") },
                    placeholder = { Text("如：平安银行") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.addStockManually(code, name)
                        saved = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = code.length == 6 && name.isNotBlank()
                ) {
                    Text("保存")
                }
            }
        }
    }
}
```

- [ ] **Step 2: Add route to AppNavigation.kt**

Add inside the NavHost block:
```kotlin
composable("manual_stock_entry") {
    com.scanfolio.ui.portfolio.ManualStockEntryScreen(navController)
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/portfolio/ManualStockEntryScreen.kt
git add app/src/main/java/com/scanfolio/ui/navigation/AppNavigation.kt
git commit -m "feat: add ManualStockEntryScreen for adding stocks without OCR"
```

---

### Task 7: Create Scan Preview Screen

**Files:**
- Create: `app/src/main/java/com/scanfolio/ui/scan/PreviewScreen.kt`
- Modify: `app/src/main/java/com/scanfolio/ui/scan/ScanViewModel.kt`
- Modify: `app/src/main/java/com/scanfolio/ui/scan/ScanScreen.kt`
- Modify: `app/src/main/java/com/scanfolio/ui/navigation/AppNavigation.kt`

- [ ] **Step 1: Update ScanViewModel with preview state and row editing**

Replace file content:

```kotlin
package com.scanfolio.ui.scan

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.ocr.OcrRow
import com.scanfolio.ocr.TableAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp

    private val _ocrResult = MutableStateFlow<TableAnalyzer.TableResult?>(null)
    val ocrResult: StateFlow<TableAnalyzer.TableResult?> = _ocrResult.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _imported = MutableStateFlow(false)
    val imported: StateFlow<Boolean> = _imported.asStateFlow()

    private val _previewRows = MutableStateFlow<List<OcrRow>>(emptyList())
    val previewRows: StateFlow<List<OcrRow>> = _previewRows.asStateFlow()

    private val _previewHeaders = MutableStateFlow<List<String>>(emptyList())
    val previewHeaders: StateFlow<List<String>> = _previewHeaders.asStateFlow()

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            _isProcessing.value = true
            _error.value = null
            _imported.value = false
            _previewRows.value = emptyList()
            _previewHeaders.value = emptyList()
            try {
                if (app.ocrEngine.getInitError() != null) {
                    _error.value = app.ocrEngine.getInitError()
                    return@launch
                }
                val result = app.tableAnalyzer.analyze(bitmap)
                if (result.rows.isEmpty()) {
                    _error.value = "未识别到有效表格数据，请确认是同花顺股票列表截图"
                    return@launch
                }
                _ocrResult.value = result
                _previewHeaders.value = result.headers
                _previewRows.value = result.rows.toList()
            } catch (e: IllegalStateException) {
                _error.value = e.message ?: "OCR引擎未就绪"
            } catch (e: Exception) {
                _error.value = "识别失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun updatePreviewCell(rowIndex: Int, columnKey: String, newValue: String) {
        val rows = _previewRows.value.toMutableList()
        if (rowIndex >= rows.size) return
        val old = rows[rowIndex]
        val newData = old.data.toMutableMap()
        newData[columnKey] = newValue
        rows[rowIndex] = old.copy(data = newData)
        _previewRows.value = rows
    }

    fun removePreviewRow(rowIndex: Int) {
        val rows = _previewRows.value.toMutableList()
        if (rowIndex in rows.indices) {
            rows.removeAt(rowIndex)
            _previewRows.value = rows
        }
    }

    fun confirmImport() {
        viewModelScope.launch {
            val rows = _previewRows.value
            val headers = _previewHeaders.value
            if (rows.isEmpty()) return@launch
            for (row in rows) {
                app.stockRepository.mergeScreenshotData(
                    code = row.stockCode,
                    name = row.stockName,
                    newData = row.data
                )
            }
            _imported.value = true
            _ocrResult.value = null
            _previewRows.value = emptyList()
            _previewHeaders.value = emptyList()
        }
    }

    fun clearError() { _error.value = null }
}
```

- [ ] **Step 2: Create PreviewScreen composable**

```kotlin
package com.scanfolio.ui.scan

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    navController: NavController,
    viewModel: ScanViewModel = viewModel()
) {
    val headers by viewModel.previewHeaders.collectAsState()
    val rows by viewModel.previewRows.collectAsState()
    val imported by viewModel.imported.collectAsState()

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
                    Text("共导入 ${rows.size} 只股票", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { navController.navigate("portfolio") }) { Text("查看持仓") }
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
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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

Let me just have proper code:

For Step 2, the PreviewScreen code should be clean. Let me write it properly.

- [ ] **Step 3: Update ScanScreen to auto-navigate to PreviewScreen after OCR**

Add a `LaunchedEffect` in ScanScreen that observes `result` — when it becomes non-null with rows, navigate to preview:

```kotlin
val result by viewModel.ocrResult.collectAsState()
// ... inside Scaffold, before the Box block:
LaunchedEffect(result) {
    if (result != null && result.rows.isNotEmpty()) {
        navController.navigate("preview")
    }
}
```

Remove the `imported` state success display from ScanScreen (preview screen handles it now).

- [ ] **Step 4: Add preview route to AppNavigation.kt**

```kotlin
composable("preview") {
    com.scanfolio.ui.scan.PreviewScreen(navController)
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/scan/PreviewScreen.kt
git add app/src/main/java/com/scanfolio/ui/scan/ScanViewModel.kt
git add app/src/main/java/com/scanfolio/ui/scan/ScanScreen.kt
git add app/src/main/java/com/scanfolio/ui/navigation/AppNavigation.kt
git commit -m "feat: add scan preview screen with editable cells and delete rows"
```

---

### Task 8: Update StockDetailViewModel with Trade Edit/Delete and Data Editing

**Files:**
- Modify: `app/src/main/java/com/scanfolio/ui/portfolio/StockDetailScreen.kt`
- Modify: `app/src/main/java/com/scanfolio/ui/portfolio/StockDetailViewModel.kt`

- [ ] **Step 1: Replace StockDetailViewModel with edit/delete methods**

```kotlin
package com.scanfolio.ui.portfolio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.data.db.entity.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StockDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp
    private val stockRepo = app.stockRepository
    val tradeRepo = app.tradeRepository
    val settingsRepo = app.settingsRepository

    private val _stock = MutableStateFlow<StockRecordEntity?>(null)
    val stock: StateFlow<StockRecordEntity?> = _stock.asStateFlow()

    private val _trades = MutableStateFlow<List<TradeRecordEntity>>(emptyList())
    val trades: StateFlow<List<TradeRecordEntity>> = _trades.asStateFlow()

    val columns = settingsRepo.getEnabledColumns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var stockId: Long = 0
    private var tradeJob: kotlinx.coroutines.Job? = null

    fun loadStock(id: Long) {
        stockId = id
        tradeJob?.cancel()
        viewModelScope.launch {
            _stock.value = stockRepo.getById(id)
        }
        tradeJob = viewModelScope.launch {
            tradeRepo.getByStockId(id).collect { _trades.value = it }
        }
    }

    fun updateDataColumn(columnName: String, newValue: String) {
        viewModelScope.launch {
            val s = _stock.value ?: return@launch
            val newMap = s.dataColumns.toMutableMap()
            newMap[columnName] = newValue
            val updated = s.copy(dataColumns = newMap)
            stockRepo.update(updated)
            _stock.value = updated
        }
    }

    fun updateTrade(trade: TradeRecordEntity) {
        viewModelScope.launch {
            tradeRepo.update(trade)
        }
    }

    fun deleteTrade(trade: TradeRecordEntity) {
        viewModelScope.launch {
            tradeRepo.delete(trade)
        }
    }
}
```

- [ ] **Step 2: Update StockDetailScreen with edit/delete on data cells and trade list**

Key changes to StockDetailScreen:
1. Make data cells clickable → show EditDataCellDialog
2. Add long-press context menu on TradeListItem
3. Pass edit/delete callbacks

The data cells section in StockDetailScreen becomes:

```kotlin
columns.forEach { col ->
    val value = s.dataColumns[col.name] ?: "--"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(col.name, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(
            onClick = {
                editColumnName = col.name
                editColumnValue = value
                showEditDataDialog = true
            },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(value, fontWeight = FontWeight.Medium,
                color = valueColor(col.name, value))
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Edit, contentDescription = "编辑",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}
```

Add at the bottom of StockDetailScreen (before the closing brace):
```kotlin
if (showEditDataDialog) {
    AlertDialog(
        onDismissRequest = { showEditDataDialog = false },
        title = { Text("编辑 $editColumnName") },
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
                viewModel.updateDataColumn(editColumnName, editColumnValue)
                showEditDataDialog = false
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = { showEditDataDialog = false }) { Text("取消") }
        }
    )
}
```

Make TradeListItem support long-press for context menu. Wrap the Card with:
```kotlin
var showMenu by remember { mutableStateOf(false) }

Box {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { showMenu = true }
            ),
        ...
    ) { ... existing content ... }

    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        DropdownMenuItem(
            text = { Text("编辑") },
            onClick = { onEdit(); showMenu = false },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("删除") },
            onClick = { onDelete(); showMenu = false },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
        )
    }
}
```

Update StockDetailScreen composable to pass callbacks and add state variables:
```kotlin
var showEditDataDialog by remember { mutableStateOf(false) }
var editColumnName by remember { mutableStateOf("") }
var editColumnValue by remember { mutableStateOf("") }

// For trade form dialog - support edit mode
var editingTrade by remember { mutableStateOf<TradeRecordEntity?>(null) }

// In the trade items section:
items(trades, key = { it.id }) { trade ->
    TradeListItem(
        trade = trade,
        onEdit = { editingTrade = trade; showFormDialog = true },
        onDelete = {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { },
                title = { Text("确认删除") },
                text = { Text("确定删除这笔交易记录？") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteTrade(trade) }) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { }) { Text("取消") }
                }
            )
        }
    )
}

// Update TradeFormSheet call to support edit mode
if (showFormDialog) {
    TradeFormSheet(
        stockId = stockId,
        viewModel = viewModel,
        editTrade = editingTrade,
        onDismiss = { showFormDialog = false; editingTrade = null }
    )
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/portfolio/StockDetailScreen.kt
git add app/src/main/java/com/scanfolio/ui/portfolio/StockDetailViewModel.kt
git commit -m "feat: add trade edit/delete and data column editing in stock detail"
```

---

### Task 9: Update TradeFormSheet with Edit Mode

**Files:**
- Modify: `app/src/main/java/com/scanfolio/ui/portfolio/TradeFormSheet.kt`

- [ ] **Step 1: Add edit mode to TradeFormSheet**

Change signature:
```kotlin
@Composable
fun TradeFormSheet(
    stockId: Long,
    viewModel: StockDetailViewModel,
    editTrade: TradeRecordEntity? = null,
    onDismiss: () -> Unit
)
```

Use `editTrade` to pre-fill fields:
```kotlin
var buyTime by remember {
    mutableStateOf(editTrade?.let { com.scanfolio.util.DateUtils.formatTimestamp(it.buyTime) } ?: "")
}
var buyPrice by remember {
    mutableStateOf(editTrade?.let { it.buyPrice.toString() } ?: "")
}
// ... similar for all fields

val scope = rememberCoroutineScope()

// Save button logic:
Button(onClick = {
    scope.launch {
        val buyTimeMs = com.scanfolio.util.DateUtils.parseTimestamp(buyTime) ?: return@launch
        val sellTimeMs = com.scanfolio.util.DateUtils.parseTimestamp(sellTime)
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
}) { Text(if (editTrade != null) "更新" else "保存") }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/portfolio/TradeFormSheet.kt
git commit -m "feat: add edit mode to TradeFormSheet with pre-filled fields"
```

---

### Task 10: Create EditDataCellDialog Component (Shared)

**Files:**
- Create: `app/src/main/java/com/scanfolio/ui/portfolio/EditDataCellDialog.kt`

- [ ] **Step 1: Create reusable data cell edit dialog**

```kotlin
package com.scanfolio.ui.portfolio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditDataCellDialog(
    columnName: String,
    currentValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑 $columnName") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/portfolio/EditDataCellDialog.kt
git commit -m "feat: add reusable EditDataCellDialog component"
```

---

### Task 11: Verify Build and Fix Compilation Errors

**Files:**
- All modified and created files

- [ ] **Step 1: Run assembleDebug to check for compilation errors**

Run: `cd D:\workspace\Scanfolio; .\gradlew.bat assembleDebug 2>&1`

Expected: BUILD SUCCESSFUL

If there are errors, fix them one by one and repeat.

- [ ] **Step 2: Commit any fixes**

```bash
git add -A
git commit -m "fix: compilation fixes after MVP feature implementation"
```

---

## V2 Phase (Future Tasks — Not in Current Scope)

The following are documented for future implementation but NOT part of this plan:

1. **Data Visualization with Charts** — AnalysisScreen charts using MPAndroidChart
2. **Portfolio Summary Dashboard Enhancements** — sparklines, sector grouping
3. **Data Backup Reminder** — periodic notification to export data
