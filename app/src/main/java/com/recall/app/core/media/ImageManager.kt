package com.recall.app.core.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.recall.app.core.util.generateUUID
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ImageManager @Inject constructor(
    private val context: Context
) {
    fun createImageFile(): File {
        val imageDir = File(context.filesDir, "images").apply { mkdirs() }
        return File(imageDir, "${generateUUID()}.jpg")
    }

    fun createImageUri(): Uri {
        val file = createImageFile()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun saveImage(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val imageFile = createImageFile()

            inputStream?.use { input ->
                FileOutputStream(imageFile).use { output ->
                    input.copyTo(output)
                }
            }

            Timber.d("Image saved: ${imageFile.absolutePath}, size: ${imageFile.length()} bytes")
            imageFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to save image")
            null
        }
    }

    fun saveBitmap(bitmap: Bitmap, quality: Int = 85): File? {
        return try {
            val imageFile = createImageFile()
            FileOutputStream(imageFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            }
            Timber.d("Bitmap saved: ${imageFile.absolutePath}")
            imageFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to save bitmap")
            null
        }
    }

    fun deleteImage(file: File): Boolean {
        return try {
            file.delete().also { deleted ->
                if (deleted) {
                    Timber.d("Image deleted: ${file.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete image")
            false
        }
    }

    fun getImageFile(filename: String): File {
        val imageDir = File(context.filesDir, "images")
        return File(imageDir, filename)
    }
}
