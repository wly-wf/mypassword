package com.mypassword.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mypassword.app.ui.navigation.Routes
import com.mypassword.app.ui.unlock.UnlockScreen
import com.mypassword.app.ui.list.ListScreen
import com.mypassword.app.ui.edit.EditScreen
import com.mypassword.app.ui.backup.BackupScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.UNLOCK
    ) {
        // 阶段三实现
        composable(Routes.UNLOCK) {
            // TODO: UnlockScreen(navController)
            UnlockScreen(
                onUnlockSuccess = {
                    navController.navigate(Routes.LIST) {
                        popUpTo(Routes.UNLOCK) { inclusive = true }
                    }
                }
            )
        }

        // 阶段四实现
        composable(Routes.LIST) {
            // TODO: ListScreen(navController)
            ListScreen(
                onAddEntry = { entryId ->
                    navController.navigate(Routes.edit(entryId))
                },
                onNavigateToBackup = {
                    navController.navigate(Routes.BACKUP)
                }
            )
        }

        // 阶段五实现
        composable(Routes.EDIT) { backStackEntry ->
            val entryId = backStackEntry.arguments
                ?.getString("entryId")?.toLongOrNull() ?: -1
            // TODO: EditScreen(navController, entryId)
            EditScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 阶段七实现
        composable(Routes.BACKUP) {
            // TODO: BackupScreen(navController)
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
