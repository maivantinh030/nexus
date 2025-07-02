package com.example.nexus

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.nexus.network.AuthManager
import com.example.nexus.network.RetrofitClient
import com.example.nexus.ui.theme.NexusTheme
import com.example.nexus.ui.timeline.TimelineViewModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@UnstableApi
class MainActivity : ComponentActivity() {


    companion object {
        private const val TAG = "MainActivity"
    }
    private lateinit var authManager: AuthManager
    private lateinit var timelineViewModel: TimelineViewModel

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authManager = AuthManager(this)
        RetrofitClient.initialize(authManager)
        val timelineViewModel = TimelineViewModel(authManager, this)
        // Get FCM token directly
        getAndSaveFCMToken()
        enableEdgeToEdge()
        setContent {
            val isLoggedIn by authManager.isLoggedIn.collectAsState()
            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) {
                    refreshFCMTokenOnLogin()
                }
            }
            NexusTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFB8D4E3), Color(0xFFE8F4F8))
                            )
                        )
                ) {
                    AppNavHost(navController = rememberNavController(), viewModel = timelineViewModel)
                }
            }

        }
    }

    private fun getAndSaveFCMToken() {
        lifecycleScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM Token: $token")

                // Save token locally
                getSharedPreferences("nexus_prefs", MODE_PRIVATE).edit()
                    .putString("fcm_token", token)
                    .apply()

                // Send to server if user is logged in
                if (authManager.isUserLoggedIn()) {
                    sendFCMTokenToServer()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting FCM token", e)
            }
        }
    }
    private fun sendFCMTokenToServer() {
        lifecycleScope.launch {
            try {
                val token = getSharedPreferences("nexus_prefs", MODE_PRIVATE)
                    .getString("fcm_token", null)

                if (token != null) {
                    val response = RetrofitClient.apiService.updateFCMToken(
                        mapOf("fcmToken" to token)
                    )
                    if (response.success) {
                        Log.d(TAG, "FCM token sent to server successfully")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending FCM token to server", e)
            }
        }
    }
    private fun refreshFCMTokenOnLogin() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "=== REFRESH TOKEN ON LOGIN ===")

                // Xóa token cũ
                FirebaseMessaging.getInstance().deleteToken().await()

                // Lấy token mới
                val newToken = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "New token after login: ${newToken?.take(20)}...")

                // Lưu và gửi lên server
                getSharedPreferences("nexus_prefs", MODE_PRIVATE).edit()
                    .putString("fcm_token", newToken)
                    .apply()

                sendFCMTokenToServer()

            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing token on login", e)
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(navController: NavHostController, viewModel: TimelineViewModel) {
    // Sử dụng AppNavGraph từ NavGraph.kt
    AppNavGraph(viewModel = viewModel)
}