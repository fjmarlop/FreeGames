package com.freesudoku.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.freesudoku.app.ui.game.GameScreen
import com.freesudoku.app.ui.home.HomeScreen
import com.freesudoku.app.ui.settings.SettingsScreen
import com.freesudoku.app.ui.stats.StatsScreen

/** Home/Stats/Settings behave like bottom-nav siblings: switching tabs never stacks duplicates. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { inclusive = route == Routes.HOME }
        launchSingleTop = true
    }
}

@Composable
fun FreeSudokuNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onPlay = { navController.navigate(Routes.GAME) },
                onOpenStats = { navController.switchTab(Routes.STATS) },
                onOpenSettings = { navController.switchTab(Routes.SETTINGS) },
            )
        }
        composable(Routes.GAME) {
            GameScreen(onExit = { navController.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsScreen(
                onOpenHome = { navController.switchTab(Routes.HOME) },
                onOpenSettings = { navController.switchTab(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onOpenHome = { navController.switchTab(Routes.HOME) },
                onOpenStats = { navController.switchTab(Routes.STATS) },
            )
        }
    }
}
