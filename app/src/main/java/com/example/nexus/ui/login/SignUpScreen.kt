package com.example.nexus.ui.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
@Composable
fun SignUpScreen(navController: NavController
                 , viewModel: LoginViewModel) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFB8D4E3), Color(0xFFE8F4F8))
                )
            )){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Đăng ký tài khoản",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Tên tài khoản") },
                modifier = Modifier.fillMaxWidth(),
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
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
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
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Họ và tên") },
                modifier = Modifier.fillMaxWidth(),
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
                    .fillMaxWidth(),
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
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Xác nhận mật khẩu") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.8f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                    focusedBorderColor = Color(0xFF7BB3D3),
                    unfocusedBorderColor = Color.Transparent
                ),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (password == confirmPassword) {
                        viewModel.signUp(username, email, fullName, password) { success, message ->
                            if (success) {
                                Toast.makeText(context, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                navController.navigate("login")
                            } else {
                                var displayMessage = message
                                if(displayMessage.equals("Username already exists")) {
                                    displayMessage = "Tên tài khoản đã tồn tại"
                                }
                                if(displayMessage.equals("Email already exists")) {
                                    displayMessage = "Email đã được sử dụng"
                                }
                                if((displayMessage?.contains("must be a well-formed email address") == true)) {
                                    displayMessage = "Email không hợp lệ"
                                }
                                if((displayMessage?.contains("size must be between 8 and 255") == true)) {
                                    displayMessage = "Mật khẩu quá ngắn"
                                }
                                if((displayMessage?.contains("size must be between 3 and 50") == true)) {
                                    displayMessage = "Vui lòng nhập tên tài khoản từ 3 đến 50 ký tự"
                                }
                                Toast.makeText(context, displayMessage ?: "Sign Up Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Mật khẩu và mật khẩu nhập lại không giống nhau!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.width(180.dp).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7BB3D3)
                )
            ) {
                Text("Đăng ký")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { navController.navigate("login") }) {
                Text("Đã có tài khoản? Đăng nhập")
            }
        }
    }
}
