package com.scanfolio.ocr

import android.graphics.Bitmap

class TableAnalyzer(private val ocrEngine: OcrEngine) {

    data class TableResult(
        val headers: List<String>,
        val rows: List<OcrRow>
    )

    fun analyze(bitmap: Bitmap): TableResult {
        val rowBoundaries = ImagePreprocessor.detectRowBoundaries(bitmap)
        if (rowBoundaries.size < 4) return TableResult(emptyList(), emptyList())

        val headerRow = rowBoundaries.getOrNull(0)?.let { top ->
            rowBoundaries.getOrNull(1)?.let { bottom ->
                if (bottom - top < 10) return@let null
                val headerBmp = Bitmap.createBitmap(bitmap, 0, top, bitmap.width, bottom - top)
                ocrEngine.recognizeText(headerBmp)
            }
        } ?: ""

        val rawHeaders = parseLineToCells(headerRow)
        val columnCount = rawHeaders.size
        if (columnCount < 2) return TableResult(emptyList(), emptyList())

        val headers = rawHeaders.mapIndexed { index, h ->
            if (index == 0) "代码名称" else h.trim()
        }

        val rows = mutableListOf<OcrRow>()
        for (i in 1 until rowBoundaries.size - 1) {
            val top = rowBoundaries[i]
            val bottom = rowBoundaries[i + 1]
            if (bottom - top < 10) continue

            val rowBmp = Bitmap.createBitmap(bitmap, 0, top, bitmap.width, bottom - top)
            val rowText = ocrEngine.recognizeText(rowBmp)
            val cells = parseLineToCells(rowText)
            if (cells.size < 2) continue

            val codeName = parseCodeAndName(cells[0])
            if (codeName == null) continue

            val dataMap = mutableMapOf<String, String>()
            for (j in 1 until cells.size.coerceAtMost(headers.size)) {
                dataMap[headers[j]] = cells[j].trim()
            }

            rows.add(OcrRow(
                stockCode = codeName.first,
                stockName = codeName.second,
                data = dataMap
            ))
        }

        return TableResult(headers = headers, rows = rows)
    }

    private fun parseLineToCells(text: String): List<String> {
        return text.split("\n")
            .firstOrNull()?.split(Regex("\\s{2,}"))
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    private fun parseCodeAndName(text: String): Pair<String, String>? {
        val clean = text.trim()
        val match = Regex("(\\d{6})\\s*(.+)").find(clean)
        return match?.let {
            Pair(it.groupValues[1], it.groupValues[2].trim())
        }
    }
}
