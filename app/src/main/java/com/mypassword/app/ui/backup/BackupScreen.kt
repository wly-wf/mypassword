package com.mypassword.app.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mypassword.app.viewmodel.BackupOperation
import com.mypassword.app.viewmodel.BackupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 文件创建器（导出）
    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.exportToUri(it) }
    }

    // 文件选择器（导入）
    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // 用户需要先输入密码再导入
        }
    }

    // 消息 Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // 合并策略对话框
    if (uiState.showMergeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMergeDialog() },
            title = { Text("导入 ${uiState.pendingImportEntries.size} 条记录") },
            text = { Text("请选择导入方式：追加到现有数据末尾，还是清空后替换全部数据？") },
            confirmButton = {
                TextButton(onClick = { viewModel.mergeAppend() }) {
                    Text("追加合并")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.dismissMergeDialog() }) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = { viewModel.mergeReplace() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("替换全部")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("备份与恢复") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // === 导出区域 ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.FileUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "导出备份",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "将所有密码加密导出为 .mpbak 文件，可用于换机迁移。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExportPasswordField(
                        label = "导出密码",
                        value = uiState.exportPassword,
                        onValueChange = { viewModel.onExportPasswordChange(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ExportPasswordField(
                        label = "确认密码",
                        value = uiState.confirmExportPassword,
                        onValueChange = { viewModel.onConfirmExportPasswordChange(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            createFileLauncher.launch("MyPassword_Backup.mpbak")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = uiState.operation != BackupOperation.EXPORTING &&
                                  uiState.exportPassword.isNotBlank() &&
                                  uiState.confirmExportPassword.isNotBlank()
                    ) {
                        if (uiState.operation == BackupOperation.EXPORTING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("选择保存位置并导出")
                    }
                }
            }

            // === 导入区域 ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "导入备份",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "从 .mpbak 文件恢复密码数据。导入时需要输入导出时设置的密码。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var importUri by remember { mutableStateOf<Uri?>(null) }

                    // 选择文件
                    val importFileLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        uri?.let {
                            context.contentResolver.takePersistableUriPermission(
                                it,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            importUri = it
                        }
                    }

                    OutlinedButton(
                        onClick = { importFileLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (importUri != null) "已选择文件"
                            else "选择 .mpbak 文件"
                        )
                    }

                    if (importUri != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        ExportPasswordField(
                            label = "导出密码",
                            value = uiState.importPassword,
                            onValueChange = { viewModel.onImportPasswordChange(it) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.importFromUri(importUri!!) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = uiState.operation != BackupOperation.IMPORTING &&
                                      uiState.importPassword.isNotBlank()
                        ) {
                            if (uiState.operation == BackupOperation.IMPORTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("解密并导入")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (showPassword) VisualTransformation.None
                              else PasswordVisualTransformation(),
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
}
