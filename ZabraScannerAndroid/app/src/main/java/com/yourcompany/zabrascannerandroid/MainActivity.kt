package com.yourcompany.zabrascannerandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.yourcompany.zabrascannerandroid.ui.theme.ZabraScannerAndroidTheme
import com.yourcompany.zabrascannerandroid.data.api.ApiClient
import com.yourcompany.zabrascannerandroid.data.local.AppDatabase
import com.yourcompany.zabrascannerandroid.data.local.TokenManager
import com.yourcompany.zabrascannerandroid.ui.screens.HomeScreen
import com.yourcompany.zabrascannerandroid.ui.screens.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ApiClient and TokenManager
        ApiClient.init(this)
        val tokenManager = TokenManager.getInstance(this)
        val database = AppDatabase.getDatabase(this)
        
        enableEdgeToEdge()
        setContent {
            ZabraScannerAndroidTheme {
                var isLoggedIn by remember { mutableStateOf(tokenManager.isLoggedIn()) }
                val username = remember { mutableStateOf(tokenManager.getUsername()) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (!isLoggedIn) {
                        LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            onLoginSuccess = { token ->
                                tokenManager.saveToken(token)
                                // Optionally save user info if available in response
                                // For now, we just update the login state
                                isLoggedIn = true
                            }
                        )
                    } else {
                        HomeScreen(
                            username = username.value,
                            onLogout = {
                                tokenManager.clearAll()
                                isLoggedIn = false
                            },
                            modifier = Modifier.padding(innerPadding),
                            database = database
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ZabraScannerAndroidTheme {
        Greeting("Android")
    }
}