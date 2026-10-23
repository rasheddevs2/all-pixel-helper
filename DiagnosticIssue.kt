package com.pixelhelper.app.data.model

data class DiagnosticIssue(
    val title: String,
    val description: String,
    val severity: Severity,
    val eventId: String? = null
)

enum class Severity {
    INFO,
    WARNING,
    ERROR
}
