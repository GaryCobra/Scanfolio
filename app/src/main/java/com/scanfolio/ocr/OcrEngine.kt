package com.scanfolio.ocr

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

class OcrEngine(private val context: Context) {

    private val tessBaseAPI = lazyTess()

    private fun lazyTess(): TessBaseAPI {
        val datapath = context.filesDir.absolutePath + File.separator + "tesseract"
        val dir = File(datapath + File.separator + "tessdata")
        if (!dir.exists()) dir.mkdirs()
        copyTrainedDataIfNeeded(dir)

        val api = TessBaseAPI()
        api.init(datapath, "chi_sim")
        api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)
        return api
    }

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
                throw RuntimeException("Failed to copy Tesseract traineddata from assets", e)
            }
        }
    }

    @Synchronized
    fun recognizeText(bitmap: Bitmap): String {
        val processed = ImagePreprocessor.binarize(ImagePreprocessor.toGrayscale(bitmap))
        tessBaseAPI.setImage(processed)
        return tessBaseAPI.getUTF8Text()
    }

    fun recognizeRegion(bitmap: Bitmap, left: Int, top: Int, width: Int, height: Int): String {
        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
        return recognizeText(cropped)
    }

    @Synchronized
    fun release() {
        try {
            val m = tessBaseAPI.javaClass.getDeclaredMethod("recycle")
            m.invoke(tessBaseAPI)
        } catch (_: Exception) {}
    }
}
