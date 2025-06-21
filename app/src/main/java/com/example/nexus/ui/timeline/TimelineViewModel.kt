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
import com.example.nexus.network.CreateMentionRequest
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
    // Chuyển sang MutableStateFlow để quản lý bất đồng bộ
    private val _listUserFollow = MutableStateFlow<List<User>>(emptyList())
    val listUserFollow: StateFlow<List<User>> = _listUserFollow.asStateFlow()

    private val _listUserFollowing = MutableStateFlow<List<User>>(emptyList())
    val listUserFollowing: StateFlow<List<User>> = _listUserFollowing.asStateFlow()
    @RequiresApi(Build.VERSION_CODES.O)
    val currentTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    init {
        // Lấy dữ liệu bài đăng từ API khi ViewModel được khởi tạo
        fetchPosts()
        fetchUserFollowersAndFollowing()
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


    fun fetchPostsUser(userId: Long) {
        viewModelScope.launch {
            try{
               val response = RetrofitClient.apiService.getPostsByUser(userId)
                if(response.success){
                    _postsState.value = PostState(
                        loading = false,
                        posts = response.data.content,
                        currentPage = 0,
                        last = response.data.last,
                        error = null
                    )
                }
            }
            catch (e: Exception) {
                _postsState.value = _postsState.value.copy(
                    loading = false,
                    error = "Không thể lấy bài đăng của người dùng"
                )
            }
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
    // Lấy danh sách người dùng theo dõi và đang theo dõi của người dùng hiện tại
    fun fetchUserFollowersAndFollowing() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                try {
                    println("Fetching followers and following for user $userId")
                    // Lấy danh sách follower
                    val followers = getFollowers(userId)
                    _listUserFollow.value = followers
                    println("Stored ${followers.size} followers: $followers")
                    // Lấy danh sách following
                    val following = getFollowing(userId)
                    _listUserFollowing.value = following
                    println("Stored ${following.size} following: $following")
                } catch (e: Exception) {
                    println("Error fetching followers/following: ${e.message}")
                }
            }
        }
    }
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
    suspend fun followUser(userId: Long) {
        currentUserId.let{
            try{
                val response = RetrofitClient.apiService.followUser(userId)
            }
            catch (e: Exception) {
                e.printStackTrace()

            }
        }
    }

    // Bỏ theo dõi một người dùng
    suspend fun unfollowUser(userId: Long) {
        currentUserId.let{
            try{
                val response = RetrofitClient.apiService.unfollowUser(userId)
            }
            catch (e: Exception) {
                e.printStackTrace()

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
        fun addPost(content: String, visibility: String = "PUBLIC", parentPostId: Long? = null, imageUris: List<Uri>) {
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

    @OptIn(UnstableApi::class)
    fun createMention(postId: Long? = null, commentId: Long? = null, mentionedUserId: Long) {
        viewModelScope.launch {
            try {
                val request = CreateMentionRequest(
                    postId = postId,
                    commentId = commentId,
                    mentionedUserId = mentionedUserId
                )
                val response = RetrofitClient.apiService.createMention(request)
                if (response.success) {
                    Log.d("TimelineViewModel", "Mention created successfully")
                } else {
                    Log.e("TimelineViewModel", "Failed to create mention: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("TimelineViewModel", "Error creating mention: ${e.message}")
            }
        }
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