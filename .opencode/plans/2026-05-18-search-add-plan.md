# Scanfolio V2: 搜索添加 + 分组 + 技术指标 + 收益曲线 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 弃用 OCR，实现股票搜索+API数据填充、自选股分组、技术指标自动计算、盈亏收益曲线

**Architecture:** StockApiClient (东方财富API) 替代 OcrEngine，技术指标从K线计算，分组复用策略表，PnL 基于交易记录+API实时价

**Tech Stack:** Kotlin, Jetpack Compose, Room, OkHttp, MPAndroidChart

---

### Task 1: StockApiClient — 股票行情API客户端

**Files:**
- Create: `app/src/main/java/com/scanfolio/data/api/StockApiClient.kt`
- Test: `app/src/test/java/com/scanfolio/data/api/StockApiClientTest.kt`

- [ ] **Step 1: Write StockQuote data class + StockApiClient test**

```kotlin
// StockApiClientTest.kt
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

    @Test
    fun `determineExchange returns SZ for 3-prefix codes`() {
        assertEquals("SZ", client.determineExchange("300750"))
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

    @Test(expected = IllegalArgumentException::class)
    fun `determineExchange throws for invalid prefix`() {
        client.determineExchange("999999")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd app && ..\gradlew testDebugUnitTest --tests "com.scanfolio.data.api.StockApiClientTest" 2>&1`
Expected: BUILD FAILED with "not found"

- [ ] **Step 3: Write StockApiClient implementation**

