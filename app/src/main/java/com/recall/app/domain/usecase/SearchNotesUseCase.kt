package com.recall.app.domain.usecase

import com.recall.app.domain.model.Note
import com.recall.app.domain.model.NoteType
import com.recall.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    operator fun invoke(
        query: String,
        filters: SearchFilters = SearchFilters()
    ): Flow<List<Note>> {
        return if (query.isBlank()) {
            noteRepository.getAllNotes(archived = filters.showArchived)
        } else {
            when {
                filters.type != null -> {
                    noteRepository.searchNotesByType(
                        query = query,
                        type = filters.type,
                        archived = filters.showArchived
                    )
                }
                else -> {
                    noteRepository.searchNotes(
                        query = query,
                        archived = filters.showArchived
                    )
                }
            }
        }
    }
}

data class SearchFilters(
    val type: NoteType? = null,
    val hasAttachments: Boolean = false,
    val showArchived: Boolean = false,
    val dateRange: DateRange? = null
)

data class DateRange(
    val start: Long,
    val end: Long
)
