package com.ldlywt.note.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ldlywt.note.bean.NoteTagCrossRef

@Dao
interface NoteTagCrossRefDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertNoteTagCrossRef(entity: NoteTagCrossRef)

    @Delete
    fun deleteCrossRef(entity: NoteTagCrossRef)

    @Query("SELECT tag FROM NoteTagCrossRef WHERE note_id = :noteId")
    fun queryTagNamesByNoteId(noteId: Long): List<String>
}