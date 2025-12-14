package com.recall.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.recall.app.domain.model.AiMetadata
import com.recall.app.domain.model.ActionItem
import com.recall.app.domain.model.NoteType

@Composable
fun AiMetadataCard(
    aiMetadata: AiMetadata,
    isProcessing: Boolean = false,
    onReRunAi: () -> Unit,
    onUpdateSummary: (String) -> Unit,
    onUpdateType: (NoteType) -> Unit,
    onToggleActionItem: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedSummary by remember(aiMetadata.summary) {
        mutableStateOf(aiMetadata.summary ?: "")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }

                    IconButton(onClick = onReRunAi, enabled = !isProcessing) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Re-run AI",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save" else "Edit",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Divider()

            // Summary
            if (isEditing) {
                OutlinedTextField(
                    value = editedSummary,
                    onValueChange = { editedSummary = it },
                    label = { Text("Summary") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Button(
                    onClick = {
                        onUpdateSummary(editedSummary)
                        isEditing = false
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Summary")
                }
            } else {
                aiMetadata.summary?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Type
            aiMetadata.type?.let { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Type:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    SuggestionChip(
                        onClick = { /* TODO: Type picker dialog */ },
                        label = {
                            Text(
                                text = type.name.lowercase()
                                    .replaceFirstChar { it.uppercase() }
                            )
                        },
                        icon = {
                            Icon(
                                when (type) {
                                    NoteType.TASK -> Icons.Default.CheckCircle
                                    NoteType.IDEA -> Icons.Default.Lightbulb
                                    NoteType.REFERENCE -> Icons.Default.Book
                                    NoteType.JOURNAL -> Icons.Default.MenuBook
                                    NoteType.QUESTION -> Icons.Default.Help
                                    NoteType.QUOTE -> Icons.Default.FormatQuote
                                    NoteType.OTHER -> Icons.Default.Article
                                },
                                contentDescription = type.name,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            // Topics
            if (aiMetadata.topics.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Topics:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        aiMetadata.topics.take(5).forEach { topic ->
                            AssistChip(
                                onClick = { },
                                label = { Text(topic) }
                            )
                        }
                    }
                }
            }

            // Entities
            if (aiMetadata.entities.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Entities:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = aiMetadata.entities.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Action Items
            if (aiMetadata.actionItems.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Action Items:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    aiMetadata.actionItems.forEachIndexed { index, item ->
                        ActionItemRow(
                            actionItem = item,
                            onToggle = { done -> onToggleActionItem(index, done) }
                        )
                    }
                }
            }

            // Transcript (if exists)
            aiMetadata.transcript?.let { transcript ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Transcript:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = transcript,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 3
                    )
                }
            }
        }
    }
}

@Composable
fun ActionItemRow(
    actionItem: ActionItem,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = actionItem.done,
            onCheckedChange = onToggle
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = actionItem.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            actionItem.dueHint?.let { hint ->
                Text(
                    text = "Due: $hint",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
