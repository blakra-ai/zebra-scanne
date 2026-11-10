package com.yourcompany.zebrascanapp.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yourcompany.zebrascanapp.ui.dashboard.history.ScanHistoryScreen
import com.yourcompany.zebrascanapp.ui.dashboard.live.LiveScanScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {

    val tabs = listOf("Live Scan", "Scan History")
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Zebra Scanner Dashboard") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> LiveScanScreen()      // Tab 1
                1 -> ScanHistoryScreen()   // Tab 2
            }
        }
    }
}
