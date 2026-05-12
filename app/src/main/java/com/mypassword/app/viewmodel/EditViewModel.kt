package com.mypassword.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.data.db.entity.Entry
import com.mypassword.app.data.db.entity.EntryType
import com.mypassword.app.data.repository.EntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditUiState(
    val isNew: Boolean = true,
    val entryId: Long = -1,
    val type: EntryType = EntryType.URL,
    val target: String = "",
    val username: String = "",
    val password: String = "",
    val note: String = "",
    val showPassword: Boolean = false,
    val targetError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val saved: Boolean = false
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
                type = entry.type,
                target = entry.target,
                username = entry.username,
                password = entry.password,
                note = entry.note
            )
        }
    }

    fun onTypeChange(type: EntryType) {
        _uiState.value = _uiState.value.copy(
            type = type,
            targetError = null
        )
    }

    fun onTargetChange(value: String) {
        _uiState.value = _uiState.value.copy(
            target = value,
            targetError = null
        )
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(
            username = value,
            usernameError = null
        )
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            passwordError = null
        )
    }

    fun onNoteChange(value: String) {
        _uiState.value = _uiState.value.copy(note = value)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            showPassword = !_uiState.value.showPassword
        )
    }

    fun onPasswordGenerated(pw: String) {
        _uiState.value = _uiState.value.copy(password = pw)
    }

    fun save(): Boolean {
        val state = _uiState.value
        var hasError = false

        // 验证 target
        if (state.target.isBlank()) {
            val label = if (state.type == EntryType.URL) "网址" else "App 名称"
            _uiState.value = _uiState.value.copy(targetError = "请输入$label")
            hasError = true
        }

        if (state.type == EntryType.URL && state.target.isNotBlank()) {
            val url = state.target.trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                _uiState.value = _uiState.value.copy(
                    targetError = "网址需要以 http:// 或 https:// 开头"
                )
                hasError = true
            }
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

        viewModelScope.launch {
            if (state.isNew) {
                repository.addEntry(
                    Entry(
                        type = state.type,
                        target = state.target.trim(),
                        username = state.username.trim(),
                        password = state.password,
                        note = state.note.trim()
                    )
                )
            } else {
                repository.updateEntry(
                    Entry(
                        id = state.entryId,
                        type = state.type,
                        target = state.target.trim(),
                        username = state.username.trim(),
                        password = state.password,
                        note = state.note.trim()
                    )
                )
            }
            _uiState.value = _uiState.value.copy(saved = true)
        }
        return true
    }
}
