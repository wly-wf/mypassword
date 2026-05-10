package com.mypassword.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.data.crypto.SessionManager
import com.mypassword.app.data.db.AppDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UnlockUiState(
    val mode: UnlockMode = UnlockMode.LOADING,
    val passwordInput: String = "",
    val confirmPassword: String = "",
    val errorMessage: String? = null,
    val errorCount: Int = 0,
    val lockoutSeconds: Int = 0,
    val canUseBiometric: Boolean = false
)

enum class UnlockMode {
    LOADING,
    FIRST_TIME_SETUP,
    PASSWORD_UNLOCK,
    BIOMETRIC_UNLOCK,
    LOCKED_OUT
}

class UnlockViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyPasswordApplication
    private val sessionManager = app.sessionManager
    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    private var lockoutJob: Job? = null
    private val maxAttempts = 5
    private val lockoutDuration = 30 // 秒

    init {
        val isFirstTime = !sessionManager.hasMasterPassword()
        val canBio = sessionManager.isBiometricAvailable()

        _uiState.value = _uiState.value.copy(
            mode = if (isFirstTime) UnlockMode.FIRST_TIME_SETUP
                   else if (canBio) UnlockMode.BIOMETRIC_UNLOCK
                   else UnlockMode.PASSWORD_UNLOCK,
            canUseBiometric = canBio
        )
    }

    // === 密码输入 ===

    fun onPasswordInput(value: String) {
        _uiState.value = _uiState.value.copy(
            passwordInput = value,
            errorMessage = null
        )
    }

    fun appendDigit(digit: Int) {
        val current = _uiState.value.passwordInput
        if (current.length < 64) {
            onPasswordInput(current + digit.toString())
        }
    }

    fun deleteLastChar() {
        val current = _uiState.value.passwordInput
        if (current.isNotEmpty()) {
            onPasswordInput(current.dropLast(1))
        }
    }

    // === 首次设置主密码 ===

    fun onSetupPasswordInput(value: String) {
        _uiState.value = _uiState.value.copy(passwordInput = value, errorMessage = null)
    }

    fun onSetupConfirmInput(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, errorMessage = null)
    }

    fun confirmSetup(): Boolean {
        val state = _uiState.value
        val pw = state.passwordInput
        val confirm = state.confirmPassword

        if (pw.length < 6) {
            _uiState.value = state.copy(errorMessage = "主密码至少需要 6 位字符")
            return false
        }
        if (pw != confirm) {
            _uiState.value = state.copy(errorMessage = "两次密码不一致")
            return false
        }

        sessionManager.setupMasterPassword(getApplication(), pw)
        openDatabase(pw)
        return true
    }

    // === 密码验证 ===

    fun verifyPassword() {
        val state = _uiState.value
        if (state.mode == UnlockMode.LOCKED_OUT) return

        _uiState.value = state.copy(errorMessage = null)

        val password = state.passwordInput
        if (password.isEmpty()) return

        viewModelScope.launch {
            val valid = try {
                sessionManager.unlockWithPassword(
                    password,
                    sessionManager.getSalt(),
                    sessionManager.getStoredHash()
                )
            } catch (e: Exception) {
                false
            }

            if (valid) {
                openDatabase(password)
            } else {
                val newCount = state.errorCount + 1
                if (newCount >= maxAttempts) {
                    startLockout()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "密码错误，还剩 ${maxAttempts - newCount} 次机会",
                        errorCount = newCount,
                        passwordInput = ""
                    )
                }
            }
        }
    }

    private fun startLockout() {
        _uiState.value = _uiState.value.copy(
            mode = UnlockMode.LOCKED_OUT,
            lockoutSeconds = lockoutDuration,
            passwordInput = ""
        )
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            var remaining = lockoutDuration
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.value = _uiState.value.copy(lockoutSeconds = remaining)
            }
            _uiState.value = _uiState.value.copy(
                mode = UnlockMode.PASSWORD_UNLOCK,
                errorCount = 0,
                errorMessage = null
            )
        }
    }

    fun switchToPasswordMode() {
        _uiState.value = _uiState.value.copy(
            mode = UnlockMode.PASSWORD_UNLOCK,
            passwordInput = ""
        )
    }

    // === 生物识别 ===

    fun getBiometricCipher() = sessionManager.getBiometricCipher()

    fun onBiometricSuccess(cipher: javax.crypto.Cipher) {
        val success = sessionManager.unlockWithBiometric(cipher)
        if (success) {
            // 生物识别解锁成功，用存储的密钥打开数据库
            val key = sessionManager.getDatabaseKey()
            createDatabase(key)
            _uiState.value = _uiState.value.copy(mode = UnlockMode.LOADING)
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "生物识别验证失败"
            )
        }
    }

    fun onBiometricError() {
        switchToPasswordMode()
    }

    // === 数据库创建 ===

    private fun openDatabase(password: String) {
        val salt = sessionManager.getSalt()
        val key = com.mypassword.app.data.crypto.KeyDerivation.deriveKey(password, salt)
        createDatabase(key)
    }

    private fun createDatabase(key: ByteArray) {
        val app = getApplication<MyPasswordApplication>()
        app.database = AppDatabase.create(app, key)
    }

    override fun onCleared() {
        super.onCleared()
        lockoutJob?.cancel()
    }
}
