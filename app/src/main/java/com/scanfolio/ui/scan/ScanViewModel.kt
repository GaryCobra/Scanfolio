package com.scanfolio.ui.scan

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.MomentumApp
import com.scanfolio.ocr.OcrRow
import com.scanfolio.ocr.TableAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MomentumApp

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
        _ocrResult.value = null
        _previewRows.value = emptyList()
        _previewHeaders.value = emptyList()
        _processedCount.value = 0

        val baiduKey = app.getBaiduOcrApiKey()
        val baiduSecret = app.getBaiduOcrSecretKey()

        val allRows = mutableListOf<OcrRow>()
        val seenCodes = mutableSetOf<String>()
        var lastHeaders = emptyList<String>()

        for (uri in uris) {
            _processedCount.value = allRows.size
            val bitmap = decodeBitmap(uri, contentResolver) ?: continue
            try {
                val result = app.tableAnalyzer.analyze(bitmap, baiduKey, baiduSecret)
                _rawOcrText.value = result.rawText
                if (result.rows.isNotEmpty()) {
                    lastHeaders = result.headers
                    for (row in result.rows) {
                        if (row.stockCode !in seenCodes) {
                            seenCodes.add(row.stockCode)
                            allRows.add(row)
                        }
                    }
                    _previewRows.value = allRows.toList()
                    _previewHeaders.value = lastHeaders
                    _ocrResult.value = TableAnalyzer.TableResult(
                        headers = lastHeaders,
                        rows = allRows.toList(),
                        rawText = result.rawText
                    )
                }
            } catch (_: Exception) {
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
            val baiduKey = app.getBaiduOcrApiKey()
            val baiduSecret = app.getBaiduOcrSecretKey()
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
        } catch (_: Exception) {
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
