package com.example.boomflix.data.models

import com.google.gson.annotations.SerializedName

data class BoomflixUser(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false
) {
    val displayInitial: String
        get() = displayName?.firstOrNull()?.uppercase()
            ?: email?.firstOrNull()?.uppercase()
            ?: "B"
}
