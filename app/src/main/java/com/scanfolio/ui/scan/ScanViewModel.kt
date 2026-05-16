package com.scanfolio.ui.scan

import android.app.Application
import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
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

    private val _importedCount = MutableStateFlow(0)
    val importedCount: StateFlow<Int> = _importedCount.asStateFlow()

    private val _previewRows = MutableStateFlow<List<OcrRow>>(emptyList())
    val previewRows: StateFlow<List<OcrRow>> = _previewRows.asStateFlow()

    private val _previewHeaders = MutableStateFlow<List<String>>(emptyList())
    val previewHeaders: StateFlow<List<String>> = _previewHeaders.asStateFlow()

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            processImageInternal(bitmap)
        }
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
        try {
            if (app.ocrEngine.getInitError() != null) {
                _error.value = app.ocrEngine.getInitError()
                return
            }
            val result = app.tableAnalyzer.analyze(bitmap)
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
        } finally {
            _isProcessing.value = false
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

    fun clearError() { _error.value = null }
}
