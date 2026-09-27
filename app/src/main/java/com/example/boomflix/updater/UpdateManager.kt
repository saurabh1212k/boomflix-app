package com.example.boomflix.updater

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.boomflix.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val downloadUrl: String,
    val apkSize: Long,
    val isUpdateAvailable: Boolean
)

sealed class UpdateCheckResult {
    data class Available(val updateInfo: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
        const val DEFAULT_GITHUB_OWNER = "saurabh1212k"
        const val DEFAULT_GITHUB_REPO = "boomflix-app"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun checkForUpdates(
        owner: String = DEFAULT_GITHUB_OWNER,
        repo: String = DEFAULT_GITHUB_REPO
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "BOOMFLIX-Android-App")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                // If 404, repository has no published releases yet
                if (response.code == 404) {
                    return@withContext UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
                }
                return@withContext UpdateCheckResult.Error("GitHub API returned HTTP ${response.code}")
            }

            val body = response.body?.string() ?: return@withContext UpdateCheckResult.Error("Empty response body")
            val release = gson.fromJson(body, GithubReleaseResponse::class.java)

            val apkAsset = release.assets.firstOrNull {
                it.name.endsWith(".apk", ignoreCase = true) ||
                        it.contentType == "application/vnd.android.package-archive"
            }

            if (apkAsset == null) {
                return@withContext UpdateCheckResult.Error("Release found but no APK asset attached")
            }

            val remoteTag = release.tagName.trim()
            val cleanedRemoteVersion = remoteTag.removePrefix("v").removePrefix("V").trim()
            val currentVersion = BuildConfig.VERSION_NAME.trim()

            val isNewer = isNewerVersion(cleanedRemoteVersion, currentVersion)

            val updateInfo = AppUpdateInfo(
                tagName = remoteTag,
                versionName = cleanedRemoteVersion,
                releaseTitle = release.name ?: "Version $cleanedRemoteVersion",
                changelog = release.body ?: "Bug fixes and performance enhancements.",
                downloadUrl = apkAsset.browserDownloadUrl,
                apkSize = apkAsset.size,
                isUpdateAvailable = isNewer
            )

            if (isNewer) {
                UpdateCheckResult.Available(updateInfo)
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            UpdateCheckResult.Error(e.message ?: "Failed to check for updates")
        }
    }

    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, percent: Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", "BOOMFLIX-Android-App")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "APK download failed with HTTP ${response.code}")
                return@withContext null
            }

            val responseBody = response.body ?: return@withContext null
            val totalBytes = responseBody.contentLength()

            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val outputFile = File(downloadDir, "boomflix-update.apk")
            if (outputFile.exists()) outputFile.delete()

            responseBody.byteStream().use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val percent = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt() else 0
                        onProgress(totalRead, totalBytes, percent)
                    }
                    outputStream.flush()
                }
            }

            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "APK download encountered exception", e)
            null
        }
    }

    fun installApk(activity: Activity, apkFile: File): Boolean {
        return try {
            if (!apkFile.exists()) {
                Log.e(TAG, "Cannot install: APK file does not exist: ${apkFile.absolutePath}")
                return false
            }

            // Android 8.0+ check for unknown app install permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(settingsIntent)
                    return false
                }
            }

            val apkUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            activity.startActivity(installIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            false
        }
    }

    private fun isNewerVersion(remoteVer: String, currentVer: String): Boolean {
        return try {
            val remoteParts = remoteVer.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = currentVer.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            false
        } catch (_: Exception) {
            // Fallback string compare
            remoteVer != currentVer
        }
    }

    // GitHub API Response Models
    private data class GithubReleaseResponse(
        @SerializedName("tag_name") val tagName: String = "",
        val name: String? = null,
        val body: String? = null,
        val assets: List<GithubAsset> = emptyList()
    )

    private data class GithubAsset(
        val name: String = "",
        val size: Long = 0L,
        @SerializedName("browser_download_url") val browserDownloadUrl: String = "",
        @SerializedName("content_type") val contentType: String? = null
    )
}
