package com.example.nexus.ui.model

data class Like(
    val id: Long? = null, // ID của lượt thích, nullable vì có thể chưa được tạo
    val userId: Long, // ID của người dùng thực hiện lượt thích
    val username: String, // Tên người dùng
    val userFullName: String, // Tên đầy đủ của người dùng
    val userProfilePicture: String, // URL ảnh đại diện
    val targetType: String, // Loại đối tượng (POST, COMMENT, v.v.)
    val targetId: Long, // ID của đối tượng được thích
    val createdAt: String// Thời gian tạo, nullable vì có thể chưa được gán
)
