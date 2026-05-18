package com.scanfolio.ocr

import android.graphics.Bitmap
import android.graphics.Color

object ImagePreprocessor {

    fun enhanceForOcr(bitmap: Bitmap): Bitmap {
        val gray = toGrayscale(bitmap)
        val contrasted = adjustContrast(gray, 1.5f)
        return otsuBinarize(contrasted)
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(result.width * result.height)
        result.getPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(gray, gray, gray)
        }
        result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        return result
    }

    private fun adjustContrast(bitmap: Bitmap, factor: Float): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(result.width * result.height)
        result.getPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        val lookup = IntArray(256) { v ->
            ((v - 128) * factor + 128).toInt().coerceIn(0, 255)
        }
        for (i in pixels.indices) {
            val gray = Color.red(pixels[i])
            pixels[i] = Color.rgb(lookup[gray], lookup[gray], lookup[gray])
        }
        result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        return result
    }

    private fun otsuBinarize(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(result.width * result.height)
        result.getPixels(pixels, 0, result.width, 0, 0, result.width, result.height)

        val histogram = IntArray(256)
        for (pixel in pixels) {
            histogram[Color.red(pixel)]++
        }
        val total = pixels.size
        var sum = 0.0
        for (i in 0..255) sum += i * histogram[i]

        var sumB = 0.0
        var wB = 0
        var wF = 0
        var maxVariance = 0.0
        var threshold = 0
        for (i in 0..255) {
            wB += histogram[i]
            if (wB == 0) continue
            wF = total - wB
            if (wF == 0) break
            sumB += i * histogram[i]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val variance = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (variance > maxVariance) {
                maxVariance = variance
                threshold = i
            }
        }

        for (i in pixels.indices) {
            val gray = Color.red(pixels[i])
            val binary = if (gray >= threshold) 255 else 0
            pixels[i] = Color.rgb(binary, binary, binary)
        }
        result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        return result
    }

    fun detectRowBoundaries(bitmap: Bitmap): List<Int> {
        val boundaries = mutableListOf(0)
        val rowGapThreshold = (bitmap.height * 0.02).toInt().coerceAtLeast(4)
        var lastNonEmptyRow = 0
        var emptyRun = 0
        for (y in 1 until bitmap.height) {
            val isEmptyRow = isRowMostlyEmpty(bitmap, y)
            if (isEmptyRow) {
                emptyRun++
                if (emptyRun >= rowGapThreshold && lastNonEmptyRow > boundaries.last() + rowGapThreshold) {
                    boundaries.add(y - emptyRun / 2)
                    boundaries.add(y)
                }
            } else {
                emptyRun = 0
                lastNonEmptyRow = y
            }
        }
        boundaries.add(bitmap.height)
        return boundaries.distinct().filter { it < bitmap.height }.sorted()
    }

    private fun isRowMostlyEmpty(bitmap: Bitmap, y: Int, threshold: Float = 0.95f): Boolean {
        val pixels = IntArray(bitmap.width)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, y, bitmap.width, 1)
        var lightPixels = 0
        var darkPixels = 0
        for (pixel in pixels) {
            val r = Color.red(pixel)
            if (r > 200) lightPixels++
            else if (r < 30) darkPixels++
        }
        val ratio = maxOf(lightPixels, darkPixels).toFloat() / bitmap.width
        return ratio > threshold
    }
}
