package com.mypassword.app.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.data.db.entity.Entry
import com.mypassword.app.data.repository.EntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class EditUiState(
    val isNew: Boolean = true,
    val entryId: Long = -1,
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val note: String = "",
    val createdAt: Long = 0,
    val showPassword: Boolean = false,
    val titleError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val saveError: String? = null,
    val saved: Boolean = false,
    val isWorking: Boolean = false
)

class EditViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyPasswordApplication
    private val repository = EntryRepository(app.database)

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    fun loadEntry(entryId: Long) {
        if (entryId <= 0) return

        viewModelScope.launch {
            val entry = repository.getEntryById(entryId) ?: return@launch
            _uiState.value = _uiState.value.copy(
                isNew = false,
                entryId = entry.id,
                title = entry.title,
                username = entry.username,
                password = entry.password,
                url = entry.url,
                note = entry.note,
                createdAt = entry.createdAt
            )
        }
    }

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value, titleError = null, saveError = null)
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, usernameError = null, saveError = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null, saveError = null)
    }

    fun onUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(url = value, saveError = null)
    }

    fun onNoteChange(value: String) {
        _uiState.value = _uiState.value.copy(note = value, saveError = null)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(showPassword = !_uiState.value.showPassword)
    }

    fun onPasswordGenerated(pw: String) {
        _uiState.value = _uiState.value.copy(password = pw)
    }

    fun save(): Boolean {
        val state = _uiState.value
        if (state.isWorking) return false

        var hasError = false

        if (state.title.isBlank()) {
            _uiState.value = _uiState.value.copy(titleError = "请输入标题")
            hasError = true
        }

        if (state.username.isBlank()) {
            _uiState.value = _uiState.value.copy(usernameError = "请输入账号")
            hasError = true
        }

        if (state.password.isBlank()) {
            _uiState.value = _uiState.value.copy(passwordError = "请输入密码")
            hasError = true
        }

        if (hasError) return false

        _uiState.value = _uiState.value.copy(isWorking = true, titleError = null, usernameError = null, passwordError = null)

        viewModelScope.launch {
            try {
                if (state.isNew) {
                    repository.addEntry(
                        Entry(
                            title = state.title.trim(),
                            username = state.username.trim(),
                            password = state.password,
                            url = state.url.trim(),
                            note = state.note.trim()
                        )
                    )
                } else {
                    repository.updateEntry(
                        Entry(
                            id = state.entryId,
                            title = state.title.trim(),
                            username = state.username.trim(),
                            password = state.password,
                            url = state.url.trim(),
                            note = state.note.trim(),
                            createdAt = state.createdAt
                        )
                    )
                }
                _uiState.value = _uiState.value.copy(saved = true, isWorking = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    saveError = "保存失败：${e.message}"
                )
            }
        }
        return true
    }
}
