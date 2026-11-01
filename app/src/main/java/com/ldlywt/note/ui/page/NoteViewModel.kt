package com.ldlywt.note.ui.page

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ldlywt.note.bean.Attachment
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

    var dateFirstImageMap = mutableStateMapOf<LocalDate, String?>()
        private set

    private suspend fun getLocalDateMap(notes: List<NoteShowBean>) = withContext(Dispatchers.IO) {
        val sortTime = SharedPreferencesUtils.sortTime.first()
        val countMap: MutableMap<LocalDate, Int> = mutableMapOf()
        val imageMap: MutableMap<LocalDate, String?> = mutableMapOf()
        
        notes.forEach { noteShow ->
            val showTime = if (sortTime == SortTime.UPDATE_TIME_DESC || sortTime == SortTime.UPDATE_TIME_ASC) 
                noteShow.note.updateTime else noteShow.note.createTime
            val localDate = Instant.ofEpochMilli(showTime).atZone(ZoneId.systemDefault()).toLocalDate()
            
            countMap[localDate] = countMap.getOrElse(localDate) { 0 } + 1
            
            // 如果当天还没记录图片，且当前笔记有图片，记录第一张
            if (!imageMap.containsKey(localDate)) {
                val firstImage = noteShow.note.attachments.firstOrNull { it.path.isNotBlank() }
                if (firstImage != null) {
                    imageMap[localDate] = firstImage.path
                }
            }
        }
        
        withContext(Dispatchers.Main) {
            levelMemosMap.clear()
            levelMemosMap.putAll(convertToLevelMap(countMap))
            dateFirstImageMap.clear()
            dateFirstImageMap.putAll(imageMap)
        }
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

    fun getNoteShowBeanByIdFlow(noteId: Long): Flow<NoteShowBean?> = tagNoteRepo.getNoteShowBeanByIdFlow(noteId)

    fun insertOrUpdate(note: Note, removedAttachments: List<Attachment> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            tagNoteRepo.insertOrUpdate(note)
            // 笔记保存成功后再清理被移除的附件文件
            tagNoteRepo.deleteUnreferencedAttachmentFiles(removedAttachments)
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
    fun getNotesByCreateTimeRange(startTime: Long, endTime: Long): Flow<List<NoteShowBean>> = tagNoteRepo.getNotesByCreateTimeRange(startTime, endTime)

    fun getAllLocationInfo(): Flow<List<String>> = tagNoteRepo.getAllLocationInfo()

    fun getNotesByLocationInfo(targetInfo: String): Flow<List<NoteShowBean>> = tagNoteRepo.getNotesByLocationInfo(targetInfo)

    fun clearLocationInfo(locationInfo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tagNoteRepo.clearLocationInfo(locationInfo)
        }
    }

    fun getCommentsByParentId(parentNoteId: Long): Flow<List<NoteShowBean>> = tagNoteRepo.getCommentsByParentId(parentNoteId)

}

val LocalMemosViewModel = compositionLocalOf<NoteViewModel> { error("Not Found") }

val LocalMemosState = compositionLocalOf<NoteState> { error("Not Found") }

val LocalTags = compositionLocalOf<List<Tag>> { error("Not Found") }
