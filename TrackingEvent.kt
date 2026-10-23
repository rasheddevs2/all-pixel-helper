package com.pixelhelper.app.data.model

import java.util.UUID

enum class TrackingType {
    META_PIXEL,
    GA4,
    GTM_CONTAINER,
    DATA_LAYER
}

enum class DuplicateStatus {
    UNIQUE,
    SUSPECTED_DUPLICATE, // Same event name & pixel ID within short interval
    CONFIRMED_DUPLICATE  // Identical eventID received multiple times
}

data class TrackingEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: TrackingType,
    val pixelId: String,               // e.g., "123456789" or "G-XXXX" or "GTM-XXXX"
    val eventName: String,             // e.g., "PageView", "Purchase"
    val eventId: String? = null,       // CAPI deduplication eventID
    val timestamp: Long = System.currentTimeMillis(),
    val source: String,                // "Network Beacon", "JS Bridge", "dataLayer"
    val url: String,                   // URL where the event occurred
    val customParameters: Map<String, String> = emptyMap(),
    val userParameters: Map<String, String> = emptyMap(),
    val value: String? = null,
    val currency: String? = null,
    val contentIds: List<String> = emptyList(),
    val contentType: String? = null,
    val duplicateStatus: DuplicateStatus = DuplicateStatus.UNIQUE,
    val warnings: List<String> = emptyList()
)
