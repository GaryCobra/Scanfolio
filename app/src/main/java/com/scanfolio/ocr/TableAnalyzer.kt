package com.scanfolio.ocr

import android.graphics.Bitmap

class TableAnalyzer(private val ocrEngine: OcrEngine) {

    data class TableResult(
        val headers: List<String>,
        val rows: List<OcrRow>,
        val rawText: String
    )

    fun analyze(bitmap: Bitmap, baiduKey: String?, baiduSecret: String?): TableResult {
        return TableResult(
            headers = emptyList(),
            rows = emptyList(),
            rawText = ""
        )
    }
}
