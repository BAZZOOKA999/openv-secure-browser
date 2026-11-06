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
import com.example.ui.browser.menu.DetectedMedia
import com.example.ui.browser.menu.ExtractedImage
import org.json.JSONArray
import org.json.JSONTokener

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
    isWebDarkMode: Boolean = false,
    isReaderMode: Boolean = false,
    scriptToExecute: String? = null,
    onScriptExecuted: () -> Unit = {},
    extractImagesTrigger: Long = 0L,
    onImagesExtracted: (List<ExtractedImage>) -> Unit = {},
    extractVideosTrigger: Long = 0L,
    onVideosDetected: (List<DetectedMedia>) -> Unit = {},
    viewSourceTrigger: Long = 0L,
    onSourceExtracted: (String) -> Unit = {},
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

    // React to Web Dark Mode
    LaunchedEffect(isWebDarkMode) {
        if (isWebDarkMode) {
            val darkCss = """
                (function() {
                    var id = 'openv-dark-mode-style';
                    var s = document.getElementById(id);
                    if (!s) {
                        s = document.createElement('style');
                        s.id = id;
                        s.textContent = 'html { filter: invert(90%) hue-rotate(180deg) !important; background: #121212 !important; } img, video, canvas, svg { filter: invert(100%) hue-rotate(180deg) !important; }';
                        document.head.appendChild(s);
                    }
                })()
            """.trimIndent()
            webView.evaluateJavascript(darkCss, null)
        } else {
            webView.evaluateJavascript("(function(){ var s = document.getElementById('openv-dark-mode-style'); if(s) s.remove(); })()", null)
        }
    }

    // React to Reader Mode
    LaunchedEffect(isReaderMode) {
        if (isReaderMode) {
            val readerJs = """
                (function() {
                    var id = 'openv-reader-mode-style';
                    var s = document.getElementById(id);
                    if (!s) {
                        s = document.createElement('style');
                        s.id = id;
                        s.textContent = 'aside, nav, header, footer, .ad, .ads, [class*="banner"], [id*="banner"], [class*="popup"], [id*="cookie"] { display: none !important; } body { max-width: 740px !important; margin: 0 auto !important; padding: 24px !important; line-height: 1.8 !important; font-size: 19px !important; }';
                        document.head.appendChild(s);
                    }
                })()
            """.trimIndent()
            webView.evaluateJavascript(readerJs, null)
        } else {
            webView.evaluateJavascript("(function(){ var s = document.getElementById('openv-reader-mode-style'); if(s) s.remove(); })()", null)
        }
    }

    // React to custom user script execution
    LaunchedEffect(scriptToExecute) {
        if (!scriptToExecute.isNullOrBlank()) {
            webView.evaluateJavascript(scriptToExecute, null)
            onScriptExecuted()
        }
    }

    // Extract images from DOM
    LaunchedEffect(extractImagesTrigger) {
        if (extractImagesTrigger > 0L) {
            val js = """
                (function() {
                    var imgs = Array.from(document.querySelectorAll('img, picture source'));
                    var list = [];
                    var seen = new Set();
                    imgs.forEach(function(el) {
                        var src = el.src || el.currentSrc || el.getAttribute('srcset') || '';
                        if (src && !seen.has(src) && !src.startsWith('data:image/svg+xml')) {
                            seen.add(src);
                            list.push({
                                url: src,
                                alt: el.alt || el.title || '',
                                width: el.naturalWidth || el.clientWidth || 0,
                                height: el.naturalHeight || el.clientHeight || 0,
                                mime: 'image/jpeg'
                            });
                        }
                    });
                    return JSON.stringify(list.slice(0, 60));
                })()
            """.trimIndent()
            webView.evaluateJavascript(js) { result ->
                try {
                    val jsonStr = if (result != null && result != "null") {
                        if (result.startsWith("\"") && result.endsWith("\"")) {
                            JSONTokener(result).nextValue().toString()
                        } else result
                    } else "[]"
                    val array = JSONArray(jsonStr)
                    val list = mutableListOf<ExtractedImage>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            ExtractedImage(
                                url = obj.optString("url"),
                                alt = obj.optString("alt"),
                                width = obj.optInt("width", 0),
                                height = obj.optInt("height", 0),
                                mimeType = obj.optString("mime", "image/jpeg")
                            )
                        )
                    }
                    onImagesExtracted(list)
                } catch (_: Exception) {
                    onImagesExtracted(emptyList())
                }
            }
        }
    }

    // Detect videos from DOM
    LaunchedEffect(extractVideosTrigger) {
        if (extractVideosTrigger > 0L) {
            val js = """
                (function() {
                    var vids = Array.from(document.querySelectorAll('video, video source, a[href$=".mp4"], a[href$=".webm"], a[href$=".mkv"]'));
                    var list = [];
                    var seen = new Set();
                    vids.forEach(function(el) {
                        var src = el.src || el.currentSrc || el.getAttribute('href') || '';
                        if (src && !seen.has(src)) {
                            seen.add(src);
                            var isBlob = src.startsWith('blob:') || src.startsWith('data:');
                            var ext = src.split('.').pop().split('?')[0].toLowerCase();
                            list.push({
                                title: document.title || 'Vídeo Web',
                                url: src,
                                mimeType: isBlob ? 'video/mp4' : ('video/' + (ext || 'mp4')),
                                isBlobOrStream: isBlob,
                                approximateSize: 'Desconhecido'
                            });
                        }
                    });
                    return JSON.stringify(list.slice(0, 30));
                })()
            """.trimIndent()
            webView.evaluateJavascript(js) { result ->
                try {
                    val jsonStr = if (result != null && result != "null") {
                        if (result.startsWith("\"") && result.endsWith("\"")) {
                            JSONTokener(result).nextValue().toString()
                        } else result
                    } else "[]"
                    val array = JSONArray(jsonStr)
                    val list = mutableListOf<DetectedMedia>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            DetectedMedia(
                                title = obj.optString("title", "Vídeo Web"),
                                url = obj.optString("url"),
                                mimeType = obj.optString("mimeType", "video/mp4"),
                                isBlobOrStream = obj.optBoolean("isBlobOrStream", false),
                                approximateSize = obj.optString("approximateSize", "Desconhecido")
                            )
                        )
                    }
                    onVideosDetected(list)
                } catch (_: Exception) {
                    onVideosDetected(emptyList())
                }
            }
        }
    }

    // View source
    LaunchedEffect(viewSourceTrigger) {
        if (viewSourceTrigger > 0L) {
            webView.evaluateJavascript("(function() { return document.documentElement.outerHTML; })()") { rawResult ->
                val unquoted = try {
                    if (rawResult != null && rawResult.startsWith("\"") && rawResult.endsWith("\"")) {
                        JSONTokener(rawResult).nextValue().toString()
                    } else rawResult ?: ""
                } catch (_: Exception) {
                    rawResult ?: ""
                }
                onSourceExtracted(unquoted)
            }
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
