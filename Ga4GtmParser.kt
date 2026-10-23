package com.pixelhelper.app.data.parser

import android.net.Uri
import com.pixelhelper.app.data.model.TrackingEvent
import com.pixelhelper.app.data.model.TrackingType

object Ga4GtmParser {

    fun isGa4Request(url: String): Boolean {
        return (url.contains("google-analytics.com/g/collect") || 
                url.contains("analytics.google.com/g/collect"))
    }

    fun isGtmRequest(url: String): Boolean {
        return url.contains("googletagmanager.com/gtm.js")
    }

    fun parseGa4(uri: Uri): TrackingEvent? {
        val tid = uri.getQueryParameter("tid") ?: return null
        val eventName = uri.getQueryParameter("en") ?: "page_view"
        val params = mutableMapOf<String, String>()

        for (key in uri.queryParameterNames) {
            if (key.startsWith("ep.") || key.startsWith("epn.")) {
                val cleanKey = key.removePrefix("ep.").removePrefix("epn.")
                params[cleanKey] = uri.getQueryParameter(key) ?: ""
            }
        }

        return TrackingEvent(
            type = TrackingType.GA4,
            pixelId = tid,
            eventName = eventName,
            source = "GA4 Measurement",
            url = uri.getQueryParameter("dl") ?: "",
            customParameters = params
        )
    }

    fun parseGtm(uri: Uri): TrackingEvent? {
        val gtmId = uri.getQueryParameter("id") ?: return null
        return TrackingEvent(
            type = TrackingType.GTM_CONTAINER,
            pixelId = gtmId,
            eventName = "Container Loaded",
            source = "GTM Script",
            url = uri.toString()
        )
    }
}
