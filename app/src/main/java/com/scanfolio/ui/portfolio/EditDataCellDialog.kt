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
