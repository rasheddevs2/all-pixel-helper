package com.pixelhelper.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pixelhelper.app.bridge.PixelJsBridge
import com.pixelhelper.app.bridge.TrackingScriptInjector
import com.pixelhelper.app.ui.BrowserViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserView(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    userAgentString = settings.userAgentString + " MobilePixelHelper/1.0"
                }

                val bridge = PixelJsBridge(
                    onPixelCallReceived = { pixelId, eventName, payload ->
                        viewModel.handleJsFbq(pixelId, eventName, payload)
                    },
                    onDataLayerPush = { payload ->
                        viewModel.handleDataLayerPush(payload)
                    }
                )
                addJavascriptInterface(bridge, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        request?.url?.toString()?.let { url ->
                            viewModel.handleInterceptedUrl(url)
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        viewModel.setLoading(true)
                        url?.let { viewModel.updateUrl(it) }
                        // Inject early proxy
                        view?.evaluateJavascript(TrackingScriptInjector.INJECTION_SCRIPT, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        viewModel.setLoading(false)
                        // Reinject to guarantee coverage of post-load dynamic snippets
                        view?.evaluateJavascript(TrackingScriptInjector.INJECTION_SCRIPT, null)
                    }
                }

                onWebViewCreated(this)
            }
        }
    )
}
