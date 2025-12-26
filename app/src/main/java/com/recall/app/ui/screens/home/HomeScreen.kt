package com.recall.app.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.recall.app.core.util.toDateString
import com.recall.app.domain.model.Note
import com.recall.app.ui.theme.*
import java.time.LocalTime

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Gradient Header
            GradientHeader(
                notesCount = uiState.notes.size,
                onSearchClick = onNavigateToSearch,
                onSettingsClick = onNavigateToSettings
            )

            // Stats Cards
            StatsCardsRow(
                totalNotes = uiState.notes.size,
                pinnedNotes = uiState.notes.count { it.pinned },
                tasksCount = uiState.notes.count { it.aiMetadata?.type?.name == "TASK" },
                ideasCount = uiState.notes.count { it.aiMetadata?.type?.name == "IDEA" }
            )

            // Filter chips
            ModernFilterChipsRow(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.onFilterChanged(it) }
            )

            // Notes list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Primary,
                        strokeWidth = 3.dp
                    )
                }
            } else if (uiState.notes.isEmpty()) {
                ModernEmptyState(
                    modifier = Modifier.weight(1f),
                    onAddNote = onNavigateToCapture
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.notes, key = { it.id }) { note ->
                        ModernNoteCard(
                            note = note,
                            onClick = { onNavigateToNoteDetail(note.id) },
                            onPin = { viewModel.onPinNote(note.id, !note.pinned) },
                            onArchive = { viewModel.onArchiveNote(note.id) },
                            onDelete = { viewModel.onDeleteNote(note.id) }
                        )
                    }

                    // Bottom spacing for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Bottom Navigation
        ModernBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            onHomeClick = { /* Already on home */ },
            onSearchClick = onNavigateToSearch,
            onAddClick = onNavigateToCapture,
            onRecallClick = onNavigateToDailyRecall,
            onSettingsClick = onNavigateToSettings
        )
    }
}

@Composable
fun GradientHeader(
    notesCount: Int,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Primary, PrimaryDark)
                )
            )
            .padding(top = 48.dp, bottom = 24.dp)
            .padding(horizontal = 20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$notesCount notes captured",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCardsRow(
    totalNotes: Int,
    pinnedNotes: Int,
    tasksCount: Int,
    ideasCount: Int
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatCard(
                title = "Total",
                value = totalNotes.toString(),
                icon = Icons.Outlined.Notes,
                backgroundColor = CardPurple
            )
        }
        item {
            StatCard(
                title = "Pinned",
                value = pinnedNotes.toString(),
                icon = Icons.Outlined.PushPin,
                backgroundColor = CardOrange
            )
        }
        item {
            StatCard(
                title = "Tasks",
                value = tasksCount.toString(),
                icon = Icons.Outlined.CheckCircle,
                backgroundColor = CardTeal
            )
        }
        item {
            StatCard(
                title = "Ideas",
                value = ideasCount.toString(),
                icon = Icons.Outlined.Lightbulb,
                backgroundColor = CardBlue
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}

@Composable
fun ModernFilterChipsRow(
    selectedFilter: NoteFilter,
    onFilterSelected: (NoteFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ModernFilterChip(
                selected = selectedFilter == NoteFilter.ALL,
                onClick = { onFilterSelected(NoteFilter.ALL) },
                label = "All Notes"
            )
        }
        item {
            ModernFilterChip(
                selected = selectedFilter == NoteFilter.TASKS,
                onClick = { onFilterSelected(NoteFilter.TASKS) },
                label = "Tasks"
            )
        }
        item {
            ModernFilterChip(
                selected = selectedFilter == NoteFilter.IDEAS,
                onClick = { onFilterSelected(NoteFilter.IDEAS) },
                label = "Ideas"
            )
        }
        item {
            ModernFilterChip(
                selected = selectedFilter == NoteFilter.REFERENCES,
                onClick = { onFilterSelected(NoteFilter.REFERENCES) },
                label = "References"
            )
        }
    }
}

@Composable
fun ModernFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    val backgroundColor by animateColorAsState(
        if (selected) Primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipText"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernNoteCard(
    note: Note,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val noteType = note.aiMetadata?.type?.name ?: "NOTE"
    val accentColor = when (noteType) {
        "TASK" -> CardTeal
        "IDEA" -> CardOrange
        "REFERENCE" -> CardBlue
        "JOURNAL" -> CardPink
        "QUESTION" -> CardPurple
        else -> Primary
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = accentColor.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(IntrinsicSize.Max)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.6f))
                        ),
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Type badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = accentColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = noteType.lowercase().replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor
                                    )
                                )
                            }

                            if (note.pinned) {
                                Icon(
                                    Icons.Filled.PushPin,
                                    contentDescription = "Pinned",
                                    modifier = Modifier.size(16.dp),
                                    tint = CardOrange
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Note content
                        Text(
                            text = note.aiMetadata?.summary ?: note.rawText ?: "Empty note",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                lineHeight = 24.sp
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Date
                        Text(
                            text = note.createdAt.toDateString(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Menu
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (note.pinned) "Unpin" else "Pin") },
                                leadingIcon = {
                                    Icon(
                                        if (note.pinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    onPin()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Archive, contentDescription = null)
                                },
                                onClick = {
                                    onArchive()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = ErrorRed
                                    )
                                },
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
}

@Composable
fun ModernEmptyState(
    modifier: Modifier = Modifier,
    onAddNote: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Decorative circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Primary.copy(alpha = 0.2f),
                                Primary.copy(alpha = 0.05f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.NoteAdd,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Primary
                )
            }

            Text(
                text = "No notes yet",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "Capture your first thought, idea, or task",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddNote,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Create Note",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
fun ModernBottomNavigation(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit,
    onRecallClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    icon = Icons.Filled.Home,
                    label = "Home",
                    selected = true,
                    onClick = onHomeClick
                )
                NavItem(
                    icon = Icons.Outlined.Search,
                    label = "Search",
                    selected = false,
                    onClick = onSearchClick
                )

                // Center FAB
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = Primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add note",
                        modifier = Modifier.size(28.dp)
                    )
                }

                NavItem(
                    icon = Icons.Outlined.Lightbulb,
                    label = "Recall",
                    selected = false,
                    onClick = onRecallClick
                )
                NavItem(
                    icon = Icons.Outlined.Settings,
                    label = "Settings",
                    selected = false,
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        if (selected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navIconColor"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = iconColor,
                fontSize = 10.sp
            )
        )
    }
}

// Keep for compatibility
@Composable
fun FilterChipsRow(
    selectedFilter: NoteFilter,
    onFilterSelected: (NoteFilter) -> Unit
) {
    ModernFilterChipsRow(selectedFilter, onFilterSelected)
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
    ModernNoteCard(note, onClick, onPin, onArchive, onDelete)
}

@Composable
fun EmptyState() {
    ModernEmptyState(onAddNote = {})
}
