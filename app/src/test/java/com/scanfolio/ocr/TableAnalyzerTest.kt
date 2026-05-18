package com.scanfolio.ocr

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableAnalyzerTest {

    private val mockEngine = mockk<OcrEngine>()
    private val analyzer = TableAnalyzer(mockEngine)

    private fun mockBitmap(): Bitmap = mockk(relaxed = true)

    private fun mockOcrResult(text: String): OcrEngine.OcrResult {
        return OcrEngine.OcrResult(text, OcrEngine.OcrSource.ML_KIT)
    }

    @Test
    fun `parses light theme stock screenshot`() {
        val mockOcrOutput = """
            11:10 同花顺
            自选A股
            代码 名称 涨跌幅 现价
            000001 平安银行 +2.5% 12.34
            000002 万科A -1.2% 8.56
            600036 招商银行 +0.8% 35.20
        """.trimIndent()
        every { mockEngine.recognizeText(any(), any(), any()) } returns mockOcrResult(mockOcrOutput)

        val result = analyzer.analyze(mockBitmap())

        assertEquals(3, result.rows.size)
        assertEquals(listOf("代码名称", "涨跌幅", "现价"), result.headers)

        assertEquals("000001", result.rows[0].stockCode)
        assertEquals("平安银行", result.rows[0].stockName)
        assertEquals("+2.5%", result.rows[0].data["涨跌幅"])
        assertEquals("12.34", result.rows[0].data["现价"])

        assertEquals("000002", result.rows[1].stockCode)
        assertEquals("万科A", result.rows[1].stockName)
        assertEquals("-1.2%", result.rows[1].data["涨跌幅"])
        assertEquals("8.56", result.rows[1].data["现价"])

        assertEquals("600036", result.rows[2].stockCode)
        assertEquals("招商银行", result.rows[2].stockName)
    }

    @Test
    fun `parses dark theme stock screenshot`() {
        val mockOcrOutput = """
            自选A股
            代码 名称 涨幅 现价 涨跌
            000001 平安银行 +1.25 15.20 +0.18
            002415 海康威视 -0.85 32.10 -0.28
            600519 贵州茅台 +0.50 1880.00 +9.50
        """.trimIndent()
        every { mockEngine.recognizeText(any(), any(), any()) } returns mockOcrResult(mockOcrOutput)

        val result = analyzer.analyze(mockBitmap())

        assertEquals(3, result.rows.size)
        assertEquals(listOf("代码名称", "涨幅", "现价", "涨跌"), result.headers)

        assertEquals("000001", result.rows[0].stockCode)
        assertEquals("平安银行", result.rows[0].stockName)
        assertEquals("+1.25", result.rows[0].data["涨幅"])
        assertEquals("15.20", result.rows[0].data["现价"])
        assertEquals("+0.18", result.rows[0].data["涨跌"])
    }

    @Test
    fun `returns empty when no header found`() {
        every { mockEngine.recognizeText(any(), any(), any()) } returns mockOcrResult("杂乱文字")

        val result = analyzer.analyze(mockBitmap())

        assertTrue(result.rows.isEmpty())
    }

    @Test
    fun `parses screenshot with 6 columns`() {
        val mockOcrOutput = """
            11:10 同花顺
            自选A股
            代码 名称 涨跌幅 现价 涨跌 最高 最低
            000001 平安银行 +1.25 15.20 +0.18 15.30 15.10
            002415 海康威视 -0.85 32.10 -0.28 32.50 31.80
            600519 贵州茅台 +0.50 1880.00 +9.50 1890.00 1870.00
        """.trimIndent()
        every { mockEngine.recognizeText(any(), any(), any()) } returns mockOcrResult(mockOcrOutput)

        val result = analyzer.analyze(mockBitmap())

        assertEquals(3, result.rows.size)
        assertEquals(listOf("代码名称", "涨跌幅", "现价", "涨跌", "最高", "最低"), result.headers)

        val first = result.rows[0]
        assertEquals("000001", first.stockCode)
        assertEquals("平安银行", first.stockName)
        assertEquals("+1.25", first.data["涨跌幅"])
        assertEquals("15.20", first.data["现价"])
        assertEquals("+0.18", first.data["涨跌"])
        assertEquals("15.30", first.data["最高"])
        assertEquals("15.10", first.data["最低"])
    }

    @Test
    fun `returns empty when ocr returns null`() {
        every { mockEngine.recognizeText(any(), any(), any()) } returns null

        val result = analyzer.analyze(mockBitmap())

        assertTrue(result.rows.isEmpty())
    }
}
