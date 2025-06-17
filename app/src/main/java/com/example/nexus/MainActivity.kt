package com.example.nexus

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.nexus.network.AuthManager
import com.example.nexus.network.RetrofitClient
import com.example.nexus.ui.theme.NexusTheme
import com.example.nexus.ui.timeline.TimelineViewModel

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authManager = AuthManager(this)
        val timelineViewModel = TimelineViewModel(authManager, this)
        RetrofitClient.initialize(authManager)

        setContent {
            val navController = rememberNavController()
            AppNavHost(
                navController = navController,
                viewModel = timelineViewModel
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(navController: NavHostController, viewModel: TimelineViewModel) {
    // Sử dụng AppNavGraph từ NavGraph.kt
    AppNavGraph(viewModel = viewModel)
}