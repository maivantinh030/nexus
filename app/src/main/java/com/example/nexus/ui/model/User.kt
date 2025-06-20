package com.example.nexus.ui.model

data class User(
    val id: Long? = null,
    val username: String = "",
    val email: String = "",
    val passwordHash: String = "",
    val fullName: String? = null,
    val bio: String? = null,
    val profilePicture: String? = null,
    val isVerified: Boolean = false,
    val status: String = "ACTIVE", // "ACTIVE", "SUSPENDED", "DELETED", "PENDING_VERIFICATION"
    val createdAt: String? = null,
    val updatedAt: String? = null
)
data class UserResponse(
    val status: Int,
    val success: Boolean,
    val message: String? = null,
    val data: User
)