package com.example.nexus.ui.model

import android.net.Uri

data class CreatePostRequest(
        val content: String,
        val visibility: String,
        val parentPostId: Long? = null
    )
