package com.scanfolio.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

class OcrEngine(private val context: Context) {

    private var tessApi: TessBaseAPI? = null
    private var initError: String? = null

    fun getTessApi(): TessBaseAPI? {
        if (tessApi == null && initError == null) {
            try {
                val datapath = context.filesDir.absolutePath + File.separator + "tesseract"
                val tessDir = File(datapath + File.separator + "tessdata")
                if (!tessDir.exists()) tessDir.mkdirs()
                copyTrainedDataIfNeeded(tessDir)

                TessBaseAPI().apply {
                    init(datapath, "chi_sim")
                    setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)
                    tessApi = this
                }
            } catch (e: Exception) {
                initError = "初始化OCR引擎失败: ${e.message}"
            }
        }
        return tessApi
    }

    fun getInitError(): String? = initError

    private fun copyTrainedDataIfNeeded(dir: File) {
        val targetFile = File(dir, "chi_sim.traineddata")
        if (!targetFile.exists()) {
            try {
                context.assets.open("tessdata/chi_sim.traineddata").use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("训练数据文件缺失", e)
            }
        }
    }

    fun recognizeText(bitmap: Bitmap): String {
        val resized = downscaleBitmap(bitmap, 2048)
        val processed = ImagePreprocessor.binarize(ImagePreprocessor.toGrayscale(resized))
        val api = getTessApi() ?: throw IllegalStateException(initError ?: "OCR引擎未初始化")
        synchronized(api) {
            api.setImage(processed)
            return api.getUTF8Text()
        }
    }

    private fun downscaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val (w, h) = bitmap.width to bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / maxOf(w, h)
        val newW = (w * ratio).toInt()
        val newH = (h * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    fun release() {
        try {
            tessApi?.end()
        } catch (_: Exception) {}
    }
}
