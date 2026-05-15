package com.mypassword.app.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mypassword.app.ui.address.AddressListScreen
import com.mypassword.app.ui.list.ListScreen

@Composable
fun HomeScreen(
    onPasswordClick: (Long) -> Unit,
    onPasswordAdd: () -> Unit,
    onAddressClick: (Long) -> Unit,
    onAddressAdd: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToUnlock: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    label = { Text("密码") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Place, contentDescription = null) },
                    label = { Text("地址") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> ListScreen(
                onEntryClick = onPasswordClick,
                onAddNew = onPasswordAdd,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToUnlock = onNavigateToUnlock,
                modifier = Modifier.padding(padding)
            )
            1 -> AddressListScreen(
                onAddressClick = onAddressClick,
                onAddNew = onAddressAdd,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
