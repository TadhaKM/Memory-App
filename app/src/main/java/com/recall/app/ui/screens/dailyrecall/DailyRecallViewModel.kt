package com.recall.app.ui.screens.dailyrecall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.core.util.Constants
import com.recall.app.domain.model.Note
import com.recall.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DailyRecallViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DailyRecallUiState>(DailyRecallUiState.Loading)
    val uiState: StateFlow<DailyRecallUiState> = _uiState.asStateFlow()

    init {
        loadDailyRecall()
    }

    fun loadDailyRecall() {
        viewModelScope.launch {
            _uiState.value = DailyRecallUiState.Loading

            try {
                val notes = noteRepository.getTopResurfaceNotes(Constants.DEFAULT_DAILY_RECALL_COUNT)

                if (notes.isEmpty()) {
                    _uiState.value = DailyRecallUiState.Empty
                } else {
                    _uiState.value = DailyRecallUiState.Success(notes)

                    // Mark notes as shown
                    notes.forEach { note ->
                        noteRepository.markResurfaceShown(note.id)
                    }
                }

                Timber.d("Loaded ${notes.size} notes for daily recall")
            } catch (e: Exception) {
                Timber.e(e, "Error loading daily recall")
                _uiState.value = DailyRecallUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun dismissNote(noteId: String) {
        val currentState = _uiState.value
        if (currentState is DailyRecallUiState.Success) {
            val updatedNotes = currentState.notes.filter { it.id != noteId }
            _uiState.value = if (updatedNotes.isEmpty()) {
                DailyRecallUiState.Empty
            } else {
                DailyRecallUiState.Success(updatedNotes)
            }
        }
    }

    fun neverResurface(noteId: String) {
        viewModelScope.launch {
            noteRepository.updateNeverResurface(noteId, true)
            dismissNote(noteId)
            Timber.d("Marked note $noteId as never resurface")
        }
    }

    fun pinNote(noteId: String, pinned: Boolean) {
        viewModelScope.launch {
            noteRepository.pinNote(noteId, pinned)
            Timber.d("${if (pinned) "Pinned" else "Unpinned"} note $noteId")
        }
    }
}

sealed class DailyRecallUiState {
    object Loading : DailyRecallUiState()
    data class Success(val notes: List<Note>) : DailyRecallUiState()
    object Empty : DailyRecallUiState()
    data class Error(val message: String) : DailyRecallUiState()
}
