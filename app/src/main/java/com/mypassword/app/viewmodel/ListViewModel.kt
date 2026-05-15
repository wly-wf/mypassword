package com.mypassword.app.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.data.db.entity.Entry
import com.mypassword.app.data.repository.EntryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

@Immutable
data class ListUiState(
    val searchQuery: String = ""
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
            try {
                repository.getAllEntries()
                    .combine(_uiState) { entries, state ->
                        if (state.searchQuery.isBlank()) {
                            entries
                        } else {
                            val q = state.searchQuery.trim()
                            entries.filter {
                                it.title.contains(q, ignoreCase = true)
                            }
                        }
                    }
                    .collect { filteredEntries ->
                        val collator = Collator.getInstance(Locale.CHINESE)
                        _entries.value = filteredEntries.sortedWith(compareBy(collator) { it.title })
                    }
            } catch (e: Exception) {
                _entries.value = emptyList()
            }
        }
    }

    fun onSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
}
