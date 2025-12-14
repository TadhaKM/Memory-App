package com.recall.app.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.recall.app.domain.model.NoteType
import com.recall.app.domain.usecase.SearchFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToNoteDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.padding(16.dp)
            )

            // Filter chips
            FilterChipsRow(
                filters = filters,
                onFilterChange = { viewModel.updateFilter(it) },
                onClearFilters = { viewModel.clearFilters() }
            )

            Divider()

            // Results
            when (val state = uiState) {
                is SearchUiState.Empty -> {
                    EmptySearchState()
                }

                is SearchUiState.Success -> {
                    SearchResults(
                        notes = state.notes,
                        query = state.query,
                        onNoteClick = onNavigateToNoteDetail
                    )
                }

                is SearchUiState.NoResults -> {
                    NoResultsState(query = state.query)
                }

                is SearchUiState.Error -> {
                    ErrorState(message = state.message)
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search notes...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true
    )
}

@Composable
fun FilterChipsRow(
    filters: SearchFilters,
    onFilterChange: (SearchFilters) -> Unit,
    onClearFilters: () -> Unit
) {
    var showFilterDialog by remember { mutableStateOf(false) }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = false,
                onClick = { showFilterDialog = true },
                label = { Text("Filters") },
                leadingIcon = {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filters",
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        if (filters.type != null) {
            item {
                FilterChip(
                    selected = true,
                    onClick = { onFilterChange(filters.copy(type = null)) },
                    label = { Text(filters.type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove filter",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        if (filters.showArchived) {
            item {
                FilterChip(
                    selected = true,
                    onClick = { onFilterChange(filters.copy(showArchived = false)) },
                    label = { Text("Archived") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove filter",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        if (filters.type != null || filters.showArchived) {
            item {
                TextButton(onClick = onClearFilters) {
                    Text("Clear all")
                }
            }
        }
    }

    // Filter dialog
    if (showFilterDialog) {
        FilterDialog(
            currentFilters = filters,
            onDismiss = { showFilterDialog = false },
            onApply = { newFilters ->
                onFilterChange(newFilters)
                showFilterDialog = false
            }
        )
    }
}

@Composable
fun FilterDialog(
    currentFilters: SearchFilters,
    onDismiss: () -> Unit,
    onApply: (SearchFilters) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentFilters.type) }
    var showArchived by remember { mutableStateOf(currentFilters.showArchived) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filters") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Note Type:", style = MaterialTheme.typography.labelLarge)

                Column {
                    NoteType.values().forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedType = if (selectedType == type) null else type }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = if (selectedType == type) null else type }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showArchived,
                        onCheckedChange = { showArchived = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Show archived notes")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        SearchFilters(
                            type = selectedType,
                            showArchived = showArchived
                        )
                    )
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SearchResults(
    notes: List<Note>,
    query: String,
    onNoteClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "${notes.size} result${if (notes.size != 1) "s" else ""} for \"$query\"",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                SearchResultCard(
                    note = note,
                    onClick = { onNoteClick(note.id) }
                )
            }
        }
    }
}

@Composable
fun SearchResultCard(
    note: Note,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Summary or raw text
            Text(
                text = note.aiMetadata?.summary ?: note.rawText ?: "Empty note",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

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
                    AssistChip(
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
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Topics
            if (note.aiMetadata?.topics?.isNotEmpty() == true) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    note.aiMetadata.topics.take(3).forEach { topic ->
                        SuggestionChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = topic,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Search your notes",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Type to find notes by content, topics, or entities",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoResultsState(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No results for \"$query\"",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Try different keywords or check your filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Search error",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
