package com.example.nexus.ui.timeline

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.nexus.network.AuthManager
import com.example.nexus.network.CreateMentionRequest
import com.example.nexus.network.RetrofitClient
import com.example.nexus.network.RetrofitClient.apiService
import com.example.nexus.ui.activity.ActivityViewModel
import com.example.nexus.ui.activity.FCMManager
import com.example.nexus.ui.model.ApiResponse
import com.example.nexus.ui.model.Comment
import com.example.nexus.ui.model.CreateCommentRequest
import com.example.nexus.ui.model.CreatePostRequest
import com.example.nexus.ui.model.CreateReportRequest
import com.example.nexus.ui.model.PatchUserDTO
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.ReportReason
import com.example.nexus.ui.model.ReportTargetType
import com.example.nexus.ui.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class TimelineViewModel(val authManager: AuthManager, private val context: Context) : ViewModel() {

    //Quản lý danh sách bài đăng
    private val _postsState = mutableStateOf(PostState())
    val postsState: State<PostState> = _postsState
    //Quản lý danh sách bình luận
    private val _commentState = mutableStateOf(CommentState())
    val commentState: State<CommentState> = _commentState

    //Cache user
    private val _userCache = MutableStateFlow<Map<Long, User>>(emptyMap())
    val userCache: StateFlow<Map<Long, User>> = _userCache.asStateFlow()
    //Cache post
    private val _postCache = MutableStateFlow<Map<Long, Post>>(emptyMap())
    val postCache: StateFlow<Map<Long, Post>> = _postCache.asStateFlow()
    val activityViewModel = ActivityViewModel(
        apiService = RetrofitClient.apiService,
        fcmManager = FCMManager(context)
    )
    // Danh sách người dùng theo dõi và đang theo dõi
    private val _listUserFollow = MutableStateFlow<List<User>>(emptyList())
    val listUserFollow: StateFlow<List<User>> = _listUserFollow.asStateFlow()
    private val _listUserFollowing = MutableStateFlow<List<User>>(emptyList())
    val listUserFollowing: StateFlow<List<User>> = _listUserFollowing.asStateFlow()

    var unread_count: MutableState<Long> = mutableStateOf(0)

    @OptIn(UnstableApi::class)
    private suspend fun ensureValidToken(): Boolean {
        return try {
            val token = authManager.getAccessToken()
            if (token.isNullOrBlank()) {
                // Try to refresh token
                authManager.refreshToken()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("TimelineViewModel", "Error ensuring valid token", e)
            false
        }
    }
    init {
        // Kiểm tra xem người dùng đã đăng nhập hay chưa
        if (authManager.isUserLoggedIn()) {
            try {
                fetchPosts()
                fetchUserFollowersAndFollowing()
                getUnReadNotificationCount()
                Log.d("Unread","${unread_count.value}")
            } catch (e: Exception) {

            }
        }
    }

    @OptIn(UnstableApi::class)
    fun fetchPosts(page: Int = 0){
        viewModelScope.launch {
            Log.d("TimelineViewModel", "=== FETCH POSTS DEBUG ===")
            Log.d("TimelineViewModel", "User logged in: ${authManager.isUserLoggedIn()}")
            Log.d("TimelineViewModel", "Current user ID: ${authManager.getUserId()}")
            //Kiểm tra token trước khi gọi
            if (!ensureValidToken()) {
                _postsState.value = _postsState.value.copy(
                    loading = false,
                    loadingMore = false,
                    error = "Vui lòng đăng nhập để xem bài đăng"
                )
                return@launch
            }
            try {
                val response = apiService.getPosts(page = page)
                if(response.success){
                    Log.d("TimelineViewModel", "Processing ${response.data.content.size} posts")
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
                        Log.d("TimelineViewModel", "Processing post: ${post.id} by user: ${post.user?.username}")
                        post.isLiked =checkLike("POST",post.id?: 0)
                        saveUserToCache(post.user)

                        _postCache.value = (_postCache.value + (post.id to post)) as Map<Long, Post>
                    }
                    Log.d("TimelineViewModel", "After processing - UserCache size: ${_userCache.value.size}")
                    Log.d("TimelineViewModel", "After processing - PostCache size: ${_postCache.value.size}")
                }
                else{
                    Log.e("TimelineViewModel", "API failed: ${response.message}")
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

    fun refreshPosts() {
        viewModelScope.launch {
            try {
                // Reset state
                _postsState.value = _postsState.value.copy(
                    loading = true,
                    error = null
                )
                // Clear cache nếu cần
                _postCache.value = emptyMap()
                // Fetch posts từ page 0
                fetchPosts(page = 0)

            } catch (e: Exception) {
                _postsState.value = _postsState.value.copy(
                    loading = false,
                    error = e.message ?: "Lỗi khi tải lại"
                )
            }
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

     fun getUnReadNotificationCount() {
         viewModelScope.launch {
             val response = RetrofitClient.apiService.countUnreadNotifications()
             Log.d("Unread","${unread_count.value}")
             if (response.success) {
                 unread_count.value = response.data ?: 0

             } else {
                 unread_count.value = 0
             }
         }

    }

    suspend fun getUserById(userId: Long): User? {
        return try {
            val response = apiService.getUserById(userId)
            if (response.success) {
                saveUserToCache(response.data)
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
    private fun saveUserToCache(user: User?) {
        user?.id?.let { userId ->
            _userCache.value = _userCache.value + (userId to user)
        }
    }
    suspend fun getPostById(postId: Long): Post? {
        _postCache.value[postId]?.let { return it }
        return try {
            val response = apiService.getPostById(postId)
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
    private val _follows = MutableStateFlow<List<Pair<Long, Long>>>(
        listOf(
            Pair(1, 2), // Người dùng 1 theo dõi người dùng 2
            Pair(2, 3), // Người dùng 2 theo dõi người dùng 3
            Pair(3, 1)  // Người dùng 3 theo dõi người dùng 1
        )
    )
    val follows: StateFlow<List<Pair<Long, Long>>> = _follows.asStateFlow()
    // Theo dõi một người dùng
    suspend fun followUser(userId: Long) {
        currentUserId.let{
            try{
                val response = apiService.followUser(userId)
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
                val response = apiService.unfollowUser(userId)
            }
            catch (e: Exception) {
                e.printStackTrace()

            }
        }
    }
    suspend fun LikePost(postId: Long) {
        currentUserId.let {
            try {
                val response = apiService.likePost(postId)
                if(response.success){
                    val post = _postCache.value[postId]?.copy(isLiked = true, likeCount = _postCache.value[postId]?.likeCount?.plus(1) ?: 1)
                    post?.let {p->
                        _postCache.value = _postCache.value + (postId to p)
                    }
                    _currentPost.value = _currentPost.value?.copy(isLiked = true, likeCount = (_currentPost.value!!.likeCount?.plus(1)))
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
                val response = apiService.unLike(currentUserId?:0, TagetType, targetId)
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
                val response = apiService.likeComment(commentId)
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

    @OptIn(UnstableApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    fun addPost(
        content: String,
        visibility: String = "PUBLIC",
        parentPostId: Long? = null,
        mediaUris: List<Uri> = emptyList() // Đổi tên từ imageUris thành mediaUris
    ) {
        viewModelScope.launch {
            try {
                val response = if (mediaUris.isNotEmpty()) {
                    createPostWithMedia(content, visibility, parentPostId, mediaUris)
                } else {
                    createPostWithoutMedia(content, visibility, parentPostId)
                }
                handlePostResponse(response)

            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
    // Helper: Xử lý response
    @OptIn(UnstableApi::class)
    private fun handlePostResponse(response: ApiResponse<Post>) {
        if (response.success) {
            fetchPosts(0)
            Log.d("TimelineViewModel", "Post created successfully")
        } else {
            _postsState.value = _postsState.value.copy(
                loading = false,
                error = response.message ?: "Failed to create post"
            )
        }
    }

    // Helper: Xử lý error
    @OptIn(UnstableApi::class)
    private fun handleError(e: Exception) {
        _postsState.value = _postsState.value.copy(
            loading = false,
            error = e.message ?: "Unknown error"
        )
        Log.e("TimelineViewModel", "Error creating post: ${e.message}")
    }
    // Tách thành các helper methods
    @OptIn(UnstableApi::class)
    private suspend fun createPostWithMedia(
        content: String,
        visibility: String,
        parentPostId: Long?,
        mediaUris: List<Uri>
    ): ApiResponse<Post> {
        val mediaParts = mediaUris.mapIndexedNotNull { index, uri ->
            createMediaPart(uri, "files", context)
        }
        try {
            val response = apiService.createPostWithMedia(
                content = content.toRequestBody("text/plain".toMediaTypeOrNull()),
                visibility = visibility.toRequestBody("text/plain".toMediaTypeOrNull()),
                parentPostId = parentPostId?.toString()
                    ?.toRequestBody("text/plain".toMediaTypeOrNull()),
                files = mediaParts.ifEmpty { null }
            )

            return response
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun createPostWithoutMedia(
        content: String,
        visibility: String,
        parentPostId: Long?
    ): ApiResponse<Post> {
        return RetrofitClient.apiService.createPost(
            CreatePostRequest(content,"TEXT", visibility, parentPostId)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun sharePost(originalPostId: Long, shareContent: String = "") {
        viewModelScope.launch {
            try {
                // Sử dụng addPost có sẵn với parentPostId và visibility mặc định PUBLIC
                addPost(
                    content = shareContent,
                    visibility = "PUBLIC", // Mặc định PUBLIC
                    parentPostId = originalPostId, // Set parentPostId = ID bài được share
                    mediaUris = emptyList() // Không có media khi share
                )
                // Tạo thông báo
                val originalPost = _postCache.value[originalPostId]
                activityViewModel.addNotification(
                    userId = originalPost?.user?.id ?: 0,
                    actorId = currentUserId ?: 0,
                    type = "SHARE_POST",
                    targetType = "POST",
                    targetId = originalPostId,
                    targetOwnerId = originalPost?.user?.id ?: 0
                )

            } catch (e: Exception) {
                _postsState.value = _postsState.value.copy(
                    error = e.message ?: "Lỗi khi chia sẻ bài đăng"
                )
            }
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun changePassword(
        newPassword: String
    ): Boolean {
        return try {
            val patchUserDTO = PatchUserDTO(
                password = newPassword
            )

            val response = apiService.updateCurrentUser(patchUserDTO)
            if (response.success) {
                Log.d("TimelineViewModel", "Password changed successfully")
                true
            } else {
                Log.e("TimelineViewModel", "Failed to change password: ${response.message}")
                false
            }
        } catch (e: Exception) {
            Log.e("TimelineViewModel", "Error changing password", e)
            false
        }
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
                val response = apiService.createMention(request)
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
                val response = apiService.createComment(request)
                if (response.success) {
                    val newComment = response.data
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
        var comments: List<Comment> = emptyList(),
        val error: String? = null
    )

    @OptIn(UnstableApi::class)
    suspend fun updateProfile(
        fullName: String,
        bio: String,
        email: String,
        profileImageUri: Uri? = null
    ): Boolean {
        return try {
            // Nếu có ảnh mới, upload ảnh trước
            if (profileImageUri != null) {
                val imageUploadSuccess = uploadProfileImage(profileImageUri)
                if (!imageUploadSuccess) {
                    return false
                }
            }
            // Cập nhật thông tin profile
            val patchUserDTO = PatchUserDTO(
                fullName = fullName,
                bio = bio,
                email = email
            )

            val response = apiService.updateCurrentUser(patchUserDTO)

            if (response.success) {
                // Cập nhật cache
                currentUserId?.let { userId ->
                    saveUserToCache(getUserById(userId))
                }
                true
            } else {
                Log.e("UpdateProfile", "Failed to update profile: ${response.message}")
                false
            }
        } catch (e: Exception) {
            Log.e("UpdateProfile", "Error updating profile", e)
            false
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun uploadProfileImage(imageUri: Uri): Boolean {
        return try {
            val imagePart = createMediaPart(imageUri, "file", context)
            if (imagePart == null) {
                Log.e("ProfileImageUpload", "Failed to create image part")
                return false
            }

            val response = RetrofitClient.apiService.updateProfilePicture(imagePart)

            if (response.success) {
                // Cập nhật cache với URL ảnh mới
                currentUserId?.let { userId ->
                    val currentUser = _userCache.value[userId]
                    currentUser?.let { user ->
                        val updatedUser = user.copy(profilePicture = response.data?.profilePicture)
                        saveUserToCache(updatedUser)
                    }
                }
                true
            } else {
                Log.e("ProfileImageUpload", "Failed to upload image: ${response.message}")
                false
            }
        } catch (e: Exception) {
            Log.e("ProfileImageUpload", "Error uploading image", e)
            false
        }
    }
    // Thêm function reportPost
    @OptIn(UnstableApi::class)
    suspend fun reportPost(
        postId: Long,
        reason: ReportReason,
        description: String
    ): Boolean {
        return try {
            val createReportRequest = CreateReportRequest(
                targetType = ReportTargetType.POST,
                targetId = postId,
                reason = reason,
                description = description.takeIf { it.isNotBlank() }
            )
            val response = apiService.createReport(createReportRequest)

            if (response.success) {
                Log.d("TimelineViewModel", "Report created successfully")
                true
            } else {
                Log.e("TimelineViewModel", "Failed to create report: ${response.message}")
                false
            }
        } catch (e: Exception) {
            Log.e("TimelineViewModel", "Error reporting post", e)
            false
        }
    }
}

// Helper: Tạo MultipartBody.Part từ URI mà không cần temp file
@OptIn(UnstableApi::class)
private fun createMediaPart(uri: Uri, filetype: String, context: Context): MultipartBody.Part? {
    return try {
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val extension = getExtensionFromMimeType(mimeType)
        val timestamp = System.currentTimeMillis()

        // Tạo filename unique
        val finalFileName = "upload_${timestamp}$extension"

        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bytes = inputStream.readBytes()
        inputStream.close()

        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())

        MultipartBody.Part.createFormData(
            filetype,
            finalFileName, //
            requestBody
        )
    } catch (e: Exception) {
        Log.e("MediaUpload", "Error: ${e.message}", e)
        null
    }
}
// Helper: Extension từ MIME type
private fun getExtensionFromMimeType(mimeType: String): String {
    return when (mimeType) {
        "image/jpeg" -> ".jpg"
        "image/png" -> ".png"
        "image/gif" -> ".gif"
        "image/webp" -> ".webp"
        "video/mp4" -> ".mp4"
        "video/quicktime" -> ".mov"
        "video/3gpp" -> ".3gp"
        else -> ""
    }
}

//private fun getMimeType(fileName: String): String {
//    return when {
//        fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
//        fileName.endsWith(".png", true) -> "image/png"
//        fileName.endsWith(".gif", true) -> "image/gif"
//        fileName.endsWith(".mp4", true) -> "video/mp4"
//        fileName.endsWith(".mov", true) -> "video/quicktime"
//        else -> "application/octet-stream"
//    }
//}