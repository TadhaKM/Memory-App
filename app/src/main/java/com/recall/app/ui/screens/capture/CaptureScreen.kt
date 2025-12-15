package com.recall.app.ui.screens.capture

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.recall.app.core.media.AudioRecorder
import com.recall.app.core.media.ImageManager
import com.recall.app.core.ocr.OcrProcessor
import com.recall.app.core.ocr.OcrResult
import com.recall.app.core.util.generateUUID
import com.recall.app.domain.model.Attachment
import com.recall.app.domain.model.AttachmentType
import com.recall.app.domain.model.NoteSource
import com.recall.app.domain.model.SyncState
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    onNavigateBack: () -> Unit,
    initialText: String? = null,
    initialImageUris: List<Uri> = emptyList(),
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val textInput by viewModel.textInput.collectAsState()

    val audioRecorder = remember { AudioRecorder(context) }
    val imageManager = remember { ImageManager(context) }
    val ocrProcessor = remember { OcrProcessor() }

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showOcrDialog by remember { mutableStateOf(false) }
    var ocrText by remember { mutableStateOf("") }
    var hasProcessedInitialContent by remember { mutableStateOf(false) }

    // Handle initial shared content
    LaunchedEffect(initialText, initialImageUris) {
        if (!hasProcessedInitialContent) {
            hasProcessedInitialContent = true

            // Handle shared text
            if (!initialText.isNullOrBlank()) {
                viewModel.updateText(initialText)
                viewModel.setSource(NoteSource.SHARE)
            }

            // Handle shared images
            if (initialImageUris.isNotEmpty()) {
                viewModel.setSource(NoteSource.SHARE)
                initialImageUris.forEach { uri ->
                    try {
                        val file = imageManager.saveImage(uri)
                        if (file != null) {
                            val attachment = Attachment(
                                id = generateUUID(),
                                noteId = "",
                                type = AttachmentType.IMAGE,
                                mimeType = "image/jpeg",
                                localUri = file.absolutePath,
                                sizeBytes = file.length(),
                                syncState = SyncState.LOCAL_ONLY
                            )
                            viewModel.addAttachment(attachment)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to process shared image: $uri")
                    }
                }
            }
        }
    }

    // Permissions
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    )

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturedImageUri != null) {
            scope.launch {
                val uri = capturedImageUri!!
                val file = imageManager.saveImage(uri)

                if (file != null) {
                    // Run OCR
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    when (val result = ocrProcessor.processImage(bitmap)) {
                        is OcrResult.Success -> {
                            ocrText = result.text
                            if (result.text.isNotBlank()) {
                                showOcrDialog = true
                            }
                        }
                        is OcrResult.Error -> {
                            Timber.e("OCR failed: ${result.message}")
                        }
                    }

                    // Add attachment
                    val attachment = Attachment(
                        id = generateUUID(),
                        noteId = "", // Will be set by ViewModel
                        type = AttachmentType.IMAGE,
                        mimeType = "image/jpeg",
                        localUri = file.absolutePath,
                        sizeBytes = file.length(),
                        syncState = SyncState.LOCAL_ONLY
                    )
                    viewModel.addAttachment(attachment)
                }
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val file = imageManager.saveImage(it)
                if (file != null) {
                    // Run OCR
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    when (val result = ocrProcessor.processImage(bitmap)) {
                        is OcrResult.Success -> {
                            ocrText = result.text
                            if (result.text.isNotBlank()) {
                                showOcrDialog = true
                            }
                        }
                        is OcrResult.Error -> {
                            Timber.e("OCR failed: ${result.message}")
                        }
                    }

                    val attachment = Attachment(
                        id = generateUUID(),
                        noteId = "",
                        type = AttachmentType.IMAGE,
                        mimeType = "image/jpeg",
                        localUri = file.absolutePath,
                        sizeBytes = file.length(),
                        syncState = SyncState.LOCAL_ONLY
                    )
                    viewModel.addAttachment(attachment)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorder.release()
            ocrProcessor.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture Note") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.reset()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    if (uiState.lastSaved != null) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Saved",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Text input
            OutlinedTextField(
                value = textInput,
                onValueChange = { viewModel.updateText(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What's on your mind?") },
                placeholder = { Text("Start typing...") },
                minLines = 5,
                maxLines = 15
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Audio button
                Button(
                    onClick = {
                        if (permissionsState.permissions[0].status.isGranted) {
                            if (uiState.isRecording) {
                                val file = audioRecorder.stopRecording()
                                viewModel.stopRecording()

                                file?.let {
                                    val attachment = Attachment(
                                        id = generateUUID(),
                                        noteId = "",
                                        type = AttachmentType.AUDIO,
                                        mimeType = "audio/mp4",
                                        localUri = it.absolutePath,
                                        durationMs = null, // Could be calculated
                                        sizeBytes = it.length(),
                                        syncState = SyncState.LOCAL_ONLY
                                    )
                                    viewModel.addAttachment(attachment)
                                }
                            } else {
                                audioRecorder.startRecording(generateUUID())
                                viewModel.startRecording()
                            }
                        } else {
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (uiState.isRecording) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Icon(
                        if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (uiState.isRecording) "Stop recording" else "Record audio"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isRecording) "Stop" else "Audio")
                }

                // Camera button
                OutlinedButton(
                    onClick = {
                        if (permissionsState.permissions[1].status.isGranted) {
                            capturedImageUri = imageManager.createImageUri()
                            cameraLauncher.launch(capturedImageUri!!)
                        } else {
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Take photo")
                    Spacer(Modifier.width(8.dp))
                    Text("Camera")
                }

                // Gallery button
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Choose image")
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery")
                }
            }

            // Attachments list
            if (uiState.attachments.isNotEmpty()) {
                Text(
                    text = "Attachments (${uiState.attachments.size})",
                    style = MaterialTheme.typography.titleSmall
                )

                uiState.attachments.forEach { attachment ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (attachment.type) {
                                    AttachmentType.AUDIO -> Icons.Default.AudioFile
                                    AttachmentType.IMAGE -> Icons.Default.Image
                                },
                                contentDescription = attachment.type.name
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = attachment.type.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${attachment.sizeBytes / 1024} KB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Error message
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    // OCR result dialog
    if (showOcrDialog) {
        AlertDialog(
            onDismissRequest = { showOcrDialog = false },
            title = { Text("Text detected in image") },
            text = {
                Column {
                    Text("Would you like to add this text to your note?")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ocrText,
                        onValueChange = { ocrText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentText = textInput
                        val newText = if (currentText.isBlank()) {
                            ocrText
                        } else {
                            "$currentText\n\n$ocrText"
                        }
                        viewModel.updateText(newText)
                        showOcrDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOcrDialog = false }) {
                    Text("Skip")
                }
            }
        )
    }
}
