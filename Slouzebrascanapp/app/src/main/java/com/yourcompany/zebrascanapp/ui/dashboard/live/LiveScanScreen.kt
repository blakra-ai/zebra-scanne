package com.yourcompany.zebrascanapp.ui.dashboard.live
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infomaniak.zebrascansdk.ZebraScannerCompose
import com.infomaniak.zebrascansdk.rememberScannerBinding

import com.yourcompany.zebrascanapp.data.local.entity.ScanEntity
import com.yourcompany.zebrascanapp.ui.dashboard.live.LiveScanViewModel
@Composable
fun LiveScanScreen(
    viewModel: LiveScanViewModel = hiltViewModel()
) {
    val scannerBinding = rememberScannerBinding(profileName = "ZebraProfile")

    var lastScannedCode by remember { mutableStateOf("") }
    var deviceStatus by remember { mutableStateOf("Disconnected") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Scanner Status: $deviceStatus",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Last Scan: $lastScannedCode",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        ZebraScannerCompose(
            scannerBinding = scannerBinding,

            onDecodedData = { data ->
                lastScannedCode = data

                // ارسال به ViewModel
                viewModel.handleScan(data)
            },

            onErrorResulted = { error ->
                deviceStatus = "Error: $error"
            },

            onStatusChanged = { status ->
                deviceStatus = status
            },

            onReceiveDataWedgeVersion = { version ->
                // optional
            }
        )
    }
}
