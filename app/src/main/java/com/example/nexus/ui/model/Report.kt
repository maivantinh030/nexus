package com.example.nexus.ui.model

// File: Report.kt


enum class ReportReason {
    SPAM,
    HARASSMENT,
    HATE_SPEECH,
    INAPPROPRIATE_CONTENT,
    FALSE_INFORMATION,
    VIOLENCE,
    SEXUAL_CONTENT,
    COPYRIGHT_VIOLATION,
    OTHER
}

enum class ReportTargetType {
    POST,
    COMMENT,
    USER
}

enum class ReportStatus {
    PENDING,
    REVIEWING,
    RESOLVED,
    REJECTED
}

data class CreateReportRequest(
    val targetType: ReportTargetType,
    val targetId: Long,
    val reason: ReportReason,
    val description: String? = null
)