```kotlin
// StockApiClient.kt
package com.scanfolio.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class StockQuote(
    val code: String,
    val name: String,
    val exchange: String,
    val currentPrice: Double,
    val changePercent: Double,
    val changeAmount: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val yesterdayClose: Double,
    val volume: Long,
    val turnover: Double,
    val turnoverRate: Double = 0.0,
    val amplitude: Double = 0.0,
    val volumeRatio: Double = 0.0,
    val pe: Double = 0.0,
    val pb: Double = 0.0,
    val marketCap: Double = 0.0,
    val circulatingMarketCap: Double = 0.0,
    val industry: String = "",
    val concepts: String = ""
)

data class KlineData(
    val day: String,
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Long
)

data class MoneyFlow(
    val mainForceNetInflow: Double = 0.0,  // 主力净流入
    val retailNetInflow: Double = 0.0       // 散户净流入
)

data class StockFullData(
    val quote: StockQuote,
    val kline: List<KlineData> = emptyList(),
    val moneyFlow: MoneyFlow = MoneyFlow(),
    val indicators: Map<String, String> = emptyMap()
)

class StockApiClient(private val okHttp: OkHttpClient) {

    private val jsonOkHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Referer", "https://finance.sina.com.cn")
                .build()
            chain.proceed(request)
        }
        .build()

    fun determineExchange(code: String): String {
        return when {
            code.startsWith("6") || code.startsWith("5") -> "SH"
            code.startsWith("0") || code.startsWith("3") || code.startsWith("2") -> "SZ"
            code.startsWith("4") || code.startsWith("8") -> "BJ"
            else -> throw IllegalArgumentException("未知股票代码前缀: $code")
        }
    }

    fun fetchQuote(code: String): StockQuote? {
        val exchange = determineExchange(code)
        val url = "https://hq.sinajs.cn/list=${exchange.lowercase()}$code"
        val resp = jsonOkHttp.newCall(Request.Builder().url(url).get().build()).execute()
        val body = resp.body?.string() ?: return null
        return parseQuoteResponse(body, code)
    }

    fun fetchFullQuote(code: String): StockQuote? {
        val exchange = determineExchange(code)
        val secId = if (exchange == "SH") "1.$code" else "0.$code"
        val fields = "f43,f44,f45,f46,f47,f48,f49,f50,f51,f52,f55,f57,f58,f60,f116,f117,f162,f167,f168,f169,f170,f171,f127,f128,f140"
        val url = "https://push2.eastmoney.com/api/qt/stock/get?secid=$secId&fields=$fields"
        return try {
            val resp = jsonOkHttp.newCall(Request.Builder().url(url).get().build()).execute()
            val raw = resp.body?.string() ?: return null
            val json = JSONObject(raw).optJSONObject("data") ?: return null
            val price = json.optDouble("f43", 0.0)
            val yestClose = json.optDouble("f47", 0.0)
            StockQuote(
                code = code,
                name = json.optString("f58", ""),
                exchange = exchange,
                currentPrice = price,
                changePercent = json.optDouble("f55", 0.0),
                changeAmount = json.optDouble("f55", 0.0) * yestClose / 100.0, // approximate
                open = json.optDouble("f46", 0.0),
                high = json.optDouble("f44", 0.0),
                low = json.optDouble("f45", 0.0),
                yesterdayClose = yestClose,
                volume = json.optLong("f48", 0L),
                turnover = json.optDouble("f49", 0.0),
                turnoverRate = json.optDouble("f168", 0.0),
                amplitude = json.optDouble("f169", 0.0),
                volumeRatio = json.optDouble("f50", 0.0),
                pe = json.optDouble("f60", 0.0),
                pb = json.optDouble("f167", 0.0),
                marketCap = json.optDouble("f116", 0.0),
                circulatingMarketCap = json.optDouble("f117", 0.0),
                industry = json.optString("f127", ""),
                concepts = json.optString("f140", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseQuoteResponse(raw: String, code: String): StockQuote? {
        if (!raw.contains("\"")) return null
        val content = raw.substringAfter("\"").substringBefore("\"")
        val parts = content.split(",")
        if (parts.size < 32) return null
        val exchange = determineExchange(code)
        return StockQuote(
            code = code,
            name = parts[0],
            exchange = exchange,
            open = parts[1].toDoubleOrNull() ?: 0.0,
            yesterdayClose = parts[2].toDoubleOrNull() ?: 0.0,
            currentPrice = parts[3].toDoubleOrNull() ?: 0.0,
            high = parts[4].toDoubleOrNull() ?: 0.0,
            low = parts[5].toDoubleOrNull() ?: 0.0,
            volume = parts[8].toLongOrNull() ?: 0L,
            turnover = parts[9].toDoubleOrNull() ?: 0.0,
            changeAmount = parts[3].toDoubleOrNull()?.minus(parts[2].toDoubleOrNull() ?: 0.0) ?: 0.0,
            changePercent = if (parts[2].toDoubleOrNull() != null && parts[2].toDoubleOrNull() != 0.0)
                ((parts[3].toDoubleOrNull() ?: 0.0) - parts[2].toDouble()) / parts[2].toDouble() * 100 else 0.0
        )
    }

    fun fetchKline(code: String, days: Int = 60): List<KlineData> {
        val exchange = determineExchange(code)
        val secId = if (exchange == "SH") "1.$code" else "0.$code"
        val url = "https://push2.eastmoney.com/api/qt/stock/kline/get?secid=$secId&klt=101&fqt=1&beg=0&end=20500101"
        val resp = jsonOkHttp.newCall(Request.Builder().url(url).get().build()).execute()
        val body = resp.body?.string() ?: return emptyList()
        return parseKlineResponse(body, days)
    }

    internal fun parseKlineResponse(raw: String, days: Int): List<KlineData> {
        return try {
            val json = JSONObject(raw)
            val data = json.optJSONObject("data") ?: return emptyList()
            val klinesStr = data.optString("klines", "")
            if (klinesStr.isEmpty()) return emptyList()
            val lines = JSONArray(klinesStr)
            (0 until minOf(lines.length(), days)).map { i ->
                val parts = lines.getString(i).split(",")
                KlineData(
                    day = parts[0],
                    open = parts[1].toDoubleOrNull() ?: 0.0,
                    close = parts[2].toDoubleOrNull() ?: 0.0,
                    high = parts[3].toDoubleOrNull() ?: 0.0,
                    low = parts[4].toDoubleOrNull() ?: 0.0,
                    volume = parts[5].toLongOrNull() ?: 0L
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun fetchMoneyFlow(code: String): MoneyFlow {
        val exchange = determineExchange(code)
        val secId = if (exchange == "SH") "1.$code" else "0.$code"
        val url = "https://push2.eastmoney.com/api/qt/stock/fflow/daykline/get?secid=$secId&fields1=f1,f2,f3,f7&fields2=f51,f52,f53,f54,f55,f56,f57"
        return try {
            val resp = jsonOkHttp.newCall(Request.Builder().url(url).get().build()).execute()
            val body = resp.body?.string() ?: return MoneyFlow()
            val json = JSONObject(body)
            val data = json.optJSONObject("data") ?: return MoneyFlow()
            val klines = data.optJSONArray("klines") ?: return MoneyFlow()
            if (klines.length() == 0) return MoneyFlow()
            val latest = klines.getString(0).split(",")
            MoneyFlow(
                mainForceNetInflow = latest.getOrNull(3)?.toDoubleOrNull() ?: 0.0,
                retailNetInflow = latest.getOrNull(4)?.toDoubleOrNull() ?: 0.0
            )
        } catch (_: Exception) {
            MoneyFlow()
        }
    }

    fun buildDataColumns(quote: StockQuote, kline: List<KlineData>, moneyFlow: MoneyFlow): Map<String, String> {
        val map = mutableMapOf<String, String>()
        map["最新价"] = "%.2f".format(quote.currentPrice)
        map["涨跌幅"] = "%.2f%%".format(quote.changePercent)
        map["涨跌额"] = "%.2f".format(quote.changeAmount)
        map["今开"] = "%.2f".format(quote.open)
        map["最高"] = "%.2f".format(quote.high)
        map["最低"] = "%.2f".format(quote.low)
        map["昨收"] = "%.2f".format(quote.yesterdayClose)
        map["成交量"] = if (quote.volume > 10000) "%.0f万".format(quote.volume / 10000.0) else quote.volume.toString()
        map["成交额"] = if (quote.turnover > 100000000) "%.2f亿".format(quote.turnover / 100000000.0) else "%.0f万".format(quote.turnover / 10000.0)
        map["振幅"] = "%.2f%%".format(quote.amplitude)
        map["换手率"] = "%.2f%%".format(quote.turnoverRate)
        map["量比"] = "%.2f".format(quote.volumeRatio)
        map["市盈率"] = "%.2f".format(quote.pe)
        map["市净率"] = "%.2f".format(quote.pb)
        map["总市值"] = formatMoney(quote.marketCap)
        map["流通市值"] = formatMoney(quote.circulatingMarketCap)

        if (kline.size >= 20) {
            val close20Ago = kline.getOrNull(19)?.close ?: 0.0
            val closeToday = kline.first().close
            val pct20d = if (close20Ago > 0) (closeToday - close20Ago) / close20Ago * 100 else 0.0
            map["20日涨幅"] = "%.2f%%".format(pct20d)

            val avgAmplitude = kline.take(20).map { (it.high - it.low) / it.close * 100 }.average()
            map["20日振幅"] = "%.2f%%".format(avgAmplitude)

            val consecutiveUp = countConsecutiveUp(kline)
            map["连续上涨天数"] = "$consecutiveUp"
        }

        if (kline.size >= 30) {
            val close30Ago = kline.getOrNull(29)?.close ?: 0.0
            val closeToday = kline.first().close
            val pctMonth = if (close30Ago > 0) (closeToday - close30Ago) / close30Ago * 100 else 0.0
            map["月涨跌幅"] = "%.2f%%".format(pctMonth)
        }

        if (kline.isNotEmpty()) {
            val limitUpCount = kline.count { it.close >= it.open * 1.098 }
            map["涨停次数"] = "$limitUpCount"
        }

        if (moneyFlow.mainForceNetInflow != 0.0) {
            map["主力净流入"] = formatMoney(moneyFlow.mainForceNetInflow)
        }

        if (quote.industry.isNotBlank()) map["所属行业"] = quote.industry
        if (quote.concepts.isNotBlank()) map["所属概念"] = quote.concepts

        return map
    }

    private fun countConsecutiveUp(kline: List<KlineData>): Int {
        var count = 0
        for (k in kline) {
            if (k.close >= k.open) count++
            else break
        }
        return count
    }

    private fun formatMoney(value: Double): String {
        return when {
            value >= 100000000 -> "%.2f亿".format(value / 100000000.0)
            value >= 10000 -> "%.2f万".format(value / 10000.0)
            else -> "%.2f".format(value)
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd app && ..\gradlew testDebugUnitTest --tests "com.scanfolio.data.api.StockApiClientTest" 2>&1`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/scanfolio/data/api/StockApiClient.kt app/src/test/java/com/scanfolio/data/api/StockApiClientTest.kt
git commit -m "feat: add StockApiClient for real-time stock data from East Money + Sina APIs"
```

---

### Task 2: TechnicalIndicatorCalculator — 技术指标计算

**Files:**
- Create: `app/src/main/java/com/scanfolio/data/indicator/TechnicalIndicatorCalculator.kt`
- Test: `app/src/test/java/com/scanfolio/data/indicator/TechnicalIndicatorCalculatorTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// TechnicalIndicatorCalculatorTest.kt
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd app && ..\gradlew testDebugUnitTest --tests "com.scanfolio.data.indicator.TechnicalIndicatorCalculatorTest" 2>&1`
Expected: BUILD FAILED with "not found"

- [ ] **Step 3: Write TechnicalIndicatorCalculator implementation**

```kotlin
// TechnicalIndicatorCalculator.kt
package com.scanfolio.data.indicator

