package com.sailapp.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Track : Screen("track/{tripId}") {
        fun createRoute(tripId: Long) = "track/$tripId"
    }
    object Statistics : Screen("statistics/{tripId}") {
        fun createRoute(tripId: Long) = "statistics/$tripId"
    }
    object Regatta : Screen("regatta")
}
