package com.example.nexus.ui.login

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nexus.network.AuthManager
import com.example.nexus.network.LoginRequest
import com.example.nexus.network.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController,  authManager: AuthManager){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ){
        Text(
            text = "Login to Nexus",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = email,
            onValueChange = {email = it},
            label ={Text("Username")},
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = password,
            onValueChange = {password = it},
            label ={Text("Password")},
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        Log.d("LoginScreen", "Starting login with email: $email")

                        val response = RetrofitClient.apiService.login(LoginRequest(email, password))

                        // Debug response - truy cập trực tiếp các field
                        Log.d("LoginScreen", "Response received:")
                        Log.d("LoginScreen", "Access token: ${response.access_token.take(20)}...")
                        Log.d("LoginScreen", "User ID: ${response.user_id}")
                        Log.d("LoginScreen", "Roles: ${response.roles}")

                        // Lưu tokens
                        authManager.saveTokens(response.access_token, response.refresh_token,response.user_id)

                        // Verify tokens were saved
                        val savedToken = authManager.getAccessToken()
                        Log.d("LoginScreen", "Token saved: ${savedToken?.take(20)}...")

                        // Navigate to home
                        navController.navigate("timeline") {
                            popUpTo("login") { inclusive = true }
                        }

                        Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("LoginScreen", "Exception during login", e)

                        // Chi tiết lỗi
                        when (e) {
                            is java.net.ConnectException -> {
                                errorMessage = "Không thể kết nối đến server. Kiểm tra IP và port."
                            }
                            is java.net.SocketTimeoutException -> {
                                errorMessage = "Timeout. Server phản hồi quá chậm."
                            }
                            is retrofit2.HttpException -> {
                                errorMessage = when (e.code()) {
                                    404 -> "API endpoint không tồn tại. Kiểm tra đường dẫn API."
                                    401 -> "Sai tên đăng nhập hoặc mật khẩu."
                                    500 -> "Lỗi server internal."
                                    else -> "HTTP Error: ${e.code()} - ${e.message()}"
                                }
                                Log.e("LoginScreen", "HTTP Error body: ${e.response()?.errorBody()?.string()}")
                            }
                            else -> {
                                errorMessage = "Lỗi không xác định: ${e.javaClass.simpleName}"
                            }
                        }
                    } finally {
                        isLoading = false
                    }
                }


            },
            modifier = Modifier.width(180.dp)) {
            Text("Sign in")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { navController.navigate("signup") }) {
            Text("Don't have an account? Sign Up")
        }

    }
}
