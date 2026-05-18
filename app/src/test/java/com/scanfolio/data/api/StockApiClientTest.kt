package com.scanfolio.data.api

import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.Assert.*

class StockApiClientTest {
    private val client = StockApiClient(OkHttpClient())

    @Test
    fun `determineExchange returns SH for 6-prefix codes`() {
        assertEquals("SH", client.determineExchange("600036"))
        assertEquals("SH", client.determineExchange("601318"))
    }

    @Test
    fun `determineExchange returns SZ for 0-prefix codes`() {
        assertEquals("SZ", client.determineExchange("000001"))
        assertEquals("SZ", client.determineExchange("300750"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `determineExchange throws for invalid prefix`() {
        client.determineExchange("999999")
    }

    @Test
    fun `buildDataColumns includes kline-derived fields when kline has 30 entries`() {
        val quote = StockQuote("600036", "招商银行", "SH", 38.72, 0.62, 0.24, 38.50, 38.88, 38.30, 38.48, 12345678L, 480000000.0)
        val kline = (1..30).map { i ->
            KlineData("day$i", open = 100.0, close = 101.0 + i * 0.1, high = 102.0, low = 99.0, volume = 1000L)
        }
        val cols = StockApiClient(okhttp3.OkHttpClient()).buildDataColumns(quote, kline, MoneyFlow())
        assertTrue(cols.containsKey("20日涨幅"))
        assertTrue(cols.containsKey("月涨跌幅"))
        assertTrue(cols.containsKey("连续上涨天数"))
        assertTrue(cols.containsKey("涨停次数"))
    }

    @Test
    fun `parseKlineResponse returns correct number of entries`() {
        val rawJson = """{"data":{"klines":["2026-05-18,100.0,101.0,102.0,99.0,10000","2026-05-15,99.0,100.0,101.0,98.0,8000"]}}"""
        val kline = StockApiClient(okhttp3.OkHttpClient()).parseKlineResponse(rawJson, 2)
        assertEquals(2, kline.size)
        assertEquals("2026-05-18", kline[0].day)
        assertEquals(101.0, kline[0].close, 0.001)
        assertEquals(10000L, kline[0].volume)
    }

    @Test
    fun `parseQuoteResponse extracts correct values`() {
        val raw = """var hq_str_sh600036="招商银行,38.50,38.48,38.72,38.88,38.30,38.71,38.72,12345678,480000000.00,1000,38.71,1100,38.70,1200,38.69,1300,38.68,1400,38.67,1500,38.72,1600,38.73,1700,38.74,1800,38.75,1900,38.76,2026-05-18,15:00:00,00";"""
        val quote = client.parseQuoteResponse(raw, "600036")
        assertNotNull(quote)
        assertEquals("600036", quote!!.code)
        assertEquals("招商银行", quote.name)
        assertEquals(38.72, quote.currentPrice, 0.001)
        assertEquals(12345678L, quote.volume)
    }
}
