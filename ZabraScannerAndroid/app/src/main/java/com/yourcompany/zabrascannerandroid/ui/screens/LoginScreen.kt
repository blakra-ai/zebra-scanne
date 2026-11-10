package com.yourcompany.zabrascannerandroid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.yourcompany.zabrascannerandroid.data.api.ApiClient
import com.yourcompany.zabrascannerandroid.data.model.LoginResponse
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val response: LoginResponse) : LoginUiState()
    data class Error(val errorType: ErrorType, val message: String) : LoginUiState()
}

sealed class ErrorType {
    object InvalidCredentials : ErrorType()
    object UserNotFound : ErrorType()
    object NetworkError : ErrorType()
    object ServerError : ErrorType()
    object TimeoutError : ErrorType()
    object UnknownError : ErrorType()
    object ValidationError : ErrorType()
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var uiState by remember { mutableStateOf<LoginUiState>(LoginUiState.Idle) }
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val emailPattern = remember {
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }

    val isEmailValid = remember(username) {
        username.isEmpty() || emailPattern.matches(username)
    }

    val isFormValid = remember(username, password, isEmailValid) {
        username.isNotBlank() && password.isNotBlank() && isEmailValid && emailPattern.matches(username)
    }

    // Show Snackbar for errors
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Error) {
            val errorState = uiState as LoginUiState.Error
            val result = snackbarHostState.showSnackbar(
                message = errorState.message,
                actionLabel = "DISMISS",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.Dismissed || result == SnackbarResult.ActionPerformed) {
                uiState = LoginUiState.Idle
            }
        } else if (uiState is LoginUiState.Success) {
            val successMessage = when ((uiState as LoginUiState.Success).response.message) {
                null -> "Login successful!"
                else -> (uiState as LoginUiState.Success).response.message!!
            }
            snackbarHostState.showSnackbar(
                message = successMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please sign in to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Animated Error Alert Card
            AnimatedVisibility(
                visible = uiState is LoginUiState.Error,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                if (uiState is LoginUiState.Error) {
                    ErrorAlertCard(
                        errorType = (uiState as LoginUiState.Error).errorType,
                        message = (uiState as LoginUiState.Error).message,
                        onDismiss = { uiState = LoginUiState.Idle }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Animated Success Alert Card
            AnimatedVisibility(
                visible = uiState is LoginUiState.Success,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                if (uiState is LoginUiState.Success) {
                    SuccessAlertCard(
                        message = (uiState as LoginUiState.Success).response.message ?: "Login successful!"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    emailTouched = true
                    if (uiState is LoginUiState.Error) {
                        uiState = LoginUiState.Idle
                    }
                },
                label = { Text("Email") },
                placeholder = { Text("name@example.com") },
                singleLine = true,
                isError = emailTouched && username.isNotEmpty() && !isEmailValid,
                supportingText = {
                    if (emailTouched && username.isNotEmpty() && !isEmailValid) {
                        Text(
                            text = "Please enter a valid email address",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                enabled = uiState !is LoginUiState.Loading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordTouched = true
                    if (uiState is LoginUiState.Error) {
                        uiState = LoginUiState.Idle
                    }
                },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                singleLine = true,
                enabled = uiState !is LoginUiState.Loading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isFormValid) {
                            performLogin(username, password) { newState ->
                                uiState = newState
                                if (newState is LoginUiState.Success) {
                                    val token = newState.response.data?.token ?: newState.response.token ?: ""
                                    onLoginSuccess(token)
                                }
                            }
                        }
                    }
                ),
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    performLogin(username, password) { newState ->
                        uiState = newState
                        if (newState is LoginUiState.Success) {
                            val token = newState.response.data?.token ?: newState.response.token ?: ""
                            onLoginSuccess(token)
                        }
                    }
                },
                enabled = isFormValid && uiState !is LoginUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ErrorAlertCard(
    errorType: ErrorType,
    message: String,
    onDismiss: () -> Unit
) {
    val (icon, title, containerColor, contentColor) = when (errorType) {
        is ErrorType.InvalidCredentials -> Quad(
            Icons.Default.ErrorOutline,
            "Authentication Failed",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        is ErrorType.UserNotFound -> Quad(
            Icons.Default.Warning,
            "User Not Found",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        is ErrorType.NetworkError -> Quad(
            Icons.Default.SignalWifiOff,
            "Connection Error",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        is ErrorType.TimeoutError -> Quad(
            Icons.Default.CloudOff,
            "Request Timeout",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        is ErrorType.ServerError -> Quad(
            Icons.Default.Warning,
            "Server Error",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        is ErrorType.ValidationError -> Quad(
            Icons.Default.ErrorOutline,
            "Validation Error",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        is ErrorType.UnknownError -> Quad(
            Icons.Default.Warning,
            "Error",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text(
                        text = "x",
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SuccessAlertCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "Success",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Helper data class to return 4 values
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun performLogin(
    username: String,
    password: String,
    onResult: (LoginUiState) -> Unit
) {
    onResult(LoginUiState.Loading)

    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        try {
            val response = ApiClient.login(username, password)
            if (response.success == true || response.data?.token != null) {
                onResult(LoginUiState.Success(response))
            } else {
                val (errorType, errorMessage) = parseApiError(response)
                onResult(LoginUiState.Error(errorType, errorMessage))
            }
        } catch (e: Exception) {
            val (errorType, errorMessage) = parseExceptionError(e)
            onResult(LoginUiState.Error(errorType, errorMessage))
        }
    }
}

private fun parseApiError(response: LoginResponse): Pair<ErrorType, String> {
    val apiMessage = response.message?.lowercase() ?: ""

    return when {
        apiMessage.contains("email") && apiMessage.contains("password") ->
            ErrorType.InvalidCredentials to "The email or password you entered is incorrect. Please check your credentials and try again."

        apiMessage.contains("password") ->
            ErrorType.InvalidCredentials to "Invalid password. Please check your password and try again."

        apiMessage.contains("email") ->
            ErrorType.ValidationError to "Invalid email format. Please enter a valid email address."

        apiMessage.contains("user not found") || apiMessage.contains("user") ->
            ErrorType.UserNotFound to "User not found. Please check your email or sign up for a new account."

        apiMessage.contains("credentials") ->
            ErrorType.InvalidCredentials to "Invalid credentials. Please verify your information and try again."

        apiMessage.contains("blocked") || apiMessage.contains("suspend") ->
            ErrorType.InvalidCredentials to "Your account has been blocked or suspended. Please contact support."

        apiMessage.contains("verify") || apiMessage.contains("confirm") ->
            ErrorType.ValidationError to "Please verify your email address before logging in."

        apiMessage.contains("locked") ->
            ErrorType.InvalidCredentials to "Your account has been locked due to multiple failed attempts. Please try again later."

        !response.message.isNullOrBlank() ->
            ErrorType.UnknownError to response.message

        else ->
            ErrorType.UnknownError to "Login failed. Please try again later."
    }
}

private fun parseExceptionError(e: Exception): Pair<ErrorType, String> {
    return when (e) {
        is HttpException -> {
            when (e.code()) {
                400 -> ErrorType.ValidationError to "Invalid request. Please check your information."
                401 -> ErrorType.InvalidCredentials to "Invalid credentials. Please check your email and password."
                403 -> ErrorType.InvalidCredentials to "Access denied. You don't have permission to log in."
                404 -> ErrorType.UserNotFound to "Login service not found. Please contact support."
                429 -> ErrorType.UnknownError to "Too many login attempts. Please wait a moment and try again."
                in 500..599 -> ErrorType.ServerError to "Server error (${e.code()}). Our team is working on it. Please try again later."
                else -> ErrorType.UnknownError to "An unexpected error occurred (${e.code()}). Please try again."
            }
        }
        is UnknownHostException, is IOException ->
            ErrorType.NetworkError to "No internet connection. Please check your network settings and try again."

        is SocketTimeoutException ->
            ErrorType.TimeoutError to "Connection timed out. The server is taking too long to respond. Please try again."

        else -> {
            val message = e.message?.lowercase() ?: ""
            when {
                message.contains("network") || message.contains("connection") ->
                    ErrorType.NetworkError to "Network error. Please check your internet connection."

                message.contains("timeout") ->
                    ErrorType.TimeoutError to "Request timed out. Please try again."

                message.contains("unable to resolve host") || message.contains("unknown host") ->
                    ErrorType.NetworkError to "Cannot reach the server. Please check your connection and try again."

                message.contains("ssl") || message.contains("certificate") ->
                    ErrorType.NetworkError to "Security connection error. Please try again later."

                else ->
                    ErrorType.UnknownError to "An unexpected error occurred: ${e.message ?: "Unknown error"}. Please try again."
            }
        }
    }
}
