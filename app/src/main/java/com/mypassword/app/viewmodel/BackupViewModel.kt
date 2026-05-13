package com.mypassword.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.data.crypto.BackupData
import com.mypassword.app.data.crypto.BackupEncryption
import com.mypassword.app.data.crypto.BackupEntry
import com.mypassword.app.data.db.entity.Entry
import com.mypassword.app.data.db.entity.EntryType
import com.mypassword.app.data.repository.EntryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.compose.runtime.Immutable
enum class BackupOperation {
    NONE,
    EXPORTING,
    IMPORTING
}

@androidx.compose.runtime.Immutable
data class BackupUiState(
    val exportPassword: String = "",
    val confirmExportPassword: String = "",
    val importPassword: String = "",
    val operation: BackupOperation = BackupOperation.NONE,
    val message: String? = null,
    val isError: Boolean = false,
    val showMergeDialog: Boolean = false,
    val pendingImportEntries: List<BackupEntry> = emptyList(),
    val pendingImportUri: Uri? = null
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyPasswordApplication
    private val repository = EntryRepository(app.database)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    // === 导出 ===

    fun onExportPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(exportPassword = value, message = null)
    }

    fun onConfirmExportPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmExportPassword = value, message = null)
    }

    fun exportToUri(uri: Uri) {
        val state = _uiState.value
        val pw = state.exportPassword
        val confirm = state.confirmExportPassword

        if (pw.length < 4) {
            _uiState.value = state.copy(message = "导出密码至少 4 位", isError = true)
            return
        }
        if (pw != confirm) {
            _uiState.value = state.copy(message = "两次密码不一致", isError = true)
            return
        }

        _uiState.value = state.copy(operation = BackupOperation.EXPORTING, message = null)

        viewModelScope.launch {
            try {
                val entries = withContext(Dispatchers.IO) {
                    repository.getAllEntriesList().map { entry ->
                        BackupEntry(
                            type = entry.type.name,
                            target = entry.target,
                            username = entry.username,
                            password = entry.password,
                            note = entry.note,
                            createdAt = entry.createdAt,
                            updatedAt = entry.updatedAt
                        )
                    }
                }

                val backupData = BackupData(entries = entries)
                val jsonStr = backupData.toJson()
                val encrypted = BackupEncryption.encryptForExport(
                    jsonStr.toByteArray(Charsets.UTF_8),
                    pw
                )

                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(encrypted)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    operation = BackupOperation.NONE,
                    message = "导出成功，共 ${entries.size} 条记录",
                    isError = false,
                    exportPassword = "",
                    confirmExportPassword = ""
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    operation = BackupOperation.NONE,
                    message = "导出失败：${e.message}",
                    isError = true
                )
            }
        }
    }

    // === 导入 ===

    fun onImportPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(importPassword = value, message = null)
    }

    fun importFromUri(uri: Uri) {
        val state = _uiState.value
        val pw = state.importPassword

        if (pw.isBlank()) {
            _uiState.value = state.copy(message = "请输入导出时设置的密码", isError = true)
            return
        }

        _uiState.value = state.copy(operation = BackupOperation.IMPORTING, message = null)

        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes()
                    } ?: throw Exception("无法读取文件")
                }

                val decrypted = BackupEncryption.decryptFromImport(data, pw)
                    .getOrElse { throw Exception("密码错误或文件已损坏") }

                val jsonStr = String(decrypted, Charsets.UTF_8)
                val backupData = BackupData.fromJson(jsonStr)

                if (backupData.entries.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        operation = BackupOperation.NONE,
                        message = "备份文件中没有数据",
                        isError = true
                    )
                    return@launch
                }

                // 显示合并策略选择
                _uiState.value = _uiState.value.copy(
                    operation = BackupOperation.NONE,
                    showMergeDialog = true,
                    pendingImportEntries = backupData.entries,
                    pendingImportUri = uri,
                    importPassword = ""
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    operation = BackupOperation.NONE,
                    message = "导入失败：${e.message}",
                    isError = true
                )
            }
        }
    }

    fun mergeAppend() {
        val entries = _uiState.value.pendingImportEntries
        if (entries.isEmpty()) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    entries.forEach { backupEntry ->
                        repository.addEntry(
                            Entry(
                                type = EntryType.valueOf(backupEntry.type),
                                target = backupEntry.target,
                                username = backupEntry.username,
                                password = backupEntry.password,
                                note = backupEntry.note,
                                createdAt = backupEntry.createdAt,
                                updatedAt = backupEntry.updatedAt
                            )
                        )
                    }
                }
                _uiState.value = _uiState.value.copy(
                    showMergeDialog = false,
                    pendingImportEntries = emptyList(),
                    message = "导入成功，追加 ${entries.size} 条记录",
                    isError = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    showMergeDialog = false,
                    message = "导入失败：${e.message}",
                    isError = true
                )
            }
        }
    }

    fun mergeReplace() {
        val entries = _uiState.value.pendingImportEntries
        if (entries.isEmpty()) return

        viewModelScope.launch {
            try {
                // 数据库不支持直接清空表，逐条删除再导入
                val existingEntries = repository.getAllEntriesList()
                withContext(Dispatchers.IO) {
                    existingEntries.forEach { repository.deleteEntry(it.id) }
                    entries.forEach { backupEntry ->
                        repository.addEntry(
                            Entry(
                                type = EntryType.valueOf(backupEntry.type),
                                target = backupEntry.target,
                                username = backupEntry.username,
                                password = backupEntry.password,
                                note = backupEntry.note,
                                createdAt = backupEntry.createdAt,
                                updatedAt = backupEntry.updatedAt
                            )
                        )
                    }
                }
                _uiState.value = _uiState.value.copy(
                    showMergeDialog = false,
                    pendingImportEntries = emptyList(),
                    message = "导入成功，已替换为 ${entries.size} 条记录",
                    isError = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    showMergeDialog = false,
                    message = "导入失败：${e.message}",
                    isError = true
                )
            }
        }
    }

    fun dismissMergeDialog() {
        _uiState.value = _uiState.value.copy(
            showMergeDialog = false,
            pendingImportEntries = emptyList()
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
