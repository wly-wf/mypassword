package com.mypassword.app.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    entryId: Long,
    onNavigateBack: () -> Unit,
    // TODO 阶段五：注入 ViewModel
) {
    val isNew = entryId == -1L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "添加条目" else "编辑条目") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("取消")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isNew) "添加表单（占位）" else "编辑表单（占位）",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
