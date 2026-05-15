package com.mypassword.app

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mypassword.app.ui.navigation.Routes
import com.mypassword.app.ui.unlock.UnlockScreen
import com.mypassword.app.ui.list.ListScreen
import com.mypassword.app.ui.address.AddressDetailScreen
import com.mypassword.app.ui.address.AddressEditScreen
import com.mypassword.app.ui.home.HomeScreen
import com.mypassword.app.ui.detail.DetailScreen
import com.mypassword.app.ui.edit.EditScreen
import com.mypassword.app.ui.backup.BackupScreen
import com.mypassword.app.ui.settings.SettingsScreen

private const val TRANSITION_ENTER = 220
private const val TRANSITION_EXIT = 150

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
        composable(
            route = Routes.UNLOCK,
            enterTransition = { fadeIn(tween(TRANSITION_ENTER)) },
            exitTransition = { fadeOut(tween(TRANSITION_EXIT)) },
            popEnterTransition = { fadeIn(tween(TRANSITION_ENTER)) },
            popExitTransition = { fadeOut(tween(TRANSITION_EXIT)) }
        ) {
            UnlockScreen(
                onUnlockSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.UNLOCK) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.HOME,
            enterTransition = { fadeIn(tween(TRANSITION_ENTER)) },
            exitTransition = { fadeOut(tween(TRANSITION_EXIT)) },
            popEnterTransition = { fadeIn(tween(TRANSITION_ENTER)) },
            popExitTransition = { fadeOut(tween(TRANSITION_EXIT)) }
        ) {
            HomeScreen(
                onPasswordClick = { navController.navigate(Routes.detail(it)) },
                onPasswordAdd = { navController.navigate(Routes.edit(0)) },
                onAddressClick = { navController.navigate(Routes.addrDetail(it)) },
                onAddressAdd = { navController.navigate(Routes.addrEdit(0)) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToUnlock = {
                    navController.navigate(Routes.UNLOCK) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(
            route = Routes.DETAIL,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 3 },
                    animationSpec = tween(TRANSITION_ENTER)
                ) + fadeIn(tween(TRANSITION_ENTER))
            },
            exitTransition = { fadeOut(tween(TRANSITION_EXIT)) },
            popEnterTransition = { fadeIn(tween(TRANSITION_ENTER)) },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 3 },
                    animationSpec = tween(TRANSITION_EXIT)
                ) + fadeOut(tween(TRANSITION_EXIT))
            }
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments
                ?.getString("entryId")?.toLongOrNull() ?: return@composable
            DetailScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Routes.edit(it)) }
            )
        }

        composable(
            route = Routes.EDIT,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 3 },
                    animationSpec = tween(TRANSITION_ENTER)
                ) + fadeIn(tween(TRANSITION_ENTER))
            },
            exitTransition = { fadeOut(tween(TRANSITION_EXIT)) },
            popEnterTransition = { fadeIn(tween(TRANSITION_ENTER)) },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 3 },
                    animationSpec = tween(TRANSITION_EXIT)
                ) + fadeOut(tween(TRANSITION_EXIT))
            }
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments
                ?.getString("entryId")?.toLongOrNull() ?: -1
            EditScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ADDR_DETAIL,
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(TRANSITION_ENTER)) + fadeIn(tween(TRANSITION_ENTER)) },
            exitTransition = { fadeOut(tween(TRANSITION_EXIT)) },
            popEnterTransition = { fadeIn(tween(TRANSITION_ENTER)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(TRANSITION_EXIT)) + fadeOut(tween(TRANSITION_EXIT)) }
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("addressId")?.toLongOrNull() ?: return@composable
            AddressDetailScreen(addressId = id, onNavigateBack = { navController.popBackStack() }, onNavigateToEdit = { navController.navigate(Routes.addrEdit(it)) })
        }

        composable(
            route = Routes.ADDR_EDIT,
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(TRANSITION_ENTER)) + fadeIn(tween(TRANSITION_ENTER)) },
            exitTransition = { fadeOut(tween(TRANSITION_EXIT)) },
            popEnterTransition = { fadeIn(tween(TRANSITION_ENTER)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(TRANSITION_EXIT)) + fadeOut(tween(TRANSITION_EXIT)) }
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("addressId")?.toLongOrNull() ?: -1
            AddressEditScreen(addressId = id, onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.BACKUP,
            enterTransition = { fadeIn(tween(TRANSITION_ENTER)) },
            exitTransition = { fadeOut(tween(TRANSITION_EXIT)) },
            popEnterTransition = { fadeIn(tween(TRANSITION_ENTER)) },
            popExitTransition = { fadeOut(tween(TRANSITION_EXIT)) }
        ) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.SETTINGS,
            enterTransition = { fadeIn(tween(TRANSITION_ENTER)) },
            exitTransition = { fadeOut(tween(TRANSITION_EXIT)) },
            popEnterTransition = { fadeIn(tween(TRANSITION_ENTER)) },
            popExitTransition = { fadeOut(tween(TRANSITION_EXIT)) }
        ) {
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
