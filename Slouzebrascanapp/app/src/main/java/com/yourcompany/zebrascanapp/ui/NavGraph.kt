package com.yourcompany.zebrascanapp.ui


import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yourcompany.zebrascanapp.ui.dashboard.DashboardScreen
import com.yourcompany.zebrascanapp.ui.login.LoginScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController)
        }

        composable("home") {
            DashboardScreen()
        }
    }
}
