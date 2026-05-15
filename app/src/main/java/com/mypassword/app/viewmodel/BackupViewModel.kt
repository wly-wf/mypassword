package com.mypassword.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.data.crypto.BackupAddress
import com.mypassword.app.data.crypto.BackupData
import com.mypassword.app.data.crypto.BackupEncryption
import com.mypassword.app.data.crypto.BackupEntry
import com.mypassword.app.data.db.entity.Address
import com.mypassword.app.data.db.entity.Entry
import com.mypassword.app.data.repository.AddressRepository
import com.mypassword.app.data.repository.EntryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.compose.runtime.Immutable
enum class BackupOperation { NONE, EXPORTING, IMPORTING }

@androidx.compose.runtime.Immutable
data class BackupUiState(
    val exportPassword: String = "",
    val confirmExportPassword: String = "",
    val importPassword: String = "",
    val operation: BackupOperation = BackupOperation.NONE,
    val message: String? = null, val isError: Boolean = false,
    val showMergeDialog: Boolean = false,
    val pendingImportEntries: List<BackupEntry> = emptyList(),
    val pendingImportAddresses: List<BackupAddress> = emptyList(),
    val pendingImportUri: Uri? = null
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyPasswordApplication
    private val entryRepo = EntryRepository(app.database)
    private val addrRepo = AddressRepository(app.database)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun onExportPasswordChange(v: String) { _uiState.value = _uiState.value.copy(exportPassword = v, message = null) }
    fun onConfirmExportPasswordChange(v: String) { _uiState.value = _uiState.value.copy(confirmExportPassword = v, message = null) }

    fun exportToUri(uri: Uri) {
        val s = _uiState.value
        if (s.exportPassword.length < 4) { _uiState.value = s.copy(message = "导出密码至少 4 位", isError = true); return }
        if (s.exportPassword != s.confirmExportPassword) { _uiState.value = s.copy(message = "两次密码不一致", isError = true); return }

        _uiState.value = s.copy(operation = BackupOperation.EXPORTING, message = null)

        viewModelScope.launch {
            try {
                val (entries, addresses) = withContext(Dispatchers.IO) {
                    val e = entryRepo.getAllEntriesList().map {
                        BackupEntry(it.title, it.username, it.password, it.url, it.note, it.createdAt, it.updatedAt)
                    }
                    val a = addrRepo.getAllAddressesList().map {
                        BackupAddress(it.title, it.type, it.address, it.name, it.phone, it.note, it.createdAt, it.updatedAt)
                    }
                    e to a
                }

                val json = BackupData(entries = entries, addresses = addresses).toJson()
                val encrypted = BackupEncryption.encryptForExport(json.toByteArray(Charsets.UTF_8), s.exportPassword)

                withContext(Dispatchers.IO) { app.contentResolver.openOutputStream(uri)?.use { it.write(encrypted) } }

                _uiState.value = _uiState.value.copy(operation = BackupOperation.NONE,
                    message = "导出成功，密码 ${entries.size} 条、地址 ${addresses.size} 条", isError = false,
                    exportPassword = "", confirmExportPassword = "")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(operation = BackupOperation.NONE, message = "导出失败：${e.message}", isError = true)
            }
        }
    }

    fun onImportPasswordChange(v: String) { _uiState.value = _uiState.value.copy(importPassword = v, message = null) }

    fun importFromUri(uri: Uri) {
        val s = _uiState.value
        if (s.importPassword.isBlank()) { _uiState.value = s.copy(message = "请输入导出时设置的密码", isError = true); return }

        _uiState.value = s.copy(operation = BackupOperation.IMPORTING, message = null)

        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) { app.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw Exception("无法读取文件") }
                val decrypted = BackupEncryption.decryptFromImport(data, s.importPassword).getOrElse { throw Exception("密码错误或文件已损坏") }
                val backup = BackupData.fromJson(String(decrypted, Charsets.UTF_8))

                val total = backup.entries.size + backup.addresses.size
                if (total == 0) { _uiState.value = _uiState.value.copy(operation = BackupOperation.NONE, message = "备份文件中没有数据", isError = true); return@launch }

                _uiState.value = _uiState.value.copy(operation = BackupOperation.NONE, showMergeDialog = true,
                    pendingImportEntries = backup.entries, pendingImportAddresses = backup.addresses,
                    pendingImportUri = uri, importPassword = "")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(operation = BackupOperation.NONE, message = "导入失败：${e.message}", isError = true)
            }
        }
    }

    fun mergeDedup() {
        val entries = _uiState.value.pendingImportEntries
        val addresses = _uiState.value.pendingImportAddresses
        if (entries.isEmpty() && addresses.isEmpty()) return

        viewModelScope.launch {
            try {
                var entryAdded = 0
                var addrAdded = 0
                withContext(Dispatchers.IO) {
                    val existEntries = entryRepo.getAllEntriesList()
                    val existAddrs = addrRepo.getAllAddressesList()

                    entries.forEach { be ->
                        val dup = existEntries.any { it.title == be.title && it.username == be.username && it.password == be.password && it.url == be.url }
                        if (!dup) { entryRepo.addEntry(Entry(title = be.title, username = be.username, password = be.password, url = be.url, note = be.note, createdAt = be.createdAt, updatedAt = be.updatedAt)); entryAdded++ }
                    }
                    addresses.forEach { ba ->
                        val dup = existAddrs.any { it.title == ba.title && it.type == ba.type && it.address == ba.address && it.name == ba.name && it.phone == ba.phone }
                        if (!dup) { addrRepo.addAddress(Address(title = ba.title, type = ba.type, address = ba.address, name = ba.name, phone = ba.phone, note = ba.note, createdAt = ba.createdAt, updatedAt = ba.updatedAt)); addrAdded++ }
                    }
                }
                val skipped = (entries.size - entryAdded) + (addresses.size - addrAdded)
                val msg = "导入完成，新增密码 $entryAdded 条、地址 $addrAdded 条" + if (skipped > 0) "，跳过重复 $skipped 条" else ""
                _uiState.value = _uiState.value.copy(showMergeDialog = false, pendingImportEntries = emptyList(), pendingImportAddresses = emptyList(),
                    message = msg, isError = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(showMergeDialog = false, message = "导入失败：${e.message}", isError = true)
            }
        }
    }

    fun mergeReplace() {
        val entries = _uiState.value.pendingImportEntries
        val addresses = _uiState.value.pendingImportAddresses

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    entryRepo.getAllEntriesList().forEach { entryRepo.deleteEntry(it.id) }
                    addrRepo.getAllAddressesList().forEach { addrRepo.deleteAddress(it.id) }
                    entries.forEach { entryRepo.addEntry(Entry(title = it.title, username = it.username, password = it.password, url = it.url, note = it.note, createdAt = it.createdAt, updatedAt = it.updatedAt)) }
                    addresses.forEach { addrRepo.addAddress(Address(title = it.title, type = it.type, address = it.address, name = it.name, phone = it.phone, note = it.note, createdAt = it.createdAt, updatedAt = it.updatedAt)) }
                }
                _uiState.value = _uiState.value.copy(showMergeDialog = false, pendingImportEntries = emptyList(), pendingImportAddresses = emptyList(),
                    message = "导入成功，已替换为密码 ${entries.size} 条、地址 ${addresses.size} 条", isError = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(showMergeDialog = false, message = "导入失败：${e.message}", isError = true)
            }
        }
    }

    fun dismissMergeDialog() { _uiState.value = _uiState.value.copy(showMergeDialog = false, pendingImportEntries = emptyList(), pendingImportAddresses = emptyList()) }
    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null) }
}
