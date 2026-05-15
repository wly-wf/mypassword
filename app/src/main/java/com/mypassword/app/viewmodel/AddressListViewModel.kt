package com.mypassword.app.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.data.db.entity.Address
import com.mypassword.app.data.repository.AddressRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

@Immutable
data class AddressListUiState(val searchQuery: String = "")

class AddressListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyPasswordApplication
    private val repository = AddressRepository(app.database)

    private val _uiState = MutableStateFlow(AddressListUiState())
    val uiState: StateFlow<AddressListUiState> = _uiState.asStateFlow()

    private val _addresses = MutableStateFlow<List<Address>>(emptyList())
    val addresses: StateFlow<List<Address>> = _addresses.asStateFlow()

    init { loadAllAddresses() }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadAllAddresses() {
        viewModelScope.launch {
            try {
                repository.getAllAddresses()
                    .combine(_uiState) { addresses, state ->
                        if (state.searchQuery.isBlank()) addresses
                        else addresses.filter {
                            it.title.contains(state.searchQuery.trim(), ignoreCase = true)
                        }
                    }
                    .collect {
                        val collator = Collator.getInstance(Locale.CHINESE)
                        _addresses.value = it.sortedWith(compareBy(collator) { a -> a.title })
                    }
            } catch (_: Exception) {
                _addresses.value = emptyList()
            }
        }
    }

    fun onSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
}
