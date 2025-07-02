package com.example.nexus.ui.model


data class CreatePostRequest(
        val content: String,
        val post_type: String = "TEXT",
        val visibility: String,
        val parentPostId: Long? = null
    )
