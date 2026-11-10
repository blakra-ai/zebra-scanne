package com.yourcompany.zebrascanapp.ui.dashboard.history
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourcompany.zebrascanapp.data.local.entity.ScanEntity

@Composable
fun ScanHistoryScreen(
    viewModel: ScanHistoryViewModel = hiltViewModel()
) {
    val scans by viewModel.scans.collectAsState(initial = emptyList())

    if (scans.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text("No scans yet", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(scans) { scan ->
                ScanCard(scan)
            }
        }
    }
}

@Composable
fun ScanCard(scan: ScanEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Order Number: ${scan.orderNumber}", style = MaterialTheme.typography.titleMedium)
            Text("Tracking Number: ${scan.trackingNumber}", style = MaterialTheme.typography.bodyMedium)
            Text("Shipped On: ${scan.shippedOn}", style = MaterialTheme.typography.bodyMedium)
            Text("Message: ${scan.message}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
