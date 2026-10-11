package com.example.boomflix.data.turso

import android.content.Context
import android.util.Log
import com.example.boomflix.data.auth.AuthManager
import com.example.boomflix.data.local.BoomflixDatabase
import com.example.boomflix.data.local.ContinueWatchingEntity
import com.example.boomflix.data.local.MyListEntity
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class TursoSyncManager(private val context: Context) {
    private val db = BoomflixDatabase.getInstance(context)
    private val myListDao = db.myListDao()
    private val continueWatchingDao = db.continueWatchingDao()
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var isSyncingFromCloud = false
    private val isAutoSyncStarted = AtomicBoolean(false)

    companion object {
        private const val TAG = "TursoSyncManager"
        @Volatile private var instance: TursoSyncManager? = null

        fun getInstance(context: Context): TursoSyncManager {
            return instance ?: synchronized(this) {
                val created = TursoSyncManager(context.applicationContext)
                instance = created
                created
            }
        }
    }

    init {
        scope.launch {
            try {
                TursoClient.initTable()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Turso table", e)
            }
        }
    }

    @OptIn(FlowPreview::class)
    fun startAutoSync() {
        if (!isAutoSyncStarted.compareAndSet(false, true)) return
        Log.d(TAG, "Starting automatic Turso background sync engine")

        // 1. Observe watchlist changes in Room and automatically push to Turso
        scope.launch {
            myListDao.observeAll()
                .drop(1)
                .debounce(1500L)
                .collect {
                    if (!isSyncingFromCloud) {
                        try {
                            val user = AuthManager.getInstance(context).currentUser.value
                            val uid = user?.uid
                            if (!uid.isNullOrBlank()) {
                                Log.d(TAG, "Auto-sync: pushing watchlist changes to Turso for user $uid")
                                pushWatchlistToCloud(uid)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in auto-sync watchlist push", e)
                        }
                    }
                }
        }

        // 2. Observe continue watching changes in Room and automatically push to Turso
        scope.launch {
            continueWatchingDao.observeAll()
                .drop(1)
                .debounce(1500L)
                .collect {
                    if (!isSyncingFromCloud) {
                        try {
                            val user = AuthManager.getInstance(context).currentUser.value
                            val uid = user?.uid
                            if (!uid.isNullOrBlank()) {
                                Log.d(TAG, "Auto-sync: pushing continue watching changes to Turso for user $uid")
                                pushContinueWatchingToCloud(uid)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in auto-sync continue watching push", e)
                        }
                    }
                }
        }

        // 3. Periodic background refresh (every 5 minutes) to keep in sync with web updates
        scope.launch {
            while (true) {
                delay(5 * 60 * 1000L)
                try {
                    val user = AuthManager.getInstance(context).currentUser.value
                    val uid = user?.uid
                    if (!uid.isNullOrBlank() && !isSyncingFromCloud) {
                        Log.d(TAG, "Auto-sync: periodic background pull from Turso for user $uid")
                        syncFromCloud(uid)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic Turso pull", e)
                }
            }
        }
    }

    suspend fun syncFromCloud(uid: String): Boolean = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext false
        isSyncingFromCloud = true
        try {
            val (watchlistJson, continueWatchingJson) = TursoClient.fetchUserData(uid)
            if (watchlistJson == null && continueWatchingJson == null) {
                return@withContext false
            }

            // 1. Process Watchlist
            if (!watchlistJson.isNullOrBlank() && watchlistJson != "[]") {
                val array = gson.fromJson(watchlistJson, JsonArray::class.java)
                val entities = mutableListOf<MyListEntity>()
                for (elem in array) {
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        val id = getFieldString(obj, "mediaId", "id") ?: continue
                        val title = getFieldString(obj, "title", "name", "displayTitle") ?: "Untitled"
                        val type = getFieldString(obj, "type", "media_type", "mediaType") ?: "movie"
                        val posterPath = getFieldString(obj, "poster_path", "posterPath")
                        val backdropPath = getFieldString(obj, "backdrop_path", "backdropPath")
                        val rating = getFieldString(obj, "vote_average", "rating")
                        val year = getFieldString(obj, "release_date", "first_air_date", "year")?.take(4)

                        entities.add(
                            MyListEntity(
                                mediaId = id,
                                type = type,
                                title = title,
                                posterPath = posterPath,
                                backdropPath = backdropPath,
                                rating = rating,
                                year = year
                            )
                        )
                    }
                }
                if (entities.isNotEmpty()) {
                    myListDao.upsertAll(entities)
                }
            }

            // 2. Process Continue Watching
            if (!continueWatchingJson.isNullOrBlank() && continueWatchingJson != "[]") {
                val array = gson.fromJson(continueWatchingJson, JsonArray::class.java)
                val entities = mutableListOf<ContinueWatchingEntity>()
                for (elem in array) {
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        val id = getFieldString(obj, "mediaId", "id") ?: continue
                        val title = getFieldString(obj, "title", "name", "displayTitle") ?: "Untitled"
                        val type = getFieldString(obj, "type", "media_type", "mediaType") ?: "movie"
                        val posterPath = getFieldString(obj, "poster_path", "posterPath")
                        val backdropPath = getFieldString(obj, "backdrop_path", "backdropPath")
                        val season = getFieldInt(obj, "season", "savedSeason")
                        val episode = getFieldInt(obj, "episode", "savedEpisode")

                        var currentTime = getFieldLong(obj, "currentTime") ?: 0L
                        val savedTimeSeconds = getFieldLong(obj, "savedTime")
                        if (currentTime == 0L && savedTimeSeconds != null) {
                            currentTime = savedTimeSeconds * 1000L
                        }

                        var duration = getFieldLong(obj, "duration") ?: 0L
                        if (duration in 1..99999) {
                            // If in seconds, convert to ms
                            duration *= 1000L
                        }

                        val progress = if (duration > 0) {
                            (currentTime.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        } else {
                            obj.get("progress")?.asFloat ?: 0f
                        }

                        val serverId = getFieldString(obj, "serverId", "preferredServer")
                        val updatedAt = getFieldLong(obj, "updatedAt") ?: System.currentTimeMillis()

                        entities.add(
                            ContinueWatchingEntity(
                                mediaId = id,
                                type = type,
                                title = title,
                                posterPath = posterPath,
                                backdropPath = backdropPath,
                                season = season,
                                episode = episode,
                                progress = progress,
                                currentTime = currentTime,
                                duration = duration,
                                serverId = serverId,
                                updatedAt = updatedAt
                            )
                        )
                    }
                }
                if (entities.isNotEmpty()) {
                    continueWatchingDao.upsertAll(entities)
                }
            }

            // Sync back to merge local-only items to cloud
            pushWatchlistToCloud(uid)
            pushContinueWatchingToCloud(uid)

            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncFromCloud", e)
            return@withContext false
        } finally {
            isSyncingFromCloud = false
        }
    }

    suspend fun pushWatchlistToCloud(uid: String): Boolean = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext false
        try {
            val localItems = myListDao.getAll()
            val listArray = JsonArray()
            for (item in localItems) {
                val obj = JsonObject().apply {
                    addProperty("id", item.mediaId.toIntOrNull() ?: item.mediaId.hashCode())
                    addProperty("mediaId", item.mediaId)
                    addProperty("title", item.title)
                    addProperty("name", item.title)
                    addProperty("type", item.type)
                    addProperty("media_type", item.type)
                    item.posterPath?.let { addProperty("poster_path", it) }
                    item.backdropPath?.let { addProperty("backdrop_path", it) }
                    item.rating?.let { addProperty("vote_average", it) }
                    item.year?.let { addProperty("release_date", it) }
                }
                listArray.add(obj)
            }
            return@withContext TursoClient.pushWatchlist(uid, gson.toJson(listArray))
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing watchlist to cloud", e)
            return@withContext false
        }
    }

    suspend fun pushContinueWatchingToCloud(uid: String): Boolean = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext false
        try {
            val localItems = continueWatchingDao.getAll()
            val listArray = JsonArray()
            for (item in localItems) {
                val obj = JsonObject().apply {
                    addProperty("id", item.mediaId.toIntOrNull() ?: item.mediaId.hashCode())
                    addProperty("mediaId", item.mediaId)
                    addProperty("title", item.title)
                    addProperty("type", item.type)
                    addProperty("media_type", item.type)
                    item.posterPath?.let { 
                        addProperty("poster_path", it)
                        addProperty("posterPath", it)
                    }
                    item.backdropPath?.let { 
                        addProperty("backdrop_path", it)
                        addProperty("backdropPath", it)
                    }
                    item.season?.let { 
                        addProperty("season", it)
                        addProperty("savedSeason", it)
                    }
                    item.episode?.let { 
                        addProperty("episode", it)
                        addProperty("savedEpisode", it)
                    }
                    addProperty("currentTime", item.currentTime)
                    addProperty("savedTime", item.currentTime / 1000L)
                    addProperty("duration", item.duration / 1000L)
                    addProperty("progress", item.progress)
                    item.serverId?.let { addProperty("serverId", it) }
                    addProperty("updatedAt", item.updatedAt)
                }
                listArray.add(obj)
            }
            return@withContext TursoClient.pushContinueWatching(uid, gson.toJson(listArray))
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing continue watching to cloud", e)
            return@withContext false
        }
    }

    private fun getFieldString(obj: JsonObject, vararg keys: String): String? {
        for (k in keys) {
            if (obj.has(k) && !obj.get(k).isJsonNull) {
                val v = obj.get(k).asString
                if (v.isNotBlank()) return v
            }
        }
        return null
    }

    private fun getFieldInt(obj: JsonObject, vararg keys: String): Int? {
        for (k in keys) {
            if (obj.has(k) && !obj.get(k).isJsonNull) {
                try {
                    return obj.get(k).asInt
                } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun getFieldLong(obj: JsonObject, vararg keys: String): Long? {
        for (k in keys) {
            if (obj.has(k) && !obj.get(k).isJsonNull) {
                try {
                    return obj.get(k).asLong
                } catch (_: Exception) {}
            }
        }
        return null
    }
}
