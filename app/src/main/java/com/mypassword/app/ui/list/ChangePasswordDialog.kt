package com.mypassword.app.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyPasswordApplication
    val scope = rememberCoroutineScope()

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showOld by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp)
        ) {
            if (isSuccess) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("主密码已修改", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "请使用新密码重新登录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onSuccess) {
                        Text("重新登录")
                    }
                }
            } else {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "修改主密码",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "请输入当前密码和新密码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 旧密码
                    PasswordField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; errorMessage = null },
                        label = "当前密码",
                        showPassword = showOld,
                        onToggleVisibility = { showOld = !showOld }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 新密码
                    PasswordField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMessage = null },
                        label = "新密码",
                        showPassword = showNew,
                        onToggleVisibility = { showNew = !showNew },
                        supportingText = "大小写字母 + 数字，6-16 位"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 确认新密码
                    PasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        label = "确认新密码",
                        showPassword = showConfirm,
                        onToggleVisibility = { showConfirm = !showConfirm }
                    )

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            enabled = !isWorking
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                val old = oldPassword.trim()
                                val new = newPassword.trim()
                                val confirm = confirmPassword.trim()

                                if (old.isEmpty() || new.isEmpty()) {
                                    errorMessage = "请填写所有密码字段"
                                    return@Button
                                }
                                val pwRegex = Regex("^[a-zA-Z0-9]{6,16}$")
                                if (!pwRegex.matches(new)) {
                                    errorMessage = "新密码需为大小写字母+数字，6-16位"
                                    return@Button
                                }
                                if (new != confirm) {
                                    errorMessage = "两次新密码不一致"
                                    return@Button
                                }
                                if (old == new) {
                                    errorMessage = "新密码不能与当前密码相同"
                                    return@Button
                                }

                                isWorking = true
                                errorMessage = null

                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            // 1. 验证旧密码
                                            if (!app.sessionManager.verifyOldPassword(old)) {
                                                throw Exception("当前密码错误")
                                            }

                                            // 2. 生成新凭据（不修改持久化状态）
                                            val creds = app.sessionManager.generateNewCredentials(new)

                                            // 3. 关闭 Room 数据库
                                            app.database.close()

                                            // 4. 用 SQLCipher 原生 API 修改加密密钥
                                            AppDatabase.rekeyDatabase(app, creds.oldKey, creds.newKey)

                                            // 5. rekey 成功后才持久化新凭据
                                            app.sessionManager.commitPasswordChange(
                                                app, creds.newSalt, creds.newKey, creds.newHash
                                            )

                                            // 6. 用新密钥重建 Room 数据库
                                            app.database = AppDatabase.create(app, creds.newKey)
                                        }
                                        // 如果用户开启了指纹解锁，用新密钥重建 Keystore
                                        if (app.sessionManager.isBiometricEnabled()) {
                                            app.sessionManager.setBiometricEnabled(true)
                                        }
                                        isSuccess = true
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "修改失败"
                                    } finally {
                                        isWorking = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isWorking
                        ) {
                            if (isWorking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("确认修改")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onToggleVisibility: () -> Unit,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (showPassword) VisualTransformation.None
                              else PasswordVisualTransformation(),
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
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
}