import com.scanfolio.data.api.KlineData
import kotlin.math.sqrt

data class KDJ(val k: Double, val d: Double, val j: Double)
data class MACD(val dif: Double, val dea: Double, val macd: Double)
data class RSI(val rsi6: Double, val rsi12: Double, val rsi24: Double)
data class BOLL(val upper: Double, val mid: Double, val lower: Double)

class TechnicalIndicatorCalculator {

    fun calculateKDJ(kline: List<KlineData>, n: Int = 9): KDJ? {
        if (kline.size < n) return null
        val closes = kline.take(n).map { it.close }
        val high9 = kline.take(n).maxOf { it.high }
        val low9 = kline.take(n).minOf { it.low }
        val todayClose = closes.last()
        val rsv = if (high9 != low9) (todayClose - low9) / (high9 - low9) * 100 else 50.0
        val k = rsv
        val d = k
        val j = 3 * k - 2 * d
        return KDJ(k, d, j)
    }

    fun calculateMACD(kline: List<KlineData>, fast: Int = 12, slow: Int = 26, signal: Int = 9): MACD? {
        if (kline.size < slow + signal) return null
        val closes = kline.map { it.close }.reversed()
        fun ema(data: List<Double>, period: Int): List<Double> {
            val result = mutableListOf(data.first())
            val multiplier = 2.0 / (period + 1)
            for (i in 1 until data.size) {
                result.add((data[i] - result.last()) * multiplier + result.last())
            }
            return result
        }
        val emaFast = ema(closes, fast)
        val emaSlow = ema(closes, slow)
        val dif = emaFast.zip(emaSlow).map { it.first - it.second }
        val dea = ema(dif, signal)
        val macd = dif.zip(dea).map { 2.0 * (it.first - it.second) }
        return MACD(dif = dif.last(), dea = dea.last(), macd = macd.last())
    }

    fun calculateRSI(kline: List<KlineData>, period: Int = 6): Double {
        if (kline.size <= period) return 50.0
        val closes = kline.take(period + 1).map { it.close }.reversed()
        val gains = mutableListOf<Double>()
        val losses = mutableListOf<Double>()
        for (i in 1 until closes.size) {
            val diff = closes[i] - closes[i - 1]
            if (diff >= 0) { gains.add(diff); losses.add(0.0) }
            else { gains.add(0.0); losses.add(-diff) }
        }
        val avgGain = gains.average()
        val avgLoss = losses.average()
        return if (avgLoss == 0.0) 100.0 else 100.0 - 100.0 / (1.0 + avgGain / avgLoss)
    }

    fun calculateBOLL(kline: List<KlineData>, period: Int = 20, multiplier: Int = 2): BOLL? {
        if (kline.size < period) return null
        val closes = kline.take(period).map { it.close }
        val ma = closes.average()
        val variance = closes.map { (it - ma) * (it - ma) }.average()
        val std = sqrt(variance)
        return BOLL(
            upper = ma + multiplier * std,
            mid = ma,
            lower = ma - multiplier * std
        )
    }

