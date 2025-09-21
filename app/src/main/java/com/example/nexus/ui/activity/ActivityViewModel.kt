package com.example.nexus.ui.activity

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.network.ApiService
import com.example.nexus.network.RetrofitClient
import com.example.nexus.ui.model.CreateNotificationRequest
import com.example.nexus.ui.model.MarkNotificationsReadDTO
import com.example.nexus.ui.model.Notification
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ActivityViewModel(
    private val apiService: ApiService,
    private val fcmManager: FCMManager
) : ViewModel() {

    val context = fcmManager.context
    init {
        // Khởi tạo FCM token khi ViewModel được tạo
        initializeFCM()
        debugFCMToken()
    }
    private fun initializeFCM() {
        viewModelScope.launch {
            try {
                val token = fcmManager.getFCMToken()
                token?.let {
                    // Gửi token lên server
                    sendFCMTokenToServer(it)
                }
            } catch (e: Exception) {
                Log.e("ActivityViewModel", "Error initializing FCM", e)
            }
        }
    }
    private suspend fun sendFCMTokenToServer(token: String) {
        try {
            val requestBody = mapOf("fcmToken" to token)
            apiService.updateFCMToken(requestBody)
            Log.d("ActivityViewModel", "FCM token sent to server successfully")
        } catch (e: Exception) {
            Log.e("ActivityViewModel", "Error sending FCM token to server", e)
        }
    }
    fun removeFCMToken() {
        viewModelScope.launch {
            try {
                apiService.removeFCMToken()
                Log.d("ActivityViewModel", "FCM token removed from server")
            } catch (e: Exception) {
                Log.e("ActivityViewModel", "Error removing FCM token", e)
            }
        }
    }
    fun refreshFCMToken() {
        viewModelScope.launch {
            try {
                Log.d("FCM_REFRESH", "=== REFRESH FCM TOKEN ===")

                // Xóa token cũ trước
                FirebaseMessaging.getInstance().deleteToken().await()
                Log.d("FCM_REFRESH", "Old token deleted")

                // Lấy token mới
                val newToken = fcmManager.getFCMToken() // Này sẽ tạo token mới
                newToken?.let {
                    sendFCMTokenToServer(it)
                    Log.d("FCM_REFRESH", "New token refreshed and sent successfully")
                }
            } catch (e: Exception) {
                Log.e("ActivityViewModel", "Error refreshing FCM token", e)
            }
        }
    }
    fun debugFCMToken() {
        viewModelScope.launch {
            try {
                Log.d("FCM_DEBUG", "=== FCM TOKEN DEBUG ===")

                // Lấy token mới
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d("FCM_DEBUG", "FCM Token: $token")
                Log.d("FCM_DEBUG", "Token length: ${token?.length}")

                // Kiểm tra Firebase project info
                FirebaseApp.getInstance().options.let { options ->
                    Log.d("FCM_DEBUG", "Firebase Project ID: ${options.projectId}")
                    Log.d("FCM_DEBUG", "Firebase App ID: ${options.applicationId}")
                    Log.d("FCM_DEBUG", "Package name: ${context.packageName}")
                }

                // Test gửi lên server
                token?.let {
                    Log.d("FCM_DEBUG", "Sending token to server...")
                    sendFCMTokenToServer(it)
                }

            } catch (e: Exception) {
                Log.e("FCM_DEBUG", "FCM Debug failed", e)
            }
        }
    }
    private val _notificationState = MutableStateFlow(NotificationState())
    val notificationState: StateFlow<NotificationState> = _notificationState.asStateFlow()

    fun fetchNotifications(page: Int = 0) {
        viewModelScope.launch {
            try {
                Log.d("ActivityViewModel", "Fetching notifications for page: $page")
                _notificationState.value = _notificationState.value.copy(
                    loading = page == 0,
                    loadingMore = page > 0,
                    error = null
                )

                val response = RetrofitClient.apiService.getNotifications(
                    page = page,
                    size = 20,
                    sortBy = "createdAt",
                    direction = "desc"
                )

                Log.d("ActivityViewModel", "API Response - Success: ${response.success}")
                Log.d("ActivityViewModel", "API Response - Message: ${response.message}")
                Log.d("ActivityViewModel", "API Response - Data size: ${response.data.content.size}")

                if (response.success) {
                    val newNotifications = if (page == 0) {
                        response.data.content
                    } else {
                        _notificationState.value.notifications + response.data.content
                    }

                    _notificationState.value = _notificationState.value.copy(
                        loading = false,
                        loadingMore = false,
                        notifications = newNotifications,
                        currentPage = page,
                        last = response.data.last,
                        error = null
                    )

                    Log.d("ActivityViewModel", "Updated state with ${newNotifications.size} notifications")
                } else {
                    val errorMessage = response.message ?: "Không thể lấy thông báo"
                    Log.e("ActivityViewModel", "API Error: $errorMessage")
                    _notificationState.value = _notificationState.value.copy(
                        loading = false,
                        loadingMore = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Lỗi kết nối"
                Log.e("ActivityViewModel", "Exception in fetchNotifications: $errorMessage", e)
                _notificationState.value = _notificationState.value.copy(
                    loading = false,
                    loadingMore = false,
                    error = errorMessage
                )
            }
        }
    }

    fun fetchUnreadNotifications(page: Int = 0) {
        viewModelScope.launch {
            try {
                Log.d("ActivityViewModel", "Fetching unread notifications for page: $page")
                _notificationState.value = _notificationState.value.copy(
                    loading = page == 0,
                    loadingMore = page > 0
                )

                val response = RetrofitClient.apiService.getUnreadNotifications(
                    page = page,
                    size = 20,
                    sortBy = "createdAt",
                    direction = "desc"
                )

                Log.d("ActivityViewModel", "Unread API Response - Success: ${response.success}")

                if (response.success) {
                    val newNotifications = if (page == 0) {
                        response.data.content
                    } else {
                        _notificationState.value.notifications + response.data.content
                    }

                    _notificationState.value = _notificationState.value.copy(
                        loading = false,
                        loadingMore = false,
                        notifications = newNotifications,
                        currentPage = page,
                        last = response.data.last,
                        error = null
                    )
                } else {
                    val errorMessage = response.message ?: "Không thể lấy thông báo chưa đọc"
                    Log.e("ActivityViewModel", "Unread API Error: $errorMessage")
                    _notificationState.value = _notificationState.value.copy(
                        loading = false,
                        loadingMore = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Lỗi kết nối"
                Log.e("ActivityViewModel", "Exception in fetchUnreadNotifications: $errorMessage", e)
                _notificationState.value = _notificationState.value.copy(
                    loading = false,
                    loadingMore = false,
                    error = errorMessage
                )
            }
        }
    }

    fun loadMoreNotifications() {
        if (!_notificationState.value.loadingMore && !_notificationState.value.last) {
            Log.d("ActivityViewModel", "Loading more notifications...")
            fetchNotifications(page = _notificationState.value.currentPage + 1)
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try{
                val response = RetrofitClient.apiService.markNotificationAsRead(
                    MarkNotificationsReadDTO(listOf(notificationId)))
            }
            catch (e: Exception) {
                Log.e("ActivityViewModel", "Error marking notification as read: ${e.message}", e)
            }
        }
    }

    // Cập nhật addNotification method để match với model mới
    fun addNotification(
        userId: Long,
        actorId: Long,
        type: String,
        targetType: String,
        targetId: Long?,
        targetOwnerId: Long?,
    ) {
        viewModelScope.launch {
            try{
                val createNotificationRequest = CreateNotificationRequest(
                    userId = userId,
                    actorId = actorId,
                    type = type,
                    targetType = targetType,
                    targetId = targetId ?: 0L, // Nếu không có targetId thì dùng 0
                    targetOwnerId = targetOwnerId ?: 0L // Nếu không có targetOwnerId thì dùng 0
                )
                val response = RetrofitClient.apiService.createNotification(createNotificationRequest)
                if(response.success){
                    Log.d("ActivityViewModel", "Notification added successfully: ${response.data}")
                    // Cập nhật state với thông báo mới
                    _notificationState.value = _notificationState.value.copy(
                        notifications = (_notificationState.value.notifications + response.data) as List<Notification>,
                        error = null
                    )
                } else {
                    Log.e("ActivityViewModel", "Error adding notification: ${response.message}")
                    _notificationState.value = _notificationState.value.copy(
                        error = response.message ?: "Không thể thêm thông báo"
                    )
                }
            }

            catch (e: Exception) {
                Log.e("ActivityViewModel", "Error adding notification: ${e.message}", e)

            }
        }
    }

    data class NotificationState(
        val loading: Boolean = true,
        val loadingMore: Boolean = false,
        val notifications: List<Notification> = emptyList(),
        val currentPage: Int = 0,
        val last: Boolean = false,
        val error: String? = null
    )
}
class FCMManager(internal val context: Context) {

    companion object {
        private const val TAG = "FCMManager"
        private const val PREF_FCM_TOKEN = "fcm_token"
    }

    private val sharedPreferences = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)

    suspend fun getFCMToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM Token: $token")

            // Lưu token vào SharedPreferences
            sharedPreferences.edit()
                .putString(PREF_FCM_TOKEN, token)
                .apply()

            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
            null
        }
    }

    fun getCachedToken(): String? {
        return sharedPreferences.getString(PREF_FCM_TOKEN, null)
    }

    suspend fun refreshToken(): String? {
        return try {
            FirebaseMessaging.getInstance().deleteToken().await()
            getFCMToken()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh FCM token", e)
            null
        }
    }

//    fun subscribeToTopic(topic: String) {
//        FirebaseMessaging.getInstance().subscribeToTopic(topic)
//            .addOnCompleteListener { task ->
//                var msg = "Subscribed to $topic"
//                if (!task.isSuccessful) {
//                    msg = "Subscribe failed"
//                }
//                Log.d(TAG, msg)
//            }
//    }
//
//    fun unsubscribeFromTopic(topic: String) {
//        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
//            .addOnCompleteListener { task ->
//                var msg = "Unsubscribed from $topic"
//                if (!task.isSuccessful) {
//                    msg = "Unsubscribe failed"
//                }
//                Log.d(TAG, msg)
//            }
//    }
}