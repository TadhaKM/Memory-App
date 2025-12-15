package com.recall.app.core.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.recall.app.core.util.toDateString
import com.recall.app.core.util.toDateTimeString
import com.recall.app.domain.model.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles exporting notes to various formats (TXT, Markdown)
 */
@Singleton
class NoteExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    enum class ExportFormat {
        TXT,
        MARKDOWN
    }

    /**
     * Export a single note to the specified format and return a share intent
     */
    fun exportNote(note: Note, format: ExportFormat): Intent {
        val content = when (format) {
            ExportFormat.TXT -> formatAsTxt(note)
            ExportFormat.MARKDOWN -> formatAsMarkdown(note)
        }

        val extension = when (format) {
            ExportFormat.TXT -> "txt"
            ExportFormat.MARKDOWN -> "md"
        }

        val mimeType = when (format) {
            ExportFormat.TXT -> "text/plain"
            ExportFormat.MARKDOWN -> "text/markdown"
        }

        val fileName = generateFileName(note, extension)
        val file = writeToFile(fileName, content)
        val uri = getFileUri(file)

        return createShareIntent(uri, mimeType, note)
    }

    /**
     * Export multiple notes to a single file
     */
    fun exportNotes(notes: List<Note>, format: ExportFormat): Intent {
        val content = when (format) {
            ExportFormat.TXT -> notes.joinToString("\n\n${"=".repeat(50)}\n\n") { formatAsTxt(it) }
            ExportFormat.MARKDOWN -> notes.joinToString("\n\n---\n\n") { formatAsMarkdown(it) }
        }

        val extension = when (format) {
            ExportFormat.TXT -> "txt"
            ExportFormat.MARKDOWN -> "md"
        }

        val mimeType = when (format) {
            ExportFormat.TXT -> "text/plain"
            ExportFormat.MARKDOWN -> "text/markdown"
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "recall_export_${timestamp}.$extension"
        val file = writeToFile(fileName, content)
        val uri = getFileUri(file)

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Recall Notes Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Get note content as plain text (for clipboard or quick share)
     */
    fun getNoteAsText(note: Note): String = formatAsTxt(note)

    /**
     * Get note content as markdown
     */
    fun getNoteAsMarkdown(note: Note): String = formatAsMarkdown(note)

    private fun formatAsTxt(note: Note): String = buildString {
        // Title from summary or first line
        val title = note.aiMetadata?.summary?.take(60)
            ?: note.rawText?.lines()?.firstOrNull()?.take(60)
            ?: "Untitled Note"
        appendLine(title)
        appendLine()

        // Metadata
        appendLine("Created: ${note.createdAt.toDateTimeString()}")
        note.aiMetadata?.type?.let {
            appendLine("Type: ${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }}")
        }
        if (note.pinned) appendLine("Pinned: Yes")
        appendLine()

        // Content
        note.rawText?.let {
            appendLine(it)
            appendLine()
        }

        // Transcript (if from voice)
        note.aiMetadata?.transcript?.let { transcript ->
            if (transcript != note.rawText) {
                appendLine("--- Transcript ---")
                appendLine(transcript)
                appendLine()
            }
        }

        // OCR Text (if from camera)
        note.aiMetadata?.ocrText?.let { ocrText ->
            if (ocrText != note.rawText) {
                appendLine("--- Extracted Text ---")
                appendLine(ocrText)
                appendLine()
            }
        }

        // Topics
        note.aiMetadata?.topics?.takeIf { it.isNotEmpty() }?.let { topics ->
            appendLine("Topics: ${topics.joinToString(", ")}")
        }

        // Entities
        note.aiMetadata?.entities?.takeIf { it.isNotEmpty() }?.let { entities ->
            appendLine("Entities: ${entities.joinToString(", ")}")
        }

        // Action Items
        note.aiMetadata?.actionItems?.takeIf { it.isNotEmpty() }?.let { items ->
            appendLine()
            appendLine("Action Items:")
            items.forEach { item ->
                val checkbox = if (item.done) "[x]" else "[ ]"
                val dueInfo = item.dueHint?.let { " (Due: $it)" } ?: ""
                appendLine("  $checkbox ${item.text}$dueInfo")
            }
        }
    }

    private fun formatAsMarkdown(note: Note): String = buildString {
        // Title
        val title = note.aiMetadata?.summary?.take(60)
            ?: note.rawText?.lines()?.firstOrNull()?.take(60)
            ?: "Untitled Note"
        appendLine("# $title")
        appendLine()

        // Metadata block
        appendLine("| Field | Value |")
        appendLine("|-------|-------|")
        appendLine("| Created | ${note.createdAt.toDateTimeString()} |")
        note.aiMetadata?.type?.let {
            appendLine("| Type | ${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }} |")
        }
        appendLine("| Source | ${note.source.name.lowercase().replaceFirstChar { c -> c.uppercase() }} |")
        if (note.pinned) appendLine("| Pinned | Yes |")
        appendLine()

        // Content
        note.rawText?.let {
            appendLine("## Content")
            appendLine()
            appendLine(it)
            appendLine()
        }

        // Transcript
        note.aiMetadata?.transcript?.let { transcript ->
            if (transcript != note.rawText) {
                appendLine("## Transcript")
                appendLine()
                appendLine("> ${transcript.replace("\n", "\n> ")}")
                appendLine()
            }
        }

        // OCR Text
        note.aiMetadata?.ocrText?.let { ocrText ->
            if (ocrText != note.rawText) {
                appendLine("## Extracted Text (OCR)")
                appendLine()
                appendLine("```")
                appendLine(ocrText)
                appendLine("```")
                appendLine()
            }
        }

        // Topics as tags
        note.aiMetadata?.topics?.takeIf { it.isNotEmpty() }?.let { topics ->
            appendLine("**Topics:** ${topics.joinToString(" ") { "`$it`" }}")
            appendLine()
        }

        // Entities
        note.aiMetadata?.entities?.takeIf { it.isNotEmpty() }?.let { entities ->
            appendLine("**Entities:** ${entities.joinToString(", ")}")
            appendLine()
        }

        // Action Items as checklist
        note.aiMetadata?.actionItems?.takeIf { it.isNotEmpty() }?.let { items ->
            appendLine("## Action Items")
            appendLine()
            items.forEach { item ->
                val checkbox = if (item.done) "[x]" else "[ ]"
                val dueInfo = item.dueHint?.let { " *(Due: $it)*" } ?: ""
                appendLine("- $checkbox ${item.text}$dueInfo")
            }
            appendLine()
        }

        // Summary (if different from title)
        note.aiMetadata?.summary?.let { summary ->
            if (summary.length > 60) {
                appendLine("## AI Summary")
                appendLine()
                appendLine("*$summary*")
            }
        }
    }

    private fun generateFileName(note: Note, extension: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(note.createdAt))
        val title = (note.aiMetadata?.summary ?: note.rawText ?: "note")
            .take(30)
            .replace(Regex("[^a-zA-Z0-9]"), "_")
            .lowercase()
        return "recall_${title}_$timestamp.$extension"
    }

    private fun writeToFile(fileName: String, content: String): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        // Clean up old exports (older than 1 hour)
        cleanupOldExports(exportDir)

        val file = File(exportDir, fileName)
        file.writeText(content, Charsets.UTF_8)
        Timber.d("Exported note to: ${file.absolutePath}")
        return file
    }

    private fun cleanupOldExports(exportDir: File) {
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        exportDir.listFiles()?.forEach { file ->
            if (file.lastModified() < oneHourAgo) {
                file.delete()
                Timber.d("Cleaned up old export: ${file.name}")
            }
        }
    }

    private fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun createShareIntent(uri: Uri, mimeType: String, note: Note): Intent {
        val title = note.aiMetadata?.summary?.take(50) ?: "Recall Note"
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, note.rawText?.take(200) ?: "")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
