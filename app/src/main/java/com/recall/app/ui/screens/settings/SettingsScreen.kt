package com.recall.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.recall.app.data.preferences.DarkModeOption
import com.recall.app.data.preferences.ExportFormatOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.showClearDataSuccess) {
        if (uiState.showClearDataSuccess) {
            snackbarHostState.showSnackbar("Preferences cleared successfully")
            viewModel.dismissClearDataSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Appearance Section
                SettingsSectionHeader(title = "Appearance")
                DarkModeSelector(
                    currentOption = uiState.preferences.darkMode,
                    onOptionSelected = { viewModel.updateDarkMode(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Sync Section
                SettingsSectionHeader(title = "Sync")
                SwitchSettingItem(
                    title = "Auto Sync",
                    subtitle = "Automatically sync notes to cloud",
                    checked = uiState.preferences.autoSync,
                    onCheckedChange = { viewModel.updateAutoSync(it) },
                    icon = Icons.Default.Sync
                )
                SwitchSettingItem(
                    title = "Sync on Wi-Fi Only",
                    subtitle = "Only sync when connected to Wi-Fi",
                    checked = uiState.preferences.syncOnWifiOnly,
                    onCheckedChange = { viewModel.updateSyncOnWifiOnly(it) },
                    icon = Icons.Default.Wifi,
                    enabled = uiState.preferences.autoSync
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // AI Section
                SettingsSectionHeader(title = "AI Processing")
                SwitchSettingItem(
                    title = "AI Processing",
                    subtitle = "Enable automatic AI analysis of notes",
                    checked = uiState.preferences.enableAiProcessing,
                    onCheckedChange = { viewModel.updateEnableAiProcessing(it) },
                    icon = Icons.Default.AutoAwesome
                )
                SwitchSettingItem(
                    title = "Audio Transcription",
                    subtitle = "Convert voice notes to text",
                    checked = uiState.preferences.enableTranscription,
                    onCheckedChange = { viewModel.updateEnableTranscription(it) },
                    icon = Icons.Default.RecordVoiceOver,
                    enabled = uiState.preferences.enableAiProcessing
                )
                SwitchSettingItem(
                    title = "OCR (Text Recognition)",
                    subtitle = "Extract text from images",
                    checked = uiState.preferences.enableOcr,
                    onCheckedChange = { viewModel.updateEnableOcr(it) },
                    icon = Icons.Default.DocumentScanner,
                    enabled = uiState.preferences.enableAiProcessing
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Daily Recall Section
                SettingsSectionHeader(title = "Daily Recall")
                SwitchSettingItem(
                    title = "Enable Daily Recall",
                    subtitle = "Resurface important notes daily",
                    checked = uiState.preferences.enableDailyRecall,
                    onCheckedChange = { viewModel.updateEnableDailyRecall(it) },
                    icon = Icons.Default.Lightbulb
                )
                DailyRecallCountSelector(
                    currentCount = uiState.preferences.dailyRecallCount,
                    onCountSelected = { viewModel.updateDailyRecallCount(it) },
                    enabled = uiState.preferences.enableDailyRecall
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Notifications Section
                SettingsSectionHeader(title = "Notifications")
                SwitchSettingItem(
                    title = "Enable Notifications",
                    subtitle = "Receive Daily Recall reminders",
                    checked = uiState.preferences.enableNotifications,
                    onCheckedChange = { viewModel.updateEnableNotifications(it) },
                    icon = Icons.Default.Notifications
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Export Section
                SettingsSectionHeader(title = "Export")
                ExportFormatSelector(
                    currentFormat = uiState.preferences.defaultExportFormat,
                    onFormatSelected = { viewModel.updateDefaultExportFormat(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Privacy Section
                SettingsSectionHeader(title = "Privacy")
                SwitchSettingItem(
                    title = "Analytics",
                    subtitle = "Help improve the app with usage data",
                    checked = uiState.preferences.enableAnalytics,
                    onCheckedChange = { viewModel.updateEnableAnalytics(it) },
                    icon = Icons.Default.Analytics
                )
                SwitchSettingItem(
                    title = "Crash Reporting",
                    subtitle = "Send crash reports to help fix bugs",
                    checked = uiState.preferences.enableCrashReporting,
                    onCheckedChange = { viewModel.updateEnableCrashReporting(it) },
                    icon = Icons.Default.BugReport
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Danger Zone
                SettingsSectionHeader(title = "Data")
                var showClearDialog by remember { mutableStateOf(false) }
                ClickableSettingItem(
                    title = "Clear Preferences",
                    subtitle = "Reset all settings to defaults",
                    icon = Icons.Default.DeleteForever,
                    onClick = { showClearDialog = true },
                    isDestructive = true
                )

                if (showClearDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearDialog = false },
                        title = { Text("Clear Preferences?") },
                        text = { Text("This will reset all settings to their defaults. Your notes will not be affected.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.clearAllData()
                                    showClearDialog = false
                                }
                            ) {
                                Text("Clear", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // About Section
                SettingsSectionHeader(title = "About")
                InfoSettingItem(
                    title = "Version",
                    value = "${uiState.appVersion} (${uiState.buildNumber})"
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun ClickableSettingItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InfoSettingItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DarkModeSelector(
    currentOption: DarkModeOption,
    onOptionSelected: (DarkModeOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DarkMode,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = when (currentOption) {
                    DarkModeOption.LIGHT -> "Light"
                    DarkModeOption.DARK -> "Dark"
                    DarkModeOption.SYSTEM -> "System default"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DarkModeOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (option) {
                                    DarkModeOption.LIGHT -> "Light"
                                    DarkModeOption.DARK -> "Dark"
                                    DarkModeOption.SYSTEM -> "System default"
                                }
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                        leadingIcon = {
                            if (option == currentOption) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DailyRecallCountSelector(
    currentCount: Int,
    onCountSelected: (Int) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Numbers,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Notes per day",
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = "$currentCount notes",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }

        Box {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                (1..10).forEach { count ->
                    DropdownMenuItem(
                        text = { Text("$count notes") },
                        onClick = {
                            onCountSelected(count)
                            expanded = false
                        },
                        leadingIcon = {
                            if (count == currentCount) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExportFormatSelector(
    currentFormat: ExportFormatOption,
    onFormatSelected: (ExportFormatOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.FileDownload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Default export format",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = when (currentFormat) {
                    ExportFormatOption.TXT -> "Plain Text (.txt)"
                    ExportFormatOption.MARKDOWN -> "Markdown (.md)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ExportFormatOption.values().forEach { format ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (format) {
                                    ExportFormatOption.TXT -> "Plain Text (.txt)"
                                    ExportFormatOption.MARKDOWN -> "Markdown (.md)"
                                }
                            )
                        },
                        onClick = {
                            onFormatSelected(format)
                            expanded = false
                        },
                        leadingIcon = {
                            if (format == currentFormat) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    }
}
