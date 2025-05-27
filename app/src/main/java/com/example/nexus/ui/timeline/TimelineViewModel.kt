package com.example.nexus.ui.timeline

import androidx.lifecycle.ViewModel
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.model.Comment
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TimelineViewModel : ViewModel() {
    private val _posts = MutableStateFlow<List<Post>>(createMockPosts())
    val posts: StateFlow<List<Post>> = _posts


    private val _currentPost = MutableStateFlow<Post?>(null)
    val currentPost: StateFlow<Post?> = _currentPost.asStateFlow()
    // Giả sử ID của người dùng hiện tại (lấy từ SharedPreferences hoặc Auth sau này)
    val currentUserId: Long = 1

    // Mô phỏng bảng Follows: Danh sách các cặp (follower_id, followed_id)
    private val _follows = MutableStateFlow<List<Pair<Long, Long>>>(
        listOf(
            Pair(1, 2), // Người dùng 1 theo dõi người dùng 2
            Pair(2, 3), // Người dùng 2 theo dõi người dùng 3
            Pair(3, 1)  // Người dùng 3 theo dõi người dùng 1
        )
    )
    val follows: StateFlow<List<Pair<Long, Long>>> = _follows.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    // Kiểm tra xem người dùng có id là userId1 có follow người dùng có id là userId2 không
        fun isFollowing(userId1: Long,userId2: Long): Boolean {
            return _follows.value.any { it.first == userId1 && it.second == userId2 }
        }

    // Theo dõi một người dùng
    fun followUser(userId: Long, activityViewModel: ActivityViewModel) {
        if (!isFollowing(currentUserId,userId)) {
            _follows.value = _follows.value + Pair(currentUserId, userId)
            // Tạo thông báo giả mô phỏng bảng Notifications
            activityViewModel.addNotification(
                userId = userId, // Người nhận thông báo
                actorId = currentUserId, // Người thực hiện hành động
                type = "FOLLOW",
                targetType = "USER",
                targetId = userId,
                targetOwnerId = userId,
                createdAt = "2025-05-13T14:55:00Z"
            )
        }
    }

    // Bỏ theo dõi một người dùng
    fun unfollowUser(userId: Long) {
        if (isFollowing(currentUserId,userId)) {
            _follows.value = _follows.value.filterNot { it.first == currentUserId && it.second == userId }
        }
    }
    fun getPostById(postId: Long) {
        _currentPost.value = _posts.value.find { it.id?.toInt()?.toLong() == postId }
    }
    fun toggleLike(postId: Long, activityViewModel: ActivityViewModel? = null) {
        _posts.update { currentPosts ->
            currentPosts.map { post ->
                if (post.id == postId) {
                    val newPost = post.copy(
                        isLiked = !post.isLiked,
                        like_count = if (post.isLiked) post.like_count - 1 else post.like_count + 1
                    )
                    // Tạo thông báo khi Like
                    if (!post.isLiked && activityViewModel != null) { // Chỉ tạo thông báo khi Like, không tạo khi Unlike
                        activityViewModel.addNotification(
                            userId = post.user?.id ?: return@map newPost, // Người nhận thông báo (chủ bài đăng)
                            actorId = currentUserId, // Người thực hiện hành động
                            type = "LIKE_POST",
                            targetType = "POST",
                            targetId = postId,
                            targetOwnerId = post.user?.id,
                            createdAt = "2025-05-13T14:55:00Z"
                        )
                    }
                    newPost
                } else {
                    post
                }
            }
        }

        _currentPost.value?.let { currentPost ->
            if (currentPost.id == postId) {
                _currentPost.value = currentPost.copy(
                    isLiked = !currentPost.isLiked,
                    like_count = if (currentPost.isLiked) currentPost.like_count - 1 else currentPost.like_count + 1
                )
            }
        }
    }
    // Thêm bài đăng mới
    fun addPost(content: String, imageUri: String? = null) {
        val newPost = Post(
            id = (_posts.value.maxOfOrNull { it.id ?: 0 } ?: 0) + 1,
            user = User(id = 1, username = "user1", bio = "Hello!", profile_picture = "https://example.com/avatar.jpg"),
            content = content,
            imageUri = imageUri,
            created_at = "2025-05-13T14:55:00Z",
            updated_at = "2025-05-13T14:55:00Z"
        )
        _posts.value = listOf(newPost) + _posts.value
    }
    // Repost bài đăng
    fun repost(post: Post) {
        val repost = post.copy(
            id = (_posts.value.maxOfOrNull { it.id ?: 0 } ?: 0) + 1,
            content = "Reposted: ${post.content}",
            created_at = "2025-05-13T14:55:00Z",
            updated_at = "2025-05-13T14:55:00Z"
        )
        _posts.value = listOf(repost) + _posts.value
    }
    fun replyPost(parentPostId: Long, content: String) {
        _currentPost.value?.let { currentPost ->
            val newReply = Post(
                id = (_posts.value.maxOfOrNull { it.id ?: 0 } ?: 0) + 1,
                user = User(id = 1, username = "user1", bio = "Hello!", profile_picture = "https://example.com/avatar.jpg"),
                parent_post_id = parentPostId,
                content = content,
                created_at = "2025-05-13T14:55:00Z",
                updated_at = "2025-05-13T14:55:00Z"
            )

            if (currentPost.id == parentPostId) {
                _currentPost.value = currentPost.copy(
                    replies = currentPost.replies + newReply
                )
            } else {
                val updatedReplies = currentPost.replies.map { reply ->
                    if (reply.id == parentPostId) {
                        reply.copy(parent_post_id = currentPost.id)
                    } else {
                        reply
                    }
                }
                _currentPost.value = currentPost.copy(
                    replies = updatedReplies + newReply
                )
            }

            _posts.update { currentPosts ->
                currentPosts.map { post ->
                    if (post.id == currentPost.id) {
                        _currentPost.value!!
                    } else {
                        post
                    }
                }
            }
        }
    }
    fun addComment(postId: Long, commentContent: String) {
        _currentPost.value?.let { currentPost ->
            val newComment = Comment(
                id = (currentPost.comments.maxOfOrNull { it.id ?: 0 } ?: 0) + 1,
                post_id = postId,
                user = User(id = 1, username = "user1", bio = "Hello!", profile_picture = "https://example.com/avatar.jpg"),
                content = commentContent,
                created_at = "2025-05-13T14:55:00Z",
                updated_at = "2025-05-13T14:55:00Z"
            )
            _currentPost.value = currentPost.copy(
                comments = currentPost.comments + newComment,
                comment_count = currentPost.comment_count + 1
            )
        }
    }
    fun addReply(postId: Long, parentCommentId: Long, replyContent: String) {
        _currentPost.value?.let { currentPost ->
            val newReply = Comment(
                id = (currentPost.comments.flatMap { it.replies }.maxOfOrNull { it.id ?: 0 } ?: 0) + 1,
                post_id = postId,
                user = User(id = 1, username = "user1", bio = "Hello!", profile_picture = "https://example.com/avatar.jpg"),
                parent_comment_id = parentCommentId,
                content = replyContent,
                created_at = "2025-05-13T14:55:00Z",
                updated_at = "2025-05-13T14:55:00Z"
            )
            val updatedComments = currentPost.comments.map { comment ->
                if (comment.id == parentCommentId) {
                    comment.copy(replies = comment.replies + newReply)
                } else {
                    comment
                }
            }
            _currentPost.value = currentPost.copy(
                comments = updatedComments,
                comment_count = currentPost.comment_count + 1
            )
        }
    }
    fun toggleCommentLike(postId: Long, commentId: Long, isReply: Boolean) {
        _currentPost.value?.let { currentPost ->
            if (isReply) {
                val updatedComments = currentPost.comments.map { comment ->
                    val updatedReplies = comment.replies.map { reply ->
                        if (reply.id == commentId) {
                            reply.copy(
                                isLiked = !reply.isLiked,
                                like_count = if (reply.isLiked) reply.like_count - 1 else reply.like_count + 1
                            )
                        } else {
                            reply
                        }
                    }
                    comment.copy(replies = updatedReplies)
                }
                _currentPost.value = currentPost.copy(comments = updatedComments)
            } else {
                val updatedComments = currentPost.comments.map { comment ->
                    if (comment.id == commentId) {
                        comment.copy(
                            isLiked = !comment.isLiked,
                            like_count = if (comment.isLiked) comment.like_count - 1 else comment.like_count + 1
                        )
                    } else {
                        comment
                    }
                }
                _currentPost.value = currentPost.copy(comments = updatedComments)
            }
        }
    }
    fun updatePosts(newPosts: List<Post>) {
        _posts.value = newPosts
    }

    private fun createMockPosts(): List<Post> {
        return listOf(
            Post(
                id = 1,
                user = User(id = 1, username = "user1", bio = "Hello!", profile_picture = "https://example.com/avatar.jpg"),
                content = "This is my first post!",
                created_at = "2025-05-13T14:55:00Z",
                updated_at = "2025-05-13T14:55:00Z",
                comments = listOf(
                    Comment(
                        id = 1,
                        post_id = 1,
                        user = User(id = 2, username = "user2", bio = "Loving this app!", profile_picture = "https://example.com/avatar2.jpg"),
                        content = "Great post!",
                        created_at = "2025-05-13T14:55:00Z",
                        updated_at = "2025-05-13T14:55:00Z",
                        replies = listOf(
                            Comment(
                                id = 2,
                                post_id = 1,
                                user = User(id = 1, username = "user1", bio = "Hello!", profile_picture = "https://example.com/avatar.jpg"),
                                parent_comment_id = 1,
                                content = "Thanks!",
                                created_at = "2025-05-13T14:55:00Z",
                                updated_at = "2025-05-13T14:55:00Z"
                            ),
                            Comment(
                                id = 3,
                                post_id = 1,
                                user = User(id = 2, username = "user2", bio = "Loving this app!", profile_picture = "https://example.com/avatar2.jpg"),
                                parent_comment_id = 1,
                                content = "You're welcome!",
                                created_at = "2025-05-13T14:55:00Z",
                                updated_at = "2025-05-13T14:55:00Z"
                            )
                        )
                    )
                )
            ),
            Post(
                id = 2,
                user = User(id = 2, username = "user2", bio = "Loving this app!", profile_picture = "https://example.com/avatar2.jpg"),
                content = "ThreadsClone is awesome!",
                created_at = "2025-05-13T14:55:00Z",
                updated_at = "2025-05-13T14:55:00Z"
            ),
            Post(
                id = 3,
                user = User(id = 3, username = "user3", bio = "Just joined!", profile_picture = "https://example.com/avatar3.jpg"),
                content = "Hello everyone!",
                created_at = "2025-05-13T14:55:00Z",
                updated_at = "2025-05-13T14:55:00Z"
            )
        )
    }
}