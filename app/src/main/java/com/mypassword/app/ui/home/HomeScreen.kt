package com.mypassword.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import com.mypassword.app.ui.address.AddressListScreen
import com.mypassword.app.ui.list.ListScreen
import com.mypassword.app.ui.settings.SettingsScreen

@Composable
fun HomeScreen(
    onPasswordClick: (Long) -> Unit,
    onPasswordAdd: () -> Unit,
    onAddressClick: (Long) -> Unit,
    onAddressAdd: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToUnlock: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Lock, contentDescription = null) }, label = { Text("密码") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Place, contentDescription = null) }, label = { Text("地址") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) }, label = { Text("设置") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> ListScreen(
                onEntryClick = onPasswordClick,
                onAddNew = onPasswordAdd,
                onNavigateToUnlock = onNavigateToUnlock,
                modifier = Modifier.padding(padding)
            )
            1 -> AddressListScreen(
                onAddressClick = onAddressClick,
                onAddNew = onAddressAdd,
                modifier = Modifier.padding(padding)
            )
            2 -> SettingsScreen(
                onNavigateToBackup = onNavigateToBackup,
                onNavigateToUnlock = onNavigateToUnlock,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
