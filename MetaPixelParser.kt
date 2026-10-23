package com.pixelhelper.app.data.parser

import android.net.Uri
import com.pixelhelper.app.data.model.DuplicateStatus
import com.pixelhelper.app.data.model.TrackingEvent
import com.pixelhelper.app.data.model.TrackingType
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object MetaPixelParser {

    private const val META_ENDPOINT = "facebook.com/tr"

    fun isMetaPixelRequest(url: String): Boolean {
        return url.contains(META_ENDPOINT)
    }

    fun parseUri(uri: Uri, source: String = "Network Beacon"): TrackingEvent? {
        val pixelId = uri.getQueryParameter("id") ?: return null
        val eventName = uri.getQueryParameter("ev") ?: "Unknown"
        val eventId = uri.getQueryParameter("eid")
        val documentLocation = uri.getQueryParameter("dl") ?: ""

        val customParams = mutableMapOf<String, String>()
        val userParams = mutableMapOf<String, String>()
        val warnings = mutableListOf<String>()

        var value: String? = null
        var currency: String? = null
        val contentIds = mutableListOf<String>()
        var contentType: String? = null

        for (paramName in uri.queryParameterNames) {
            val paramValue = uri.getQueryParameter(paramName)?.let {
                try {
                    URLDecoder.decode(it, StandardCharsets.UTF_8.name())
                } catch (e: Exception) {
                    it
                }
            } ?: continue

            when {
                paramName == "cd[value]" -> value = paramValue
                paramName == "cd[currency]" -> currency = paramValue
                paramName == "cd[content_type]" -> contentType = paramValue
                paramName == "cd[content_ids]" -> {
                    val cleaned = paramValue.removeSurrounding("[", "]").replace("\"", "")
                    contentIds.addAll(cleaned.split(",").map { it.trim() })
                }
                paramName.startsWith("cd[") -> {
                    val key = paramName.removeSurrounding("cd[", "]")
                    customParams[key] = paramValue
                }
                paramName.startsWith("ud[") -> {
                    val key = paramName.removeSurrounding("ud[", "]")
                    userParams[key] = "[Hashed: $paramValue]"
                }
            }
        }

        // Validate critical parameters
        if (eventName in listOf("Purchase", "InitiateCheckout") && value == null) {
            warnings.add("Missing parameter: '$eventName' should usually contain a 'value' and 'currency'.")
        }
        if (value != null && currency == null) {
            warnings.add("Value was sent without a matching ISO 4217 Currency code.")
        }
        if (eventId == null) {
            warnings.add("No 'eventID' found. If implementing Meta Conversions API (CAPI), deduplication will fail.")
        }

        return TrackingEvent(
            type = TrackingType.META_PIXEL,
            pixelId = pixelId,
            eventName = eventName,
            eventId = eventId,
            source = source,
            url = documentLocation,
            customParameters = customParams,
            userParameters = userParams,
            value = value,
            currency = currency,
            contentIds = contentIds,
            contentType = contentType,
            duplicateStatus = DuplicateStatus.UNIQUE,
            warnings = warnings
        )
    }
}
