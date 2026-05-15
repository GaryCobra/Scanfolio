package com.scanfolio.ocr

import android.graphics.Bitmap
import android.graphics.Color

object ImagePreprocessor {

    fun toGrayscale(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        for (x in 0 until result.width) {
            for (y in 0 until result.height) {
                val pixel = result.getPixel(x, y)
                val gray = (Color.red(pixel) * 0.299 +
                        Color.green(pixel) * 0.587 +
                        Color.blue(pixel) * 0.114).toInt()
                result.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        return result
    }

    fun binarize(bitmap: Bitmap, threshold: Int = 128): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        for (x in 0 until result.width) {
            for (y in 0 until result.height) {
                val pixel = result.getPixel(x, y)
                val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                val binary = if (gray > threshold) Color.WHITE else Color.BLACK
                result.setPixel(x, y, binary)
            }
        }
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
        var emptyPixels = 0
        for (x in 0 until bitmap.width) {
            if (Color.red(bitmap.getPixel(x, y)) > 200) emptyPixels++
        }
        return emptyPixels.toFloat() / bitmap.width > threshold
    }
}
