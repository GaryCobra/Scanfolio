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
