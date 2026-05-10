package com.mypassword.app.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onAddEntry: (Long) -> Unit,
    onNavigateToBackup: () -> Unit,
    // TODO 阶段四：注入 ViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("网址", "App")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyPassword") },
                actions = {
                    IconButton(onClick = onNavigateToBackup) {
                        Text("↕")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddEntry(-1) }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无条目\n点击右下角 + 添加",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
