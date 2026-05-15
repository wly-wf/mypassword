package com.mypassword.app.ui.address

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mypassword.app.viewmodel.AddressDetailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressDetailScreen(
    addressId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: AddressDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(addressId) { viewModel.loadAddress(addressId) }
    LaunchedEffect(uiState.deleted) { if (uiState.deleted) onNavigateBack() }

    var showDelete by remember { mutableStateOf(false) }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false }, title = { Text("确认删除") },
            text = { Text("确定要删除「${uiState.address?.title}」吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAddress(); showDelete = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("取消") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("地址详情") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
                actions = { IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { uiState.address?.let { onNavigateToEdit(it.id) } },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) { Icon(Icons.Filled.Edit, contentDescription = "编辑") }
        }
    ) { padding ->
        val addr = uiState.address ?: return@Scaffold
        val clipboard = LocalClipboardManager.current
        val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(addr.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            // 类型
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(addr.type, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            // 地址
            Column { Text("地址", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(4.dp)); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(addr.address, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)); IconButton(onClick = { clipboard.setText(AnnotatedString(addr.address)); scope.launch { snackbarHostState.showSnackbar("地址已复制") } }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp)) } } }

            // 姓名
            if (addr.name.isNotBlank()) {
                Column { Text("姓名", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(4.dp)); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(addr.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)); IconButton(onClick = { clipboard.setText(AnnotatedString(addr.name)); scope.launch { snackbarHostState.showSnackbar("姓名已复制") } }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp)) } } }
            }

            // 电话
            if (addr.phone.isNotBlank()) {
                Column { Text("电话", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(4.dp)); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(addr.phone, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)); IconButton(onClick = { clipboard.setText(AnnotatedString(addr.phone)); scope.launch { snackbarHostState.showSnackbar("电话已复制") } }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.ContentCopy, contentDescription = "复制", modifier = Modifier.size(18.dp)) } } }
            }

            // 备注
            if (addr.note.isNotBlank()) {
                Column { Text("备注", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(4.dp)); Text(addr.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            // 时间
            Column {
                Text("创建于 ${fmt.format(Date(addr.createdAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                Spacer(Modifier.height(2.dp))
                Text("最后编辑于 ${fmt.format(Date(addr.updatedAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }

            Spacer(Modifier.height(72.dp))
        }
    }
}
