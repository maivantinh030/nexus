package com.example.nexus.ui.model

data class MarkNotificationsReadDTO(
    val notificationIds: List<Long>? = null,
    val allNotifications: Boolean = false
)
