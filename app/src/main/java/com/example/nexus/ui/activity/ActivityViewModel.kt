package com.example.nexus.ui.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.network.RetrofitClient
import com.example.nexus.ui.model.CreateNotificationRequest
import com.example.nexus.ui.model.Notification
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActivityViewModel : ViewModel() {

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
        _notificationState.value = _notificationState.value.copy(
            notifications = _notificationState.value.notifications.map { notification ->
                if (notification.id == notificationId) {  // Đổi từ notificationId thành id
                    notification.copy(isRead = true)
                } else {
                    notification
                }
            }
        )
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