package com.example.nexus.ui.login

import android.net.http.HttpException
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresExtension
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.nexus.network.AuthManager
import com.example.nexus.network.ErrorResponse
import com.example.nexus.network.LoginRequest
import com.example.nexus.network.RetrofitClient
import com.example.nexus.ui.model.CreateUserRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import retrofit2.HttpException as RetrofitHttpException

class LoginViewModel(private val authManager: AuthManager) : ViewModel(){

    @OptIn(UnstableApi::class)
    fun signIn (username :String, password: String, onResult: (Boolean,String?)->Unit){
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(username,password))
                authManager.saveTokens(response.access_token, response.refresh_token,response.user_id)
                val userResponse = RetrofitClient.apiService.getUserById(response.user_id)
                val user = if (userResponse.success) userResponse.data else null
                Log.d("LoginViewModel", "User data: $user")
                // Lưu auth data
                authManager.saveAuthData(accessToken = response.access_token,
                    refreshToken = response.refresh_token,
                    userId = response.user_id,
                    user = user)
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
                    onResult(true, null)
                } else {
                    onResult(false, response.message ?: "Đăng ký không thành công")
                }
            }
            catch (e: RetrofitHttpException) {
                // Parse error response để lấy detail
                val errorMessage = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody != null) {
                        val gson = Gson()
                        val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                        errorResponse.detail ?: errorResponse.title ?: "Đăng ký không thành công"
                    } else {
                        "Đăng ký không thành công"
                    }
                } catch (parseException: Exception) {
                    "Đăng ký không thành công"
                }
                onResult(false, errorMessage)
            }
            catch (e: Exception) {
                onResult(false, e.message ?: "Đăng ký không thành công")
            }
        }
    }
//    fun signUp(username:String,email: String,fullName:String, password: String, onResult: (Boolean, String?) -> Unit) {
//        viewModelScope.launch {
//            try{
//                val response = RetrofitClient.apiService.registerUser(CreateUserRequest(username, email, fullName, password))
//                if (response.success) {
//                    // Đăng ký thành công, có thể tự động đăng nhập hoặc thông báo cho người dùng
//                    onResult(true, null)
//                } else {
//                    // Xử lý lỗi nếu có
//                    onResult(false, response.message ?: "Đăng ký không thành công")
//                }
//            }
//            catch (e: Exception) {
//                onResult(false, e.message)
//            }
//        }
//    }
}