package com.example.boomflix.player

import okhttp3.Interceptor
import okhttp3.Response

data class CdnRule(
    val pattern: Regex,
    val headers: Map<String, String>
)

object CdnHeaderInterceptor : Interceptor {

    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private val CDN_RULES = listOf(
        CdnRule(
            Regex("hlsx\\d+cdn\\.|burntburst\\d+\\.store|echovideo\\.ru", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://play2.echovideo.ru/", "Origin" to "https://play2.echovideo.ru")
        ),
        CdnRule(
            Regex("1embed\\.cc|videasy\\.to|bcine\\.ru", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://bcine.ru/", "Origin" to "https://bcine.ru")
        ),
        CdnRule(
            Regex("cinesrc\\.st|bright\\d+\\.online|glendale-plumbing\\.com", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://cinesrc.st/", "Origin" to "https://cinesrc.st")
        ),
        CdnRule(
            Regex("cine\\.su", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://cine.su/", "Origin" to "https://cine.su")
        ),
        CdnRule(
            Regex("flaxmovies\\.xyz|flix2watch\\.pro", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://flaxmovies.xyz/", "Origin" to "https://flaxmovies.xyz")
        ),
        CdnRule(
            Regex("vidrock\\.ru|proxy\\.vidrock\\.store", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://vidrock.ru/", "Origin" to "https://vidrock.ru")
        ),
        CdnRule(
            Regex("fsharetv\\.cc", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://fsharetv.cc/", "Origin" to "https://fsharetv.cc")
        ),
        CdnRule(
            Regex("player\\.vidzee\\.wtf|vidzee", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://player.vidzee.wtf/", "Origin" to "https://player.vidzee.wtf")
        ),
        CdnRule(
            Regex("vixsrc\\.to", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://vixsrc.to/", "Origin" to "https://vixsrc.to")
        ),
        CdnRule(
            Regex("megapplay\\.buzz|megap\\.|zhaevor\\.top|megaplay\\.buzz", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://megaplay.buzz/", "Origin" to "https://megaplay.buzz")
        ),
        CdnRule(
            Regex("ani\\.pm", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://ani.pm/", "Origin" to "https://ani.pm")
        ),
        CdnRule(
            Regex("pstream", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://pstream.net/", "Origin" to "https://pstream.net")
        ),
        CdnRule(
            Regex("rivestream\\.app", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://www.rivestream.app/", "Origin" to "https://www.rivestream.app")
        ),
        CdnRule(
            Regex("movy\\.bz|wecollege\\.net", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://www.movy.bz/", "Origin" to "https://www.movy.bz")
        ),
        CdnRule(
            Regex("peestream\\.in|api\\.peestream\\.in", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://peestream.in/", "Origin" to "https://peestream.in")
        ),
        CdnRule(
            Regex("vidcore\\.net", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://vidcore.net/", "Origin" to "https://vidcore.net")
        ),
        CdnRule(
            Regex("vidlink\\.pro", RegexOption.IGNORE_CASE),
            mapOf("Referer" to "https://vidlink.pro/", "Origin" to "https://vidlink.pro")
        )
    )

    private val dynamicHeaders = java.util.concurrent.ConcurrentHashMap<String, Map<String, String>>()

    fun registerStreamHeaders(url: String, headers: Map<String, String>) {
        if (headers.isNotEmpty()) {
            try {
                val host = java.net.URI(url).host?.lowercase()
                if (host != null) {
                    dynamicHeaders[host] = headers
                }
            } catch (_: Exception) {
                dynamicHeaders[url] = headers
            }
        }
    }

    fun getHeadersForUrl(url: String): Map<String, String> {
        val headers = mutableMapOf("User-Agent" to USER_AGENT)
        val matchedRule = CDN_RULES.firstOrNull { it.pattern.containsMatchIn(url) }
        if (matchedRule != null) {
            headers.putAll(matchedRule.headers)
        }
        try {
            val host = java.net.URI(url).host?.lowercase()
            if (host != null) {
                dynamicHeaders[host]?.let { headers.putAll(it) }
            }
        } catch (_: Exception) {
            dynamicHeaders[url]?.let { headers.putAll(it) }
        }
        return headers
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val cdnHeaders = getHeadersForUrl(url)

        val newRequest = request.newBuilder().apply {
            cdnHeaders.forEach { (key, value) ->
                header(key, value)
            }
        }.build()

        return chain.proceed(newRequest)
    }
}
