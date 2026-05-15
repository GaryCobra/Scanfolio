package com.scanfolio.ocr

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

class OcrEngine(private val context: Context) {

    private val tessBaseAPI: TessBaseAPI by lazy {
        val datapath = context.filesDir.absolutePath + File.separator + "tesseract"
        val dir = File(datapath + File.separator + "tessdata")
        if (!dir.exists()) dir.mkdirs()
        copyTrainedDataIfNeeded(dir)

        TessBaseAPI().apply {
            init(datapath, "chi_sim")
            setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)
        }
    }

    private fun copyTrainedDataIfNeeded(dir: File) {
        val targetFile = File(dir, "chi_sim.traineddata")
        if (!targetFile.exists()) {
            context.assets.open("tessdata/chi_sim.traineddata").use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun recognizeText(bitmap: Bitmap): String {
        val processed = ImagePreprocessor.binarize(ImagePreprocessor.toGrayscale(bitmap))
        synchronized(tessBaseAPI) {
            tessBaseAPI.setImage(processed)
            return tessBaseAPI.utF8Text
        }
    }

    fun recognizeRegion(bitmap: Bitmap, left: Int, top: Int, width: Int, height: Int): String {
        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
        return recognizeText(cropped)
    }

    fun release() {
        synchronized(tessBaseAPI) { tessBaseAPI.recycle() }
    }
}
