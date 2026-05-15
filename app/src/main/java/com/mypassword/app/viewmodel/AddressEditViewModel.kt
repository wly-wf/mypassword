package com.mypassword.app.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.data.db.entity.Address
import com.mypassword.app.data.repository.AddressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val ADDRESS_TYPES = listOf("外卖", "快递", "家", "学校", "公司", "其他")

@Immutable
data class AddressEditUiState(
    val isNew: Boolean = true,
    val addressId: Long = -1,
    val title: String = "",
    val type: String = "外卖",
    val address: String = "",
    val name: String = "",
    val phone: String = "",
    val note: String = "",
    val createdAt: Long = 0,
    val origTitle: String = "", val origType: String = "外卖",
    val origAddress: String = "", val origName: String = "",
    val origPhone: String = "", val origNote: String = "",
    val titleError: String? = null,
    val addressError: String? = null,
    val nameError: String? = null,
    val phoneError: String? = null,
    val saveError: String? = null,
    val saved: Boolean = false,
    val isWorking: Boolean = false
) {
    val isDirty: Boolean get() = isNew || title != origTitle || type != origType ||
        address != origAddress || name != origName || phone != origPhone || note != origNote
}

class AddressEditViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyPasswordApplication
    private val repository = AddressRepository(app.database)

    private val _uiState = MutableStateFlow(AddressEditUiState())
    val uiState: StateFlow<AddressEditUiState> = _uiState.asStateFlow()

    fun loadAddress(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            val a = repository.getAddressById(id) ?: return@launch
            _uiState.value = _uiState.value.copy(
                isNew = false, addressId = a.id,
                title = a.title, type = a.type, address = a.address,
                name = a.name, phone = a.phone, note = a.note,
                createdAt = a.createdAt,
                origTitle = a.title, origType = a.type, origAddress = a.address,
                origName = a.name, origPhone = a.phone, origNote = a.note
            )
        }
    }

    fun onTitleChange(v: String) { _uiState.value = _uiState.value.copy(title = v, titleError = null, saveError = null) }
    fun onTypeChange(v: String) { _uiState.value = _uiState.value.copy(type = v, saveError = null) }
    fun onAddressChange(v: String) { _uiState.value = _uiState.value.copy(address = v, addressError = null, saveError = null) }
    fun onNameChange(v: String) { _uiState.value = _uiState.value.copy(name = v, nameError = null, saveError = null) }
    fun onPhoneChange(v: String) { _uiState.value = _uiState.value.copy(phone = v, phoneError = null, saveError = null) }
    fun onNoteChange(v: String) { _uiState.value = _uiState.value.copy(note = v, saveError = null) }

    fun save(): Boolean {
        val s = _uiState.value
        if (s.isWorking) return false
        var err = false
        if (s.title.isBlank()) { _uiState.value = _uiState.value.copy(titleError = "请输入标题"); err = true }
        if (s.address.isBlank()) { _uiState.value = _uiState.value.copy(addressError = "请输入地址"); err = true }
        if (s.name.isBlank()) { _uiState.value = _uiState.value.copy(nameError = "请输入姓名"); err = true }
        if (s.phone.isBlank()) { _uiState.value = _uiState.value.copy(phoneError = "请输入电话"); err = true }
        if (err) return false

        _uiState.value = _uiState.value.copy(isWorking = true, titleError = null, addressError = null, nameError = null, phoneError = null)
        viewModelScope.launch {
            try {
                if (s.isNew) {
                    repository.addAddress(Address(title = s.title.trim(), type = s.type, address = s.address.trim(), name = s.name.trim(), phone = s.phone.trim(), note = s.note.trim()))
                } else {
                    repository.updateAddress(Address(id = s.addressId, title = s.title.trim(), type = s.type, address = s.address.trim(), name = s.name.trim(), phone = s.phone.trim(), note = s.note.trim(), createdAt = s.createdAt))
                }
                _uiState.value = _uiState.value.copy(saved = true, isWorking = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isWorking = false, saveError = "保存失败：${e.message}")
            }
        }
        return true
    }
}
