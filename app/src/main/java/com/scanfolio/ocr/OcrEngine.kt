package com.scanfolio.ocr

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.android.gms.tasks.Tasks

class OcrEngine(private val context: Context) {
    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    enum class OcrSource { ML_KIT, PREPROCESSED_ML_KIT, BAIDU_API }

    data class OcrBlock(
        val text: String,
        val lines: List<OcrLine>
    )

    data class OcrLine(
        val text: String,
        val elements: List<OcrElement>
    )

    data class OcrElement(
        val text: String,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    data class OcrResult(
        val text: String,
        val source: OcrSource,
        val blocks: List<OcrBlock>? = null
    )

    fun recognizeText(bitmap: Bitmap, baiduApiKey: String? = null, baiduSecretKey: String? = null): OcrResult? {
        val mlResult = recognizeWithMlKit(bitmap)
        if (mlResult != null && mlResult.text.isNotBlank() && hasReasonableContent(mlResult.text)) {
            return OcrResult(mlResult.text, OcrSource.ML_KIT, mlResult.blocks)
        }

        val preprocessed = ImagePreprocessor.enhanceForOcr(bitmap)
        val mlRetry = recognizeWithMlKit(preprocessed)
        if (mlRetry != null && mlRetry.text.isNotBlank() && hasReasonableContent(mlRetry.text)) {
            return OcrResult(mlRetry.text, OcrSource.PREPROCESSED_ML_KIT, mlRetry.blocks)
        }

        if (!baiduApiKey.isNullOrBlank() && !baiduSecretKey.isNullOrBlank()) {
            val baiduResult = runCatching {
                BaiduOcrApi(baiduApiKey, baiduSecretKey).recognizeText(bitmap)
            }.getOrNull()
            if (!baiduResult.isNullOrBlank()) {
                return OcrResult(baiduResult, OcrSource.BAIDU_API)
            }
        }

        return null
    }

    private fun recognizeWithMlKit(bitmap: Bitmap): MlKitResult? {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val text = Tasks.await(recognizer.process(image))
            val blocks = text.textBlocks.map { block ->
                OcrBlock(
                    text = block.text,
                    lines = block.lines.map { line ->
                        OcrLine(
                            text = line.text,
                            elements = line.elements.map { elem ->
                                val box = elem.boundingBox
                                OcrElement(
                                    text = elem.text,
                                    left = box?.left ?: 0,
                                    top = box?.top ?: 0,
                                    right = box?.right ?: 0,
                                    bottom = box?.bottom ?: 0
                                )
                            }
                        )
                    }
                )
            }
            MlKitResult(text.text, blocks)
        } catch (e: Exception) {
            null
        }
    }

    private fun hasReasonableContent(text: String): Boolean {
        val stockCodeRegex = Regex("\\d{6}")
        val chineseRegex = Regex("[\\u4e00-\\u9fff]")
        return stockCodeRegex.containsMatchIn(text) || chineseRegex.containsMatchIn(text)
    }

    private data class MlKitResult(
        val text: String,
        val blocks: List<OcrBlock>
    )

    fun release() {
        recognizer.close()
    }
}
