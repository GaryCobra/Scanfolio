package com.scanfolio.ocr

data class OcrRow(
    val stockCode: String,
    val stockName: String,
    val data: Map<String, String>
)
