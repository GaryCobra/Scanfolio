# OCR Fix + Batch Processing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix OCR recognition failure and add multi-image batch upload support

**Architecture:** Three-layer OCR routing (ML Kit block-structured → Preprocess+ML Kit retry → Baidu OCR API fallback), plus batch image selection via `GetMultipleContents` and result merge/dedup.

**Tech Stack:** Kotlin, Jetpack Compose, Google ML Kit, OkHttp, Baidu OCR API

---

### Task 1: Build Config — OkHttp + INTERNET permission

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add OkHttp to build.gradle.kts**

```kotlin
// Add under the dependencies block:
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

- [ ] **Step 2: Add INTERNET permission to AndroidManifest.xml**

```xml
<!-- Add after CAMERA permission -->
<uses-permission android:name="android.permission.INTERNET" />
```

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "build: add OkHttp dependency and INTERNET permission for OCR API fallback"
```

---

### Task 2: Implement ImagePreprocessor (grayscale, contrast, OTSU binarization)

**Files:**
- Rewrite: `app/src/main/java/com/scanfolio/ocr/ImagePreprocessor.kt`

- [ ] **Step 1: Rewrite ImagePreprocessor**

```kotlin
package com.scanfolio.ocr

import android.graphics.Bitmap
import android.graphics.Color

object ImagePreprocessor {

    fun enhanceForOcr(bitmap: Bitmap): Bitmap {
        val gray = toGrayscale(bitmap)
        val contrasted = adjustContrast(gray, 1.5f)
        return otsuBinarize(contrasted)
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(result.width * result.height)
        result.getPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(gray, gray, gray)
        }
        result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        return result
    }

    private fun adjustContrast(bitmap: Bitmap, factor: Float): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(result.width * result.height)
        result.getPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        val lookup = IntArray(256) { v ->
            ((v - 128) * factor + 128).toInt().coerceIn(0, 255)
        }
        for (i in pixels.indices) {
            val gray = Color.red(pixels[i])
            pixels[i] = Color.rgb(lookup[gray], lookup[gray], lookup[gray])
        }
        result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        return result
    }

    private fun otsuBinarize(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(result.width * result.height)
        result.getPixels(pixels, 0, result.width, 0, 0, result.width, result.height)

        val histogram = IntArray(256)
        for (pixel in pixels) {
            histogram[Color.red(pixel)]++
        }
        val total = pixels.size
        var sum = 0.0
        for (i in 0..255) sum += i * histogram[i]

        var sumB = 0.0
        var wB = 0
        var wF = 0
        var maxVariance = 0.0
        var threshold = 0
        for (i in 0..255) {
            wB += histogram[i]
            if (wB == 0) continue
            wF = total - wB
            if (wF == 0) break
            sumB += i * histogram[i]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val variance = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (variance > maxVariance) {
                maxVariance = variance
                threshold = i
            }
        }

        for (i in pixels.indices) {
            val gray = Color.red(pixels[i])
            val binary = if (gray >= threshold) 255 else 0
            pixels[i] = Color.rgb(binary, binary, binary)
        }
        result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        return result
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/scanfolio/ocr/ImagePreprocessor.kt
git commit -m "feat: implement ImagePreprocessor with grayscale, contrast, OTSU binarization"
```

---

### Task 3: BaiduOcrApi client + OcrEngine rewrite (3-layer routing)

**Files:**
- Create: `app/src/main/java/com/scanfolio/ocr/BaiduOcrApi.kt`
- Rewrite: `app/src/main/java/com/scanfolio/ocr/OcrEngine.kt`

- [ ] **Step 1: Create BaiduOcrApi.kt**

```kotlin
package com.scanfolio.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class BaiduOcrApi(private val apiKey: String, private val secretKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun recognizeText(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken() ?: return@withContext null
            val base64 = bitmapToBase64(bitmap)
            val body = "image=$base64".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val request = Request.Builder()
                .url("https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic?access_token=$token")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: return@withContext null)
            val wordsResult = json.optJSONArray("words_result") ?: return@withContext null
            if (wordsResult.length() == 0) return@withContext null
            val sb = StringBuilder()
            for (i in 0 until wordsResult.length()) {
                val words = wordsResult.getJSONObject(i).optString("words", "")
                if (sb.isNotEmpty()) sb.append("\n")
                sb.append(words)
            }
            sb.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun getAccessToken(): String? {
        val body = "grant_type=client_credentials&client_id=$apiKey&client_secret=$secretKey"
            .toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val request = Request.Builder()
            .url("https://aip.baidubce.com/oauth/2.0/token")
            .post(body)
            .build()
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: return null)
        return json.optString("access_token", null)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
    }
}
```

