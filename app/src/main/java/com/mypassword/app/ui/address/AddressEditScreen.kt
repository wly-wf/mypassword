package com.mypassword.app.ui.address

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mypassword.app.viewmodel.ADDRESS_TYPES
import com.mypassword.app.viewmodel.AddressEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressEditScreen(
    addressId: Long,
    onNavigateBack: () -> Unit,
    viewModel: AddressEditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var typeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(addressId) { if (addressId > 0) viewModel.loadAddress(addressId) }
    LaunchedEffect(uiState.saved) { if (uiState.saved) onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "添加地址" else "编辑地址") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            OutlinedTextField(uiState.title, { viewModel.onTitleChange(it) }, label = { Text("标题") }, placeholder = { Text("例如：家里、公司地址") }, singleLine = true, isError = uiState.titleError != null, supportingText = uiState.titleError?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            // 类型下拉框
            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = uiState.type, onValueChange = {}, readOnly = true,
                    label = { Text("类型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    ADDRESS_TYPES.forEach { t ->
                        DropdownMenuItem(text = { Text(t) }, onClick = { viewModel.onTypeChange(t); typeExpanded = false })
                    }
                }
            }

            // 地址
            OutlinedTextField(uiState.address, { viewModel.onAddressChange(it) }, label = { Text("地址") }, placeholder = { Text("详细地址") }, maxLines = 2, isError = uiState.addressError != null, supportingText = uiState.addressError?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            // 姓名
            OutlinedTextField(uiState.name, { viewModel.onNameChange(it) }, label = { Text("姓名") }, placeholder = { Text("收件人姓名") }, singleLine = true, isError = uiState.nameError != null, supportingText = uiState.nameError?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            // 电话
            OutlinedTextField(uiState.phone, { viewModel.onPhoneChange(it) }, label = { Text("电话") }, placeholder = { Text("手机号码") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), isError = uiState.phoneError != null, supportingText = uiState.phoneError?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            // 备注
            OutlinedTextField(uiState.note, { viewModel.onNoteChange(it) }, label = { Text("备注（可选）") }, placeholder = { Text("附加说明") }, maxLines = 3, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isWorking && uiState.isDirty && uiState.title.isNotBlank() && uiState.address.isNotBlank() && uiState.name.isNotBlank() && uiState.phone.isNotBlank()
            ) {
                if (uiState.isWorking) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)) }
                Text(if (uiState.isWorking) "保存中..." else if (uiState.isNew) "添加" else "保存修改", style = MaterialTheme.typography.labelLarge)
            }

            uiState.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}