    fun toDataColumns(kline: List<KlineData>): Map<String, String> {
        val map = mutableMapOf<String, String>()

        calculateKDJ(kline)?.let {
            map["KDJ_K"] = "%.2f".format(it.k)
            map["KDJ_D"] = "%.2f".format(it.d)
            map["KDJ_J"] = "%.2f".format(it.j)
        }

        calculateMACD(kline)?.let {
            map["MACD_DIF"] = "%.2f".format(it.dif)
            map["MACD_DEA"] = "%.2f".format(it.dea)
            map["MACD"] = "%.2f".format(it.macd)
        }

        map["RSI_6"] = "%.2f".format(calculateRSI(kline, 6))
        map["RSI_12"] = "%.2f".format(calculateRSI(kline, 12))
        map["RSI_24"] = "%.2f".format(calculateRSI(kline, 24))

        calculateBOLL(kline)?.let {
            map["BOLL_UPPER"] = "%.2f".format(it.upper)
            map["BOLL_MID"] = "%.2f".format(it.mid)
            map["BOLL_LOWER"] = "%.2f".format(it.lower)
        }

        return map
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd app && ..\gradlew testDebugUnitTest --tests "com.scanfolio.data.indicator.TechnicalIndicatorCalculatorTest" 2>&1`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/scanfolio/data/indicator/TechnicalIndicatorCalculator.kt app/src/test/java/com/scanfolio/data/indicator/TechnicalIndicatorCalculatorTest.kt
git commit -m "feat: add TechnicalIndicatorCalculator for KDJ/MACD/RSI/BOLL"
```

---

### Task 3: 删除OCR相关文件 + 更新依赖

**Files:**
- Delete: `app/src/main/java/com/scanfolio/ocr/OcrEngine.kt`
- Delete: `app/src/main/java/com/scanfolio/ocr/TableAnalyzer.kt`
- Delete: `app/src/main/java/com/scanfolio/ocr/BaiduOcrApi.kt`
- Delete: `app/src/main/java/com/scanfolio/ocr/ImagePreprocessor.kt`
- Delete: `app/src/main/java/com/scanfolio/ocr/HeaderRecognizer.kt`
- Delete: `app/src/main/java/com/scanfolio/ocr/OcrResult.kt`
- Delete: `app/src/main/java/com/scanfolio/ui/scan/PreviewScreen.kt`
- Modify: `app/build.gradle.kts`
- Delete: `app/src/test/java/com/scanfolio/ocr/TableAnalyzerTest.kt`
- Delete: `app/src/test/java/com/scanfolio/ui/scan/ScanViewModelTest.kt`

- [ ] **Step 1: Remove ML Kit dependency from build.gradle.kts**

Remove line 42: `implementation("com.google.mlkit:text-recognition-chinese:16.0.1")`

Result: OkHttp and MPAndroidChart remain.

- [ ] **Step 2: Delete all OCR source files**

Delete files in `ocr/` package and `PreviewScreen.kt`.

- [ ] **Step 3: Delete OCR test files**

Delete `TableAnalyzerTest.kt` and `ScanViewModelTest.kt`.

- [ ] **Step 4: Commit**

```bash
git rm app/src/main/java/com/scanfolio/ocr/*.kt
git rm app/src/main/java/com/scanfolio/ui/scan/PreviewScreen.kt
git rm app/src/test/java/com/scanfolio/ocr/TableAnalyzerTest.kt
git rm app/src/test/java/com/scanfolio/ui/scan/ScanViewModelTest.kt
git add app/build.gradle.kts
git commit -m "refactor: remove OCR engine, ML Kit dependency, and all OCR-related files"
```

---

### Task 4: 清理 ScanfolioApp + Settings 中的OCR残留

**Files:**
- Modify: `app/src/main/java/com/scanfolio/ScanfolioApp.kt`
- Modify: `app/src/main/java/com/scanfolio/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/scanfolio/ui/settings/SettingsViewModel.kt`

- [ ] **Step 1: Clean ScanfolioApp.kt**

Remove OCR-related code:
```kotlin
// REMOVE these lines:
import com.scanfolio.ocr.*
// ...
val ocrEngine by lazy { OcrEngine(this) }
val tableAnalyzer by lazy { TableAnalyzer(ocrEngine) }
// ...
fun getBaiduOcrApiKey(): String? = prefs.getString(...)
fun getBaiduOcrSecretKey(): String? = prefs.getString(...)
fun setBaiduOcrKeys(apiKey: String, secretKey: String) { ... }
```

Add StockApiClient + TechnicalIndicatorCalculator:
```kotlin
import com.scanfolio.data.api.StockApiClient
import com.scanfolio.data.indicator.TechnicalIndicatorCalculator
// ...
val stockApiClient by lazy { StockApiClient(okHttpClient) }
val indicatorCalculator by lazy { TechnicalIndicatorCalculator() }

private val okHttpClient by lazy { okhttp3.OkHttpClient() }
```

Result:
```kotlin
package com.scanfolio

import android.app.Application
import android.content.Context
import com.scanfolio.data.api.StockApiClient
import com.scanfolio.data.db.AppDatabase
import com.scanfolio.data.indicator.TechnicalIndicatorCalculator
import com.scanfolio.data.repository.*

class ScanfolioApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val stockRepository by lazy { StockRepository(database.stockRecordDao()) }
    val tradeRepository by lazy { TradeRepository(database.tradeRecordDao()) }
    val settingsRepository by lazy {
        SettingsRepository(database.columnDefinitionDao(), database.strategyTypeDao())
    }
    val marketIndexRepository by lazy {
        MarketIndexRepository(
            database.marketIndexDefinitionDao(),
            database.marketIndexDailyRecordDao()
        )
    }

    val stockApiClient by lazy { StockApiClient(okhttp3.OkHttpClient()) }
    val indicatorCalculator by lazy { TechnicalIndicatorCalculator() }
}
```

- [ ] **Step 2: Remove Baidu OCR card from SettingsScreen.kt**

Delete lines 103-130 (the Card block with Baidu API Key/Secret Key fields).

- [ ] **Step 3: Remove Baidu API key state from SettingsViewModel.kt**

Delete lines 63-70:
```kotlin
val baiduApiKey = MutableStateFlow(app.getBaiduOcrApiKey() ?: "")
val baiduSecretKey = MutableStateFlow(app.getBaiduOcrSecretKey() ?: "")
fun updateBaiduOcrKeys(apiKey: String, secretKey: String) { ... }
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/scanfolio/ScanfolioApp.kt app/src/main/java/com/scanfolio/ui/settings/SettingsScreen.kt app/src/main/java/com/scanfolio/ui/settings/SettingsViewModel.kt
git commit -m "refactor: remove OCR engine init and Baidu API key config from settings"
```

---

### Task 5: DB Migration v2→v3 — 添加 strategy_name + quantity 字段

**Files:**
- Modify: `app/src/main/java/com/scanfolio/data/db/entity/StockRecordEntity.kt`
- Modify: `app/src/main/java/com/scanfolio/data/db/entity/TradeRecordEntity.kt`
- Modify: `app/src/main/java/com/scanfolio/data/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/scanfolio/data/db/dao/StockRecordDao.kt`

- [ ] **Step 1: Add strategy_name to StockRecordEntity**

```kotlin
// StockRecordEntity.kt - add field
@ColumnInfo(name = "strategy_name")
val strategyName: String? = null,
```

Full entity:
```kotlin
@Entity(tableName = "stock_records")
data class StockRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    @ColumnInfo(name = "data_columns")
    val dataColumns: Map<String, String> = emptyMap(),
    @ColumnInfo(name = "strategy_name")
    val strategyName: String? = null,
    @ColumnInfo(name = "last_screenshot")
    val lastScreenshot: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: Add quantity to TradeRecordEntity**

```kotlin
// TradeRecordEntity.kt - add field
@ColumnInfo(name = "quantity")
val quantity: Int? = null,
```

- [ ] **Step 3: Create MIGRATION_2_3 in AppDatabase.kt**

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `stock_records` ADD COLUMN `strategy_name` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `trade_records` ADD COLUMN `quantity` INTEGER DEFAULT NULL")
    }
}
```

Update version and migration list:
```kotlin
@Database(
    entities = [...],
    version = 3,
    exportSchema = false
)
// In getInstance:
.addMigrations(MIGRATION_1_2, MIGRATION_2_3)
```

- [ ] **Step 4: Add DAO query for grouping**

In `StockRecordDao.kt`:
```kotlin
@Query("SELECT * FROM stock_records ORDER BY strategy_name, last_screenshot DESC")
fun getAllGrouped(): Flow<List<StockRecordEntity>>
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/scanfolio/data/db/entity/StockRecordEntity.kt app/src/main/java/com/scanfolio/data/db/entity/TradeRecordEntity.kt app/src/main/java/com/scanfolio/data/db/AppDatabase.kt app/src/main/java/com/scanfolio/data/db/dao/StockRecordDao.kt
git commit -m "feat(db): add strategy_name to stock_records and quantity to trade_records, migration v2->v3"
```

---

### Task 6: SearchStockScreen + SearchStockViewModel — 搜索添加页

**Files:**
- Rewrite: `app/src/main/java/com/scanfolio/ui/scan/ScanScreen.kt`
- Rewrite: `app/src/main/java/com/scanfolio/ui/scan/ScanViewModel.kt`

- [ ] **Step 1: Rewrite ScanViewModel as SearchStockViewModel**

```kotlin
// ScanViewModel.kt - complete rewrite
package com.scanfolio.ui.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.data.api.StockFullData
import com.scanfolio.data.api.StockQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp
    private val stockRepo = app.stockRepository
    private val api = app.stockApiClient
    private val indicatorCalc = app.indicatorCalculator

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResult = MutableStateFlow<StockQuote?>(null)
    val searchResult: StateFlow<StockQuote?> = _searchResult.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _addedMessage = MutableStateFlow<String?>(null)
    val addedMessage: StateFlow<String?> = _addedMessage.asStateFlow()

    val strategies = app.settingsRepository.getStrategies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(query: String) {
        if (query.length <= 6 && query.all { it.isDigit() }) {
            _searchQuery.value = query
            if (query.length == 6) {
                searchStock(query)
            } else {
                _searchResult.value = null
                _error.value = null
            }
        }
    }

    private fun searchStock(code: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _error.value = null
            _searchResult.value = null
            try {
                val quote = withContext(Dispatchers.IO) { api.fetchQuote(code) }
                if (quote != null) {
                    _searchResult.value = quote
                } else {
                    _error.value = "未找到该股票代码：$code"
                }
            } catch (e: Exception) {
                _error.value = "查询失败: ${e.message ?: "网络错误"}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addStock(strategyName: String? = null) {
        val quote = _searchResult.value ?: return
        viewModelScope.launch {
            try {
                // Fetch full quote data from East Money API for rich fields
                val fullQuote = withContext(Dispatchers.IO) {
                    api.fetchFullQuote(quote.code) ?: quote
                }
                val kline = withContext(Dispatchers.IO) {
                    api.fetchKline(quote.code, 60)
                }
                val moneyFlow = withContext(Dispatchers.IO) {
                    api.fetchMoneyFlow(quote.code)
                }

                val dataColumns = api.buildDataColumns(fullQuote, kline, moneyFlow)
                val indicatorColumns = indicatorCalc.toDataColumns(kline)
                val allColumns = dataColumns + indicatorColumns

                stockRepo.mergeScreenshotData(
                    code = quote.code,
                    name = fullQuote.name,
                    newData = allColumns
                )

                _addedMessage.value = "${fullQuote.name} 已加入自选"
                _searchQuery.value = ""
                _searchResult.value = null
            } catch (e: Exception) {
                // Fallback: add with basic Sina data only
                stockRepo.mergeScreenshotData(
                    code = quote.code,
                    name = quote.name,
                    newData = api.buildDataColumns(quote, emptyList(), com.scanfolio.data.api.MoneyFlow())
                )
                _addedMessage.value = "${quote.name} 已加入自选"
                _searchQuery.value = ""
                _searchResult.value = null
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearAddedMessage() { _addedMessage.value = null }

    fun updateStockStrategy(stockId: Long, strategyName: String) {
        viewModelScope.launch {
            val stock = stockRepo.getById(stockId) ?: return@launch
            stockRepo.update(stock.copy(strategyName = strategyName))
        }
    }
}
```

- [ ] **Step 2: Rewrite ScanScreen as SearchStockScreen**

```kotlin
// ScanScreen.kt - complete rewrite
package com.scanfolio.ui.scan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.scanfolio.ui.theme.DownGreen
import com.scanfolio.ui.theme.UpRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    navController: NavController,
    viewModel: ScanViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val error by viewModel.error.collectAsState()
    val addedMessage by viewModel.addedMessage.collectAsState()
    val strategies by viewModel.strategies.collectAsState()

    var selectedStrategy by remember { mutableStateOf<String?>(null) }
    var showStrategyPicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(addedMessage) {
        addedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAddedMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加自选", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateQuery(it) },
                label = { Text("输入6位股票代码") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如: 600036") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isSearching -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("正在查询...")
                }

                error != null -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            error!!,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                searchResult != null -> {
                    val quote = searchResult!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = quote.code,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = quote.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "%.2f".format(quote.currentPrice),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val changeColor = when {
                                    quote.changePercent > 0 -> UpRed
                                    quote.changePercent < 0 -> DownGreen
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (quote.changePercent >= 0) "+%.2f%%".format(quote.changePercent) else "%.2f%%".format(quote.changePercent),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = changeColor
                                    )
                                    Text(
                                        text = if (quote.changeAmount >= 0) "+%.2f".format(quote.changeAmount) else "%.2f".format(quote.changeAmount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = changeColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedButton(
                                onClick = { showStrategyPicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedStrategy ?: "选择分组（可选）")
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.addStock(selectedStrategy) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("加入自选")
                            }
                        }
                    }
                }

                else -> {
                    Icon(
                        Icons.Default.TravelExplore,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "输入股票代码查询并加入自选",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showStrategyPicker) {
        AlertDialog(
            onDismissRequest = { showStrategyPicker = false },
            title = { Text("选择分组") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            selectedStrategy = null
                            showStrategyPicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("不分组")
                    }
                    if (strategies.isEmpty()) {
                        Text("暂无分组，请先在设置中创建战法", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    strategies.forEach { strategy ->
                        TextButton(
                            onClick = {
                                selectedStrategy = strategy.name
                                showStrategyPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strategy.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStrategyPicker = false }) { Text("取消") }
            }
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/scan/ScanScreen.kt app/src/main/java/com/scanfolio/ui/scan/ScanViewModel.kt
git commit -m "feat: rewrite ScanScreen as stock search and add page"
```

---

### Task 7: 更新导航 — 改Tab名、移除旧路由、添加PnL路由

**Files:**
- Modify: `app/src/main/java/com/scanfolio/ui/navigation/AppNavigation.kt`

- [ ] **Step 1: Update Navigation**

```kotlin
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Portfolio : Screen("portfolio", "持仓", Icons.Default.List)
    data object Scan : Screen("scan", "添加", Icons.Default.AddCircle)
    data object Analysis : Screen("analysis", "分析", Icons.Default.BarChart)
}
```

Changes:
- `"扫描"` → `"添加"`
- `Icons.Default.CameraAlt` → `Icons.Default.AddCircle`
- Remove `composable("preview")` block (lines 85-87)
- Add `composable("pnl_detail")` route:

```kotlin
composable("pnl_detail") {
    com.scanfolio.ui.pnl.PnLDetailScreen(navController)
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/navigation/AppNavigation.kt
git commit -m "feat: rename scan tab to 添加, add pnl_detail route, remove preview route"
```

---

### Task 8: 自选股分组 — PortfolioScreen分组展示

**Files:**
- Modify: `app/src/main/java/com/scanfolio/ui/portfolio/PortfolioViewModel.kt`
- Modify: `app/src/main/java/com/scanfolio/ui/portfolio/PortfolioScreen.kt`
- Modify: `app/src/main/java/com/scanfolio/ui/portfolio/DashboardCard.kt`

- [ ] **Step 1: Update PortfolioViewModel for grouping**

Modify the `stocks` Flow to group by `strategyName`:

```kotlin
// Add new StateFlow for grouped stocks
data class StockGroup(
    val strategyName: String?,
    val stocks: List<StockRecordEntity>,
    val winRate: Double = 0.0,
    val totalPnl: Double = 0.0
)

val groupedStocks: StateFlow<List<StockGroup>> = combine(
    allStocks, allTrades, _searchQuery, _sortOption
) { stocks, trades, query, sort ->
    var filtered = stocks
    if (query.isNotBlank()) {
        val q = query.lowercase()
        filtered = stocks.filter {
            it.code.lowercase().contains(q) || it.name.lowercase().contains(q)
        }
    }
    val grouped = filtered.groupBy { it.strategyName }
    grouped.map { (strategy, groupStocks) ->
        val groupTrades = trades.filter { t ->
            groupStocks.any { s -> s.id == t.stockRecordId }
        }
        val closed = groupTrades.filter { it.sellTime != null }
        val winRate = if (closed.isNotEmpty())
            closed.count { it.isSuccess }.toDouble() / closed.size else 0.0
        val totalPnl = closed.sumOf { it.profitAmount ?: 0.0 }
        StockGroup(
            strategyName = strategy,
            stocks = groupStocks,
            winRate = winRate,
            totalPnl = totalPnl
        )
    }.sortedBy { it.strategyName ?: "zzz" }
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

// Keep original flat stocks for search results
val stocks: StateFlow<List<StockRecordEntity>> = combine(
    allStocks, _searchQuery, _sortOption
) { stocks, query, sort ->
    ...
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

- [ ] **Step 2: Update PortfolioScreen for grouped display**

Replace the flat stock list with expandable group sections:

```kotlin
// In PortfolioScreen, replace LazyColumn items block:
val groupedStocks by viewModel.groupedStocks.collectAsState()

if (searchQuery.isNotBlank()) {
    // Flat list when searching (stocks Flow already applies search/sort)
    LazyColumn(...) {
        items(stocks, key = { it.id }) { stock ->
            StockListItem(...)
        }
    }
} else {
    // Grouped display
    LazyColumn(...) {
        groupedStocks.forEach { group ->
            item {
                GroupHeader(
                    name = group.strategyName ?: "未分组",
                    stockCount = group.stocks.size,
                    winRate = group.winRate,
                    totalPnl = group.totalPnl
                )
            }
            items(group.stocks, key = { it.id }) { stock ->
                StockListItem(...)
            }
        }
    }
}
```

Add GroupHeader composable:
```kotlin
@Composable
private fun GroupHeader(
    name: String,
    stockCount: Int,
    winRate: Double,
    totalPnl: Double
) {
    var expanded by remember { mutableStateOf(true) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(name, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("$stockCount 只", style = MaterialTheme.typography.bodySmall)
            }
            if (winRate > 0) {
                Text(
                    "胜率: %.0f%%".format(winRate * 100),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (winRate >= 0.5) UpRed else DownGreen
                )
            }
        }
    }
    if (!expanded) {
        // Render nothing for collapsed group
    }
}
```

- [ ] **Step 3: Update DashboardCard to add link to PnL detail**

Make the DashboardCard clickable to navigate to PnL detail:

```kotlin
@Composable
fun DashboardCard(
    stats: DashboardStats?,
    onViewDetail: () -> Unit = {}  // Add this parameter
) {
    ...
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onViewDetail),  // Add clickable
        ...
    )
}
```

Update PortfolioScreen call site:
```kotlin
DashboardCard(
    stats = dashboardStats,
    onViewDetail = { navController.navigate("pnl_detail") }
)
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/portfolio/PortfolioViewModel.kt app/src/main/java/com/scanfolio/ui/portfolio/PortfolioScreen.kt app/src/main/java/com/scanfolio/ui/portfolio/DashboardCard.kt
git commit -m "feat: add stock grouping by strategy and PnL detail entry point"
```

---

### Task 9: PnL Detail Screen — 盈亏详情 + 收益曲线

**Files:**
- Create: `app/src/main/java/com/scanfolio/ui/pnl/PnLViewModel.kt`
- Create: `app/src/main/java/com/scanfolio/ui/pnl/PnLDetailScreen.kt`

- [ ] **Step 1: Write PnLViewModel**

```kotlin
// PnLViewModel.kt
package com.scanfolio.ui.pnl

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.data.db.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class PnLStats(
    val totalRealizedPnl: Double = 0.0,
    val totalUnrealizedPnl: Double = 0.0,
    val winRate: Double = 0.0,
    val totalTrades: Int = 0,
    val avgWin: Double = 0.0,
    val avgLoss: Double = 0.0,
    val largestWin: Double = 0.0,
    val largestLoss: Double = 0.0,
    val monthlyPnL: List<MonthlyPnL> = emptyList(),
    val profitCurve: List<CurvePoint> = emptyList()
)

