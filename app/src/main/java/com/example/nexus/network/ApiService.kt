package com.example.nexus.network

import com.example.nexus.ui.model.ApiResponse
import com.example.nexus.ui.model.CommentResponse
import com.example.nexus.ui.model.CreateCommentRequest
import com.example.nexus.ui.model.CreateCommentResponse
import com.example.nexus.ui.model.CreateNotificationRequest
import com.example.nexus.ui.model.CreatePostRequest
import com.example.nexus.ui.model.CreateReportRequest
import com.example.nexus.ui.model.CreateUserRequest
import com.example.nexus.ui.model.Like
import com.example.nexus.ui.model.NotificationResponse
import com.example.nexus.ui.model.PatchUserDTO
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.PostResponse
import com.example.nexus.ui.model.PostResponseSingle
import com.example.nexus.ui.model.User
import com.example.nexus.ui.model.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

data class FollowDTO(
    val followerId: Long,
    val followedId: Long,
    val createdAt: String?
)

data class FollowResponse(
    val status: Int,
    val success: Boolean,
    val message: String? = null,
    val data: List<FollowDTO>
)
data class LoginRequest(
    val username: String,
    val password: String
)

data class CreateMentionRequest(
    val postId: Long?,
    val commentId: Long?,
    val mentionedUserId: Long
)

data class AuthenticationResponse(
    val access_token: String,
    val refresh_token: String,
    val user_id: Long,
    val roles: List<String>
)
data class ErrorResponse(
    val type: String?,
    val title: String?,
    val status: Int,
    val detail: String?,
    val instance: String?,
    val errorCode: String?
)
interface ApiService {

    @GET("yapping/api/posts")
    suspend fun getPosts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 5
    ): PostResponse
    @GET("yapping/api/posts/user/{userId}")
    suspend fun getPostsByUser(
        @Path("userId") userId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 5
    ): PostResponse

    @GET("yapping/api/posts/{id}")
    suspend fun getPostById(
        @Path("id") postId: Long
    ): PostResponseSingle

    @GET("yapping/api/comments/post/{postId}/root")
    suspend fun getCommentsRoot(
        @Path("postId") postId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): CommentResponse

    @GET("yapping/api/comments/{commentId}/replies")
    suspend fun getChildComments(
        @Path("commentId") commentId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("sortBy") sortBy: String = "createdAt",
        @Query("direction") direction: String = "asc"
    ): CommentResponse


    @GET("yapping/api/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "createdAt",
        @Query("direction") direction: String = "desc"
    ): NotificationResponse

    @GET("yapping/api/notifications/unread")
    suspend fun getUnreadNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "createdAt",
        @Query("direction") direction: String = "desc"
    ): NotificationResponse

    @GET("yapping/api/users/{id}")
    suspend fun getUserById(
        @Path("id") userId: Long
    ): UserResponse

    @GET("yapping/api/users/{userId}/followers")
    suspend fun getFollowers(
        @Path("userId") userId: Long
    ): FollowResponse

    @GET("yapping/api/users/{userId}/following")
    suspend fun getFollowing(
        @Path("userId") userId: Long
    ): FollowResponse
    @POST("yapping/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthenticationResponse

    @POST("yapping/api/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): AuthenticationResponse

    @POST("yapping/api/auth/logout")
    suspend fun logout(): ApiResponse<Any>

    @GET("yapping/api/likes/check")
    suspend fun hasUserLiked(
        @Query("userId") userId: Long,
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long
    ): ApiResponse<Boolean>

    @POST("yapping/api/likes/post/{postId}")
    suspend fun likePost(
        @Path("postId") postId: Long
    ): ApiResponse<Like>

    @POST("yapping/api/likes/comment/{commentId}")
    suspend fun likeComment(
        @Path("commentId") commentId: Long
    ): ApiResponse<Like>
    @DELETE("yapping/api/likes/user/{userId}/target")
    suspend fun unLike(
        @Path("userId") userId: Long,
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long
    ): ApiResponse<Any>

    @GET("yapping/api/likes/count")
    suspend fun countLikes(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long
    ): ApiResponse<Long>

    @POST("yapping/api/comments")
    suspend fun createComment(
        @Body request: CreateCommentRequest
    ): CommentResponse

    @Multipart
    @POST("yapping/api/posts-with-media")
    suspend fun createPostWithMedia(
        @Part("content") content: RequestBody,
        @Part("visibility") visibility: RequestBody,
        @Part("parentPostId") parentPostId: RequestBody?,
        @Part files: List<MultipartBody.Part>? //
    ): ApiResponse<Post>

    @POST("yapping/api/notifications")
    suspend fun createNotification(
        @Body request: CreateNotificationRequest
    ): ApiResponse<NotificationResponse>

    @POST("yapping/api/posts")
    suspend fun createPost(
        @Body request: CreatePostRequest
    ): ApiResponse<Post>

    @POST("yapping/api/users/{userId}/follow")
    suspend fun followUser(
        @Path("userId") userId: Long
    ): ApiResponse<Any>
    @DELETE("yapping/api/users/{userId}/unfollow")
    suspend fun unfollowUser(
        @Path("userId") userId: Long
    ): ApiResponse<Any>

    @POST("yapping/api/mentions/from-text")
    suspend fun createMention(
        @Body request: CreateMentionRequest
    ): ApiResponse<Any>
    @POST("yapping/api/users/register")
    suspend fun registerUser(
        @Body request: CreateUserRequest
    ): ApiResponse<UserResponse>

    @PATCH("yapping/api/users/me")
    suspend fun updateCurrentUser(
        @Body patchUserDTO: PatchUserDTO
    ): ApiResponse<User>

    @Multipart
    @PATCH("yapping/api/users/me/profile-picture")
    suspend fun updateProfilePicture(
        @Part file: MultipartBody.Part
    ): ApiResponse<User>

    @POST("/yapping/api/fcm/token")
    suspend fun updateFCMToken(
        @Body request: Map<String, String>
    ): ApiResponse<Any>

    @DELETE("/yapping/api/fcm/token")
    suspend fun removeFCMToken(): ApiResponse<Any>

    @POST("/yapping/api/reports")
    suspend fun createReport(
        @Body request: CreateReportRequest
    ): ApiResponse<Any>
}