package com.example.nexus.ui.model

data class CreateCommentRequest(
    val postId: Long,
    val content: String,
    val parentCommentId: Long?
)
data class CreateCommentResponse(
    val status: Int,
    val success: Boolean,
    val message: String? = null,
    val data: Comment
)