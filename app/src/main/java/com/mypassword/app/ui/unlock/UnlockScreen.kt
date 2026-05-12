package com.mypassword.app.ui.unlock

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mypassword.app.viewmodel.UnlockMode
import com.mypassword.app.viewmodel.UnlockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(
    onUnlockSuccess: () -> Unit,
    viewModel: UnlockViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 生物识别模式
    if (uiState.mode == UnlockMode.BIOMETRIC_UNLOCK) {
        BiometricPromptScreen(viewModel, onUnlockSuccess)
    }

    // 数据库创建成功即跳转
    LaunchedEffect(uiState.mode) {
        if (uiState.mode == UnlockMode.LOADING) {
            onUnlockSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 图标
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "🔐",
                        fontSize = 36.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 标题
            Text(
                text = "MyPassword",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (uiState.mode) {
                    UnlockMode.FIRST_TIME_SETUP -> "设置主密码以保护您的数据"
                    UnlockMode.LOCKED_OUT -> "密码错误次数过多，请稍后再试"
                    UnlockMode.WORKING -> "正在验证..."
                    else -> "请输入主密码解锁"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (uiState.mode) {
                UnlockMode.FIRST_TIME_SETUP -> {
                    SetupPasswordForm(viewModel, uiState)
                }
                UnlockMode.PASSWORD_UNLOCK,
                UnlockMode.BIOMETRIC_UNLOCK -> {
                    UnlockPasswordForm(viewModel, uiState)
                }
                UnlockMode.LOCKED_OUT -> {
                    LockedOutView(uiState)
                }
                UnlockMode.WORKING -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "正在处理...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                UnlockMode.LOADING -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun SetupPasswordForm(
    viewModel: UnlockViewModel,
    uiState: com.mypassword.app.viewmodel.UnlockUiState
) {
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = uiState.passwordInput,
        onValueChange = { viewModel.onSetupPasswordInput(it) },
        label = { Text("主密码") },
        placeholder = { Text("输入主密码") },
        singleLine = true,
        visualTransformation = if (showPassword) VisualTransformation.None
                              else PasswordVisualTransformation(),
        supportingText = { Text("大小写字母 + 数字，6-16 位") },
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    if (showPassword) Icons.Outlined.VisibilityOff
                    else Icons.Outlined.Visibility,
                    contentDescription = null
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = uiState.confirmPassword,
        onValueChange = { viewModel.onSetupConfirmInput(it) },
        label = { Text("确认主密码") },
        placeholder = { Text("再次输入主密码") },
        singleLine = true,
        visualTransformation = if (showConfirm) VisualTransformation.None
                              else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showConfirm = !showConfirm }) {
                Icon(
                    if (showConfirm) Icons.Outlined.VisibilityOff
                    else Icons.Outlined.Visibility,
                    contentDescription = null
                )
            }
        },
        isError = uiState.errorMessage != null,
        supportingText = uiState.errorMessage?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = {
            if (viewModel.confirmSetup()) {
                // 设置成功 → onUnlockSuccess 由 LaunchedEffect 触发
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = uiState.mode != UnlockMode.WORKING &&
                  uiState.passwordInput.isNotEmpty() &&
                  uiState.confirmPassword.isNotEmpty()
    ) {
        Text("设置并进入", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun UnlockPasswordForm(
    viewModel: UnlockViewModel,
    uiState: com.mypassword.app.viewmodel.UnlockUiState
) {
    var showPw by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = uiState.passwordInput,
        onValueChange = { viewModel.onPasswordInput(it) },
        label = { Text("主密码") },
        placeholder = { Text("输入主密码解锁") },
        singleLine = true,
        visualTransformation = if (showPw) VisualTransformation.None
                              else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showPw = !showPw }) {
                Icon(
                    if (showPw) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                viewModel.verifyPassword()
            }
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )

    AnimatedVisibility(visible = uiState.errorMessage != null) {
        Text(
            text = uiState.errorMessage ?: "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }

    if (uiState.canUseBiometric) {
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { viewModel.switchToBiometricMode() }) {
            Icon(
                Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("使用指纹解锁")
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = {
            focusManager.clearFocus()
            viewModel.verifyPassword()
        },
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = uiState.mode != UnlockMode.WORKING &&
                  uiState.passwordInput.isNotEmpty()
    ) {
        Text("解锁", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LockedOutView(uiState: com.mypassword.app.viewmodel.UnlockUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "已锁定",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "请等待 ${uiState.lockoutSeconds} 秒后再试",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        CircularProgressIndicator(
            progress = { uiState.lockoutSeconds / 30f },
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun BiometricPromptScreen(
    viewModel: UnlockViewModel,
    onUnlockSuccess: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fragmentActivity = context as? FragmentActivity ?: return

    val promptInfo = remember {
        androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("验证身份")
            .setSubtitle("使用指纹或面部识别解锁")
            .setNegativeButtonText("使用密码")
            .setConfirmationRequired(false)
            .build()
    }

    val biometricPrompt = remember {
        androidx.biometric.BiometricPrompt(
            fragmentActivity,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: androidx.biometric.BiometricPrompt.AuthenticationResult
                ) {
                    val cipher = result.cryptoObject?.cipher
                    if (cipher != null) {
                        viewModel.onBiometricSuccess(cipher)
                    }
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    viewModel.onBiometricError()
                }

                override fun onAuthenticationFailed() {
                    // 单次识别失败，不处理
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        val cipher = viewModel.getBiometricCipher()
        if (cipher != null) {
            biometricPrompt.authenticate(
                promptInfo,
                androidx.biometric.BiometricPrompt.CryptoObject(cipher)
            )
        } else {
            viewModel.switchToPasswordMode()
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.mode) {
        if (uiState.mode == UnlockMode.LOADING) {
            onUnlockSuccess()
        }
    }
}
