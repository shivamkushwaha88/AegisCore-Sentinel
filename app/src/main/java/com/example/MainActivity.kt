package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AegisBottomNavigationBar
import com.example.ui.components.AegisTopBar
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.InspectScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.TrustScreen
import com.example.ui.theme.AegisVoid
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.NavigationTab
import com.example.viewmodel.SentinelViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AegisMainScreen()
            }
        }
    }
}

@Composable
fun AegisMainScreen(
    viewModel: SentinelViewModel = viewModel()
) {
    val currentTab by viewModel.selectedTab.collectAsState()
    val context = LocalContext.current

    // Handle back button: if in a sub-tab, return to OVERVIEW
    BackHandler(enabled = currentTab != NavigationTab.OVERVIEW) {
        viewModel.selectTab(NavigationTab.OVERVIEW)
    }

    Scaffold(
        containerColor = AegisVoid,
        topBar = {
            AegisTopBar(
                onExportReport = {
                    viewModel.exportReport { reportText ->
                        shareReport(context, reportText)
                    }
                }
            )
        },
        bottomBar = {
            AegisBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { tab -> viewModel.selectTab(tab) }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.OVERVIEW -> OverviewScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { tab -> viewModel.selectTab(tab) }
                )
                NavigationTab.APPS -> AppsScreen(viewModel = viewModel)
                NavigationTab.INSPECT -> InspectScreen(viewModel = viewModel)
                NavigationTab.TRUST -> TrustScreen(viewModel = viewModel)
                NavigationTab.HISTORY -> HistoryScreen(viewModel = viewModel)
            }
        }
    }
}

private fun shareReport(context: Context, text: String) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val chooser = Intent.createChooser(intent, "Share AegisCore Audit Report")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
