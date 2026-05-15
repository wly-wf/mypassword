package com.mypassword.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.data.db.entity.Address
import com.mypassword.app.data.repository.AddressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddressDetailUiState(
    val address: Address? = null,
    val deleted: Boolean = false
)

class AddressDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyPasswordApplication
    private val repository = AddressRepository(app.database)

    private val _uiState = MutableStateFlow(AddressDetailUiState())
    val uiState: StateFlow<AddressDetailUiState> = _uiState.asStateFlow()

    fun loadAddress(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(address = repository.getAddressById(id))
        }
    }

    fun deleteAddress() {
        val a = _uiState.value.address ?: return
        viewModelScope.launch {
            repository.deleteAddress(a.id)
            _uiState.value = _uiState.value.copy(deleted = true)
        }
    }
}
