package com.mypassword.app.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mypassword.app.data.db.entity.EntryType
import com.mypassword.app.ui.generator.PasswordGeneratorDialog
import com.mypassword.app.viewmodel.EditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    entryId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showGenerator by remember { mutableStateOf(false) }

    // 密码生成器弹窗
    if (showGenerator) {
        PasswordGeneratorDialog(
            onDismiss = { showGenerator = false },
            onPasswordSelected = {
                viewModel.onPasswordGenerated(it)
                showGenerator = false
            }
        )
    }

    // 加载已有条目数据
    LaunchedEffect(entryId) {
        if (entryId > 0) {
            viewModel.loadEntry(entryId)
        }
    }

    // 保存成功后返回
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isNew) "添加条目" else "编辑条目")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isWorking &&
                                  uiState.target.isNotBlank() &&
                                  uiState.username.isNotBlank() &&
                                  uiState.password.isNotBlank()
                    ) {
                        Text("保存")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 类型选择
            TypeSelector(
                selectedType = uiState.type,
                onTypeChange = { viewModel.onTypeChange(it) }
            )

            // 标识字段（网址或 App 名）
            OutlinedTextField(
                value = uiState.target,
                onValueChange = { viewModel.onTargetChange(it) },
                label = {
                    Text(if (uiState.type == EntryType.URL) "网址" else "App 名称")
                },
                placeholder = {
                    Text(
                        if (uiState.type == EntryType.URL) "https://example.com"
                        else "例如：微信、钉钉"
                    )
                },
                singleLine = true,
                isError = uiState.targetError != null,
                supportingText = uiState.targetError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (uiState.type == EntryType.URL) KeyboardType.Uri
                                  else KeyboardType.Text
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // 账号
            OutlinedTextField(
                value = uiState.username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = { Text("账号") },
                placeholder = { Text("邮箱、手机号或用户名") },
                singleLine = true,
                isError = uiState.usernameError != null,
                supportingText = uiState.usernameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // 密码
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("密码") },
                placeholder = { Text("输入或生成密码") },
                singleLine = true,
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError?.let { { Text(it) } },
                visualTransformation = if (uiState.showPassword)
                    VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                            Icon(
                                if (uiState.showPassword) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = null
                            )
                        }
                        // 密码生成器按钮
                        IconButton(onClick = { showGenerator = true }) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "生成密码",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // 备注
            OutlinedTextField(
                value = uiState.note,
                onValueChange = { viewModel.onNoteChange(it) },
                label = { Text("备注") },
                placeholder = { Text("可选，附加信息") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 保存按钮
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isWorking &&
                          uiState.target.isNotBlank() &&
                          uiState.username.isNotBlank() &&
                          uiState.password.isNotBlank()
            ) {
                if (uiState.isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (uiState.isWorking) "保存中..."
                    else if (uiState.isNew) "添加"
                    else "保存修改",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            uiState.saveError?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TypeSelector(
    selectedType: EntryType,
    onTypeChange: (EntryType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        EntryType.entries.forEach { type ->
            val selected = selectedType == type
            val label = if (type == EntryType.URL) "网址" else "App"

            FilterChip(
                selected = selected,
                onClick = { onTypeChange(type) },
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}