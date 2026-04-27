package com.sailapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.sailapp.service.RecordingService
import com.sailapp.ui.dashboard.DashboardScreen
import com.sailapp.ui.regatta.RegattaScreen
import com.sailapp.ui.statistics.StatisticsScreen
import com.sailapp.ui.track.TrackScreen

private data class BottomNavItem(val label: String, val icon: ImageVector, val screen: Screen)

private val bottomNavItems = listOf(
    BottomNavItem("Dashboard", Icons.Default.Dashboard, Screen.Dashboard),
    BottomNavItem("Regatta", Icons.Default.SportsScore, Screen.Regatta),
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val recordingState by RecordingService.recordingState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.screen.route,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    contentPadding = padding,
                    onNavigateToTrack = { tripId ->
                        navController.navigate(Screen.Track.createRoute(tripId))
                    },
                    onNavigateToStats = { tripId ->
                        navController.navigate(Screen.Statistics.createRoute(tripId))
                    }
                )
            }
            composable(
                route = Screen.Track.route,
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) { backStack ->
                val tripId = backStack.arguments?.getLong("tripId") ?: return@composable
                TrackScreen(tripId = tripId, onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.Statistics.route,
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) { backStack ->
                val tripId = backStack.arguments?.getLong("tripId") ?: return@composable
                StatisticsScreen(tripId = tripId, onBack = { navController.popBackStack() })
            }
            composable(Screen.Regatta.route) {
                RegattaScreen(contentPadding = padding)
            }
        }
    }
}
