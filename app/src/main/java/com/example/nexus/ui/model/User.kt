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
data class CreateUserRequest(
    val username: String,
    val email: String,
    val fullName: String,
    val password: String,
    val bio: String? = null,
    val profilePicture: String? = null
)
data class PatchUserDTO(
    val username: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val bio: String? = null,
    val profilePicture: String? = null
)
