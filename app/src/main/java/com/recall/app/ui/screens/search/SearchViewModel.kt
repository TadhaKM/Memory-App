package com.recall.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.domain.model.Note
import com.recall.app.domain.model.NoteType
import com.recall.app.domain.usecase.SearchFilters
import com.recall.app.domain.usecase.SearchNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchNotesUseCase: SearchNotesUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Empty)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Debounce search query
        combine(
            _searchQuery.debounce(300),
            _filters
        ) { query, filters ->
            Pair(query, filters)
        }
            .flatMapLatest { (query, filters) ->
                if (query.isBlank()) {
                    flowOf(SearchUiState.Empty)
                } else {
                    searchNotesUseCase(query, filters)
                        .map<List<Note>, SearchUiState> { notes ->
                            if (notes.isEmpty()) {
                                SearchUiState.NoResults(query)
                            } else {
                                SearchUiState.Success(notes, query)
                            }
                        }
                        .catch { exception ->
                            Timber.e(exception, "Search error")
                            emit(SearchUiState.Error(exception.message ?: "Unknown error"))
                        }
                }
            }
            .onEach { state ->
                _uiState.value = state
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filters: SearchFilters) {
        _filters.value = filters
    }

    fun clearFilters() {
        _filters.value = SearchFilters()
    }
}

sealed class SearchUiState {
    object Empty : SearchUiState()
    data class Success(val notes: List<Note>, val query: String) : SearchUiState()
    data class NoResults(val query: String) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
