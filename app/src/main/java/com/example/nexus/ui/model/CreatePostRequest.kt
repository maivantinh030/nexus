package com.example.nexus.ui.model

class CreatePostRequest {
    data class CreatePostRequest(
        val content: String,
        val visibility: String,
        val postType: Int?
    )
}