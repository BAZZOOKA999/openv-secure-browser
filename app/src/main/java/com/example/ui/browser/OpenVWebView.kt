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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.data.model.TabItem
import com.example.security.TrackerBlocker

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
    onDownloadRequested: (url: String, userAgent: String, contentDisposition: String, mimeType: String) -> Unit,
    savedState: Bundle?,
    externalLoadUrl: String?,
    onExternalUrlConsumed: () -> Unit,
    findQuery: String? = null,
    zoomAction: Int = 0, // 1 = zoomIn, -1 = zoomOut
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize Safe Browsing
    LaunchedEffect(safeBrowsingEnabled) {
        if (safeBrowsingEnabled && WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(context) { /* initialized */ }
        }
    }

    val webView = remember(tab.id) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = !tab.isPrivate
                databaseEnabled = !tab.isPrivate

                // Prevent local file system access
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false

                // Block mixed HTTP/HTTPS content
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                // Prevent automatic popups
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)

                // Cache policy based on mode
                cacheMode = if (tab.isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT

                // Zoom controls
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false

                userAgentString = if (tab.isDesktopMode) DESKTOP_USER_AGENT else userAgentString
            }

            // Cookie policy
            val cookieManager = CookieManager.getInstance()
            if (tab.isPrivate) {
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, false)
            } else {
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, !blockThirdPartyCookies)
            }

            setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                onDownloadRequested(url, userAgent, contentDisposition, mimetype)
            }

            if (savedState != null) {
                restoreState(savedState)
            }
        }
    }

    BackHandler(enabled = tab.canGoBack) { if (webView.canGoBack()) webView.goBack() }

    LaunchedEffect(safeBrowsingEnabled, webView) {
        webView.settings.safeBrowsingEnabled = safeBrowsingEnabled
    }

    // React to Find in Page query
    LaunchedEffect(findQuery) {
        if (!findQuery.isNullOrBlank()) {
            webView.findAllAsync(findQuery)
        } else {
            webView.clearMatches()
        }
    }

    // React to Zoom In / Out
    LaunchedEffect(zoomAction) {
        if (zoomAction > 0) {
            webView.zoomIn()
        } else if (zoomAction < 0) {
            webView.zoomOut()
        }
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
                        callback?.invoke(origin, false, false)
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
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

                        if (url.startsWith("https://") || url.startsWith("http://")) {
                            return false
                        }

                        val scheme = uri.scheme?.lowercase()
                        if (request.isForMainFrame && request.hasGesture() && (scheme == "tel" || scheme == "mailto" || scheme == "geo")) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            return true
                        }

                        return true
                    }

                    // Real Tracker Blocker interceptor
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val reqUrl = request?.url?.toString() ?: return null
                        if (TrackerBlocker.shouldBlockUrl(reqUrl)) {
                            return TrackerBlocker.createEmptyResponse()
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onLoadingStateChanged(true)
                        if (url != null) {
                            onUrlChanged(url)
                            onSslStatusChanged(url.startsWith("https://"))
                        }
                        onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingStateChanged(false)
                        onNavigationStateChanged(view?.canGoBack() == true, view?.canGoForward() == true)
                        if (url != null) {
                            onUrlChanged(url)
                            onSslStatusChanged(url.startsWith("https://"))
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
                        handler?.cancel()
                        onSslStatusChanged(false)
                        onErrorOccurred("Certificado SSL inválido ou inseguro.")
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        onErrorOccurred("O processo do navegador foi reiniciado para liberar memória.")
                        return true
                    }
                }

                if (tab.url != "openv://home" && tab.url != "about:blank") {
                    loadUrl(tab.url)
                }
            }
        },
        update = { wv ->
            onNavigationStateChanged(wv.canGoBack(), wv.canGoForward())
        },
        modifier = modifier.fillMaxSize()
    )
    }
}