- [ ] **Step 2: Rewrite OcrEngine.kt**

```kotlin
package com.scanfolio.ocr

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.android.gms.tasks.Tasks

class OcrEngine(private val context: Context) {
    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    enum class OcrSource { ML_KIT, PREPROCESSED_ML_KIT, BAIDU_API }

    data class OcrResult(
        val text: String,
        val source: OcrSource,
        val mlKitBlocks: List<OcrBlock>? = null
    )

    data class OcrBlock(
        val text: String,
        val lines: List<OcrLine>
    )

    data class OcrLine(
        val text: String,
        val elements: List<OcrElement>
    )

    data class OcrElement(
        val text: String,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    fun recognizeText(bitmap: Bitmap, baiduApiKey: String? = null, baiduSecretKey: String? = null): OcrResult? {
        // Layer 1: ML Kit with structured blocks
        val mlResult = recognizeWithMlKit(bitmap)
        if (mlResult != null && mlResult.text.isNotBlank() && hasReasonableContent(mlResult.text)) {
            return OcrResult(mlResult.text, OcrSource.ML_KIT, mlResult.blocks)
        }

        // Layer 2: Preprocess + ML Kit retry
        val preprocessed = ImagePreprocessor.enhanceForOcr(bitmap)
        val mlRetry = recognizeWithMlKit(preprocessed)
        if (mlRetry != null && mlRetry.text.isNotBlank() && hasReasonableContent(mlRetry.text)) {
            return OcrResult(mlRetry.text, OcrSource.PREPROCESSED_ML_KIT, mlRetry.blocks)
        }

        // Layer 3: Baidu OCR API fallback
        if (!baiduApiKey.isNullOrBlank() && !baiduSecretKey.isNullOrBlank()) {
            val baiduResult = runCatching {
                BaiduOcrApi(baiduApiKey, baiduSecretKey).recognizeText(bitmap)
            }.getOrNull()
            if (!baiduResult.isNullOrBlank()) {
                return OcrResult(baiduResult, OcrSource.BAIDU_API)
            }
        }

        return null
    }

    private fun recognizeWithMlKit(bitmap: Bitmap): MlKitResult? {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val text = Tasks.await(recognizer.process(image))
            val blocks = text.textBlocks.map { block ->
                OcrBlock(
                    text = block.text,
                    lines = block.lines.map { line ->
                        OcrLine(
                            text = line.text,
                            elements = line.elements.map { elem ->
                                val box = elem.boundingBox
                                OcrElement(
                                    text = elem.text,
                                    left = box?.left ?: 0,
                                    top = box?.top ?: 0,
                                    right = box?.right ?: 0,
                                    bottom = box?.bottom ?: 0
                                )
                            }
                        )
                    }
                )
            }
            MlKitResult(text.text, blocks)
        } catch (e: Exception) {
            null
        }
    }

    private fun hasReasonableContent(text: String): Boolean {
        val stockCodeRegex = Regex("\\d{6}")
        val chineseRegex = Regex("[\\u4e00-\\u9fff]")
        return stockCodeRegex.containsMatchIn(text) || chineseRegex.containsMatchIn(text)
    }

    private data class MlKitResult(
        val text: String,
        val blocks: List<OcrBlock>
    )

    fun release() {
        recognizer.close()
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/scanfolio/ocr/BaiduOcrApi.kt app/src/main/java/com/scanfolio/ocr/OcrEngine.kt
git commit -m "feat: add BaiduOcrApi client + rewrite OcrEngine with 3-layer routing"
```

---

### Task 4: Enhance TableAnalyzer with ML Kit block-level structured parsing

**Files:**
- Modify: `app/src/main/java/com/scanfolio/ocr/TableAnalyzer.kt`
- Modify: `app/src/main/java/com/scanfolio/ocr/OcrResult.kt`

