package com.recall.app.core.share

import android.content.Intent
import android.net.Uri
import timber.log.Timber

/**
 * Handles incoming share intents from other apps
 */
object ShareHandler {

    /**
     * Data class representing shared content
     */
    data class SharedContent(
        val text: String? = null,
        val imageUris: List<Uri> = emptyList(),
        val source: String? = null
    ) {
        val hasContent: Boolean
            get() = !text.isNullOrBlank() || imageUris.isNotEmpty()
    }

    /**
     * Parse an incoming intent and extract shared content
     */
    fun parseIntent(intent: Intent?): SharedContent? {
        if (intent == null) return null

        return when (intent.action) {
            Intent.ACTION_SEND -> handleSendIntent(intent)
            Intent.ACTION_SEND_MULTIPLE -> handleSendMultipleIntent(intent)
            else -> null
        }
    }

    private fun handleSendIntent(intent: Intent): SharedContent? {
        val type = intent.type ?: return null

        return when {
            type.startsWith("text/") -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

                val fullText = buildString {
                    if (!subject.isNullOrBlank()) {
                        appendLine(subject)
                        appendLine()
                    }
                    if (!text.isNullOrBlank()) {
                        append(text)
                    }
                }

                if (fullText.isNotBlank()) {
                    Timber.d("Received shared text: ${fullText.take(100)}...")
                    SharedContent(
                        text = fullText,
                        source = intent.`package`
                    )
                } else null
            }

            type.startsWith("image/") -> {
                @Suppress("DEPRECATION")
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) {
                    Timber.d("Received shared image: $uri")
                    SharedContent(
                        imageUris = listOf(uri),
                        source = intent.`package`
                    )
                } else null
            }

            else -> {
                Timber.w("Unsupported share type: $type")
                null
            }
        }
    }

    private fun handleSendMultipleIntent(intent: Intent): SharedContent? {
        val type = intent.type ?: return null

        return when {
            type.startsWith("image/") -> {
                @Suppress("DEPRECATION")
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                if (!uris.isNullOrEmpty()) {
                    Timber.d("Received ${uris.size} shared images")
                    SharedContent(
                        imageUris = uris,
                        source = intent.`package`
                    )
                } else null
            }

            else -> {
                Timber.w("Unsupported multi-share type: $type")
                null
            }
        }
    }
}
