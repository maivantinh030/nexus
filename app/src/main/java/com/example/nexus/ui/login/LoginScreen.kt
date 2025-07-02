package com.example.nexus.ui.login

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel){
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFB8D4E3), Color(0xFFE8F4F8))
                )
            )

    ){
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center

        ){
            Text(
                text = "Đăng nhập",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(26.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Tài khoản") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.8f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                    focusedBorderColor = Color(0xFF7BB3D3),
                    unfocusedBorderColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.8f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                    focusedBorderColor = Color(0xFF7BB3D3),
                    unfocusedBorderColor = Color.Transparent
                ),
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
                    viewModel.signIn (
                        username = username,
                        password = password,
                        onResult = { success, message ->
                            isLoading = false
                            if (success) {
                                // Navigate to the timeline screen or dashboard
                                navController.navigate("timeline") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                errorMessage = message ?: "Login failed"
                                Toast.makeText(context, "Tài khoản hoặc mật khẩu sai", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

            },
                modifier = Modifier.width(180.dp).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7BB3D3)
                )

            )
            {
                Text("Đăng nhập")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { navController.navigate("signup") }) {
                Text("Bạn không có tài khoản? Đăng ký ngay")
            }

        }
    }
}


