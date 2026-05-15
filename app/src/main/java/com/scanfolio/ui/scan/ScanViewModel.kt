package com.scanfolio.ui.scan

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.ocr.TableAnalyzer
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

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            _isProcessing.value = true
            _error.value = null
            _imported.value = false
            try {
                if (app.ocrEngine.getInitError() != null) {
                    _error.value = app.ocrEngine.getInitError()
                    return@launch
                }
                val result = app.tableAnalyzer.analyze(bitmap)
                if (result.rows.isEmpty()) {
                    _error.value = "未识别到有效表格数据，请确认是同花顺股票列表截图"
                }
                _ocrResult.value = result
            } catch (e: java.lang.IllegalStateException) {
                _error.value = e.message ?: "OCR引擎未就绪"
            } catch (e: Exception) {
                _error.value = "识别失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun confirmImport() {
        viewModelScope.launch {
            val result = _ocrResult.value ?: return@launch
            for (row in result.rows) {
                app.stockRepository.mergeScreenshotData(
                    code = row.stockCode,
                    name = row.stockName,
                    newData = row.data
                )
            }
            _imported.value = true
            _ocrResult.value = null
        }
    }

    fun clearError() { _error.value = null }
}
