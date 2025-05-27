package com.example.nexus.ui.model

data class Post(
    val id: Long? = null,
    val user: User? = null,
    val parent_post_id: Long? = null,
    val content: String = "",
    val visibility: String = "PUBLIC", // "PUBLIC", "FOLLOWERS_ONLY", "PRIVATE"
    val post_type: String = "TEXT", // "TEXT", "RESOURCE"
    val like_count: Int = 0, // Đổi tên từ likesCount để đồng bộ với database
    val comment_count: Int = 0,
    val repost_count: Int = 0,
    val quote_count: Int = 0,
    val isLiked: Boolean = false, // Không có trong database, dùng để mô phỏng trạng thái Like
    val comments: List<Comment> = emptyList(),
    val replies: List<Post> = emptyList(),
    val imageUri: String? = null, // Tạm giữ để mô phỏng Media
    val created_at: String? = null, // Lưu dạng String vì API thường trả về định dạng ISO
    val updated_at: String? = null
)

data class Comment(
    val id: Long? = null,
    val post_id: Long? = null,
    val user: User? = null,
    val parent_comment_id: Long? = null,
    val content: String = "",
    val like_count: Int = 0, // Đổi tên từ likesCount để đồng bộ với database
    val isLiked: Boolean = false, // Không có trong database, dùng để mô phỏng trạng thái Like
    val replies: List<Comment> = emptyList(),
    val created_at: String? = null,
    val updated_at: String? = null
)