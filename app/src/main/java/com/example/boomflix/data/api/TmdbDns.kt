package com.example.boomflix.data.api

import android.util.Log
import okhttp3.Dns
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

object TmdbDns : Dns {

    private const val TAG = "TmdbDns"

    private val FALLBACK_API_IPS = listOf(
        "143.204.55.102",
        "143.204.55.46",
        "143.204.55.23",
        "143.204.55.91",
        "99.84.152.53",
        "99.84.152.85",
        "99.84.152.8",
        "99.84.152.32"
    )

    private val FALLBACK_IMAGE_IPS = listOf(
        "169.150.207.216",
        "169.150.207.217"
    )

    override fun lookup(hostname: String): List<InetAddress> {
        val lowerHost = hostname.lowercase()

        if (lowerHost == "api.themoviedb.org") {
            val dohResult = resolveDoh(lowerHost)
            if (dohResult.isNotEmpty()) {
                return dohResult
            }
            Log.d(TAG, "Using CloudFront fallback IPs for $hostname")
            return FALLBACK_API_IPS.mapNotNull { ipToInetAddress(lowerHost, it) }
        }

        if (lowerHost == "image.tmdb.org") {
            val dohResult = resolveDoh(lowerHost)
            if (dohResult.isNotEmpty()) {
                return dohResult
            }
            Log.d(TAG, "Using CDN fallback IPs for $hostname")
            return FALLBACK_IMAGE_IPS.mapNotNull { ipToInetAddress(lowerHost, it) }
        }

        return try {
            Dns.SYSTEM.lookup(hostname)
        } catch (e: Exception) {
            Log.w(TAG, "System DNS failed for $hostname, trying DoH", e)
            val dohResult = resolveDoh(lowerHost)
            if (dohResult.isNotEmpty()) dohResult else throw e
        }
    }

    private fun resolveDoh(hostname: String): List<InetAddress> {
        return try {
            val url = URL("https://dns.google/resolve?name=$hostname&type=A")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val answers = json.optJSONArray("Answer") ?: return emptyList()
                val result = mutableListOf<InetAddress>()
                for (i in 0 until answers.length()) {
                    val obj = answers.getJSONObject(i)
                    if (obj.optInt("type") == 1) { // Type A
                        val ip = obj.optString("data")
                        ipToInetAddress(hostname, ip)?.let { result.add(it) }
                    }
                }
                result
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.d(TAG, "DoH lookup failed for $hostname: ${e.message}")
            emptyList()
        }
    }

    private fun ipToInetAddress(hostname: String, ipStr: String): InetAddress? {
        return try {
            val parts = ipStr.split(".").map { it.toInt().toByte() }
            if (parts.size == 4) {
                InetAddress.getByAddress(hostname, parts.toByteArray())
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