data class MonthlyPnL(val month: String, val pnl: Double)
data class CurvePoint(val date: String, val cumulativePnl: Double)

class PnLViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp
    private val tradeRepo = app.tradeRepository
    private val stockRepo = app.stockRepository
    private val api = app.stockApiClient

    private val _pnlStats = MutableStateFlow(PnLStats())
    val pnlStats: StateFlow<PnLStats> = _pnlStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPnL()
    }

    fun loadPnL() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val trades = tradeRepo.getAll().first()
                val stocks = stockRepo.getAll().first()

                val closedTrades = trades.filter { it.sellTime != null }
                val openTrades = trades.filter { it.sellTime == null }

                val totalRealized = closedTrades.sumOf { it.profitAmount ?: 0.0 }

                // Calculate unrealized PnL from open positions
                var totalUnrealized = 0.0
                for (trade in openTrades) {
                    val stock = stocks.find { it.id == trade.stockRecordId }
                    if (stock != null && trade.quantity != null) {
                        val quote = withContext(Dispatchers.IO) {
                            api.fetchQuote(stock.code)
                        }
                        if (quote != null) {
                            val unrealized = (quote.currentPrice - trade.buyPrice) * trade.quantity!!
                            totalUnrealized += unrealized
                        }
                    }
                }

                // Win rate
                val wins = closedTrades.count { it.isSuccess }
                val winRate = if (closedTrades.isNotEmpty()) wins.toDouble() / closedTrades.size else 0.0

                // Average win/loss
                val avgWin = closedTrades.filter { it.isSuccess }.let {
                    if (it.isEmpty()) 0.0 else it.sumOf { t -> t.profitRatio ?: 0.0 } / it.size
                }
                val avgLoss = closedTrades.filter { !it.isSuccess }.let {
                    if (it.isEmpty()) 0.0 else it.sumOf { t -> t.profitRatio ?: 0.0 } / it.size
                }

                // Largest win/loss
                val largestWin = closedTrades.maxOfOrNull { it.profitAmount ?: 0.0 } ?: 0.0
                val largestLoss = closedTrades.minOfOrNull { it.profitAmount ?: 0.0 } ?: 0.0

                // Monthly PnL
                val monthlyMap = mutableMapOf<String, Double>()
                val dateFormat = SimpleDateFormat("yyyy-MM", Locale.CHINA)
                for (trade in closedTrades) {
                    val month = dateFormat.format(Date(trade.buyTime))
                    monthlyMap[month] = (monthlyMap[month] ?: 0.0) + (trade.profitAmount ?: 0.0)
                }
                val monthlyPnL = monthlyMap.entries
                    .sortedBy { it.key }
                    .map { MonthlyPnL(it.key, it.value) }

                // Profit curve (cumulative over time)
                val sortedTrades = closedTrades.sortedBy { it.sellTime ?: it.buyTime }
                val curve = mutableListOf<CurvePoint>()
                var cumulative = 0.0
                val dayFormat = SimpleDateFormat("MM-dd", Locale.CHINA)
                for (trade in sortedTrades) {
                    cumulative += trade.profitAmount ?: 0.0
                    curve.add(CurvePoint(dayFormat.format(Date(trade.sellTime ?: trade.buyTime)), cumulative))
                }

                _pnlStats.value = PnLStats(
                    totalRealizedPnl = totalRealized,
                    totalUnrealizedPnl = totalUnrealized,
                    winRate = winRate,
                    totalTrades = closedTrades.size,
                    avgWin = avgWin,
                    avgLoss = kotlin.math.abs(avgLoss),
                    largestWin = largestWin,
                    largestLoss = kotlin.math.abs(largestLoss),
                    monthlyPnL = monthlyPnL,
                    profitCurve = curve
                )
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

