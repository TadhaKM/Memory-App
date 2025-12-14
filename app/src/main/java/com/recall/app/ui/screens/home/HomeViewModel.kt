package com.recall.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.core.util.generateUUID
import com.recall.app.domain.model.*
import com.recall.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            noteRepository.getAllNotes(archived = _uiState.value.showArchived)
                .catch { exception ->
                    Timber.e(exception, "Error loading notes")
                    _uiState.update { it.copy(error = exception.message) }
                }
                .collect { notes ->
                    _uiState.update { it.copy(notes = notes, isLoading = false) }
                }
        }
    }

    fun onFilterChanged(filter: NoteFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onToggleArchived() {
        val showArchived = !_uiState.value.showArchived
        _uiState.update { it.copy(showArchived = showArchived, isLoading = true) }
        loadNotes()
    }

    fun onArchiveNote(noteId: String) {
        viewModelScope.launch {
            noteRepository.archiveNote(noteId, true)
        }
    }

    fun onPinNote(noteId: String, pinned: Boolean) {
        viewModelScope.launch {
            noteRepository.pinNote(noteId, pinned)
        }
    }

    fun onDeleteNote(noteId: String) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }

    fun createQuickNote(text: String) {
        viewModelScope.launch {
            val note = Note(
                id = generateUUID(),
                userId = "temp_user", // Will be replaced with actual user ID after auth
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                rawText = text,
                source = NoteSource.MANUAL,
                syncState = SyncState.LOCAL_ONLY,
                aiState = AiState.PENDING
            )
            noteRepository.createNote(note)
        }
    }
}

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val selectedFilter: NoteFilter = NoteFilter.ALL,
    val showArchived: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

enum class NoteFilter {
    ALL,
    TASKS,
    IDEAS,
    REFERENCES,
    AUDIO,
    IMAGES
}
