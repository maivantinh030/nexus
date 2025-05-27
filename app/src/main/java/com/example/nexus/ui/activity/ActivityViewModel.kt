package com.example.nexus.ui.activity

import androidx.lifecycle.ViewModel
import com.example.nexus.ui.model.Post
import com.example.nexus.ui.model.User
import com.example.nexus.ui.timeline.TimelineViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActivityViewModel(private val timelineViewModel: TimelineViewModel) {
    data class Notification(
        val notification_id: Long,
        val user_id: Long, // Người nhận thông báo
        val actor_id: Long?, // Người thực hiện hành động
        val type: String, // "LIKE_POST", "LIKE_COMMENT", "COMMENT", "REPLY_POST", "FOLLOW", v.v.
        val target_type: String?, // "POST", "COMMENT", "USER"
        val target_id: Long?, // ID của bài đăng, bình luận, hoặc người dùng
        val target_owner_id: Long?, // ID của người sở hữu mục tiêu (dùng để điều hướng)
        val is_read: Boolean = false,
        val created_at: String
    )

    private val _notifications = MutableStateFlow<List<Notification>>(createMockNotifications())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    fun addNotification(
        userId: Long,
        actorId: Long?,
        type: String,
        targetType: String?,
        targetId: Long?,
        targetOwnerId: Long?,
        createdAt: String
    ) {
        val newNotification = Notification(
            notification_id = (_notifications.value.maxOfOrNull { it.notification_id } ?: 0) + 1,
            user_id = userId,
            actor_id = actorId,
            type = type,
            target_type = targetType,
            target_id = targetId,
            target_owner_id = targetOwnerId,
            is_read = false,
            created_at = createdAt
        )
        _notifications.value = _notifications.value + newNotification
    }

    fun markAsRead(notificationId: Long) {
        _notifications.value = _notifications.value.map { notification ->
            if (notification.notification_id == notificationId) {
                notification.copy(is_read = true)
            } else {
                notification
            }
        }
    }

    private fun createMockNotifications(): List<Notification> {
        return listOf(
            Notification(
                notification_id = 1,
                user_id = 1, // Thông báo gửi đến người dùng 1
                actor_id = 2,
                type = "LIKE_POST",
                target_type = "POST",
                target_id = 1,
                target_owner_id = 1,
                is_read = false,
                created_at = "2025-05-13T14:55:00Z"
            ),
            Notification(
                notification_id = 2,
                user_id = 1,
                actor_id = 3,
                type = "COMMENT",
                target_type = "POST",
                target_id = 1,
                target_owner_id = 1,
                is_read = false,
                created_at = "2025-05-13T14:55:00Z"
            ),
            Notification(
                notification_id = 3,
                user_id = 1,
                actor_id = 2,
                type = "FOLLOW",
                target_type = "USER",
                target_id = 1,
                target_owner_id = 1,
                is_read = false,
                created_at = "2025-05-13T14:55:00Z"
            )
        )
    }
}