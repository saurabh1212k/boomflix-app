package com.example.boomflix.extractor

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.boomflix.data.api.TmdbDns
import com.example.boomflix.data.models.ServerItem
import com.example.boomflix.data.models.StreamInfo
import com.example.boomflix.data.models.StreamResult
import com.example.boomflix.data.models.SubtitleInfo
import com.example.boomflix.player.CdnHeaderInterceptor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ExtractorBridge(private val context: Context) {
    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val gson = Gson()
    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentDeferred: CompletableDeferred<StreamResult>? = null
    private var currentServerDeferred: CompletableDeferred<List<ServerItem>>? = null
    private var isReady = false
    private val readyDeferred = CompletableDeferred<Unit>()
    private val sessionCounter = java.util.concurrent.atomic.AtomicLong(0)
    @Volatile private var activeSessionId: Long = 0L

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(TmdbDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        private const val TAG = "ExtractorBridge"
        private const val EXTRACT_TIMEOUT_MS = 60_000L
        private const val SERVERS_TIMEOUT_MS = 10_000L
        private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initialize() {
        mainHandler.post {
            val wv = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.allowFileAccessFromFileURLs = true
                settings.allowUniversalAccessFromFileURLs = true
                settings.mediaPlaybackRequiresUserGesture = false

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        val level = consoleMessage?.messageLevel() ?: ConsoleMessage.MessageLevel.LOG
                        val msg = consoleMessage?.message() ?: ""
                        val src = consoleMessage?.sourceId() ?: ""
                        val line = consoleMessage?.lineNumber() ?: 0
                        Log.d("ExtractorBridge-JS", "[$level] $msg ($src:$line)")
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "Bridge HTML page finished loading: $url")
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        Log.w(TAG, "Bridge resource error: ${request?.url} -> ${error?.description}")
                    }
                }

                addJavascriptInterface(JSBridge(), "Android")
            }
            wv.loadUrl("file:///android_asset/vyla/bridge.html")
            webView = wv
        }
    }

    suspend fun getServers(isAnime: Boolean = false): List<ServerItem> {
        try {
            withTimeout(15_000L) { readyDeferred.await() }
        } catch (_: Exception) {}

        val deferred = CompletableDeferred<List<ServerItem>>()
        currentServerDeferred = deferred

        mainHandler.post {
            webView?.evaluateJavascript("window.getServers($isAnime)", null)
        }

        return try {
            withTimeout(SERVERS_TIMEOUT_MS) {
                deferred.await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch dynamic servers list: ${e.message}")
            emptyList()
        }
    }

    suspend fun extract(
        type: String,
        id: Int,
        title: String,
        year: String = "",
        season: Int = -1,
        episode: Int = -1,
        isAnime: Boolean = false
    ): StreamResult {
        withTimeout(15_000L) { readyDeferred.await() }

        val sessionId = sessionCounter.incrementAndGet()
        activeSessionId = sessionId

        currentDeferred?.cancel()
        val deferred = CompletableDeferred<StreamResult>()
        currentDeferred = deferred

        val escapedTitle = title.replace("'", "\\'")
        val js = "window.extractStream($sessionId,'$type',$id,$season,$episode,'$escapedTitle','$year',$isAnime)"

        mainHandler.post {
            webView?.evaluateJavascript(js, null)
        }

        return withTimeout(EXTRACT_TIMEOUT_MS) {
            deferred.await()
        }
    }

    suspend fun extractServer(
        serverKey: String,
        type: String,
        id: Int,
        title: String,
        year: String = "",
        season: Int = -1,
        episode: Int = -1
    ): StreamResult {
        withTimeout(15_000L) { readyDeferred.await() }

        val sessionId = sessionCounter.incrementAndGet()
        activeSessionId = sessionId

        currentDeferred?.cancel()
        val deferred = CompletableDeferred<StreamResult>()
        currentDeferred = deferred

        val escapedTitle = title.replace("'", "\\'")
        val js = "window.extractSingleServer($sessionId,'$serverKey','$type',$id,$season,$episode,'$escapedTitle','$year')"

        mainHandler.post {
            webView?.evaluateJavascript(js, null)
        }

        return withTimeout(EXTRACT_TIMEOUT_MS) {
            deferred.await()
        }
    }

    fun cancelPending() {
        activeSessionId = sessionCounter.incrementAndGet()
        currentDeferred?.cancel()
        currentDeferred = null
        mainHandler.post {
            webView?.evaluateJavascript("if (window.cancelExtraction) window.cancelExtraction();", null)
        }
    }

    fun destroy() {
        cancelPending()
        bridgeScope.cancel()
        mainHandler.post {
            webView?.destroy()
            webView = null
        }
    }

    inner class JSBridge {
        @JavascriptInterface
        fun onReady() {
            Log.d(TAG, "Bridge JS ready signal received")
            if (!isReady) {
                isReady = true
                readyDeferred.complete(Unit)
            }
        }

        @JavascriptInterface
        fun executeHttp(id: Int, url: String, method: String, headersJson: String, body: String?) {
            bridgeScope.launch {
                handleHttpExecution(id, url, method, headersJson, body)
            }
        }

        @JavascriptInterface
        fun onServers(json: String) {
            Log.d(TAG, "Servers list received: $json")
            try {
                val type = object : TypeToken<List<ServerItem>>() {}.type
                val servers: List<ServerItem> = gson.fromJson(json, type)
                currentServerDeferred?.complete(servers)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse servers", e)
                currentServerDeferred?.complete(emptyList())
            }
        }

        @JavascriptInterface
        fun onSessionResult(sessionId: Long, json: String) {
            if (sessionId != activeSessionId) {
                Log.w(TAG, "Ignoring stale extraction result for session $sessionId (active: $activeSessionId)")
                return
            }
            parseAndCompleteResult(json)
        }

        @JavascriptInterface
        fun onSessionError(sessionId: Long, message: String) {
            if (sessionId != activeSessionId) {
                Log.w(TAG, "Ignoring stale extraction error for session $sessionId (active: $activeSessionId)")
                return
            }
            Log.e(TAG, "Extraction error: $message")
            currentDeferred?.completeExceptionally(Exception(message))
        }

        @JavascriptInterface
        fun onSingleSessionResult(sessionId: Long, json: String) {
            onSessionResult(sessionId, json)
        }

        @JavascriptInterface
        fun onSingleSessionError(sessionId: Long, serverKey: String, message: String) {
            if (sessionId != activeSessionId) return
            Log.e(TAG, "Single server error for $serverKey: $message")
            currentDeferred?.completeExceptionally(Exception("Server failed: $message"))
        }

        // Fallback backward compatibility methods
        @JavascriptInterface
        fun onResult(json: String) {
            parseAndCompleteResult(json)
        }

        @JavascriptInterface
        fun onSingleServerResult(json: String) {
            parseAndCompleteResult(json)
        }

        @JavascriptInterface
        fun onSingleServerError(serverKey: String, message: String) {
            currentDeferred?.completeExceptionally(Exception("Server failed: $message"))
        }

        @JavascriptInterface
        fun onError(message: String) {
            Log.e(TAG, "Extraction error: $message")
            currentDeferred?.completeExceptionally(Exception(message))
        }
    }

    private fun parseAndCompleteResult(json: String) {
        try {
            val parsed = gson.fromJson(json, RawStreamResult::class.java)
            val result = StreamResult(
                server = parsed.server,
                streams = parsed.streams.map {
                    StreamInfo(
                        url = it.url,
                        type = it.type,
                        quality = it.quality,
                        server = it.server,
                        headers = it.headers ?: emptyMap()
                    )
                },
                subtitles = parsed.subtitles?.map {
                    SubtitleInfo(
                        url = it.url,
                        lang = it.lang,
                        label = it.label
                    )
                } ?: emptyList()
            )
            currentDeferred?.complete(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse extraction result", e)
            currentDeferred?.completeExceptionally(e)
        }
    }

    private fun handleHttpExecution(id: Int, rawUrl: String, method: String, headersJson: String, body: String?) {
        try {
            var targetUrl = rawUrl
            val customHeaders = mutableMapOf<String, String>()

            // 1. Check for /api/proxy and unwrap target URL and headers
            if (targetUrl.contains("/api/proxy")) {
                val uri = Uri.parse(if (targetUrl.startsWith("/")) "http://localhost$targetUrl" else targetUrl)
                val extractedUrl = uri.getQueryParameter("url")
                if (!extractedUrl.isNullOrBlank()) {
                    targetUrl = extractedUrl
                }
                val rawHeadersParam = uri.getQueryParameter("headers")
                if (!rawHeadersParam.isNullOrBlank()) {
                    try {
                        val parsedType = object : TypeToken<Map<String, String>>() {}.type
                        val parsed: Map<String, String> = gson.fromJson(rawHeadersParam, parsedType)
                        customHeaders.putAll(parsed)
                    } catch (_: Exception) {}
                }
            }

            if (targetUrl.startsWith("//")) {
                targetUrl = "https:$targetUrl"
            }

            if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                notifyFetchResult(id, 0, "", "{}", null, null, "Invalid HTTP URL: $targetUrl")
                return
            }

            // 2. Parse headers from JS options
            val incomingHeaders: Map<String, String> = try {
                val parsedType = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson(headersJson, parsedType) ?: emptyMap()
            } catch (_: Exception) {
                emptyMap()
            }
            customHeaders.putAll(incomingHeaders)

            // 3. Process spoof headers and CDN rules
            var spoofReferer: String? = null
            var spoofOrigin: String? = null
            var spoofUserAgent: String? = null
            val finalHeaders = mutableMapOf<String, String>()

            for ((k, v) in customHeaders) {
                when (k.lowercase()) {
                    "x-spoof-referer" -> spoofReferer = v
                    "x-spoof-origin" -> spoofOrigin = v
                    "x-spoof-user-agent" -> spoofUserAgent = v
                    "referer" -> if (spoofReferer == null) spoofReferer = v
                    "origin" -> if (spoofOrigin == null) spoofOrigin = v
                    "user-agent" -> if (spoofUserAgent == null) spoofUserAgent = v
                    else -> finalHeaders[k] = v
                }
            }

            val cdnHeaders = CdnHeaderInterceptor.getHeadersForUrl(targetUrl)
            for ((k, v) in cdnHeaders) {
                when (k.lowercase()) {
                    "referer" -> if (spoofReferer == null) spoofReferer = v
                    "origin" -> if (spoofOrigin == null) spoofOrigin = v
                    "user-agent" -> if (spoofUserAgent == null) spoofUserAgent = v
                }
            }

            if (spoofReferer != null) finalHeaders["Referer"] = spoofReferer
            if (spoofOrigin != null) finalHeaders["Origin"] = spoofOrigin
            finalHeaders["User-Agent"] = spoofUserAgent ?: DEFAULT_USER_AGENT

            // 4. Build OkHttp request
            val reqBuilder = Request.Builder().url(targetUrl)
            for ((k, v) in finalHeaders) {
                if (k.isNotBlank() && v.isNotBlank()) {
                    try {
                        val cleanKey = k.trim()
                        val cleanVal = v.trim().replace("\r", "").replace("\n", "")
                        reqBuilder.header(cleanKey, cleanVal)
                    } catch (_: Exception) {}
                }
            }

            when (method.uppercase()) {
                "GET" -> reqBuilder.get()
                "HEAD" -> reqBuilder.head()
                "POST" -> {
                    val contentType = finalHeaders["Content-Type"] ?: "application/x-www-form-urlencoded; charset=UTF-8"
                    val reqBody = (body ?: "").toRequestBody(contentType.toMediaTypeOrNull())
                    reqBuilder.post(reqBody)
                }
                "PUT" -> {
                    val contentType = finalHeaders["Content-Type"] ?: "application/json"
                    val reqBody = (body ?: "").toRequestBody(contentType.toMediaTypeOrNull())
                    reqBuilder.put(reqBody)
                }
                else -> reqBuilder.get()
            }

            // 5. Execute request
            okHttpClient.newCall(reqBuilder.build()).execute().use { response ->
                val code = response.code
                val message = response.message
                val finalUrl = response.request.url.toString()
                val respHeadersMap = mutableMapOf<String, String>()
                for (i in 0 until response.headers.size) {
                    respHeadersMap[response.headers.name(i).lowercase()] = response.headers.value(i)
                }
                val respHeadersJson = gson.toJson(respHeadersMap)
                val bytes = response.body?.bytes() ?: ByteArray(0)
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                notifyFetchResult(id, code, message, respHeadersJson, base64, finalUrl, null)
            }
        } catch (e: Exception) {
            val errMsg = e.message ?: e.toString()
            Log.w(TAG, "Native fetch error for $rawUrl: $errMsg")
            notifyFetchResult(id, 0, "", "{}", null, null, errMsg)
        }
    }

    private fun notifyFetchResult(
        id: Int,
        status: Int,
        statusText: String,
        headersJson: String,
        base64Body: String?,
        finalUrl: String?,
        errorMsg: String?
    ) {
        mainHandler.post {
            val js = "window.__onNativeFetchResult($id, $status, ${gson.toJson(statusText)}, ${gson.toJson(headersJson)}, ${gson.toJson(base64Body)}, ${gson.toJson(finalUrl)}, ${gson.toJson(errorMsg)});"
            webView?.evaluateJavascript(js, null)
        }
    }

    private data class RawStreamResult(
        val server: String = "",
        val streams: List<RawStreamInfo> = emptyList(),
        val subtitles: List<RawSubtitleInfo>? = null
    )
    private data class RawStreamInfo(
        val url: String = "",
        val type: String = "",
        val quality: String = "",
        val server: String = "",
        val headers: Map<String, String>? = null
    )
    private data class RawSubtitleInfo(
        val url: String = "",
        val lang: String = "",
        val label: String = ""
    )
}
