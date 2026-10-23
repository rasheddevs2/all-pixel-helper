package com.pixelhelper.app.bridge

object TrackingScriptInjector {

    val INJECTION_SCRIPT = """
        (function() {
            // Guard against multiple injections
            if (window.__pixel_helper_injected) return;
            window.__pixel_helper_injected = true;

            // 1. Monkey patch fbq
            var originalFbq = window.fbq;
            function proxyFbq() {
                var args = Array.prototype.slice.call(arguments);
                try {
                    var action = args[0];
                    if (action === 'track' || action === 'trackCustom') {
                        var eventName = args[1];
                        var payload = args[2] || {};
                        var eventIdObj = args[3] || {};
                        
                        var merged = Object.assign({}, payload);
                        if (eventIdObj && eventIdObj.eventID) {
                            merged.__event_id = eventIdObj.eventID;
                        }
                        
                        if (window.AndroidBridge && window.AndroidBridge.postFbqEvent) {
                            window.AndroidBridge.postFbqEvent(null, eventName, JSON.stringify(merged));
                        }
                    }
                } catch(e) {
                    console.error("PixelHelper error wrapping fbq", e);
                }
                if (originalFbq) {
                    return originalFbq.apply(this, args);
                }
            }

            if (window.fbq) {
                for (var key in window.fbq) {
                    if (window.fbq.hasOwnProperty(key)) {
                        proxyFbq[key] = window.fbq[key];
                    }
                }
            }
            window.fbq = proxyFbq;

            // 2. Monkey patch dataLayer
            window.dataLayer = window.dataLayer || [];
            var originalPush = window.dataLayer.push;
            window.dataLayer.push = function() {
                var args = Array.prototype.slice.call(arguments);
                for (var i = 0; i < args.length; i++) {
                    try {
                        if (window.AndroidBridge && window.AndroidBridge.postDataLayerEvent) {
                            window.AndroidBridge.postDataLayerEvent(JSON.stringify(args[i]));
                        }
                    } catch(e) {}
                }
                return originalPush.apply(window.dataLayer, args);
            };

            // Initial poll for existing GTM tags
            if (window.google_tag_manager) {
                var keys = Object.keys(window.google_tag_manager);
                keys.forEach(function(k) {
                    if (k.indexOf('GTM-') === 0) {
                        if (window.AndroidBridge && window.AndroidBridge.postFbqEvent) {
                            window.AndroidBridge.postFbqEvent(k, "Container Discovered", "{}");
                        }
                    }
                });
            }
        })();
    """.trimIndent()
}
