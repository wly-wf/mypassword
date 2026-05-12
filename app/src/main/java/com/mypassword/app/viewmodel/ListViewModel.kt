package com.mypassword.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.data.db.entity.Entry
import com.mypassword.app.data.db.entity.EntryType
import com.mypassword.app.data.repository.EntryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ListUiState(
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val entries: List<Entry> = emptyList(),
    val showDeleteDialog: Boolean = false,
    val entryToDelete: Entry? = null
)

class ListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyPasswordApplication
    private val repository = EntryRepository(app.database)

    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    init {
        loadAllEntries()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadAllEntries() {
        viewModelScope.launch {
            repository.getAllEntries()
                .combine(_uiState.map { it.searchQuery }.distinctUntilChanged()) { entries, query ->
                    val filtered = if (query.isBlank()) {
                        entries
                    } else {
                        entries.filter {
                            it.target.contains(query, ignoreCase = true) ||
                                it.username.contains(query, ignoreCase = true)
                        }
                    }
                    val tab = _uiState.value.selectedTab
                    val type = if (tab == 0) EntryType.URL else EntryType.APP
                    filtered.filter { it.type == type }
                }
                .collect { filteredEntries ->
                    _entries.value = filteredEntries
                }
        }
    }

    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun onSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun requestDelete(entry: Entry) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            entryToDelete = entry
        )
    }

    fun confirmDelete() {
        val entry = _uiState.value.entryToDelete ?: return
        viewModelScope.launch {
            repository.deleteEntry(entry.id)
        }
        dismissDelete()
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            entryToDelete = null
        )
    }
}
