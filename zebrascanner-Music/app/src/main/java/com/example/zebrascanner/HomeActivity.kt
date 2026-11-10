package com.example.zebrascanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.zebrascanner.data.database.AppDatabase
import com.example.zebrascanner.ui.components.GradientCard
import com.example.zebrascanner.ui.components.ModernButton
import com.example.zebrascanner.ui.theme.ZebrascannerTheme
import com.example.zebrascanner.utils.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons as MaterialIcons
import androidx.compose.material.icons.filled.Home as HomeIcon
import androidx.compose.material.icons.filled.List as ListIcon
import androidx.compose.material.icons.filled.Settings as SettingsIcon

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check if user is logged in
        if (!TokenManager.isLoggedIn(this)) {
            navigateToLogin()
            return
        }
        
        setContent {
            ZebrascannerTheme {
                MainScreenWithNavigation(
                    onLogout = {
                        lifecycleScope.launch {
                            // Clear all database storage
                            val database = AppDatabase.getDatabase(this@HomeActivity)
                            database.shippedOrderDao().deleteAllOrders()
                            
                            // Clear token and login status
                            TokenManager.logout(this@HomeActivity)
                            
                            // Navigate to login
                            navigateToLogin()
                        }
                    }
                )
            }
        }
    }
    
    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithNavigation(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val navigationItems = listOf(
        NavigationItem("Home", Icons.Rounded.Dashboard, "home"),
        NavigationItem("History", Icons.Rounded.History, "list"),
        NavigationItem("Profile", Icons.Rounded.Settings, "settings")
    )
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "ZEBRA SCANNER",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { /* Refresh or something */ }) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                navigationItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            androidx.compose.animation.Crossfade(targetState = selectedTab, label = "TabSwitch") { tabIndex ->
                when (tabIndex) {
                    0 -> HomeScreen()
                    1 -> ListScreen()
                    2 -> SettingsScreen(onLogout = onLogout)
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    var numberInput by remember { mutableStateOf("") }
    var submittedNumber by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    var orderCount by remember { mutableStateOf(0) }
    
    val database = remember { AppDatabase.getDatabase(context) }
    
    LaunchedEffect(Unit) {
        orderCount = database.shippedOrderDao().getOrderCount()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stats Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GradientCard(
                title = "Shipped",
                value = orderCount.toString(),
                icon = Icons.Rounded.LocalShipping,
                gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6)),
                modifier = Modifier.weight(1f)
            )
            GradientCard(
                title = "Status",
                value = "Active",
                icon = Icons.Rounded.Sensors,
                gradientColors = listOf(Color(0xFF0369A1), Color(0xFF0EA5E9)),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Input Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ship New Order",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Scan or type the tracking number below",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                OutlinedTextField(
                    value = numberInput,
                    onValueChange = { 
                        numberInput = it
                        errorMessage = null
                        successMessage = null
                    },
                    label = { Text("Tracking Number") },
                    placeholder = { Text("EX: TRACK-XXXX-XXXX") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null,
                    enabled = !isLoading,
                    leadingIcon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
                
                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                AnimatedVisibility(visible = successMessage != null) {
                    Card(
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertizontally) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF166534))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Order marked as shipped successfully!",
                                color = Color(0xFF166534),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                ModernButton(
                    text = "MARK AS SHIPPED",
                    onClick = {
                        if (numberInput.isBlank()) {
                            errorMessage = "Please enter a tracking number"
                        } else {
                            isLoading = true
                            errorMessage = null
                            successMessage = null
                            
                            scope.launch {
                                try {
                                    val token = TokenManager.getToken(context)
                                    val response = com.example.zebrascanner.data.api.RetrofitClient.apiService.markOrderShipped(
                                        authorization = "Bearer $token",
                                        request = com.example.zebrascanner.data.model.MarkShippedRequest(trackingNumber = numberInput)
                                    )
                                    
                                    isLoading = false
                                    if (response.isSuccessful) {
                                        val data = response.body()?.data
                                        if (data != null) {
                                            // Check if tracking number already exists locally to prevent duplicates
                                            val existingOrder = database.shippedOrderDao().getOrderByTrackingNumber(data.trackingNumber)
                                            if (existingOrder == null) {
                                                val orderEntity = com.example.zebrascanner.data.database.ShippedOrderEntity(
                                                    orderNumber = data.orderNumber,
                                                    trackingNumber = data.trackingNumber,
                                                    shippedOn = data.shippedOn,
                                                    message = data.message
                                                )
                                                database.shippedOrderDao().insertOrder(orderEntity)
                                                orderCount = database.shippedOrderDao().getOrderCount()
                                            }
                                            successMessage = data.message
                                            numberInput = ""
                                        }
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        when (response.code()) {
                                            409 -> {
                                                val error = try { Gson().fromJson(errorBody, com.example.zebrascanner.data.model.AlreadyShippedResponse::class.java) } catch (e: Exception) { null }
                                                errorMessage = error?.data?.message ?: "Order already shipped."
                                            }
                                            422 -> {
                                                val error = try { Gson().fromJson(errorBody, com.example.zebrascanner.data.model.ValidationErrorResponse::class.java) } catch (e: Exception) { null }
                                                errorMessage = error?.trackingNumber?.firstOrNull() ?: "Invalid tracking number."
                                            }
                                            else -> {
                                                errorMessage = "Shipping failed. Please check the tracking number."
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = "Network error: ${e.localizedMessage ?: "Please try again."}"
                                }
                            }
                        }
                    },
                    isLoading = isLoading
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Info Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Auto-Sync Active", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Your data is safe and encrypted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun ListScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { com.example.zebrascanner.data.database.AppDatabase.getDatabase(context) }
    val orders by database.shippedOrderDao().getAllOrders().androidx.compose.runtime.collectAsState(initial = emptyList())
    
    var selectedOrderIds by remember { mutableStateOf(setOf<Long>()) }
    var dateFilter by remember { mutableStateOf("") }
    var timeFilter by remember { mutableStateOf("") }
    
    val filteredOrders = orders.filter { order ->
        val dateMatches = dateFilter.isEmpty() || order.shippedOn.contains(dateFilter)
        val timeMatches = timeFilter.isEmpty() || order.shippedOn.contains(timeFilter)
        dateMatches && timeMatches
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertizontally
        ) {
            Column {
                Text(
                    text = "Shipping History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredOrders.size} orders found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Row {
                IconButton(onClick = {
                    if (selectedOrderIds.isEmpty()) {
                        android.widget.Toast.makeText(context, "Select items to export", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        val ordersToExport = orders.filter { it.id in selectedOrderIds }
                        com.example.zebrascanner.utils.PdfExportUtil.exportOrdersToPdf(
                            context, ordersToExport,
                            onSuccess = { file -> android.widget.Toast.makeText(context, "PDF saved to ${file.absolutePath}", android.widget.Toast.LENGTH_LONG).show() },
                            onError = { error -> android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show() }
                        )
                    }
                }) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                }
                
                IconButton(onClick = {
                    if (selectedOrderIds.isEmpty()) {
                        android.widget.Toast.makeText(context, "Select items to print", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        val ordersToExport = orders.filter { it.id in selectedOrderIds }
                        com.example.zebrascanner.utils.PdfExportUtil.exportOrdersToPdf(
                            context, ordersToExport,
                            onSuccess = { file ->
                                val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                                val printAdapter = object : android.print.PrintDocumentAdapter() {
                                    override fun onLayout(oldAttributes: android.print.PrintAttributes?, newAttributes: android.print.PrintAttributes?, cancellationSignal: android.os.CancellationSignal?, callback: LayoutResultCallback?, extras: android.os.Bundle?) {
                                        val builder = android.print.PrintDocumentInfo.Builder("ShippedOrders").setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                        callback?.onLayoutFinished(builder.build(), true)
                                    }
                                    override fun onWrite(pages: Array<out android.print.PageRange>?, destination: android.os.ParcelFileDescriptor?, cancellationSignal: android.os.CancellationSignal?, callback: WriteResultCallback?) {
                                        val input = java.io.FileInputStream(file)
                                        val output = java.io.FileOutputStream(destination?.fileDescriptor)
                                        input.copyTo(output)
                                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                                        input.close(); output.close()
                                    }
                                }
                                printManager.print("Orders Report", printAdapter, null)
                            },
                            onError = { error -> android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show() }
                        )
                    }
                }) {
                    Icon(Icons.Rounded.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = dateFilter,
                onValueChange = { dateFilter = it },
                label = { Text("Filter Date") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Rounded.Event, null, modifier = Modifier.size(18.dp)) },
                singleLine = true
            )
            OutlinedTextField(
                value = timeFilter,
                onValueChange = { timeFilter = it },
                label = { Text("Filter Time") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(18.dp)) },
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertizontally,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertizontally) {
                Checkbox(
                    checked = filteredOrders.isNotEmpty() && selectedOrderIds.size == filteredOrders.size,
                    onCheckedChange = { isChecked ->
                        selectedOrderIds = if (isChecked) filteredOrders.map { it.id }.toSet() else emptySet()
                    }
                )
                Text(text = "Select All", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }
        
        if (filteredOrders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (dateFilter.isEmpty() && timeFilter.isEmpty()) "No history found" else "No matching results",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredOrders) { order ->
                    val isSelected = selectedOrderIds.contains(order.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertizontally
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { isChecked ->
                                    selectedOrderIds = if (isChecked) selectedOrderIds + order.id else selectedOrderIds - order.id
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Order #${order.orderNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = order.trackingNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(verticalAlignment = Alignment.CenterVertizontally) {
                                    Icon(Icons.Rounded.AccessTime, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = formatDate(order.shippedOn),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatDate(dateString: String): String {
    return try {
        // Simple formatting - you can enhance this with proper date formatting
        val parts = dateString.split("T")
        if (parts.size >= 2) {
            val datePart = parts[0]
            val timePart = parts[1].split(".")[0]
            "$datePart $timePart"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoSyncEnabled by remember { mutableStateOf(true) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Header
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Logistics Manager", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Active Session", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "App Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )
        
        SettingsToggle(
            title = "Notifications",
            description = "Get shipping updates",
            icon = Icons.Rounded.NotificationsActive,
            checked = notificationsEnabled,
            onCheckedChange = { notificationsEnabled = it }
        )
        
        SettingsToggle(
            title = "Cloud Sync",
            description = "Auto-sync with server",
            icon = Icons.Rounded.CloudUpload,
            checked = autoSyncEnabled,
            onCheckedChange = { autoSyncEnabled = it }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        ModernButton(
            text = "LOGOUT & CLEAR DATA",
            onClick = onLogout,
            containerColor = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Zebra Scanner v2.5.0 Premium",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun SettingsToggle(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertizontally
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = {
                    if (checked) Icon(Icons.Rounded.Check, null, Modifier.size(12.dp))
                }
            )
        }
    }
}
