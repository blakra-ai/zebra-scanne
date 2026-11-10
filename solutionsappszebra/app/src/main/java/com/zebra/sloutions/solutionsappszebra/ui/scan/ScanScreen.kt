package com.zebra.sloutions.solutionsappszebra.ui.scan

import com.zebra.sloutions.solutionsappszebra.data.local.entity.ScannedItemEntity


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScanScreen(viewModel: ScanViewModel) {
    val scannedItems by viewModel.scannedItems.collectAsState()

    var input by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Scan / Enter Tracking Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            if (input.isNotBlank()) {
                viewModel.scanBarcode(input) { success, message ->
                    statusMessage = message
                    if (success) input = ""
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Submit Scan")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = statusMessage, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Scanned Items:", style = MaterialTheme.typography.titleMedium)

        LazyColumn {
            items(scannedItems) { item ->
                ScannedItemRow(item)
            }
        }
    }
}

@Composable
fun ScannedItemRow(item: ScannedItemEntity) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("Tracking: ${item.trackingNumber}")
        Text("Order: ${item.orderNumber}")
        Text("Shipped On: ${item.shippedOn}")
    }
}
