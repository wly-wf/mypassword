package com.mypassword.app.ui.backup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    // TODO 阶段七：注入 ViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("备份与恢复") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { /* TODO 阶段七 */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("导出加密备份")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* TODO 阶段七 */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("导入加密备份")
            }
        }
    }
}
