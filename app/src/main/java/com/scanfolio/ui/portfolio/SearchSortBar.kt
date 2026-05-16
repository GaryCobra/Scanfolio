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