- [ ] **Step 2: Write PnLDetailScreen**

```kotlin
// PnLDetailScreen.kt
package com.scanfolio.ui.pnl

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.scanfolio.ui.theme.DownGreen
import com.scanfolio.ui.theme.UpRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PnLDetailScreen(
    navController: NavController,
    viewModel: PnLViewModel = viewModel()
) {
    val stats by viewModel.pnlStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("盈亏统计") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val totalPnl = stats.totalRealizedPnl + stats.totalUnrealizedPnl
                    Text(
                        "总盈亏",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (totalPnl >= 0) "+¥%.2f".format(totalPnl) else "-¥%.2f".format(-totalPnl),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (totalPnl >= 0) UpRed else DownGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("胜率", "%.1f%%".format(stats.winRate * 100))
                        StatItem("交易次数", "${stats.totalTrades}")
                        StatItem("持仓浮盈", "¥%.2f".format(stats.totalUnrealizedPnl))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("平均盈利", "+%.2f%%".format(stats.avgWin))
                        StatItem("平均亏损", "-%.2f%%".format(stats.avgLoss))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Monthly PnL bar chart
            if (stats.monthlyPnL.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("月度盈亏", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        MonthlyBarChart(stats.monthlyPnL, Modifier.height(200.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profit curve
            if (stats.profitCurve.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("收益曲线", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfitLineChart(stats.profitCurve, Modifier.height(200.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonthlyBarChart(monthlyPnL: List<MonthlyPnL>, modifier: Modifier) {
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                setFitBars(true)
                setScaleEnabled(false)
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                legend.isEnabled = false
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    valueFormatter = IndexAxisValueFormatter(
                        monthlyPnL.map {
                            val parts = it.month.split("-")
                            if (parts.size >= 2) "${parts[1]}月" else it.month
                        }
                    )
                }
                val entries = monthlyPnL.mapIndexed { index, item ->
                    BarEntry(index.toFloat(), item.pnl.toFloat())
                }
                val dataSet = BarDataSet(entries, "月度盈亏").apply {
                    colors = entries.map { entry ->
                        if (entry.y >= 0) android.graphics.Color.rgb(220, 38, 38) else android.graphics.Color.rgb(34, 197, 94)
                    }
                    valueTextSize = 10f
                }
                data = BarData(dataSet)
                invalidate()
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ProfitLineChart(curve: List<CurvePoint>, modifier: Modifier) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setScaleEnabled(true)
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
                legend.isEnabled = false
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    labelCount = minOf(curve.size, 5)
                    valueFormatter = IndexAxisValueFormatter(
                        curve.map { it.date }.let {
                            if (it.size <= 5) it
                            else listOf(it.first(), it[it.size / 4], it[it.size / 2], it[3 * it.size / 4], it.last())
                        }
                    )
                }
                val entries = curve.mapIndexed { index, point ->
                    Entry(index.toFloat(), point.cumulativePnl.toFloat())
                }
                val dataSet = LineDataSet(entries, "收益").apply {
                    color = android.graphics.Color.rgb(220, 38, 38)
                    valueTextSize = 10f
                    setCircleColor(android.graphics.Color.rgb(220, 38, 38))
                    circleRadius = 3f
                    lineWidth = 2f
                    setDrawValues(false)
                }
                data = LineData(dataSet)
                invalidate()
            }
        },
        modifier = modifier
    )
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/scanfolio/ui/pnl/
git commit -m "feat: add PnL detail screen with profit curve and monthly bar chart"
```

---

### Task 10: 验证构建

- [ ] **Step 1: Clean build**

Run: `cd app && ..\gradlew clean 2>&1`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Full build**

Run: `cd app && ..\gradlew assembleDebug 2>&1`
Expected: BUILD SUCCESSFUL, APK generated

- [ ] **Step 3: Run all unit tests**

Run: `cd app && ..\gradlew testDebugUnitTest 2>&1`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 4: Final commit if needed**

```bash
git add -A
git commit -m "chore: finalize V2 update with search, grouping, indicators, and PnL"
```
