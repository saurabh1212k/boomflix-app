package com.example.boomflix.data.turso

import android.util.Log
import com.example.boomflix.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object TursoClient {
    private const val TAG = "TursoClient"
    private const val PIPELINE_URL = "${BuildConfig.TURSO_DATABASE_URL}/v2/pipeline"
    private const val AUTH_TOKEN = BuildConfig.TURSO_AUTH_TOKEN

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun initTable(): Boolean = withContext(Dispatchers.IO) {
        val sql = """
            CREATE TABLE IF NOT EXISTS user_data (
                uid TEXT PRIMARY KEY,
                watchlist TEXT,
                continue_watching TEXT,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        return@withContext executeStatement(sql, emptyList())
    }

    suspend fun fetchUserData(uid: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val sql = "SELECT watchlist, continue_watching FROM user_data WHERE uid = ?"
            val args = listOf(mapOf("type" to "text", "value" to uid))
            val body = mapOf(
                "requests" to listOf(
                    mapOf(
                        "type" to "execute",
                        "stmt" to mapOf(
                            "sql" to sql,
                            "args" to args
                        )
                    ),
                    mapOf("type" to "close")
                )
            )

            val request = Request.Builder()
                .url(PIPELINE_URL)
                .addHeader("Authorization", "Bearer $AUTH_TOKEN")
                .addHeader("Content-Type", "application/json")
                .post(gson.toJson(body).toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: return@withContext Pair(null, null)

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch user data: ${response.code} $responseString")
                return@withContext Pair(null, null)
            }

            val jsonObject = gson.fromJson(responseString, JsonObject::class.java)
            val results = jsonObject.getAsJsonArray("results") ?: return@withContext Pair(null, null)
            if (results.size() == 0) return@withContext Pair(null, null)

            val firstResult = results.get(0).asJsonObject
            val respObj = firstResult.getAsJsonObject("response") ?: return@withContext Pair(null, null)
            val resultObj = respObj.getAsJsonObject("result") ?: return@withContext Pair(null, null)
            val rows = resultObj.getAsJsonArray("rows") ?: return@withContext Pair(null, null)

            if (rows.size() == 0) {
                return@withContext Pair("[]", "[]")
            }

            val firstRow = rows.get(0).asJsonArray
            var watchlist: String? = null
            var continueWatching: String? = null

            if (firstRow.size() > 0 && !firstRow.get(0).isJsonNull) {
                val col0 = firstRow.get(0).asJsonObject
                watchlist = col0.get("value")?.asString
            }
            if (firstRow.size() > 1 && !firstRow.get(1).isJsonNull) {
                val col1 = firstRow.get(1).asJsonObject
                continueWatching = col1.get("value")?.asString
            }

            return@withContext Pair(watchlist ?: "[]", continueWatching ?: "[]")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user data from Turso", e)
            return@withContext Pair(null, null)
        }
    }

    suspend fun pushUserData(uid: String, watchlistJson: String, continueWatchingJson: String): Boolean = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO user_data (uid, watchlist, continue_watching, updated_at) 
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(uid) DO UPDATE SET 
              watchlist = excluded.watchlist,
              continue_watching = excluded.continue_watching,
              updated_at = CURRENT_TIMESTAMP
        """.trimIndent()

        val args = listOf(
            mapOf("type" to "text", "value" to uid),
            mapOf("type" to "text", "value" to watchlistJson),
            mapOf("type" to "text", "value" to continueWatchingJson)
        )

        return@withContext executeStatement(sql, args)
    }

    suspend fun pushWatchlist(uid: String, watchlistJson: String): Boolean = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO user_data (uid, watchlist, continue_watching, updated_at) 
            VALUES (?, ?, '[]', CURRENT_TIMESTAMP)
            ON CONFLICT(uid) DO UPDATE SET 
              watchlist = excluded.watchlist,
              updated_at = CURRENT_TIMESTAMP
        """.trimIndent()

        val args = listOf(
            mapOf("type" to "text", "value" to uid),
            mapOf("type" to "text", "value" to watchlistJson)
        )

        return@withContext executeStatement(sql, args)
    }

    suspend fun pushContinueWatching(uid: String, continueWatchingJson: String): Boolean = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO user_data (uid, watchlist, continue_watching, updated_at) 
            VALUES (?, '[]', ?, CURRENT_TIMESTAMP)
            ON CONFLICT(uid) DO UPDATE SET 
              continue_watching = excluded.continue_watching,
              updated_at = CURRENT_TIMESTAMP
        """.trimIndent()

        val args = listOf(
            mapOf("type" to "text", "value" to uid),
            mapOf("type" to "text", "value" to continueWatchingJson)
        )

        return@withContext executeStatement(sql, args)
    }

    private fun executeStatement(sql: String, args: List<Map<String, Any>>): Boolean {
        try {
            val body = mapOf(
                "requests" to listOf(
                    mapOf(
                        "type" to "execute",
                        "stmt" to mapOf(
                            "sql" to sql,
                            "args" to args
                        )
                    ),
                    mapOf("type" to "close")
                )
            )

            val request = Request.Builder()
                .url(PIPELINE_URL)
                .addHeader("Authorization", "Bearer $AUTH_TOKEN")
                .addHeader("Content-Type", "application/json")
                .post(gson.toJson(body).toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            if (!success) {
                Log.e(TAG, "Execute statement failed: ${response.code} ${response.body?.string()}")
            }
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error executing statement on Turso", e)
            return false
        }
    }
}
