package com.example.boomflix.data.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.boomflix.data.models.BoomflixUser
import com.example.boomflix.data.turso.TursoSyncManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("boomflix_auth_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val syncManager = TursoSyncManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentUser = MutableStateFlow<BoomflixUser?>(loadSavedUser())
    val currentUser: StateFlow<BoomflixUser?> = _currentUser.asStateFlow()

    companion object {
        private const val TAG = "AuthManager"
        private const val KEY_USER = "saved_user"

        @Volatile private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                val created = AuthManager(context.applicationContext)
                instance = created
                created
            }
        }
    }

    init {
        // Sync on app startup if user is logged in
        _currentUser.value?.let { user ->
            scope.launch {
                syncManager.syncFromCloud(user.uid)
            }
        }
    }

    private fun loadSavedUser(): BoomflixUser? {
        val raw = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(raw, BoomflixUser::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    fun handleGoogleSignInResult(account: GoogleSignInAccount): BoomflixUser {
        val uid = account.id ?: ("g_" + (account.email?.hashCode()?.toString() ?: System.currentTimeMillis().toString()))
        val user = BoomflixUser(
            uid = uid,
            email = account.email,
            displayName = account.displayName ?: "Google User",
            photoUrl = account.photoUrl?.toString(),
            isAnonymous = false
        )
        saveUser(user)
        return user
    }

    fun signInAsGuest(): BoomflixUser {
        val user = BoomflixUser(
            uid = "guest_user",
            email = null,
            displayName = "Guest User",
            photoUrl = null,
            isAnonymous = true
        )
        saveUser(user)
        return user
    }

    private fun saveUser(user: BoomflixUser) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()
        _currentUser.value = user

        scope.launch {
            try {
                syncManager.syncFromCloud(user.uid)
            } catch (e: Exception) {
                Log.e(TAG, "Failed initial sync on sign-in", e)
            }
        }
    }

    fun signOut(activity: Activity? = null) {
        prefs.edit().remove(KEY_USER).apply()
        _currentUser.value = null

        if (activity != null) {
            try {
                val client = getGoogleSignInClient(activity)
                client.signOut()
            } catch (e: Exception) {
                Log.e(TAG, "Error signing out of Google client", e)
            }
        }
    }

    fun triggerTursoSync() {
        val user = _currentUser.value ?: return
        scope.launch {
            syncManager.syncFromCloud(user.uid)
        }
    }
}
