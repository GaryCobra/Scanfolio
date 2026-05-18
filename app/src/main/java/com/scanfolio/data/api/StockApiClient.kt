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
    val mainForceNetInflow: Double = 0.0,
    val retailNetInflow: Double = 0.0
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
                changeAmount = json.optDouble("f55", 0.0) * yestClose / 100.0,
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
