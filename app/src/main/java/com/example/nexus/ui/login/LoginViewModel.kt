package com.example.nexus.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.network.AuthManager
import com.example.nexus.network.LoginRequest
import com.example.nexus.network.RetrofitClient
import com.example.nexus.ui.model.CreateUserRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(private val authManager: AuthManager) : ViewModel(){

    fun signIn (username :String, password: String, onResult: (Boolean,String?)->Unit){
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(username,password))
                authManager.saveTokens(response.access_token, response.refresh_token,response.user_id)
                val savedToken = authManager.getAccessToken()
                onResult(true, null)
            }
            catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
    fun signUp(username:String,email: String,fullName:String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try{
                val response = RetrofitClient.apiService.registerUser(CreateUserRequest(username, email, fullName, password))
                if (response.success) {
                    // Đăng ký thành công, có thể tự động đăng nhập hoặc thông báo cho người dùng
                    onResult(true, null)
                } else {
                    // Xử lý lỗi nếu có
                    onResult(false, response.message ?: "Đăng ký không thành công")
                }
            }
            catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}