package com.recall.app.ui.screens.notedetail

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.core.export.NoteExporter
import com.recall.app.domain.model.Note
import com.recall.app.domain.model.SyncState
import com.recall.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val noteExporter: NoteExporter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String = checkNotNull(savedStateHandle["noteId"])

    private val _uiState = MutableStateFlow<NoteDetailUiState>(NoteDetailUiState.Loading)
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            noteRepository.getNoteById(noteId)
                .catch { exception ->
                    Timber.e(exception, "Error loading note")
                    _uiState.value = NoteDetailUiState.Error(exception.message ?: "Unknown error")
                }
                .collect { note ->
                    if (note != null) {
                        _uiState.value = NoteDetailUiState.Success(note)
                    } else {
                        _uiState.value = NoteDetailUiState.Error("Note not found")
                    }
                }
        }
    }

    fun updateNoteText(text: String) {
        val currentState = _uiState.value
        if (currentState is NoteDetailUiState.Success) {
            viewModelScope.launch {
                val updatedNote = currentState.note.copy(
                    rawText = text,
                    updatedAt = System.currentTimeMillis(),
                    syncState = SyncState.DIRTY // Mark as needing sync
                )
                noteRepository.updateNote(updatedNote)
            }
        }
    }

    fun togglePin() {
        val currentState = _uiState.value
        if (currentState is NoteDetailUiState.Success) {
            viewModelScope.launch {
                noteRepository.pinNote(noteId, !currentState.note.pinned)
            }
        }
    }

    fun archiveNote() {
        viewModelScope.launch {
            noteRepository.archiveNote(noteId, true)
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }

    fun exportNote(format: NoteExporter.ExportFormat): Intent? {
        val currentState = _uiState.value
        return if (currentState is NoteDetailUiState.Success) {
            noteExporter.exportNote(currentState.note, format)
        } else {
            null
        }
    }

    fun getNoteAsText(): String? {
        val currentState = _uiState.value
        return if (currentState is NoteDetailUiState.Success) {
            noteExporter.getNoteAsText(currentState.note)
        } else {
            null
        }
    }
}

sealed class NoteDetailUiState {
    object Loading : NoteDetailUiState()
    data class Success(val note: Note) : NoteDetailUiState()
    data class Error(val message: String) : NoteDetailUiState()
}
