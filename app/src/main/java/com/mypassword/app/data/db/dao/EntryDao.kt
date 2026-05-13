package com.mypassword.app.data.db.dao

import androidx.room.*
import com.mypassword.app.data.db.entity.Entry
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Query("SELECT * FROM entries ORDER BY updated_at DESC")
    fun getAllEntries(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getEntryById(id: Long): Entry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: Entry): Long

    @Update
    suspend fun updateEntry(entry: Entry)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("SELECT * FROM entries ORDER BY updated_at DESC")
    suspend fun getAllEntriesList(): List<Entry>
}
