package com.example.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.data.model.TabItem

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OpenVWebView(
    tab: TabItem,
    blockThirdPartyCookies: Boolean,
    safeBrowsingEnabled: Boolean,
    onTitleChanged: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onLoadingStateChanged: (Boolean) -> Unit,
    onNavigationStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onUrlChanged: (String) -> Unit,
    onSslStatusChanged: (Boolean) -> Unit,
    onErrorOccurred: (String) -> Unit,
    onSaveState: (Bundle) -> Unit,
    savedState: Bundle?,
    externalLoadUrl: String?,
    onExternalUrlConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize Android Safe Browsing once if supported
    LaunchedEffect(safeBrowsingEnabled) {
        if (safeBrowsingEnabled && WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(context) { /* Safe browsing initialized */ }
        }
    }

    val webView = remember(tab.id) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Security Hardening
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = !tab.isPrivate
                databaseEnabled = !tab.isPrivate

                // Prevent access to local filesystem and content providers
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false

                // Block mixed HTTP/HTTPS content
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                // Prevent popups from automatically spawning
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)

                // Cache policy based on private mode
                if (tab.isPrivate) {
                    cacheMode = WebSettings.LOAD_NO_CACHE
                } else {
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                // Desktop Mode User Agent switcher
                userAgentString = if (tab.isDesktopMode) {
                    DESKTOP_USER_AGENT
                } else {
                    userAgentString
                }
            }

            // Cookie isolation policy
            val cookieManager = CookieManager.getInstance()
            if (tab.isPrivate) {
                // In private tabs, accept session cookies temporarily, but do not retain
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, false)
            } else {
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, !blockThirdPartyCookies)
            }

            // Restore state if available
            if (savedState != null) {
                restoreState(savedState)
            }
        }
    }

    BackHandler(enabled = tab.canGoBack) {
        if (webView.canGoBack()) webView.goBack()
    }

    LaunchedEffect(safeBrowsingEnabled, webView) {
        webView.settings.safeBrowsingEnabled = safeBrowsingEnabled
    }

    // Update settings when desktop mode or cookie policy changes
    LaunchedEffect(tab.isDesktopMode) {
        webView.settings.userAgentString = if (tab.isDesktopMode) {
            DESKTOP_USER_AGENT
        } else {
            WebSettings.getDefaultUserAgent(context)
        }
        if (!tab.url.startsWith("openv://")) {
            webView.reload()
        }
    }

    LaunchedEffect(blockThirdPartyCookies) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !blockThirdPartyCookies && !tab.isPrivate)
    }

    // React to external URL load commands
    LaunchedEffect(externalLoadUrl) {
        if (externalLoadUrl != null && !externalLoadUrl.startsWith("openv://")) {
            webView.loadUrl(externalLoadUrl)
            onExternalUrlConsumed()
        }
    }

    DisposableEffect(tab.id) {
        val bundle = Bundle()
        onDispose {
            try {
                webView.saveState(bundle)
                onSaveState(bundle)
                if (tab.isPrivate) {
                    webView.clearHistory()
                    webView.clearCache(true)
                }
                webView.stopLoading()
                webView.destroy()
            } catch (_: Exception) {}
        }
    }

    androidx.compose.runtime.key(tab.id) {
    AndroidView(
        factory = {
            webView.apply {
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                        onLoadingStateChanged(newProgress < 100)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        if (!title.isNullOrBlank()) {
                            onTitleChanged(title)
                        }
                    }

                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: GeolocationPermissions.Callback?
                    ) {
                        // Strict security: do not give silent access to page geolocation
                        callback?.invoke(origin, false, false)
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        // Secure sandbox: deny automated camera/mic access from web pages
                        request?.deny()
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        val uri = request.url

                        // Handle HTTPS and HTTP
                        if (url.startsWith("https://") || url.startsWith("http://")) {
                            return false // Let WebView load it
                        }

                        // Handle external authorized intents safely (tel, mailto, geo)
                        val scheme = uri.scheme?.lowercase()
                        if (request.isForMainFrame && request.hasGesture() && (scheme == "tel" || scheme == "mailto" || scheme == "geo")) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            return true
                        }

                        // Block custom intent:// or apk downloads to protect user
                        return true
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onLoadingStateChanged(true)
                        if (url != null) {
                            onUrlChanged(url)
                            val isHttps = url.startsWith("https://")
                            onSslStatusChanged(isHttps)
                        }
                        onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingStateChanged(false)
                        onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                        if (url != null) {
                            onUrlChanged(url)
                            val isHttps = url.startsWith("https://")
                            onSslStatusChanged(isHttps)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                error?.description?.toString() ?: "Falha ao carregar página."
                            } else {
                                "Falha ao carregar página."
                            }
                            onErrorOccurred(description)
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        // Strict SSL Policy: Cancel on SSL errors to prevent MITM
                        handler?.cancel()
                        onSslStatusChanged(false)
                        onErrorOccurred("Certificado SSL inválido ou inseguro.")
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        // Prevent application crash if renderer runs out of memory or dies
                        onErrorOccurred("O processo do navegador foi reiniciado para liberar memória.")
                        return true
                    }
                }

                // Initial load if not on custom home
                if (tab.url != "openv://home" && tab.url != "about:blank") {
                    loadUrl(tab.url)
                }
            }
        },
        update = { wv ->
            // Handle back/forward updates
            onNavigationStateChanged(wv.canGoBack(), wv.canGoForward())
        },
        modifier = modifier.fillMaxSize()
    )
    }
}
