package com.recall.app.ui.screens.dailyrecall

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.recall.app.core.util.toDateString
import com.recall.app.domain.model.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyRecallScreen(
    onNavigateToNoteDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: DailyRecallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Recall") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDailyRecall() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DailyRecallUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is DailyRecallUiState.Success -> {
                    DailyRecallContent(
                        notes = state.notes,
                        onNoteClick = onNavigateToNoteDetail,
                        onDismiss = { viewModel.dismissNote(it) },
                        onNeverResurface = { viewModel.neverResurface(it) },
                        onPin = { noteId, pinned -> viewModel.pinNote(noteId, pinned) }
                    )
                }

                is DailyRecallUiState.Empty -> {
                    EmptyDailyRecall(modifier = Modifier.align(Alignment.Center))
                }

                is DailyRecallUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun DailyRecallContent(
    notes: List<Note>,
    onNoteClick: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onNeverResurface: (String) -> Unit,
    onPin: (String, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Today's Recalls",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${notes.size} note${if (notes.size != 1) "s" else ""} selected for you",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Notes list
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                DailyRecallCard(
                    note = note,
                    onClick = { onNoteClick(note.id) },
                    onDismiss = { onDismiss(note.id) },
                    onNeverResurface = { onNeverResurface(note.id) },
                    onPin = { onPin(note.id, !note.pinned) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyRecallCard(
    note: Note,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onNeverResurface: () -> Unit,
    onPin: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (note.pinned) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Summary
                Text(
                    text = note.aiMetadata?.summary ?: note.rawText ?: "Empty note",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (note.pinned) "Unpin" else "Pin") },
                            onClick = {
                                onPin()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (note.pinned) Icons.Default.PushPin else Icons.Default.PushPin,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Dismiss") },
                            onClick = {
                                onDismiss()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Never show again") },
                            onClick = {
                                onNeverResurface()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Block, contentDescription = null)
                            }
                        )
                    }
                }
            }

            // Metadata
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.createdAt.toDateString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                note.aiMetadata?.type?.let { type ->
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }

                if (note.attachments.isNotEmpty()) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Has attachments",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Topics
            if (note.aiMetadata?.topics?.isNotEmpty() == true) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    note.aiMetadata.topics.take(3).forEach { topic ->
                        AssistChip(
                            onClick = { },
                            label = { Text(topic, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Action items preview
            if (note.aiMetadata?.actionItems?.isNotEmpty() == true) {
                val unfinishedCount = note.aiMetadata.actionItems.count { !it.done }
                if (unfinishedCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$unfinishedCount task${if (unfinishedCount != 1) "s" else ""} remaining",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDailyRecall(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "All caught up!",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "No notes to recall right now. Come back tomorrow for fresh insights!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = "Error loading recalls",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
