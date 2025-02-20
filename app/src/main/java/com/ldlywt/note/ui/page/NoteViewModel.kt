package com.ldlywt.note.ui.page

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ldlywt.note.bean.Note
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.bean.Tag
import com.ldlywt.note.db.repo.TagNoteRepo
import com.ldlywt.note.state.NoteState
import com.ldlywt.note.ui.page.settings.Level
import com.ldlywt.note.utils.SharedPreferencesUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class SortTime {
    UPDATE_TIME_DESC, UPDATE_TIME_ASC, CREATE_TIME_DESC, CREATE_TIME_ASC
}

@HiltViewModel
class NoteViewModel @Inject constructor(private val tagNoteRepo: TagNoteRepo) : ViewModel() {

    val sortTime = SharedPreferencesUtils.sortTime

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _notes: StateFlow<List<NoteShowBean>> = sortTime.flatMapLatest { newSortTime ->
        tagNoteRepo.queryAllMemosFlow(newSortTime.name)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        emptyList()
    )

    private val _state = MutableStateFlow(NoteState())


    val state = combine(_state, _notes) { state, notes ->
        val sortedNotes = notes.sortedWith(compareByDescending { it.note.isCollected }/*.thenBy { it. }*/)
        val filteredNotes = if (state.searchQuery.isBlank()) {
            sortedNotes
        } else {
            sortedNotes.filter { it.doesMatchSearchQuery(state.searchQuery) }
        }
        getLocalDateMap(notes)
        state.copy(
            notes = filteredNotes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NoteState())

    val tags: StateFlow<List<Tag>> = tagNoteRepo.queryAllTagFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        emptyList()
    )

    var levelMemosMap = mutableStateMapOf<LocalDate, Level>()
        private set

    private suspend fun getLocalDateMap(notes: List<NoteShowBean>) = withContext(Dispatchers.IO) {
        val sortTime = SharedPreferencesUtils.sortTime.first()
        val map: MutableMap<LocalDate, Int> = mutableMapOf()
        notes.forEach {
            val showTime =
                if (sortTime == SortTime.UPDATE_TIME_DESC || sortTime == SortTime.UPDATE_TIME_ASC) it.note.updateTime else it.note.createTime
            val localDate = Instant.ofEpochMilli(showTime).atZone(ZoneId.systemDefault()).toLocalDate()
            map[localDate] = map.getOrElse(localDate) { 0 } + 1
        }
        levelMemosMap.clear()
        levelMemosMap.putAll(convertToLevelMap(map))
    }

    private fun convertToLevelMap(inputMap: Map<LocalDate, Int>): Map<LocalDate, Level> {
        return inputMap.mapValues { (_, value) ->
            when (value) {
                in 0 until 1 -> Level.Zero
                in 1 until 3 -> Level.One
                in 3 until 5 -> Level.Two
                in 5 until 8 -> Level.Three
                else -> Level.Four
            }
        }
    }


    fun getNoteListByTagFlow(tagName: String): Flow<List<NoteShowBean>> = tagNoteRepo.getNoteListWithByTag(tagName)

    fun queryNoteById(noteId: Int): Note = tagNoteRepo.queryNoteById(noteId)

    fun getNoteShowBeanById(noteId: Long): NoteShowBean? = tagNoteRepo.getNoteShowBeanById(noteId)

    fun insertOrUpdate(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            tagNoteRepo.insertOrUpdate(note)
        }
    }


    fun getNotesOnSelectedDate(selectedDate: LocalDate): List<NoteShowBean> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = selectedDate.format(formatter)
        return tagNoteRepo.getNotesOnSelectedDate(formattedDate)
    }

    suspend fun deleteNote(card: Note, tags: List<Tag>) = withContext(Dispatchers.IO) {
        tagNoteRepo.deleteNote(card, tags)
    }

    suspend fun getAllDistinctYears(): List<String> = withContext(Dispatchers.IO) {
        tagNoteRepo.getAllDistinctYears()
    }

    fun getNotesByYear(year: String): Flow<List<NoteShowBean>> = tagNoteRepo.getNotesByYear(year)

    fun getAllLocationInfo(): Flow<List<String>> = tagNoteRepo.getAllLocationInfo()

    fun getNotesByLocationInfo(targetInfo: String): Flow<List<NoteShowBean>> = tagNoteRepo.getNotesByLocationInfo(targetInfo)

}

val LocalMemosViewModel = compositionLocalOf<NoteViewModel> { error("Not Found") }

val LocalMemosState = compositionLocalOf<NoteState> { error("Not Found") }

val LocalTags = compositionLocalOf<List<Tag>> { error("Not Found") }
