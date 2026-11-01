package com.ldlywt.note.db.repo

import androidx.room.Query
import androidx.room.Transaction
import com.ldlywt.note.bean.Attachment
import com.ldlywt.note.bean.Note
import com.ldlywt.note.bean.NoteShowBean
import com.ldlywt.note.bean.NoteTagCrossRef
import com.ldlywt.note.bean.Tag
import com.ldlywt.note.db.dao.NoteDao
import com.ldlywt.note.db.dao.NoteTagCrossRefDao
import com.ldlywt.note.db.dao.TagDao
import com.ldlywt.note.db.dao.TagNoteDao
import com.ldlywt.note.utils.CityRegexUtils
import com.ldlywt.note.utils.TopicUtils
import com.ldlywt.note.utils.deleteAttachmentFile
import com.ldlywt.note.ui.page.SortTime
import kotlinx.coroutines.flow.Flow
import org.zeroturnaround.zip.FileSource.pair

class TagNoteRepo(
    private val noteDao: NoteDao,
    private val tagNoteDao: TagNoteDao,
    private val tagDao: TagDao,
    private val noteTagCrossRefDao: NoteTagCrossRefDao
) {

    fun queryAllNoteList(): List<Note> {
        return noteDao.queryAllData()
    }

    fun queryAllTagList(): List<Tag> {
        return tagDao.queryAllTagList().filterNot { it.tag.isBlank() }
    }

    fun queryAllTagFlow(): Flow<List<Tag>> = tagDao.queryAll()

    fun updateTag(tag: Tag) {
        tagDao.update(tag)
    }

    fun queryAllMemosFlow(sortTime: String): Flow<List<NoteShowBean>> {
        return when (sortTime) {
            SortTime.UPDATE_TIME_DESC.name -> {
                tagNoteDao.getAll("update_time", "desc")
            }

            SortTime.UPDATE_TIME_ASC.name -> {
                tagNoteDao.getAll("update_time", "asc")
            }

            SortTime.CREATE_TIME_DESC.name -> {
                tagNoteDao.getAll("create_time", "desc")
            }

            SortTime.CREATE_TIME_ASC.name -> {
                tagNoteDao.getAll("create_time", "asc")
            }

            else -> {
                tagNoteDao.getAll("update_time", "desc")
            }
        }
    }

    fun getAllRandom(): Flow<List<NoteShowBean>> = tagNoteDao.getAllRandom()

    fun queryAllNoteShowBeanList(): List<NoteShowBean> = tagNoteDao.getAllNoteWithTagList()

    fun countNoteListWithByTag(tagName: String): Int {
        return tagNoteDao.countNoteListWithByTag(tagName)
    }

    fun getNoteListWithByTag(tagName: String): Flow<List<NoteShowBean>> {
        return tagNoteDao.getNoteListWithByTag(tagName)
    }

    fun getTagListByNoteId(noteId: Long): Flow<List<Tag>> {
        return tagNoteDao.getTagListByNoteId(noteId)
    }

    @Query("")
    @Transaction
    fun insertOrUpdate(card: Note) {
        val tagList = TopicUtils.getTopicListByString(card.content)
//        if (card.locationInfo.isNullOrBlank()) {
//            val pair = CityRegexUtils.getCityByString(card.content.trim())
//            card.locationInfo = pair?.second
//            if (card.locationInfo != null) {
//                card.content = pair?.first ?: ""
//            }
//        }
        val noteId = noteDao.insert(card)
        if (tagList.isEmpty()) {
            val tempTag = Tag(tag = "")
            tagDao.insertOrUpdate(tempTag)
            noteTagCrossRefDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = noteId, tag = tempTag.tag))
            return
        }
        tagList.forEach {
            tagDao.insertOrUpdate(it)
            noteTagCrossRefDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = noteId, tag = it.tag))
        }
    }

    @Query("")
    @Transaction
    fun deleteNote(card: Note, tags: List<Tag>) {
        noteDao.delete(card)
        tags.forEach {
            tagDao.deleteOrUpdate(it)
            noteTagCrossRefDao.deleteCrossRef(NoteTagCrossRef(noteId = card.noteId, tag = it.tag))
        }
        deleteUnreferencedAttachmentFiles(card.attachments)
    }

    /**
     * 删除不再被任何笔记引用的附件文件，避免留下孤儿文件。
     */
    fun deleteUnreferencedAttachmentFiles(attachments: Collection<Attachment>) {
        if (attachments.isEmpty()) return
        val referencedPaths = noteDao.queryAllData()
            .asSequence()
            .flatMap { it.attachments.asSequence() }
            .map { it.path }
            .toSet()
        attachments
            .filter { it.path.isNotBlank() && it.path !in referencedPaths }
            .forEach { deleteAttachmentFile(it.path) }
    }

    fun queryNoteById(noteId: Int): Note = noteDao.queryById(noteId)

    fun getNoteShowBeanById(noteId: Long): NoteShowBean? = tagNoteDao.getNoteShowBeanById(noteId)

    fun getNoteShowBeanByIdFlow(noteId: Long): Flow<NoteShowBean?> = tagNoteDao.getNoteShowBeanByIdFlow(noteId)

    fun getNotesOnSelectedDate(selectedDate: String): List<NoteShowBean> = tagNoteDao.getNoteShowOnDate(selectedDate)

    suspend fun getAllDistinctYears(): List<String> {
        return tagNoteDao.getAllDistinctYears()
    }

    fun getNotesByYear(year: String): Flow<List<NoteShowBean>> = tagNoteDao.getNotesByYear(year)

    fun getNotesByCreateTimeRange(startTime: Long, endTime: Long): Flow<List<NoteShowBean>> = tagNoteDao.getNotesByCreateTimeRange(startTime, endTime)


    fun getAllLocationInfo(): Flow<List<String>> = noteDao.getAllLocationInfo()

    fun getNotesByLocationInfo(targetInfo: String): Flow<List<NoteShowBean>> = noteDao.getNotesByLocationInfo(targetInfo)

    fun clearLocationInfo(locationInfo: String) {
        noteDao.clearLocationInfo(locationInfo)
    }

    fun getCommentsByParentId(parentNoteId: Long): Flow<List<NoteShowBean>> = tagNoteDao.getCommentsByParentId(parentNoteId)

}