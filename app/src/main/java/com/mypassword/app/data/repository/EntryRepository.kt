package com.mypassword.app.data.repository

import com.mypassword.app.data.db.AppDatabase
import com.mypassword.app.data.db.entity.Entry
import kotlinx.coroutines.flow.Flow

class EntryRepository(private val database: AppDatabase) {

    private val dao = database.entryDao()

    fun getAllEntries(): Flow<List<Entry>> = dao.getAllEntries()

    suspend fun getEntryById(id: Long): Entry? = dao.getEntryById(id)

    suspend fun addEntry(entry: Entry): Long = dao.insertEntry(entry)

    suspend fun updateEntry(entry: Entry) {
        dao.updateEntry(entry.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteEntry(id: Long) = dao.deleteEntry(id)

    suspend fun getAllEntriesList(): List<Entry> = dao.getAllEntriesList()
}
