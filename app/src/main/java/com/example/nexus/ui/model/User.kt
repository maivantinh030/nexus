package com.example.nexus.ui.model

data class User(
    val id: Long? = null,
    val username: String = "",
    val email: String = "",
    val password_hash: String = "",
    val full_name: String? = null,
    val bio: String? = null,
    val profile_picture: String? = null,
    val is_verified: Boolean = false,
    val status: String = "ACTIVE", // "ACTIVE", "SUSPENDED", "DELETED", "PENDING_VERIFICATION"
    val created_at: String? = null,
    val updated_at: String? = null
)