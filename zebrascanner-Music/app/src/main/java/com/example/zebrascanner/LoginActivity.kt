package com.example.zebrascanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zebrascanner.data.api.RetrofitClient
import com.example.zebrascanner.data.model.ErrorResponse
import com.example.zebrascanner.data.model.LoginRequest
import com.example.zebrascanner.ui.theme.ZebrascannerTheme
import com.example.zebrascanner.utils.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if already logged in
        if (TokenManager.isLoggedIn(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            ZebrascannerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                        onLoginSuccess = { token ->
                            TokenManager.saveToken(this, token)
                            
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Rounded.Scanner,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "ZEBRA SCANNER",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Enter your credentials to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                errorMessage = null
            },
            label = { Text("Email Address") },
            placeholder = { Text("email@example.com") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            enabled = !isLoading,
            isError = errorMessage != null,
            leadingIcon = { Icon(androidx.compose.material.icons.Icons.Rounded.Email, null) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            placeholder = { Text("••••••••") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            enabled = !isLoading,
            isError = errorMessage != null,
            leadingIcon = { Icon(androidx.compose.material.icons.Icons.Rounded.Lock, null) }
        )

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        com.example.zebrascanner.ui.components.ModernButton(
            text = "SUBMIT & LOGIN",
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please fill in all fields"
                    return@ModernButton
                }

                isLoading = true
                errorMessage = null

                scope.launch {
                    try {
                        val response = RetrofitClient.apiService.login(
                            LoginRequest(username = email, password = password)
                        )

                        isLoading = false

                        if (response.isSuccessful && response.code() == 200) {
                            val loginResponse = response.body()
                            if (loginResponse?.data != null) {
                                val token = loginResponse.data.token
                                val message = loginResponse.data.message
                                
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                onLoginSuccess(token)
                            } else {
                                errorMessage = "Invalid response from server"
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            val error = try {
                                Gson().fromJson(errorBody, ErrorResponse::class.java)
                            } catch (e: Exception) {
                                null
                            }
                            errorMessage = error?.message ?: error?.error ?: "Login failed"
                        }
                    } catch (e: java.net.ConnectException) {
                        isLoading = false
                        errorMessage = "Cannot connect to server. Please check your internet."
                    } catch (e: java.net.SocketTimeoutException) {
                        isLoading = false
                        errorMessage = "Connection timed out. Try again later."
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = "Network error: ${e.localizedMessage ?: "Unknown error"}"
                    }
                }
            },
            isLoading = isLoading
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Protected by Zebra Cloud Security",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
