package com.example.nexus.ui.model

data class CommentResponse(
    val status: Int,
    val success: Boolean,
    val message: String? = null,
    val data: CommentData
)

data class CommentData(
    val content: List<Comment>
)

data class Comment(
    val id: Long,
    val postId: Long,
    val userId: Long,
    val username: String,
    val userFullName: String?,
    val userProfilePicture: String?,
    val content: String,
    var likeCount: Long,
    val createdAt: String,
    val updatedAt: String,
    val replies: List<Comment> = emptyList(),
    val parentCommentId: Long?,
    var isLiked: Boolean = false
)
