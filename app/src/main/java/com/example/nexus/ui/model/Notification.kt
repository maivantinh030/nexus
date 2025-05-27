package com.example.nexus.ui.model

data class Notification(
    val notification_id: Long,
    val user_id: Long,
    val actor_id: Long?,
    val type: String,
    val target_type: String?,
    val target_id: Long?,
    val target_owner_id: Long?,
    val is_read: Boolean = false,
    val created_at: String
)