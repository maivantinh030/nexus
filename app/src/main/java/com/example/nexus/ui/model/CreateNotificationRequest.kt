package com.example.nexus.ui.model

data class CreateNotificationRequest( // Thêm data class cho request tạo thông báo
    val userId: Long, // Người nhận thông báo
    val type: String, // e.g., LIKE_POST
    val targetType: String, // e.g., POST
    val targetId: Long, // ID bài viết
    val targetOwnerId: Long, // Chủ bài viết
    val actorId: Long
)
