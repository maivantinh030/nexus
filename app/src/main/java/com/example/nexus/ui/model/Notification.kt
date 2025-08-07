package com.example.nexus.ui.model

data class Notification(
    val id: Long,                    // Đổi từ notificationId thành id
    val userId: Long,
    val username: String,
    val userFullName: String,
    val userProfilePicture: String?,
    val actorId: Long?,
    val actorUsername: String?,
    val actorFullName: String?,
    val actorProfilePicture: String?,
    val type: String,
    val targetType: String,
    val targetId: Long?,
    val targetOwnerId: Long?,
    val targetOwnerUsername: String?,
    val targetOwnerFullName: String?,
    val isRead: Boolean,
    val createdAt: String,
    val message: String,
    val redirectUrl: String
)
// NotificationResponse Model
data class NotificationResponse(
    val status: Int,
    val success: Boolean,
    val message: String,
    val data: NotificationPageData
)
// NotificationPageData Model
data class NotificationPageData(
    val content: List<Notification>,
    val pageable: Pageable,
    val totalElements: Int,
    val totalPages: Int,
    val last: Boolean,
    val first: Boolean,
    val size: Int,
    val number: Int,
    val sort: Sort,
    val numberOfElements: Int,
    val empty: Boolean
)

// Supporting classes
data class Pageable(
    val pageNumber: Int,
    val pageSize: Int,
    val sort: Sort
)

data class Sort(
    val orders: List<SortOrder>
)

data class SortOrder(
    val direction: String,
    val property: String
)