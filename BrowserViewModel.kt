package com.pixelhelper.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelhelper.app.data.model.DiagnosticIssue
import com.pixelhelper.app.data.model.DuplicateStatus
import com.pixelhelper.app.data.model.Severity
import com.pixelhelper.app.data.model.TrackingEvent
import com.pixelhelper.app.data.model.TrackingType
import com.pixelhelper.app.data.parser.Ga4GtmParser
import com.pixelhelper.app.data.parser.MetaPixelParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class BrowserUiState(
    val currentUrl: String = "https://example.com",
    val isLoading: Boolean = false,
    val events: List<TrackingEvent> = emptyList(),
    val detectedPixelIds: Set<String> = emptySet(),
    val detectedGtmIds: Set<String> = emptySet(),
    val detectedGa4Ids: Set<String> = emptySet(),
    val filterPixelId: String? = null,
    val filterEventName: String? = null,
    val diagnostics: List<DiagnosticIssue> = emptyList()
)

class BrowserViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    fun updateUrl(newUrl: String) {
        _uiState.update { it.copy(currentUrl = newUrl) }
    }

    fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    fun clearEvents() {
        _uiState.update {
            it.copy(
                events = emptyList(),
                detectedPixelIds = emptySet(),
                detectedGtmIds = emptySet(),
                detectedGa4Ids = emptySet(),
                diagnostics = emptyList()
            )
        }
    }

    fun setFilterPixel(pixelId: String?) {
        _uiState.update { it.copy(filterPixelId = pixelId) }
    }

    fun handleInterceptedUrl(urlString: String) {
        val uri = try {
            Uri.parse(urlString)
        } catch (e: Exception) {
            return
        }

        when {
            MetaPixelParser.isMetaPixelRequest(urlString) -> {
                val event = MetaPixelParser.parseUri(uri, source = "Network Beacon")
                if (event != null) {
                    processNewEvent(event)
                }
            }
            Ga4GtmParser.isGa4Request(urlString) -> {
                val event = Ga4GtmParser.parseGa4(uri)
                if (event != null) {
                    processNewEvent(event)
                }
            }
            Ga4GtmParser.isGtmRequest(urlString) -> {
                val event = Ga4GtmParser.parseGtm(uri)
                if (event != null) {
                    processNewEvent(event)
                }
            }
        }
    }

    fun handleJsFbq(pixelId: String?, eventName: String, paramsJson: String) {
        val params = mutableMapOf<String, String>()
        var eventId: String? = null
        try {
            val json = JSONObject(paramsJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key == "__event_id") {
                    eventId = json.optString(key)
                } else {
                    params[key] = json.optString(key)
                }
            }
        } catch (e: Exception) {
            // Non-JSON or primitive payload
        }

        val event = TrackingEvent(
            type = TrackingType.META_PIXEL,
            pixelId = pixelId ?: "Inferred (JS)",
            eventName = eventName,
            eventId = eventId,
            source = "JS Bridge (fbq)",
            url = _uiState.value.currentUrl,
            customParameters = params,
            value = params["value"],
            currency = params["currency"]
        )
        processNewEvent(event)
    }

    fun handleDataLayerPush(payloadJson: String) {
        val params = mutableMapOf<String, String>()
        var eventName = "dataLayer.push"
        try {
            val json = JSONObject(payloadJson)
            if (json.has("event")) {
                eventName = json.getString("event")
            }
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                params[k] = json.optString(k)
            }
        } catch (e: Exception) {
            params["raw"] = payloadJson
        }

        val event = TrackingEvent(
            type = TrackingType.DATA_LAYER,
            pixelId = "dataLayer",
            eventName = eventName,
            source = "window.dataLayer",
            url = _uiState.value.currentUrl,
            customParameters = params
        )
        processNewEvent(event)
    }

    @Synchronized
    private fun processNewEvent(rawEvent: TrackingEvent) {
        viewModelScope.launch {
            val currentList = _uiState.value.events
            var duplicateStatus = DuplicateStatus.UNIQUE

            // Deduplication Engine
            val confirmedDup = currentList.firstOrNull { existing ->
                rawEvent.eventId != null &&
                rawEvent.eventId == existing.eventId &&
                rawEvent.pixelId == existing.pixelId &&
                rawEvent.eventName == existing.eventName
            }

            if (confirmedDup != null) {
                duplicateStatus = DuplicateStatus.CONFIRMED_DUPLICATE
            } else {
                // Suspected duplicate: matching pixel & name within 1500ms
                val suspectedDup = currentList.firstOrNull { existing ->
                    rawEvent.pixelId == existing.pixelId &&
                    rawEvent.eventName == existing.eventName &&
                    Math.abs(rawEvent.timestamp - existing.timestamp) < 1500
                }
                if (suspectedDup != null) {
                    duplicateStatus = DuplicateStatus.SUSPECTED_DUPLICATE
                }
            }

            val finalEvent = rawEvent.copy(duplicateStatus = duplicateStatus)

            _uiState.update { state ->
                val updatedEvents = listOf(finalEvent) + state.events
                val pixelIds = updatedEvents
                    .filter { it.type == TrackingType.META_PIXEL && it.pixelId != "Inferred (JS)" }
                    .map { it.pixelId }.toSet()
                val gtmIds = updatedEvents
                    .filter { it.type == TrackingType.GTM_CONTAINER }
                    .map { it.pixelId }.toSet()
                val ga4Ids = updatedEvents
                    .filter { it.type == TrackingType.GA4 }
                    .map { it.pixelId }.toSet()

                val diagnostics = recomputeDiagnostics(updatedEvents)

                state.copy(
                    events = updatedEvents,
                    detectedPixelIds = pixelIds,
                    detectedGtmIds = gtmIds,
                    detectedGa4Ids = ga4Ids,
                    diagnostics = diagnostics
                )
            }
        }
    }

    private fun recomputeDiagnostics(events: List<TrackingEvent>): List<DiagnosticIssue> {
        val issues = mutableListOf<DiagnosticIssue>()

        val duplicateCount = events.count { it.duplicateStatus != DuplicateStatus.UNIQUE }
        if (duplicateCount > 0) {
            issues.add(
                DiagnosticIssue(
                    title = "Potential Duplicated Events Detected",
                    description = "$duplicateCount event(s) appeared multiple times. Verify that tracking scripts are not injected multiple times (e.g., both natively and through GTM).",
                    severity = Severity.WARNING
                )
            )
        }

        val purchaseEvents = events.filter { it.eventName.equals("Purchase", ignoreCase = true) }
        purchaseEvents.forEach { p ->
            if (p.value == null || p.currency == null) {
                issues.add(
                    DiagnosticIssue(
                        title = "Invalid Purchase Event",
                        description = "Purchase event fired without required 'value' and/or 'currency' parameters.",
                        severity = Severity.ERROR,
                        eventId = p.id
                    )
                )
            }
        }

        val noEventIdCount = events.filter { it.type == TrackingType.META_PIXEL && it.eventId.isNullOrBlank() }.size
        if (noEventIdCount > 0) {
            issues.add(
                DiagnosticIssue(
                    title = "Missing EventID for Meta CAPI Deduplication",
                    description = "$noEventIdCount Meta Pixel event(s) have no eventID. If Conversions API is active, Meta cannot deduplicate these hits against server events.",
                    severity = Severity.INFO
                )
            )
        }

        return issues
    }
}