- [ ] **Step 1: Update OcrResult.kt to carry source info**

```kotlin
package com.scanfolio.ocr

data class OcrResult(
    val header: List<String>,
    val rows: List<OcrRow>,
    val source: OcrEngine.OcrSource? = null
)

data class OcrRow(
    val stockCode: String,
    val stockName: String,
    val data: Map<String, String>
)

data class OcrCell(
    val text: String,
    val confidence: Float
)
```

- [ ] **Step 2: Rewrite TableAnalyzer.kt**

```kotlin
package com.scanfolio.ocr

import android.graphics.Bitmap

class TableAnalyzer(private val ocrEngine: OcrEngine) {
    data class TableResult(
        val headers: List<String>,
        val rows: List<OcrRow>,
        val rawText: String = "",
        val source: OcrEngine.OcrSource? = null
    )

    fun analyze(bitmap: Bitmap, baiduApiKey: String? = null, baiduSecretKey: String? = null): TableResult {
        val maxDimension = 2048
        val (w, h) = bitmap.width to bitmap.height
        val processed = if (w > maxDimension || h > maxDimension) {
            val ratio = maxDimension.toFloat() / maxOf(w, h)
            Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
        } else bitmap

        val ocrResult = ocrEngine.recognizeText(processed, baiduApiKey, baiduSecretKey)
            ?: return TableResult(emptyList(), emptyList(), source = null)

        // Try structured parsing from ML Kit blocks first
        if (ocrResult.mlKitBlocks != null && ocrResult.source != OcrEngine.OcrSource.BAIDU_API) {
            val structured = parseWithBlocks(ocrResult.mlKitBlocks)
            if (structured != null) {
                return structured.copy(
                    rawText = ocrResult.text,
                    source = ocrResult.source
                )
            }
        }

        // Fallback to whitespace-based parsing
        val fallback = parseWhitespaceDelimited(ocrResult.text)
        return if (fallback != null) {
            fallback.copy(rawText = ocrResult.text, source = ocrResult.source)
        } else {
            TableResult(emptyList(), emptyList(), rawText = ocrResult.text, source = ocrResult.source)
        }
    }

    private fun parseWithBlocks(blocks: List<OcrEngine.OcrBlock>): TableResult? {
        val allLines = mutableListOf<String>()
        val blockLines = blocks.flatMap { it.lines }
        if (blockLines.size < 3) return null

        // Detect column boundaries using X coordinates of elements in first few lines
        val sampleSize = minOf(5, blockLines.size)
        val xClusters = mutableListOf<List<Int>>()
        for (i in 0 until sampleSize) {
            val elems = blockLines[i].elements
            if (elems.size >= 2) {
                val xStarts = elems.map { it.left }
                xClusters.add(xStarts)
            }
        }
        if (xClusters.isEmpty()) return null

        // Reconstruct each line with tab-separated columns
        for (line in blockLines) {
            if (line.elements.isEmpty()) continue
            val sb = StringBuilder()
            for (elem in line.elements) {
                if (sb.isNotEmpty()) sb.append('\t')
                sb.append(elem.text)
            }
            allLines.add(sb.toString())
        }

        return parseLines(allLines)
    }

    private fun parseWhitespaceDelimited(text: String): TableResult? {
        val lines = text.split("\n").filter { it.isNotBlank() }
        return parseLines(lines.map { line ->
            line.split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString("\t")
        })
    }

    private fun parseLines(lines: List<String>): TableResult? {
        val headerIdx = lines.indexOfFirst { line ->
            val cells = line.split("\t")
            cells.size >= 2 &&
                !Regex("\\d{6}").matches(cells[0].trim()) &&
                !Regex("\\d{1,2}:\\d{2}").matches(cells[0].trim()) &&
                cells[0].trim().contains(Regex("[\\u4e00-\\u9fff]"))
        }
        if (headerIdx < 0) return null

        val rawHeaders = lines[headerIdx].split("\t")
        val headers = rawHeaders.mapIndexedNotNull { index, h ->
            when (index) {
                0 -> "代码名称"
                1 -> null
                else -> h.trim()
            }
        }

        val rows = mutableListOf<OcrRow>()
        for (i in (headerIdx + 1) until lines.size) {
            val cells = lines[i].split("\t").filter { it.isNotBlank() }
            if (cells.size < 3) continue
            val codeName = parseCodeAndName(cells[0], if (cells.size > 1) cells[1] else "")
            if (codeName == null) continue

            val dataMap = mutableMapOf<String, String>()
            val dataCount = (cells.size - 2).coerceAtMost(headers.size - 1)
            for (j in 0 until dataCount) {
                dataMap[headers[j + 1]] = cells[j + 2].trim()
            }
            rows.add(OcrRow(codeName.first, codeName.second, dataMap))
        }

        return if (rows.isEmpty()) null
        else TableResult(headers = headers, rows = rows)
    }

    private fun parseCodeAndName(code: String, name: String): Pair<String, String>? {
        val clean = "$code $name".trim()
        val match = Regex("(\\d{6})\\s*(.+)").find(clean)
        return match?.let {
            Pair(it.groupValues[1], it.groupValues[2].trim())
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/scanfolio/ocr/TableAnalyzer.kt app/src/main/java/com/scanfolio/ocr/OcrResult.kt
git commit -m "feat: enhance TableAnalyzer with ML Kit block-level structured parsing"
```

