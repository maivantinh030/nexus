package com.example.nexus.ui.model

data class ApiResponse<T>(
    val status: Int,
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)