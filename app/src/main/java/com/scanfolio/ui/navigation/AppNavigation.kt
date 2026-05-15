package com.scanfolio.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scanfolio.ui.portfolio.PortfolioScreen
import com.scanfolio.ui.scan.ScanScreen
import com.scanfolio.ui.analysis.AnalysisScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Portfolio : Screen("portfolio", "持仓", Icons.Default.List)
    data object Scan : Screen("scan", "扫描", Icons.Default.CameraAlt)
    data object Analysis : Screen("analysis", "分析", Icons.Default.BarChart)
}

val bottomNavItems = listOf(Screen.Portfolio, Screen.Scan, Screen.Analysis)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Portfolio.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Portfolio.route) { PortfolioScreen(navController) }
            composable(Screen.Scan.route) { ScanScreen(navController) }
            composable(Screen.Analysis.route) { AnalysisScreen() }
            composable("stock_detail/{stockId}") { backStackEntry ->
                val stockId = backStackEntry.arguments?.getString("stockId")?.toLongOrNull() ?: return@composable
                com.scanfolio.ui.portfolio.StockDetailScreen(
                    stockId = stockId,
                    navController = navController
                )
            }
            composable("settings") {
                com.scanfolio.ui.settings.SettingsScreen(navController)
            }
            composable("column_manage") {
                com.scanfolio.ui.settings.ColumnManageScreen(navController)
            }
            composable("strategy_manage") {
                com.scanfolio.ui.settings.StrategyManageScreen(navController)
            }
        }
    }
}
