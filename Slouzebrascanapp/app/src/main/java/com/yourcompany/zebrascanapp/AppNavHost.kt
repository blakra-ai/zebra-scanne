package com.yourcompany.zebrascanapp

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourcompany.zebrascanapp.ui.login.LoginScreen

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourcompany.zebrascanapp.ui.dashboard.DashboardScreen

@Composable
fun AppNavHost() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {

        composable("login") {
            LoginScreen(
                navController = navController
            )
        }

        composable("home") {
            DashboardScreen()
        }
    }
}

