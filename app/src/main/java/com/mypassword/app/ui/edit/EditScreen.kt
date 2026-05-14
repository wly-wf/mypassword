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

    if (showGenerator) {
        PasswordGeneratorDialog(
            onDismiss = { showGenerator = false },
            onPasswordSelected = {
                viewModel.onPasswordGenerated(it)
                showGenerator = false
            }
        )
    }

    LaunchedEffect(entryId) {
        if (entryId > 0) {
            viewModel.loadEntry(entryId)
        }
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "添加条目" else "编辑条目") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("标题") },
                placeholder = { Text("例如：个人邮箱、公司 VPN") },
                singleLine = true,
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it) } },
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
                visualTransformation = if (uiState.showPassword) VisualTransformation.None
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

            // 网址（可选）
            OutlinedTextField(
                value = uiState.url,
                onValueChange = { viewModel.onUrlChange(it) },
                label = { Text("网址（可选）") },
                placeholder = { Text("https://example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // 备注
            OutlinedTextField(
                value = uiState.note,
                onValueChange = { viewModel.onNoteChange(it) },
                label = { Text("备注（可选）") },
                placeholder = { Text("附加说明信息") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 保存按钮
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isWorking && uiState.isDirty &&
                          uiState.title.isNotBlank() &&
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
