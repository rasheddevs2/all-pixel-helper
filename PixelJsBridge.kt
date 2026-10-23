package com.pixelhelper.app.bridge

import android.webkit.JavascriptInterface
import org.json.JSONObject

class PixelJsBridge(
    private val onPixelCallReceived: (pixelId: String?, eventName: String, paramsJson: String) -> Unit,
    private val onDataLayerPush: (payloadJson: String) -> Unit
) {
    @JavascriptInterface
    fun postFbqEvent(pixelId: String?, eventName: String?, payloadJson: String?) {
        if (eventName != null) {
            onPixelCallReceived(pixelId, eventName, payloadJson ?: "{}")
        }
    }

    @JavascriptInterface
    fun postDataLayerEvent(payloadJson: String?) {
        if (payloadJson != null) {
            onDataLayerPush(payloadJson)
        }
    }
}
