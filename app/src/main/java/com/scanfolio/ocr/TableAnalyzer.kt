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

        if (ocrResult.blocks != null && ocrResult.source != OcrEngine.OcrSource.BAIDU_API) {
            val structured = parseWithBlocks(ocrResult.blocks)
            if (structured != null) {
                return structured.copy(rawText = ocrResult.text, source = ocrResult.source)
            }
        }

        val fallback = parseWhitespaceDelimited(ocrResult.text)
        return if (fallback != null) {
            fallback.copy(rawText = ocrResult.text, source = ocrResult.source)
        } else {
            TableResult(emptyList(), emptyList(), rawText = ocrResult.text, source = ocrResult.source)
        }
    }

    private fun parseWithBlocks(blocks: List<OcrEngine.OcrBlock>): TableResult? {
        val blockLines = blocks.flatMap { it.lines }
        if (blockLines.size < 3) return null

        val allLines = mutableListOf<String>()
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
            val cells = lines[i].split("\t").filter { isNotBlank(it) }
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

    private fun isNotBlank(s: String) = s.isNotBlank()

    private fun parseCodeAndName(code: String, name: String): Pair<String, String>? {
        val clean = "$code $name".trim()
        val match = Regex("(\\d{6})\\s*(.+)").find(clean)
        return match?.let {
            Pair(it.groupValues[1], it.groupValues[2].trim())
        }
    }
}
