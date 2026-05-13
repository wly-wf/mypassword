package com.mypassword.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UnlockUiState(
    val mode: UnlockMode = UnlockMode.LOADING,
    val passwordInput: String = "",
    val confirmPassword: String = "",
    val errorMessage: String? = null,
    val errorCount: Int = 0,
    val lockoutSeconds: Int = 0,
    val canUseBiometric: Boolean = false,
    val isWorking: Boolean = false,
    val biometricTrigger: Int = 0
)

enum class UnlockMode {
    LOADING,
    FIRST_TIME_SETUP,
    PASSWORD_UNLOCK,
    BIOMETRIC_UNLOCK,
    LOCKED_OUT,
    WORKING
}

class UnlockViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyPasswordApplication
    private val sessionManager = app.sessionManager
    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    private var lockoutJob: Job? = null
    private val maxAttempts = 5
    private val lockoutDuration = 30

    private val passwordRegex = Regex("^[a-zA-Z0-9]{6,16}$")

    init {
        val isFirstTime = !sessionManager.hasMasterPassword()
        val bioEnabled = sessionManager.isBiometricEnabled() && sessionManager.isBiometricAvailable()

        _uiState.value = _uiState.value.copy(
            mode = if (isFirstTime) UnlockMode.FIRST_TIME_SETUP
                   else if (bioEnabled) UnlockMode.BIOMETRIC_UNLOCK
                   else UnlockMode.PASSWORD_UNLOCK,
            canUseBiometric = bioEnabled
        )
    }

    // === 密码输入 ===

    fun onPasswordInput(value: String) {
        _uiState.value = _uiState.value.copy(passwordInput = value, errorMessage = null)
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

        if (!passwordRegex.matches(pw)) {
            _uiState.value = state.copy(errorMessage = "主密码需为大小写字母+数字，6-16位")
            return false
        }
        if (pw != confirm) {
            _uiState.value = state.copy(errorMessage = "两次密码不一致")
            return false
        }

        _uiState.value = state.copy(mode = UnlockMode.WORKING, isWorking = true)

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    sessionManager.setupMasterPassword(app, pw)
                    if (app.isDatabaseInitialized()) {
                        app.database.close()
                    }
                    app.database = AppDatabase.create(app, sessionManager.getDatabaseKey())
                }
                _uiState.value = _uiState.value.copy(mode = UnlockMode.LOADING, isWorking = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    mode = UnlockMode.FIRST_TIME_SETUP,
                    isWorking = false,
                    errorMessage = "设置失败：${e.message}"
                )
            }
        }
        return true
    }

    // === 密码验证 ===

    fun verifyPassword() {
        val state = _uiState.value
        if (state.mode == UnlockMode.LOCKED_OUT || state.mode == UnlockMode.WORKING) return

        _uiState.value = state.copy(errorMessage = null)
        val password = state.passwordInput
        if (password.isEmpty()) return

        _uiState.value = _uiState.value.copy(mode = UnlockMode.WORKING, isWorking = true)

        viewModelScope.launch {
            try {
                val valid = withContext(Dispatchers.IO) {
                    sessionManager.unlockWithPassword(
                        password,
                        sessionManager.getSalt(),
                        sessionManager.getStoredHash()
                    )
                }

                if (valid) {
                    withContext(Dispatchers.IO) {
                        if (app.isDatabaseInitialized()) {
                            app.database.close()
                        }
                        app.database = AppDatabase.create(app, sessionManager.getDatabaseKey())
                    }
                    _uiState.value = _uiState.value.copy(mode = UnlockMode.LOADING, isWorking = false)
                } else {
                    val newCount = state.errorCount + 1
                    if (newCount >= maxAttempts) {
                        startLockout()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            mode = UnlockMode.PASSWORD_UNLOCK,
                            isWorking = false,
                            errorMessage = "密码错误，还剩 ${maxAttempts - newCount} 次机会",
                            errorCount = newCount,
                            passwordInput = ""
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    mode = UnlockMode.PASSWORD_UNLOCK,
                    isWorking = false,
                    errorMessage = "验证失败：${e.message}",
                    passwordInput = ""
                )
            }
        }
    }

    private fun startLockout() {
        _uiState.value = _uiState.value.copy(
            mode = UnlockMode.LOCKED_OUT,
            isWorking = false,
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

    fun switchToBiometricMode() {
        _uiState.value = _uiState.value.copy(
            mode = UnlockMode.BIOMETRIC_UNLOCK,
            errorMessage = null,
            biometricTrigger = _uiState.value.biometricTrigger + 1
        )
    }

    // === 生物识别 ===

    fun onBiometricSuccess() {
        val success = sessionManager.unlockWithBiometric()
        if (success) {
            _uiState.value = _uiState.value.copy(mode = UnlockMode.WORKING, isWorking = true)
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        if (app.isDatabaseInitialized()) {
                            app.database.close()
                        }
                        app.database = AppDatabase.create(app, sessionManager.getDatabaseKey())
                    }
                    _uiState.value = _uiState.value.copy(mode = UnlockMode.LOADING, isWorking = false)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        mode = UnlockMode.PASSWORD_UNLOCK,
                        isWorking = false,
                        errorMessage = "解锁失败：${e.message}"
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = "生物识别验证失败")
        }
    }

    fun onBiometricError() {
        switchToPasswordMode()
    }

    override fun onCleared() {
        super.onCleared()
        lockoutJob?.cancel()
    }
}
