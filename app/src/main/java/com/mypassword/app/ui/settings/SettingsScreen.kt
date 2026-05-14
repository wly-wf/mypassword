package com.mypassword.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mypassword.app.MyPasswordApplication
import com.mypassword.app.ui.list.ChangePasswordDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToUnlock: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyPasswordApplication
    var fingerprintEnabled by remember { mutableStateOf(app.sessionManager.isBiometricEnabled()) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val biometricAvailable = remember { app.sessionManager.isBiometricHardwareAvailable(app) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showChangePassword) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onSuccess = {
                showChangePassword = false
                onNavigateToUnlock()
            }
        )
    }

    if (showAbout) {
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) { "1.0.0" }

        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("关于 MyPassword") },
            text = {
                Column {
                    Text("极简离线密码管理器")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "版本 $versionName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "纯本地存储，Material You 设计，指纹+密码双解锁。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("关闭")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wly-wf/mypassword"))
                        context.startActivity(intent)
                    }) {
                        Text("GitHub")
                    }
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wly-wf/mypassword/releases"))
                        context.startActivity(intent)
                    }) {
                        Text("检查更新")
                    }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 安全
            SectionHeader("安全")
            SettingsToggleItem(
                icon = { Icon(Icons.Outlined.Fingerprint, contentDescription = null) },
                title = "指纹解锁",
                subtitle = if (biometricAvailable) "开启后可使用指纹或密码解锁"
                            else "您的设备不支持指纹解锁",
                checked = fingerprintEnabled,
                enabled = biometricAvailable,
                onCheckedChange = { enabled ->
                    val ok = app.sessionManager.setBiometricEnabled(enabled)
                    fingerprintEnabled = ok && enabled
                    if (enabled && !ok) {
                        scope.launch {
                            snackbarHostState.showSnackbar("指纹解锁开启失败，请检查设备是否录入指纹")
                        }
                    }
                }
            )
            SettingsItem(
                icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                title = "修改主密码",
                subtitle = "更改用于加密数据库的主密码",
                onClick = { showChangePassword = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 数据
            SectionHeader("数据")
            SettingsItem(
                icon = { Icon(Icons.Outlined.FileUpload, contentDescription = null) },
                title = "备份与恢复",
                subtitle = "加密导出或导入 .mpbak 备份文件",
                onClick = onNavigateToBackup
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 关于
            SectionHeader("关于")
            SettingsItem(
                icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                title = "关于 MyPassword",
                subtitle = "版本 1.0 · 查看开源仓库",
                onClick = { showAbout = true }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.5f else 0.25f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}
