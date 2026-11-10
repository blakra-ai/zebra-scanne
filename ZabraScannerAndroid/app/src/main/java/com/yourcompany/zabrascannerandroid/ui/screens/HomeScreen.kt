package com.yourcompany.zabrascannerandroid.ui.screens

import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import com.yourcompany.zabrascannerandroid.data.api.ApiClient
import com.yourcompany.zabrascannerandroid.data.local.AppDatabase
import com.yourcompany.zabrascannerandroid.data.local.ShippedOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    username: String?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    database: AppDatabase? = null
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Submit", "List", "Settings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> SubmitTab(username = username)
                1 -> ListTab(database = database)
                2 -> SettingsTab(onLogout = onLogout)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubmitTab(
    username: String?
) {
    var inputNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main) }

    val database = remember { AppDatabase.getDatabase(context) }

    fun submitTrackingNumber() {
        val trackingNumber = inputNumber.trim()
        if (trackingNumber.isEmpty()) {
            Toast.makeText(context, "Please enter a tracking number", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.markShipped(trackingNumber)
                }
                val message = response.data?.message ?: "Success"
                
                response.data?.let { data ->
                    val order = ShippedOrder(
                        orderNumber = data.orderNumber ?: "",
                        trackingNumber = data.trackingNumber ?: trackingNumber,
                        shippedOn = data.shippedOn ?: ""
                    )
                    withContext(Dispatchers.IO) {
                        database.shippedOrderDao().insertOrder(order)
                    }
                }
                
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                inputNumber = ""
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Toast.makeText(context, "Error: ${e.code()} - $errorBody", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Welcome Back!",
            style = MaterialTheme.typography.headlineMedium
        )

        if (!username.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = username,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = inputNumber,
            onValueChange = { if (it.all { char -> char.isDigit() }) inputNumber = it },
            label = { Text("Enter Number") },
            placeholder = { Text("e.g. 12345") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { submitTrackingNumber() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Submit")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Information",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter a number above and click submit to process your request.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ListTab(
    database: AppDatabase?
) {
    val context = LocalContext.current
    val db = remember { database ?: AppDatabase.getDatabase(context) }
    val orders by db.shippedOrderDao().getAllOrders().collectAsState(initial = emptyList())

    if (orders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No shipped orders yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(orders) { order ->
                OrderCard(order = order)
            }
        }
    }
}

@Composable
private fun OrderCard(order: ShippedOrder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Order: ${order.orderNumber}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tracking: ${order.trackingNumber}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Shipped: ${order.shippedOn}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsTab(
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
    }
}
