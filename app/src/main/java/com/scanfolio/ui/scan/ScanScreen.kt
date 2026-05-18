package com.scanfolio.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    navController: NavController,
    viewModel: ScanViewModel = viewModel(LocalContext.current as ComponentActivity)
) {
    val context = LocalContext.current
    val isProcessing by viewModel.isProcessing.collectAsState()
    val result by viewModel.ocrResult.collectAsState()
    val error by viewModel.error.collectAsState()
    val processedCount by viewModel.processedCount.collectAsState()
    var hasNavigatedToPreview by remember { mutableStateOf(false) }
    var titleTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var showDebug by remember { mutableStateOf(false) }

    LaunchedEffect(result, isProcessing) {
        if (isProcessing) {
            hasNavigatedToPreview = false
        }
        val r = result
        if (r != null && r.rows.isNotEmpty() && !hasNavigatedToPreview) {
            hasNavigatedToPreview = true
            navController.navigate("preview")
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.processImages(uris, context.contentResolver)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            viewModel.setError("需要相机权限才能拍照")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel.processImage(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("截图识别",
                        modifier = Modifier.clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime > 3000) titleTapCount = 1
                            else titleTapCount++
                            lastTapTime = now
                            if (titleTapCount >= 5) {
                                titleTapCount = 0
                                showDebug = true
                            }
                        })
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在识别截图... ($processedCount 只)", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "选择同花顺股票列表截图进行识别\n支持一次性选择多张图片",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("拍照")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("从相册选择（可多选）")
                    }

                    error?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                it,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }

    val rawText by viewModel.rawOcrText.collectAsState()

    if (showDebug && rawText != null) {
        AlertDialog(
            onDismissRequest = { showDebug = false; viewModel.clearRawOcrText() },
            title = { Text("原始OCR文本") },
            text = {
                SelectionContainer {
                    Text(rawText!!, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDebug = false; viewModel.clearRawOcrText() }) {
                    Text("关闭")
                }
            }
        )
    }
}
