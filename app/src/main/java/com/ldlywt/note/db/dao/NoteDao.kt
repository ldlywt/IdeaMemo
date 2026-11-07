package com.ldlywt.note.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ldlywt.note.bean.Note
import com.ldlywt.note.bean.NoteShowBean
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<Note>)

    @Delete
    fun delete(note: Note)

    @Update
    fun update(note: Note)

    @Transaction
    @Query("SELECT * FROM Note WHERE is_deleted = 0 order by update_time desc")
    fun queryAll(): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM Note WHERE is_deleted = 0 order by update_time desc")
    fun queryAllData(): List<Note>

    @Query("select * from Note where note_id =:id AND is_deleted = 0")
    fun queryById(id: Int): Note

    @Query("select count(*) from Note WHERE is_deleted = 0")
    fun getCount(): Int

    @Query("delete from Note")
    fun deleteAll()
    @Transaction
    @Query("SELECT DISTINCT location_info FROM Note WHERE is_deleted = 0 AND location_info IS NOT NULL AND location_info != ''")
    fun getAllLocationInfo(): Flow<List<String>>
    @Transaction
    @Query("SELECT * FROM Note WHERE is_deleted = 0 AND location_info = :targetInfo")
    fun getNotesByLocationInfo(targetInfo: String): Flow<List<NoteShowBean>>
    @Transaction
    @Query("SELECT * FROM Note WHERE is_deleted = 0 AND strftime('%Y-%m', create_time/1000) = :yearMonth")
    fun queryAllNotesByYearMonth(yearMonth: String): Flow<List<Note>>

    @Query("UPDATE Note SET location_info = NULL WHERE location_info = :locationInfo")
    fun clearLocationInfo(locationInfo: String)

}

