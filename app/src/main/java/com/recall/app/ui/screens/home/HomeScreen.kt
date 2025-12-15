package com.recall.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
fun HomeScreen(
    onNavigateToNoteDetail: (String) -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToDailyRecall: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recall") },
                actions = {
                    IconButton(onClick = onNavigateToDailyRecall) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Daily Recall")
                    }
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { viewModel.onToggleArchived() }) {
                        Icon(Icons.Default.Archive, contentDescription = "Archive")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCapture) {
                Icon(Icons.Default.Add, contentDescription = "Add note")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            FilterChipsRow(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.onFilterChanged(it) }
            )

            // Notes list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.notes.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNavigateToNoteDetail(note.id) },
                            onPin = { viewModel.onPinNote(note.id, !note.pinned) },
                            onArchive = { viewModel.onArchiveNote(note.id) },
                            onDelete = { viewModel.onDeleteNote(note.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: NoteFilter,
    onFilterSelected: (NoteFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedFilter == NoteFilter.ALL,
                onClick = { onFilterSelected(NoteFilter.ALL) },
                label = { Text("All") }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == NoteFilter.TASKS,
                onClick = { onFilterSelected(NoteFilter.TASKS) },
                label = { Text("Tasks") }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == NoteFilter.IDEAS,
                onClick = { onFilterSelected(NoteFilter.IDEAS) },
                label = { Text("Ideas") }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == NoteFilter.REFERENCES,
                onClick = { onFilterSelected(NoteFilter.REFERENCES) },
                label = { Text("References") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Summary or raw text
                    Text(
                        text = note.aiMetadata?.summary ?: note.rawText ?: "Empty note",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Metadata row
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
                    }
                }

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
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            onClick = {
                                onArchive()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No notes yet",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Tap + to capture your first note",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
