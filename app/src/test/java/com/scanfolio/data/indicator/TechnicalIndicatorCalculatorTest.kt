package com.scanfolio.data.indicator

import com.scanfolio.data.api.KlineData
import org.junit.Test
import org.junit.Assert.*

class TechnicalIndicatorCalculatorTest {

    private val calculator = TechnicalIndicatorCalculator()

    @Test
    fun `calculateKDJ returns expected values for steady uptrend`() {
        val kline = (1..20).map { i ->
            KlineData("day$i", open = 100.0 + i, close = 101.0 + i, high = 102.0 + i, low = 99.0 + i, volume = 1000L)
        }
        val kdj = calculator.calculateKDJ(kline)
        assertNotNull(kdj)
        assertTrue(kdj!!.k in 0.0..100.0)
        assertTrue(kdj.d in 0.0..100.0)
    }

    @Test
    fun `calculateMACD returns dif dea macd`() {
        val kline = (1..30).map { i ->
            KlineData("day$i", open = 100.0, close = 100.0 + i * 0.5, high = 101.0, low = 99.0, volume = 1000L)
        }
        val macd = calculator.calculateMACD(kline)
        assertNotNull(macd)
    }

    @Test
    fun `calculateRSI returns 0-100 range`() {
        val kline = (1..20).map { i ->
            KlineData("day$i", open = 100.0, close = 100.0 + (i % 5) * 2.0, high = 101.0, low = 99.0, volume = 1000L)
        }
        val rsi = calculator.calculateRSI(kline, 6)
        assertTrue(rsi in 0.0..100.0)
    }

    @Test
    fun `calculateBOLL returns upper greater than mid greater than lower`() {
        val kline = (1..25).map { i ->
            KlineData("day$i", open = 100.0, close = 100.0 + i * 0.3, high = 101.0, low = 99.0, volume = 1000L)
        }
        val boll = calculator.calculateBOLL(kline)
        assertNotNull(boll)
        assertTrue(boll!!.upper > boll.mid)
        assertTrue(boll.mid > boll.lower)
    }

    @Test
    fun `toDataColumns returns map with indicator keys`() {
        val kline = (1..30).map { i ->
            KlineData("day$i", open = 100.0, close = 100.0 + i * 0.5, high = 101.0, low = 99.0, volume = 1000L)
        }
        val cols = calculator.toDataColumns(kline)
        assertTrue(cols.containsKey("KDJ_K"))
        assertTrue(cols.containsKey("MACD_DIF"))
        assertTrue(cols.containsKey("RSI_6"))
        assertTrue(cols.containsKey("BOLL_UPPER"))
    }
}
