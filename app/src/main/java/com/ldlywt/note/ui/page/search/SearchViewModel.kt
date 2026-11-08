package com.ldlywt.note.ui.page.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.db.repo.TagNoteRepo
import com.ldlywt.note.utils.SharedPreferencesUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val tagNoteRepo: TagNoteRepo) : ViewModel() {

    private val _query: MutableStateFlow<String> = MutableStateFlow(value = "")
    val query: StateFlow<String>
        get() = _query

    /** Saved search keywords, newest first. */
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>>
        get() = _searchHistory

    lateinit var notes: List<NoteShowBean>

    init {

        viewModelScope.launch(Dispatchers.IO) {
            notes = tagNoteRepo.queryAllNoteShowBeanList()
        }
        viewModelScope.launch {
            SharedPreferencesUtils.searchHistory.collect { history ->
                _searchHistory.value = history
            }
        }
    }

    fun clearSearchQuery() {
        _query.value = ""
        dataFlow.value = emptyList()
    }

    fun onQuery(query: String) {
        _query.value = query
    }

    val dataFlow: MutableStateFlow<List<NoteShowBean>> = MutableStateFlow(value = emptyList())

    private fun getSearchResults(
        searchKey: String,
        notes: List<NoteShowBean>,
    ): List<NoteShowBean> = notes.filter { note ->
        fun String.matches(): Boolean = contains(searchKey.trim(), true)
        note.note.content.matches() || note.note.attachments.any { it.description.matches() } || note.tagList.any { it.tag.matches() }
    }

    fun onSearch(str: String) {
        dataFlow.value = getSearchResults(str, notes)
        if (str.isNotBlank()) {
            recordSearchHistory(str)
        }
    }

    /** Saves a search keyword, newest first, de-duplicated and capped. */
    fun recordSearchHistory(keyword: String) {
        viewModelScope.launch {
            val updated = (listOf(keyword.trim()) + _searchHistory.value)
                .distinct()
                .take(HISTORY_MAX)
            _searchHistory.value = updated
            SharedPreferencesUtils.updateSearchHistory(updated)
        }
    }

    fun removeSearchHistory(keyword: String) {
        viewModelScope.launch {
            val updated = _searchHistory.value.filterNot { it == keyword }
            _searchHistory.value = updated
            SharedPreferencesUtils.updateSearchHistory(updated)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            _searchHistory.value = emptyList()
            SharedPreferencesUtils.updateSearchHistory(emptyList())
        }
    }

    companion object {
        private const val HISTORY_MAX = 20
    }
}
