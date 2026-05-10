package com.mypassword.app.data.repository

import com.mypassword.app.data.db.AppDatabase
import com.mypassword.app.data.db.entity.Entry
import com.mypassword.app.data.db.entity.EntryType
import kotlinx.coroutines.flow.Flow

class EntryRepository(private val database: AppDatabase) {

    private val dao = database.entryDao()

    fun getAllEntries(): Flow<List<Entry>> = dao.getAllEntries()

    fun getEntriesByType(type: EntryType): Flow<List<Entry>> = dao.getEntriesByType(type)

    fun searchEntries(query: String): Flow<List<Entry>> {
        return if (query.isBlank()) {
            dao.getAllEntries()
        } else {
            dao.searchEntries(query.trim())
        }
    }

    suspend fun getEntryById(id: Long): Entry? = dao.getEntryById(id)

    suspend fun addEntry(entry: Entry): Long = dao.insertEntry(entry)

    suspend fun updateEntry(entry: Entry) {
        dao.updateEntry(entry.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteEntry(id: Long) = dao.deleteEntry(id)

    /**
     * 导出用：获取全部条目列表（不通过 Flow）
     */
    suspend fun getAllEntriesList(): List<Entry> = dao.getAllEntriesList()

    /**
     * 导入用：批量插入条目
     */
    suspend fun importEntries(entries: List<Entry>) {
        entries.forEach { dao.insertEntry(it) }
    }
}
