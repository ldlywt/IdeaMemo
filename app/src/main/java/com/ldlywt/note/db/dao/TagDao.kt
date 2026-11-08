package com.ldlywt.note.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ldlywt.note.bean.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Transaction
    fun insertOrUpdate(tag: Tag) {
        val oldTag = getByName(tag.tag)
        if (oldTag == null) {
            tag.count = 1
            insert(tag)
        } else {
            tag.count = ++oldTag.count
            update(tag)
        }
    }

    @Transaction
    fun deleteOrUpdate(tag: Tag) {
        tag.count = --tag.count
        if (tag.count <= 0) {
            delete(tag)
        } else {
            update(tag)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tag: Tag): Long

    @Delete
    fun delete(note: Tag)

    @Update
    fun update(note: Tag)

    @Transaction
    @Query("SELECT * FROM Tag WHERE tag IS NOT NULL AND tag != '' AND is_city_tag IS 0 order by update_time desc")
    fun queryAll(): Flow<List<Tag>>

    @Transaction
    @Query("SELECT * FROM Tag order by update_time desc")
    fun queryAllTagList(): List<Tag>

    @Query("SELECT * FROM Tag WHERE tag =:name LIMIT 1")
    fun getByName(name: String): Tag?

    @Query("select count(*) from Tag")
    fun getCount(): Int

    @Query("delete from Tag")
    fun deleteAll()
}