---

### Task 5: Update ScanScreen for batch image selection

**Files:**
- Modify: `app/src/main/java/com/scanfolio/ui/scan/ScanScreen.kt`

- [ ] **Step 1: Replace GetContent with GetMultipleContents**

Replace `ActivityResultContracts.GetContent()` with `ActivityResultContracts.GetMultipleContents()`:

```kotlin
val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetMultipleContents()
) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
        viewModel.processImages(uris.map { it.toString() })
    }
}
```

Also, remove the single-URI bitmap decode and add batch processing instead. Full modified file:

```kotlin
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
                    Text("正在识别截图...($processedCount)")
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
```

Note: we also need to add the import for `androidx.core.content.ContextCompat` which is already imported.

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/scan/ScanScreen.kt
git commit -m "feat: support batch image selection with GetMultipleContents"
```

---

### Task 6: Update ScanViewModel for batch processing + merge/dedup

**Files:**
- Modify: `app/src/main/java/com/scanfolio/ui/scan/ScanViewModel.kt`

- [ ] **Step 1: Rewrite ScanViewModel with batch processing**

```kotlin
package com.scanfolio.ui.scan

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.ocr.OcrRow
import com.scanfolio.ocr.TableAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp

    private val _ocrResult = MutableStateFlow<TableAnalyzer.TableResult?>(null)
    val ocrResult: StateFlow<TableAnalyzer.TableResult?> = _ocrResult.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _rawOcrText = MutableStateFlow<String?>(null)
    val rawOcrText: StateFlow<String?> = _rawOcrText.asStateFlow()

    private val _imported = MutableStateFlow(false)
    val imported: StateFlow<Boolean> = _imported.asStateFlow()

    private val _importedCount = MutableStateFlow(0)
    val importedCount: StateFlow<Int> = _importedCount.asStateFlow()

    private val _previewRows = MutableStateFlow<List<OcrRow>>(emptyList())
    val previewRows: StateFlow<List<OcrRow>> = _previewRows.asStateFlow()

    private val _previewHeaders = MutableStateFlow<List<String>>(emptyList())
    val previewHeaders: StateFlow<List<String>> = _previewHeaders.asStateFlow()

    private val _processedCount = MutableStateFlow(0)
    val processedCount: StateFlow<Int> = _processedCount.asStateFlow()

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            processImageInternal(bitmap)
        }
    }

    fun processImages(uris: List<Uri>, contentResolver: ContentResolver) {
        viewModelScope.launch {
            processImagesInternal(uris, contentResolver)
        }
    }

    @VisibleForTesting
    internal suspend fun processImagesInternal(uris: List<Uri>, contentResolver: ContentResolver) {
        _isProcessing.value = true
        _error.value = null
        _imported.value = false
        _importedCount.value = 0
        _processedCount.value = 0

        val baiduKey = app.settingsRepository.getBaiduOcrApiKey()
        val baiduSecret = app.settingsRepository.getBaiduOcrSecretKey()

        val allRows = mutableListOf<OcrRow>()
        val seenCodes = mutableSetOf<String>()

        for (uri in uris) {
            _processedCount.value = allRows.size
            val bitmap = decodeBitmap(uri, contentResolver) ?: continue
            try {
                val result = app.tableAnalyzer.analyze(bitmap, baiduKey, baiduSecret)
                _rawOcrText.value = result.rawText
                if (result.rows.isNotEmpty()) {
                    _previewHeaders.value = result.headers
                    for (row in result.rows) {
                        if (row.stockCode !in seenCodes) {
                            seenCodes.add(row.stockCode)
                            allRows.add(row)
                        }
                    }
                    _previewRows.value = allRows.toList()
                    _ocrResult.value = TableAnalyzer.TableResult(
                        headers = result.headers,
                        rows = allRows,
                        rawText = result.rawText
                    )
                }
            } catch (e: Exception) {
                // Continue with next image
            }
        }

        _processedCount.value = allRows.size

        if (allRows.isEmpty()) {
            _error.value = "未识别到有效表格数据，请确认是同花顺股票列表截图"
        }

        _isProcessing.value = false
    }

    @VisibleForTesting
    internal suspend fun processImageInternal(bitmap: Bitmap) {
        _isProcessing.value = true
        _error.value = null
        _imported.value = false
        _importedCount.value = 0
        _ocrResult.value = null
        _previewRows.value = emptyList()
        _previewHeaders.value = emptyList()
        _processedCount.value = 0
        try {
            val baiduKey = app.settingsRepository.getBaiduOcrApiKey()
            val baiduSecret = app.settingsRepository.getBaiduOcrSecretKey()
            val result = app.tableAnalyzer.analyze(bitmap, baiduKey, baiduSecret)
            _rawOcrText.value = result.rawText
            if (result.rows.isEmpty()) {
                _error.value = "未识别到有效表格数据，请确认是同花顺股票列表截图"
                return
            }
            _ocrResult.value = result
            _previewHeaders.value = result.headers
            _previewRows.value = result.rows.toList()
        } catch (e: IllegalStateException) {
            _error.value = e.message ?: "OCR引擎未就绪"
        } catch (e: Exception) {
            _error.value = "识别失败: ${e.message}"
        } catch (e: OutOfMemoryError) {
            _error.value = "图片过大，无法识别，请使用较小的图片"
        } finally {
            _isProcessing.value = false
        }
    }

    private fun decodeBitmap(uri: Uri, contentResolver: ContentResolver): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
            val maxDimension = 2048
            val originalMax = maxOf(opts.outWidth, opts.outHeight)
            val target = if (originalMax > 0) (originalMax + maxDimension - 1) / maxDimension else 1
            var inSampleSize = 1
            while (inSampleSize < target) {
                inSampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSampleSize }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (e: Exception) {
            null
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
            val count = rows.size
            for (row in rows) {
                app.stockRepository.mergeScreenshotData(
                    code = row.stockCode,
                    name = row.stockName,
                    newData = row.data
                )
            }
            _importedCount.value = count
            _imported.value = true
            _ocrResult.value = null
            _previewRows.value = emptyList()
            _previewHeaders.value = emptyList()
        }
    }

    fun setError(message: String) { _error.value = message }

    fun clearError() { _error.value = null }

    fun clearRawOcrText() { _rawOcrText.value = null }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/scan/ScanViewModel.kt
git commit -m "feat: add batch OCR processing with merge/dedup"
```

---

### Task 7: Add Baidu OCR API Key settings

**Files:**
- Modify: `app/src/main/java/com/scanfolio/data/repository/SettingsRepository.kt`
- Modify: `app/src/main/java/com/scanfolio/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Update AppDatabase with Baidu settings entity**

Add a simple settings table or use SharedPreferences. Since this is a simple key-value pair, let's use SharedPreferences via a SettingsDataStore approach. But looking at the existing code, they use Room. Let me check the existing SettingsRepository.

Actually wait, let me check what the existing SettingsRepository looks like and what DAOs it uses.<｜end▁of▁thinking｜>

<｜｜DSML｜｜tool_calls>
<｜｜DSML｜｜invoke name="read">
<｜｜DSML｜｜parameter name="filePath" string="true">D:\workspace\Scanfolio\app\src\main\java\com\scanfolio\data\repository\SettingsRepository.kt