package com.mypassword.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mypassword.app.ui.list.ChangePasswordDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToUnlock: () -> Unit
) {
    var showChangePassword by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("关于 MyPassword") },
            text = {
                Column {
                    Text("极简离线密码管理器")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "纯本地存储，Material You 设计，指纹+密码双解锁。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "开源仓库",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        "github.com/wly-wf/mypassword",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("关闭")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wly-wf/mypassword"))
                    context.startActivity(intent)
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("打开仓库")
                }
            }
        )
    }

    Scaffold(
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
                title = "导出备份",
                subtitle = "将密码数据加密导出为 .mpbak 文件",
                onClick = onNavigateToBackup
            )
            SettingsItem(
                icon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                title = "导入备份",
                subtitle = "从 .mpbak 文件恢复密码数据",
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
