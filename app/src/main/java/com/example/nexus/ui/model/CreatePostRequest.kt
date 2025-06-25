package com.example.nexus.ui.model


data class CreatePostRequest(
        val content: String,
        val visibility: String,
        val parentPostId: Long? = null
    )
