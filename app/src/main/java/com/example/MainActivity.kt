package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.JarvisTab
import com.example.ui.JarvisViewModel
import com.example.ui.components.JarvisBottomNav
import com.example.ui.components.JarvisTopAppBar
import com.example.ui.screens.AgendaScreen
import com.example.ui.screens.CoreHudScreen
import com.example.ui.screens.MultiDeviceSyncScreen
import com.example.ui.screens.SystemDiagnosticsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                JarvisApp()
            }
        }
    }
}

@Composable
fun JarvisApp(
    viewModel: JarvisViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val voiceFeedbackEnabled by viewModel.speechManager.voiceEnabled.collectAsState()

    // Back button navigation: return to CORE if on secondary tab
    BackHandler(enabled = selectedTab != JarvisTab.CORE) {
        viewModel.selectTab(JarvisTab.CORE)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            JarvisTopAppBar(
                telemetry = telemetry,
                voiceFeedbackEnabled = voiceFeedbackEnabled,
                onToggleVoiceFeedback = { viewModel.toggleVoiceFeedback() }
            )
        },
        bottomBar = {
            JarvisBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        when (selectedTab) {
            JarvisTab.CORE -> CoreHudScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            JarvisTab.AGENDA -> AgendaScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            JarvisTab.SYSTEM -> SystemDiagnosticsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            JarvisTab.SYNC -> MultiDeviceSyncScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
