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
        if (kline.size < slow) return null
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
