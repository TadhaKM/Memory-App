package com.recall.app.ui.screens.notedetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.recall.app.core.export.NoteExporter
import com.recall.app.core.util.toDateTimeString
import com.recall.app.domain.model.Note
import com.recall.app.domain.model.NoteType
import com.recall.app.ui.components.AiMetadataCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    onNavigateBack: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(snackbarMessage)
            showSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    when (val state = uiState) {
                        is NoteDetailUiState.Success -> {
                            // Copy to clipboard
                            IconButton(onClick = {
                                viewModel.getNoteAsText()?.let { text ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Note", text))
                                    snackbarMessage = "Copied to clipboard"
                                    showSnackbar = true
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }

                            // Export/Share menu
                            Box {
                                IconButton(onClick = { showExportMenu = true }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                                DropdownMenu(
                                    expanded = showExportMenu,
                                    onDismissRequest = { showExportMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Export as Text") },
                                        onClick = {
                                            viewModel.exportNote(NoteExporter.ExportFormat.TXT)?.let { intent ->
                                                context.startActivity(Intent.createChooser(intent, "Share note"))
                                            }
                                            showExportMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Description, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export as Markdown") },
                                        onClick = {
                                            viewModel.exportNote(NoteExporter.ExportFormat.MARKDOWN)?.let { intent ->
                                                context.startActivity(Intent.createChooser(intent, "Share note"))
                                            }
                                            showExportMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Code, contentDescription = null)
                                        }
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.togglePin() }) {
                                Icon(
                                    if (state.note.pinned) Icons.Filled.PushPin else Icons.Default.PushPin,
                                    contentDescription = if (state.note.pinned) "Unpin" else "Pin"
                                )
                            }
                            IconButton(onClick = { viewModel.archiveNote(); onNavigateBack() }) {
                                Icon(Icons.Default.Archive, contentDescription = "Archive")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                        else -> {}
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = uiState) {
            is NoteDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is NoteDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is NoteDetailUiState.Success -> {
                NoteContent(
                    note = state.note,
                    onTextChanged = { viewModel.updateNoteText(it) },
                    onReRunAi = { /* TODO: Implement re-run AI */ },
                    onUpdateSummary = { /* TODO: Implement update summary */ },
                    onUpdateType = { /* TODO: Implement update type */ },
                    onToggleActionItem = { _, _ -> /* TODO: Implement toggle action item */ },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote()
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NoteContent(
    note: Note,
    onTextChanged: (String) -> Unit,
    onReRunAi: () -> Unit,
    onUpdateSummary: (String) -> Unit,
    onUpdateType: (NoteType) -> Unit,
    onToggleActionItem: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(note.id) { mutableStateOf(note.rawText ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Metadata
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Created: ${note.createdAt.toDateTimeString()}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Updated: ${note.updatedAt.toDateTimeString()}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Source: ${note.source.name}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // AI Metadata
        note.aiMetadata?.let { aiMetadata ->
            AiMetadataCard(
                aiMetadata = aiMetadata,
                isProcessing = false, // TODO: Add processing state
                onReRunAi = onReRunAi,
                onUpdateSummary = onUpdateSummary,
                onUpdateType = onUpdateType,
                onToggleActionItem = onToggleActionItem
            )
        }

        // Note text
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onTextChanged(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note") },
            placeholder = { Text("Write your note here...") },
            minLines = 10,
            maxLines = 20
        )

        // Attachments
        if (note.attachments.isNotEmpty()) {
            Text(
                text = "Attachments (${note.attachments.size})",
                style = MaterialTheme.typography.titleSmall
            )
            note.attachments.forEach { attachment ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${attachment.type.name}: ${attachment.localUri}",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
