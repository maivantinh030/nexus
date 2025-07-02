package com.example.nexus.ui.model

data class Post(
    val id: Long? = null,
    val user: User? = null,
    val parentPostId: Long? = null,
    val content: String = "",
    val visibility: String = "PUBLIC", // "PUBLIC", "FOLLOWERS_ONLY", "PRIVATE"
    val postType: String = "TEXT", // "TEXT", "RESOURCE"
    var likeCount: Long? = 0, // Đổi tên từ likesCount để đồng bộ với database
    val commentCount: Int = 0,
    val repostCount: Int = 0,
    val quoteCount: Int = 0,
    var isLiked: Boolean = false, // Không có trong database, dùng để mô phỏng trạng thái Like
    val replies: List<Post> = emptyList(),
    val media: List<Media>? = null,
    val createdAt: String? = null, // Lưu dạng String vì API thường trả về định dạng ISO
    val updatedAt: String? = null
){
    // Helper function để check xem có phải bài share không
    fun isSharedPost(): Boolean = parentPostId != null
}

data class Media(
    val id: Long?= null,
    val mediaUrl: String = "",
    val mediaType : String = "", // "IMAGE", "VIDEO", "AUDIO"
    val sortOrder: Int = 0, // Thứ tự hiển thị trong bài đăng
    val createdAt: String? = null
)

data class PostResponse(
    val status: Int,
    val success: Boolean,
    val message: String? = null,
    val data: PostData
)

data class PostData(
    val content: List<Post>,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
    val size: Int,
    val number: Int,
    val sort: Sort,
    val numberOfElements: Int,
    val first: Boolean,
    val empty: Boolean
)

//data class Sort(
//    val sorted: Boolean,
//    val unsorted: Boolean,
//    val empty: Boolean
//)

data class PostResponseSingle(
    val status: Int,
    val success: Boolean,
    val message: String? = null,
    val data: Post
)