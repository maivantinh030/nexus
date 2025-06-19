package com.example.nexus.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL =
        "http://192.168.1.120:8080/" // Dùng cho emulator, thay bằng IP nếu chạy trên thiết bị thật
    const val MEDIA_BASE_URL = "http://192.168.1.120:8080/yapping"

    private lateinit var authManager: AuthManager

    fun initialize(authManager: AuthManager) {
        this.authManager = authManager
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        authManager.getAccessToken()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        val request = requestBuilder.build()
        Log.d("RetrofitClient", "Request: ${request.url}")
        val response: Response = chain.proceed(request)
        Log.d("RetrofitClient", "Response: ${response.code}")
        response
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // Đảm bảo cổng đúng
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}