package com.recall.app.core.ocr

import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class OcrProcessor @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImage(bitmap: Bitmap): OcrResult {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()

            val text = visionText.text
            val blocks = visionText.textBlocks.map { block ->
                TextBlock(
                    text = block.text,
                    confidence = block.confidence ?: 0f,
                    boundingBox = block.boundingBox
                )
            }

            Timber.d("OCR completed: ${text.length} chars, ${blocks.size} blocks")
            OcrResult.Success(text, blocks)
        } catch (e: Exception) {
            Timber.e(e, "OCR failed")
            OcrResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun processImageUri(uri: Uri, bitmap: Bitmap): OcrResult {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()

            val text = visionText.text
            val blocks = visionText.textBlocks.map { block ->
                TextBlock(
                    text = block.text,
                    confidence = block.confidence ?: 0f,
                    boundingBox = block.boundingBox
                )
            }

            Timber.d("OCR from URI completed: ${text.length} chars")
            OcrResult.Success(text, blocks)
        } catch (e: Exception) {
            Timber.e(e, "OCR from URI failed")
            OcrResult.Error(e.message ?: "Unknown error")
        }
    }

    fun close() {
        recognizer.close()
    }
}

sealed class OcrResult {
    data class Success(val text: String, val blocks: List<TextBlock>) : OcrResult()
    data class Error(val message: String) : OcrResult()
}

data class TextBlock(
    val text: String,
    val confidence: Float,
    val boundingBox: android.graphics.Rect?
)
