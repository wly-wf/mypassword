package com.mypassword.app

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mypassword.app.ui.navigation.Routes
import com.mypassword.app.ui.unlock.UnlockScreen
import com.mypassword.app.ui.list.ListScreen
import com.mypassword.app.ui.edit.EditScreen
import com.mypassword.app.ui.backup.BackupScreen
import com.mypassword.app.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val app = androidx.compose.ui.platform.LocalContext.current
        .applicationContext as MyPasswordApplication
    val screenLocked by app.screenLocked.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(screenLocked) {
        if (screenLocked && currentRoute != Routes.UNLOCK) {
            navController.navigate(Routes.UNLOCK) {
                popUpTo(0) { inclusive = true }
            }
            app.clearScreenLocked()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.UNLOCK
    ) {
        // 解锁
        composable(Routes.UNLOCK) {
            UnlockScreen(
                onUnlockSuccess = {
                    navController.navigate(Routes.LIST) {
                        popUpTo(Routes.UNLOCK) { inclusive = true }
                    }
                }
            )
        }

        // 列表
        composable(Routes.LIST) {
            ListScreen(
                onAddEntry = { entryId ->
                    navController.navigate(Routes.edit(entryId))
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToUnlock = {
                    navController.navigate(Routes.UNLOCK) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 编辑（待阶段五实现）
        composable(Routes.EDIT) { backStackEntry ->
            val entryId = backStackEntry.arguments
                ?.getString("entryId")?.toLongOrNull() ?: -1
            EditScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 备份
        composable(Routes.BACKUP) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 设置
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBackup = {
                    navController.navigate(Routes.BACKUP)
                },
                onNavigateToUnlock = {
                    navController.navigate(Routes.UNLOCK) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
