package com.example.nexus.ui.timeline

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.nexus.network.AuthManager
import com.example.nexus.network.RetrofitClient
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.model.Comment
import com.example.nexus.ui.model.CreateCommentRequest
import com.example.nexus.ui.model.CreatePostRequest
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.User
import com.example.nexus.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class TimelineViewModel(private val authManager: AuthManager,private val context: Context) : ViewModel() {

    private val _postsState = mutableStateOf(PostState())
    val postsState: State<PostState> = _postsState

    private val _commentState = mutableStateOf(CommentState())
    val commentState: State<CommentState> = _commentState

    private val _userCache = MutableStateFlow<Map<Long, User>>(emptyMap())
    val userCache: StateFlow<Map<Long, User>> = _userCache.asStateFlow()
    private val _postCache = MutableStateFlow<Map<Long, Post>>(emptyMap())
    val postCache: StateFlow<Map<Long, Post>> = _postCache.asStateFlow()
    val activityViewModel = ActivityViewModel()
    @RequiresApi(Build.VERSION_CODES.O)
    val currentTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    init {
        // Lấy dữ liệu bài đăng từ API khi ViewModel được khởi tạo
        fetchPosts()
    }
    private fun fetchPosts(page: Int = 0){
        viewModelScope.launch {
            val token = authManager.getAccessToken()
            if (token == null) {
                _postsState.value = _postsState.value.copy(
                    loading = false,
                    loadingMore = false,
                    error = "Vui lòng đăng nhập để xem bài đăng"
                )
                return@launch
            }
            try {
                val response = RetrofitClient.apiService.getPosts(page = page)
                if(response.success){
                    val posts = response.data.content
                    _postsState.value = PostState(
                        loading = false,
                        posts = if (page == 0) response.data.content else _postsState.value.posts + response.data.content,
                        currentPage = page,
                        last = response.data.last,
                        loadingMore = false,
                        error = null
                    )
                    // Cache users trước
                    response.data.content.forEach { post ->
                        post.isLiked =checkLike("POST",post.id?: 0)
                        post.user?.id?.let { userId ->
                            _userCache.value = _userCache.value + (userId to post.user)
                        }
                        _postCache.value = (_postCache.value + (post.id to post)) as Map<Long, Post>
                    }
                }
                else{
                    _postsState.value = _postsState.value.copy(
                        loading = false,
                        loadingMore = false,
                        error = response.message ?: "Unknown error"
                    )
                }
            }
            catch (e: Exception){
                _postsState.value = _postsState.value.copy(
                    loading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    fun loadMorePosts() {
        if (!_postsState.value.loadingMore && !_postsState.value.last) {
            _postsState.value = _postsState.value.copy(loadingMore = true)
            fetchPosts(page = _postsState.value.currentPage + 1)
        }
    }

    // Thêm _currentPost để lưu trữ bài đăng hiện tại
    private val _currentPost = mutableStateOf<Post?>(null)
    val currentPost: State<Post?> = _currentPost

    // Thêm phương thức công khai để truy xuất token
    fun getAccessToken(): String? {
        return authManager.getAccessToken()
    }
    // Giả sử ID của người dùng hiện tại (lấy từ SharedPreferences hoặc Auth sau này)
    val currentUserId: Long?
        get() = authManager.getUserId()
    val currentUser : User?
        get() = currentUserId?.let { _userCache.value[it] }
    suspend fun getUserById(userId: Long): User? {
        _userCache.value[userId]?.let { return it }
        return try {
            val response = RetrofitClient.apiService.getUserById(userId)
            if (response.success) {
                _userCache.value = _userCache.value + (userId to response.data)
                response.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchLikeCount(targetType: String, targetId: Long): Long {
        return try {
            val response = RetrofitClient.apiService.countLikes(targetType, targetId)
            if (response.success) {
                if(targetType.equals("POST", ignoreCase = true)) {
                    _postCache.value = _postCache.value.mapValues { (id, post) ->
                        if (id == targetId) {
                            post.copy(likeCount = response.data ?: 0)
                        } else {
                            post
                        }
                    }
                } else if(targetType.equals("COMMENT", ignoreCase = true)) {
                    _commentState.value = _commentState.value.copy(
                        comments = _commentState.value.comments.map { comment ->
                            if (comment.id == targetId) {
                                comment.copy(likeCount = response.data?:0 )
                            } else {
                                comment
                            }
                        }
                    )
                }
                response.data?:0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    suspend fun checkLike(TargetType: String, targetId: Long): Boolean {
        return try {
            val response = RetrofitClient.apiService.hasUserLiked(currentUserId?: 0,TargetType,targetId)
            if (response.success) {
                if(TargetType.equals("POST", ignoreCase = true)){
                    _postCache.value = _postCache.value.mapValues { (id, post) ->
                        if (id == targetId) {
                            post.copy(isLiked = response.data == true)
                        } else {
                            post
                        }
                    }
                }
                else if(TargetType.equals("COMMENT", ignoreCase = true)){
                    _commentState.value = _commentState.value.copy(
                        comments = _commentState.value.comments.map { comment ->
                            if (comment.id == targetId) {
                                comment.copy(isLiked = response.data == true)
                            } else {
                                comment
                            }
                        }
                    )
                }
                response.data == true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    suspend fun getPostById(postId: Long): Post? {
        _postCache.value[postId]?.let { return it }
        return try {
            val response = RetrofitClient.apiService.getPostById(postId)
            if (response.success) {
                val post = response.data
                val user = post.user?.id?.let { getUserById(it) }
                val finalPost = post.copy(user = user)
                _postCache.value = _postCache.value + (postId to finalPost)
                finalPost
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    suspend fun getFollowers(userId: Long): List<User> {
        return try {
            val response = RetrofitClient.apiService.getFollowers(userId)
            if (response.success) {
                response.data.mapNotNull { follow ->
                    getUserById(follow.followerId)
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFollowing(userId: Long): List<User> {
        return try {
            val response = RetrofitClient.apiService.getFollowing(userId)
            if (response.success) {
                response.data.mapNotNull { follow ->
                    getUserById(follow.followedId)
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    fun getCommentsForPost(postId: Long){
        viewModelScope.launch {
            try{
                val rootResponse = RetrofitClient.apiService.getCommentsRoot(postId)
                if(rootResponse.success){
                    val rootComments = rootResponse.data.content ?: emptyList()
                    // Lấy bình luận con cho mỗi bình luận cha
                    val commentsWithReplies = rootComments.mapNotNull { rootComment ->
                        rootComment.id.let { commentId ->
                            val childResponse = RetrofitClient.apiService.getChildComments(commentId)

                            val childComments = childResponse.data?.content ?: emptyList() // Bảo vệ chống null
                            rootComment.copy(replies = childComments)
                        }

                    }
                    _commentState.value = CommentState(
                        loading = false,
                        comments = commentsWithReplies,
                        error = null
                    )
                }
                else{
                    _commentState.value = _commentState.value.copy(
                        loading = false,
                        error = rootResponse.message ?: "Unknown error"
                    )
                }

            }
            catch(e: Exception) {
                _commentState.value = _commentState.value.copy(
                    loading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    // Mô phỏng bảng Follows: Danh sách các cặp (follower_id, followed_id)
    private val _follows = MutableStateFlow<List<Pair<Long, Long>>>(
        listOf(
            Pair(1, 2), // Người dùng 1 theo dõi người dùng 2
            Pair(2, 3), // Người dùng 2 theo dõi người dùng 3
            Pair(3, 1)  // Người dùng 3 theo dõi người dùng 1
        )
    )
    val follows: StateFlow<List<Pair<Long, Long>>> = _follows.asStateFlow()

    // Kiểm tra xem người dùng có id là userId1 có follow người dùng có id là userId2 không
        fun isFollowing(userId1: Long,userId2: Long): Boolean {
            return _follows.value.any { it.first == userId1 && it.second == userId2 }
        }

    // Theo dõi một người dùng
    fun followUser(userId: Long) {
        currentUserId?.let { currentId ->
            if (!isFollowing(currentId, userId)) {
                _follows.value = _follows.value + Pair(currentId, userId)
                activityViewModel.addNotification(
                    userId = userId,
                    actorId = currentId,
                    type = "FOLLOW",
                    targetType = "USER",
                    targetId = userId,
                    targetOwnerId = userId
                )
            }
        }
    }

    // Bỏ theo dõi một người dùng
    fun unfollowUser(userId: Long) {
        currentUserId?.let { currentId ->
            if (isFollowing(currentId, userId)) {
                _follows.value = _follows.value.filterNot { it.first == currentId && it.second == userId }
            }
        }
    }
    suspend fun LikePost(postId: Long) {
        currentUserId.let {
            try {
                val response = RetrofitClient.apiService.likePost(postId)
                if(response.success){
                    val post = _postCache.value[postId]?.copy(isLiked = true, likeCount = _postCache.value[postId]?.likeCount?.plus(1) ?: 1)
                    post?.let {p->
                        _postCache.value = _postCache.value + (postId to p)
                    }
                    _currentPost.value = _currentPost.value?.copy(isLiked = true, likeCount = (_currentPost.value!!.likeCount?.plus(1)))
                    activityViewModel.addNotification(
                        userId = _postCache.value[postId]?.user?.id ?: 0,
                        actorId = currentUserId ?: 0,
                        type = "LIKE_POST",
                        targetType = "POST",
                        targetId = postId,
                        targetOwnerId = _postCache.value[postId]?.user?.id ?: 0
                    )

                }
                else{
                    _postsState.value = _postsState.value.copy(error = response.message ?: "Failed to like post")
                }
            }
            catch(e: Exception) {
                e.printStackTrace()

            }
        }
    }

    suspend fun unLike(TagetType: String, targetId: Long) {
        currentUserId.let {
            try {
                val response = RetrofitClient.apiService.unLike(currentUserId?:0, TagetType, targetId)
                if(response.success){
                    if (TagetType.equals("POST", ignoreCase = true)) {
                        val post = _postCache.value[targetId]?.copy(isLiked = false, likeCount = _postCache.value[targetId]?.likeCount?.minus(1) ?: 0)
                        post?.let { p ->
                            _postCache.value = _postCache.value + (targetId to p)
                        }
                        _currentPost.value = _currentPost.value?.copy(isLiked = false, likeCount = (_currentPost.value!!.likeCount?.minus(1)))
                    } else if (TagetType.equals("COMMENT", ignoreCase = true)) {
                        _commentState.value = _commentState.value.copy(
                            comments = _commentState.value.comments.map { comment ->
                                if (comment.id == targetId) {
                                    comment.copy(isLiked = false, likeCount = comment.likeCount - 1)
                                } else {
                                    comment
                                }
                            }
                        )
                    }
                }
                else{
                    _postsState.value = _postsState.value.copy(error = response.message ?: "Failed to unlike post")
                }
            }
            catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun likeComment(commentId: Long){
        currentUserId.let{
            try {
                val response = RetrofitClient.apiService.likeComment(commentId)
                if(response.success){
                    val comment = _commentState.value.comments.find { it.id == commentId }
                    if (comment != null) {
                        val updatedComment = comment.copy(isLiked = true, likeCount = comment.likeCount + 1)
                        _commentState.value = _commentState.value.copy(
                            comments = _commentState.value.comments.map { if (it.id == commentId) updatedComment else it }
                        )
                    }
                } else {
                    _commentState.value = _commentState.value.copy(error = response.message ?: "Failed to like comment")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    // Thêm bài đăng mới
    @OptIn(UnstableApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
        fun addPost(
        content: String,
        visibility: String = "PUBLIC",
        parentPostId: Long? = null,
        imageUris: List<Uri>
    ) {
            viewModelScope.launch {
                try {
                    if(imageUris.isEmpty()){
                        val fileParts = mutableListOf<MultipartBody.Part>()
                        imageUris.forEachIndexed { index, uri ->
                            val file = uriToFile(context, uri, "image_$index")
                            if (file != null) {
                                val mimeType = getMimeType(file.name)
                                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                                // Quan trọng: Tất cả files phải có cùng tên parameter "files"
                                val part = MultipartBody.Part.createFormData("files", file.name, requestFile)
                                fileParts.add(part)
                            }
                        }
                        val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
                        val visibilityBody = visibility.toRequestBody("text/plain".toMediaTypeOrNull())
                        val parentPostIdBody = parentPostId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                        val response = RetrofitClient.apiService.createPostWithMedia(
                            content = contentBody,
                            visibility = visibilityBody,
                            parentPostId = parentPostIdBody,
                            files = if (fileParts.isNotEmpty()) fileParts else null
                        )
                        if (response.success) {
                            fetchPosts(0)
                            Log.d("TimelineViewModel", "Post added successfully: ${response.data}")
                        } else {
                            _postsState.value = _postsState.value.copy(
                                loading = false,
                                error = response.message ?: "Failed to add post"
                            )
                            Log.e("TimelineViewModel", "Failed to add post: ${response.message}")
                        }
                    }
                    else{
                        val createPostRequest = CreatePostRequest(
                            content = content,
                            visibility = visibility,
                            parentPostId = parentPostId
                        )
                        val response = RetrofitClient.apiService.createPost(createPostRequest)
                        if (response.success) {
                            fetchPosts(0)
                            Log.d("TimelineViewModel", "Post added successfully: ${response.data}")
                        } else {
                            _postsState.value = _postsState.value.copy(
                                loading = false,
                                error = response.message ?: "Failed to add post"
                            )
                            Log.e("TimelineViewModel", "Failed to add post: ${response.message}")
                        }
                    }

                } catch (e: Exception) {
                    _postsState.value = _postsState.value.copy(
                        loading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    // Repost bài đăng
    fun repost(post: Post) {
//        val repost = post.copy(
//            id = (_posts.value.maxOfOrNull { it.id ?: 0 } ?: 0) + 1,
//            content = "Reposted: ${post.content}",
//            created_at = "2025-05-13T14:55:00Z",
//            updated_at = "2025-05-13T14:55:00Z"
//        )
//        _posts.value = listOf(repost) + _posts.value
    }
    fun replyPost(parentPostId: Long, content: String) {
//        _currentPost.value?.let { currentPost ->
//            val newReply = Post(
//                id = (_posts.value.maxOfOrNull { it.id ?: 0 } ?: 0) + 1,
//                user = User(id = 1, username = "user1", bio = "Hello!", profile_picture = "https://example.com/avatar.jpg"),
//                parent_post_id = parentPostId,
//                content = content,
//                created_at = "2025-05-13T14:55:00Z",
//                updated_at = "2025-05-13T14:55:00Z"
//            )
//
//            if (currentPost.id == parentPostId) {
//                _currentPost.value = currentPost.copy(
//                    replies = currentPost.replies + newReply
//                )
//            } else {
//                val updatedReplies = currentPost.replies.map { reply ->
//                    if (reply.id == parentPostId) {
//                        reply.copy(parent_post_id = currentPost.id)
//                    } else {
//                        reply
//                    }
//                }
//                _currentPost.value = currentPost.copy(
//                    replies = updatedReplies + newReply
//                )
//            }
//
//            _posts.update { currentPosts ->
//                currentPosts.map { post ->
//                    if (post.id == currentPost.id) {
//                        _currentPost.value!!
//                    } else {
//                        post
//                    }
//                }
//            }
//        }
    }


    fun addComment(postId: Long, content: String, parentCommentId: Long? = null) {
        viewModelScope.launch {
            try {
                val request = CreateCommentRequest(
                    postId = postId,
                    content = content,
                    parentCommentId = parentCommentId
                )
                val response = RetrofitClient.apiService.createComment(request)
                if (response.success) {
                    val newComment =  response.data
                    _commentState.value = if (parentCommentId == null) {
                        _commentState.value.copy(
                            comments = (listOf(newComment) + _commentState.value.comments) as List<Comment>,
                            loading = false,
                            error = null
                        )
                    } else {
                        _commentState.value.copy(
                            comments = _commentState.value.comments.map { comment ->
                                if (comment.id == parentCommentId) {
                                    comment.copy(replies = (comment.replies + newComment) as List<Comment>)
                                } else {
                                    comment
                                }
                            },
                            loading = false,
                            error = null
                        )
                    }
                } else {
                    _commentState.value = _commentState.value.copy(
                        loading = false,
                        error = response.message ?: "Failed to add comment"
                    )
                }
            } catch (e: Exception) {
                _commentState.value = _commentState.value.copy(
                    loading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }


    fun toggleCommentLike(postId: Long, commentId: Long, isReply: Boolean) {
//        _currentPost.value?.let { currentPost ->
//            if (isReply) {
//                val updatedComments = currentPost.comments.map { comment ->
//                    val updatedReplies = comment.replies.map { reply ->
//                        if (reply.id == commentId) {
//                            reply.copy(
//                                isLiked = !reply.isLiked,
//                                like_count = if (reply.isLiked) reply.like_count - 1 else reply.like_count + 1
//                            )
//                        } else {
//                            reply
//                        }
//                    }
//                    comment.copy(replies = updatedReplies)
//                }
//                _currentPost.value = currentPost.copy(comments = updatedComments)
//            } else {
//                val updatedComments = currentPost.comments.map { comment ->
//                    if (comment.id == commentId) {
//                        comment.copy(
//                            isLiked = !comment.isLiked,
//                            like_count = if (comment.isLiked) comment.like_count - 1 else comment.like_count + 1
//                        )
//                    } else {
//                        comment
//                    }
//                }
//                _currentPost.value = currentPost.copy(comments = updatedComments)
//            }
//        }
    }

//    fun updatePosts(newPosts: List<Post>) {
//        _posts.value = newPosts
//    }

//    private fun createMockPosts(): List<Post> {
//        return listOf(
//            Post(
//                id = 1,
//                user = User(id = 1, username = "Static", bio = "Hello!", profile_picture = "https://scontent.fhan20-1.fna.fbcdn.net/v/t39.30808-6/475308019_1955546638268380_6109831544270026139_n.jpg?_nc_cat=103&ccb=1-7&_nc_sid=6ee11a&_nc_eui2=AeFdGNNCtWVrzSOP6vQg4tGmtXYybfc9VQa1djJt9z1VBmZgSYjXfmhe951gwvIOrh84zRZtb7YIDn8WGOjHAdzn&_nc_ohc=8pApWMt6OrQQ7kNvwGdi4N9&_nc_oc=AdkD1w1GiqenC1qbHrrQ3TNUTCNCiYhnNac6hPJWmMYl6VaOqV1DcsORW5q2h4_q3e4&_nc_zt=23&_nc_ht=scontent.fhan20-1.fna&_nc_gid=H6suFPkvvqfq1xfoaanbDA&oh=00_AfJsqKffcOZips3yWtW7AluCTsWZV-vaz3W98WRiPQngVA&oe=68430D30"),
//                content = "This is my first post!",
//                imageUri = "https://scontent.fhan2-4.fna.fbcdn.net/v/t1.6435-9/97472353_1133757603684786_9168272966566281216_n.jpg?_nc_cat=110&ccb=1-7&_nc_sid=94e2a3&_nc_eui2=AeF0P8eM1XIbw07XoH5ymXHbpLbfEhYNV_6ktt8SFg1X_gTvH7LivzJVLSfSKg3LPpytPCBcsCg7d6kYD5VyMa5W&_nc_ohc=TmHTOTPJX7kQ7kNvwEh7IH-&_nc_oc=Adl1Pgtjm_50JEASOooX_Y0rzvr0GVJheSMtpj4O0zRm172NMqQQxC5BPr_6ejVN-Vg&_nc_zt=23&_nc_ht=scontent.fhan2-4.fna&_nc_gid=jWzh3B8Pvo0TaC64_X4_3g&oh=00_AfKSX1HyyTLzKIVw_AtHivoGJQt3yYwQDDH8HjYAMuSTjg&oe=6864CC4E",
//                created_at = "2025-05-13T14:55:00Z",
//                updated_at = "2025-05-13T14:55:00Z",
//                comments = listOf(
//                    Comment(
//                        id = 1,
//                        post_id = 1,
//                        user = User(id = 2, username = "user2", bio = "Loving this app!", profile_picture = "https://example.com/avatar2.jpg"),
//                        content = "Great post!",
//                        created_at = "2025-05-13T14:55:00Z",
//                        updated_at = "2025-05-13T14:55:00Z",
//                        replies = listOf(
//                            Comment(
//                                id = 2,
//                                post_id = 1,
//                                user = User(id = 1, username = "user1", bio = "Hello!", profile_picture = "https://scontent.fhan20-1.fna.fbcdn.net/v/t39.30808-6/475308019_1955546638268380_6109831544270026139_n.jpg?_nc_cat=103&ccb=1-7&_nc_sid=6ee11a&_nc_eui2=AeFdGNNCtWVrzSOP6vQg4tGmtXYybfc9VQa1djJt9z1VBmZgSYjXfmhe951gwvIOrh84zRZtb7YIDn8WGOjHAdzn&_nc_ohc=8pApWMt6OrQQ7kNvwGdi4N9&_nc_oc=AdkD1w1GiqenC1qbHrrQ3TNUTCNCiYhnNac6hPJWmMYl6VaOqV1DcsORW5q2h4_q3e4&_nc_zt=23&_nc_ht=scontent.fhan20-1.fna&_nc_gid=H6suFPkvvqfq1xfoaanbDA&oh=00_AfJsqKffcOZips3yWtW7AluCTsWZV-vaz3W98WRiPQngVA&oe=68430D30"),
//                                parent_comment_id = 1,
//                                content = "Thanks!",
//                                created_at = "2025-05-13T14:55:00Z",
//                                updated_at = "2025-05-13T14:55:00Z"
//                            ),
//                            Comment(
//                                id = 3,
//                                post_id = 1,
//                                user = User(id = 2, username = "user2", bio = "Loving this app!", profile_picture = "https://example.com/avatar2.jpg"),
//                                parent_comment_id = 1,
//                                content = "You're welcome!",
//                                created_at = "2025-05-13T14:55:00Z",
//                                updated_at = "2025-05-13T14:55:00Z"
//                            )
//                        )
//                    )
//                )
//            ),
//            Post(
//                id = 2,
//                user = User(id = 2, username = "Nghĩa", bio = "Loving this app!", profile_picture = "https://scontent.fhan2-4.fna.fbcdn.net/v/t39.30808-6/467222594_1257944542111805_7180808345332579320_n.jpg?_nc_cat=100&ccb=1-7&_nc_sid=6ee11a&_nc_eui2=AeFrUWF1yI-xCz0oCwTSJSpNwPkFIZ5dyZTA-QUhnl3JlBNDdwd98JhN-O_0EpK2XOFp7nhe6FqlQ28LXl8_XPOB&_nc_ohc=CyI0sH7lEWMQ7kNvwEjxL6u&_nc_oc=Adk8f-CfA9x1rzY73KnPIAln5pgZZ0g4jGicomUeeaCMWs9a_Jvfi-jb4KshP1BiGaE&_nc_zt=23&_nc_ht=scontent.fhan2-4.fna&_nc_gid=CnEqSe_neqY6ZmrX605T_g&oh=00_AfJrebZhwBj9i4qbuBWF5qFH6uNCqg-LiCmQQZm7nHAaJQ&oe=6843075D"),
//                content = "Bài đăng thứ 2!",
//                imageUri = "https://image.dienthoaivui.com.vn/x,webp,q90/https://dashboard.dienthoaivui.com.vn/uploads/dashboard/editor_upload/anh-meme-1.jpg",
//                created_at = "2025-05-13T14:55:00Z",
//                updated_at = "2025-05-13T14:55:00Z"
//            ),
//            Post(
//                id = 3,
//                user = User(id = 3, username = "user3", bio = "Just joined!", profile_picture = "https://example.com/avatar3.jpg"),
//                content = "Hello everyone!",
//                created_at = "2025-05-13T14:55:00Z",
//                updated_at = "2025-05-13T14:55:00Z"
//            )
//        )
//    }
data class PostState(
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val posts: List<Post> = emptyList(),
    val currentPage: Int = 0,
    val last: Boolean = false,
    val error: String? = null
)
    data class CommentState(
        val loading: Boolean = true,
        val comments: List<Comment> = emptyList(),
        val error: String? = null
    )
}

private fun uriToFile(context: Context, uri: Uri, fileName: String): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "$fileName.jpg")
        val outputStream = FileOutputStream(file)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getMimeType(fileName: String): String {
    return when {
        fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
        fileName.endsWith(".png", true) -> "image/png"
        fileName.endsWith(".gif", true) -> "image/gif"
        fileName.endsWith(".mp4", true) -> "video/mp4"
        fileName.endsWith(".mov", true) -> "video/quicktime"
        else -> "application/octet-stream"
    }
}