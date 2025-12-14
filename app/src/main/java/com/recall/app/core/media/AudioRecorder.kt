package com.recall.app.core.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

class AudioRecorder @Inject constructor(
    private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(fileName: String): File? {
        return try {
            val audioDir = File(context.filesDir, "audio").apply { mkdirs() }
            val file = File(audioDir, "$fileName.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)

                try {
                    prepare()
                    start()
                    outputFile = file
                    Timber.d("Recording started: ${file.absolutePath}")
                } catch (e: IOException) {
                    Timber.e(e, "Failed to prepare recorder")
                    release()
                    return null
                }
            }

            file
        } catch (e: Exception) {
            Timber.e(e, "Failed to start recording")
            null
        }
    }

    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            outputFile?.also {
                Timber.d("Recording stopped: ${it.absolutePath}, size: ${it.length()} bytes")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop recording")
            null
        } finally {
            mediaRecorder = null
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            outputFile?.delete()
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel recording")
        } finally {
            mediaRecorder = null
            outputFile = null
        }
    }

    fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    fun isRecording(): Boolean = mediaRecorder != null
}
