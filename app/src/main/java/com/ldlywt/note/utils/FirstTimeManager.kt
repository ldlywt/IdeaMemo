package com.ldlywt.note.utils

import com.ldlywt.note.App
import com.ldlywt.note.bean.Note
import com.ldlywt.note.db.repo.TagNoteRepo
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class FirstTimeManager @Inject constructor() {

    @Inject
    lateinit var tagNoteRepo: TagNoteRepo

    fun generateIntroduceNoteList() {
        lunchIo {
            if (!SettingsPreferences.firstLaunch.first()) {
                return@lunchIo
            }
            if (App.instance.isSystemLanguageEnglish()) {
                generateEnglishIntroduceNoteList()
            } else {
                generateChineseIntroduceNoteList()
            }
        }
    }

    private fun generateChineseIntroduceNoteList() {
        val functionNote = Note(
            content = "#灵感 \n生活不止眼前的苟且 还有诗和远方。@深圳市",
        )
        tagNoteRepo.insertOrUpdate(functionNote)
    }

    private fun generateEnglishIntroduceNoteList() {
        val functionNote = Note(
            content = "#Life \nLess is more.@New York City",
        )
        tagNoteRepo.insertOrUpdate(functionNote)
    }
}