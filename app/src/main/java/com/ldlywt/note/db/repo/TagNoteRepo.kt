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
        // REPLACE 插入会让 Room 删除旧笔记行并级联删除旧标签关联，先记录旧标签用于修正计数
        val oldTagNames = if (card.noteId == 0L) emptyList() else noteTagCrossRefDao.queryTagNamesByNoteId(card.noteId)
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
        } else {
            tagList.forEach {
                tagDao.insertOrUpdate(it)
                noteTagCrossRefDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = noteId, tag = it.tag))
            }
        }
        recountAndCleanupTags(oldTagNames + tagList.map { it.tag } + (if (tagList.isEmpty()) listOf("") else emptyList()))
    }

    /**
     * Inserts a note together with an explicit tag list, merging tags detected in
     * the content. Used by imports so that tags stored outside the content
     * (for example the HTML backup's `data-tags` attribute) survive a round trip.
     */
    @Query("")
    @Transaction
    fun insertWithExplicitTags(card: Note, explicitTags: List<Tag>) {
        // REPLACE 插入会级联删除旧标签关联，先记录旧标签用于修正计数
        val oldTagNames = if (card.noteId == 0L) emptyList() else noteTagCrossRefDao.queryTagNamesByNoteId(card.noteId)
        val noteId = noteDao.insert(card)
        val allTags = (TopicUtils.getTopicListByString(card.content) + explicitTags)
            .filter { it.tag.isNotBlank() }
            .distinctBy { it.tag }
        if (allTags.isEmpty()) {
            val tempTag = Tag(tag = "")
            tagDao.insertOrUpdate(tempTag)
            noteTagCrossRefDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = noteId, tag = tempTag.tag))
        } else {
            allTags.forEach {
                tagDao.insertOrUpdate(it)
                noteTagCrossRefDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = noteId, tag = it.tag))
            }
        }
        recountAndCleanupTags(oldTagNames + allTags.map { it.tag } + (if (allTags.isEmpty()) listOf("") else emptyList()))
    }

    fun queryDeletedMemosFlow(): Flow<List<NoteShowBean>> = tagNoteDao.getDeletedNotes()

    /**
     * Moves a note to the recycle bin without dropping its tag relations, so that
     * the note can be restored afterwards. If a tag's live note count drops to 0
     * the tag row is deleted; a later restore rebuilds it from the note content.
     */
    @Query("")
    @Transaction
    fun deleteNote(card: Note, tags: List<Tag>) {
        card.isDeleted = true
        card.updateTime = System.currentTimeMillis()
        noteDao.update(card)
        recountAndCleanupTags(tags.map { it.tag })
    }

    /**
     * Restores a note from the recycle bin and rebuilds any tag rows (and cross
     * references) that were deleted while the note was in the trash.
     */
    @Query("")
    @Transaction
    fun restoreNote(card: Note) {
        card.isDeleted = false
        card.updateTime = System.currentTimeMillis()
        noteDao.update(card)
        val tagNames = (noteTagCrossRefDao.queryTagNamesByNoteId(card.noteId) +
                TopicUtils.getTopicListByString(card.content).map { it.tag })
            .filter { it.isNotBlank() }
            .distinct()
        // 先补齐被级联删除的标签行，否则关联记录会因外键约束插入失败
        tagNames.forEach { tagName ->
            if (tagDao.getByName(tagName) == null) {
                tagDao.insert(Tag(tag = tagName))
            }
        }
        tagNames.forEach { tagName ->
            noteTagCrossRefDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = card.noteId, tag = tagName))
        }
        recountAndCleanupTags(tagNames)
    }

    @Query("")
    @Transaction
    fun deleteNotePermanently(card: Note, tags: List<Tag>) {
        noteDao.delete(card)
        tags.forEach {
            noteTagCrossRefDao.deleteCrossRef(NoteTagCrossRef(noteId = card.noteId, tag = it.tag))
        }
        // 笔记被彻底删除后，若某个标签不再被任何有效笔记引用则直接删除该标签
        recountAndCleanupTags(tags.map { it.tag })
        deleteUnreferencedAttachmentFiles(card.attachments)
    }

    /**
     * 把一批标签的冗余计数同步为实际引用它们的有效（未删除）笔记数，
     * 计数为 0 的标签直接删除（关联记录随 Tag 外键级联清除）。
     */
    private fun recountAndCleanupTags(tagNames: Collection<String>) {
        tagNames.distinct().forEach { tagName ->
            val oldTag = tagDao.getByName(tagName) ?: return@forEach
            val liveCount = tagNoteDao.countNoteListWithByTag(tagName)
            if (liveCount <= 0) {
                tagDao.delete(oldTag)
            } else if (oldTag.count != liveCount) {
                oldTag.count = liveCount
                tagDao.update(oldTag)
            }
        }
    }

    /**
     * 重建全库标签计数，并把计数为 0 的标签删掉，用于手动修复数据。
     */
    fun recountAllTagsAndCleanup() {
        queryAllTagList().forEach { tag ->
            recountAndCleanupTags(listOf(tag.tag))
        }
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

    @Query("")
    @Transaction
    fun emptyRecycleBin(cards: List<NoteShowBean>) {
        cards.forEach { bean ->
            deleteNotePermanently(bean.note, bean.tagList)
        }
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