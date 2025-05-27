package com.example.nexus.network

import com.example.nexus.ui.model.Post
import retrofit2.http.GET

interface ApiService {
    @GET("api/posts")
    suspend fun getPosts(): List<Post>
}