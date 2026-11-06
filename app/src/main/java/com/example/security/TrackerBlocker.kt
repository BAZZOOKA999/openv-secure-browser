package com.example.security

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

object TrackerBlocker {

    private val blockedTrackerDomains = listOf(
        "google-analytics.com",
        "googletagmanager.com",
        "doubleclick.net",
        "googleadservices.com",
        "pagead2.googlesyndication.com",
        "analytics.twitter.com",
        "connect.facebook.net",
        "pixel.facebook.com",
        "hotjar.com",
        "criteo.com",
        "outbrain.com",
        "taboola.com",
        "scorecardresearch.com",
        "segment.io",
        "mixpanel.com",
        "amplitude.com",
        "clarity.ms",
        "adnxs.com",
        "advertising.com",
        "rubiconproject.com",
        "pubmatic.com",
        "quantserve.com",
        "chartbeat.com",
        "newrelic.com",
        "bugsnag.com",
        "sentry.io",
        "adroll.com"
    )

    private val blockedCounter = AtomicInteger(0)

    val blockedCount: Int
        get() = blockedCounter.get()

    fun shouldBlockUrl(url: String): Boolean {
        val lower = url.lowercase()
        for (domain in blockedTrackerDomains) {
            if (lower.contains("://$domain") || lower.contains(".$domain")) {
                blockedCounter.incrementAndGet()
                return true
            }
        }
        return false
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            200,
            "OK",
            mapOf("Access-Control-Allow-Origin" to "*"),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    fun resetCounter() {
        blockedCounter.set(0)
    }
}
