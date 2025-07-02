package com.example.nexus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.R
import com.example.nexus.network.AuthManager
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    authManager: AuthManager
) {
    val isLoggedIn by authManager.isLoggedIn.collectAsState()
    var isChecking by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // ✅ Hiển thị splash screen ít nhất 2 giây
        delay(2000)

        // ✅ Kiểm tra authentication
        if (isLoggedIn && authManager.isUserLoggedIn()) {
            // Đã đăng nhập, kiểm tra token
            val token = authManager.getAccessToken()
            if (!token.isNullOrBlank()) {
                // Token còn hạn
                onNavigateToMain()
            } else {
                // Token hết hạn, thử refresh
                val refreshSuccess = try {
                    authManager.refreshToken()
                } catch (e: Exception) {
                    false
                }

                if (refreshSuccess) {
                    onNavigateToMain()
                } else {
                    onNavigateToLogin()
                }
            }
        } else {
            // Chưa đăng nhập
            onNavigateToLogin()
        }

        isChecking = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E88E5),
                        Color(0xFF42A5F5),
                        Color(0xFF64B5F6)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ✅ App Logo - thay bằng logo của bạn
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(120.dp)
            )

            // ✅ App Name
            Text(
                text = "Yapping",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "Kết nối & chia sẻ",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyLarge
            )

            if (isChecking) {
                // ✅ Loading indicator
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.size(32.dp)
                )

                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}