package com.recall.app.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.core.util.generateUUID
import com.recall.app.domain.model.*
import com.recall.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    private var currentNoteId: String? = null

    init {
        // Auto-save with 600ms debounce
        viewModelScope.launch {
            _textInput
                .debounce(600)
                .distinctUntilChanged()
                .collect { text ->
                    if (text.isNotBlank()) {
                        saveOrUpdateNote(text)
                    }
                }
        }
    }

    fun updateText(text: String) {
        _textInput.value = text
    }

    private suspend fun saveOrUpdateNote(text: String) {
        try {
            val noteId = currentNoteId ?: generateUUID().also { currentNoteId = it }

            val note = Note(
                id = noteId,
                userId = "temp_user", // Will be replaced with actual user ID
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                rawText = text,
                source = NoteSource.MANUAL,
                syncState = SyncState.LOCAL_ONLY,
                aiState = AiState.PENDING
            )

            noteRepository.createNote(note)
            _uiState.update { it.copy(lastSaved = System.currentTimeMillis()) }
            Timber.d("Auto-saved note: $noteId")
        } catch (e: Exception) {
            Timber.e(e, "Error saving note")
            _uiState.update { it.copy(error = e.message) }
        }
    }

    fun addAttachment(attachment: Attachment) {
        viewModelScope.launch {
            try {
                // Ensure we have a note first
                val noteId = currentNoteId ?: generateUUID().also {
                    currentNoteId = it
                    val note = Note(
                        id = it,
                        userId = "temp_user",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        rawText = _textInput.value.ifBlank { null },
                        source = when (attachment.type) {
                            AttachmentType.AUDIO -> NoteSource.VOICE
                            AttachmentType.IMAGE -> NoteSource.CAMERA
                        },
                        syncState = SyncState.LOCAL_ONLY,
                        aiState = AiState.PENDING
                    )
                    noteRepository.createNote(note)
                }

                val attachmentWithNoteId = attachment.copy(noteId = noteId)
                noteRepository.addAttachment(attachmentWithNoteId)

                _uiState.update { state ->
                    state.copy(
                        attachments = state.attachments + attachmentWithNoteId,
                        lastSaved = System.currentTimeMillis()
                    )
                }

                Timber.d("Added attachment: ${attachment.id} to note: $noteId")
            } catch (e: Exception) {
                Timber.e(e, "Error adding attachment")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun startRecording() {
        _uiState.update { it.copy(isRecording = true) }
    }

    fun stopRecording() {
        _uiState.update { it.copy(isRecording = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun reset() {
        currentNoteId = null
        _textInput.value = ""
        _uiState.value = CaptureUiState()
    }
}

data class CaptureUiState(
    val attachments: List<Attachment> = emptyList(),
    val isRecording: Boolean = false,
    val lastSaved: Long? = null,
    val error: String? = null
)
