package com.example.boomflix.data.config

object Blocklist {
    val BLOCKED_IDS = setOf(
        969681, // Spider-Man: Brand New Day (2026) - DMCA notice from Columbia Pictures
        262838  // Quantext Media - DMCA notice via AiPlex, 2026-09-29
    )

    fun isBlocked(id: Int?): Boolean {
        if (id == null) return false
        return BLOCKED_IDS.contains(id)
    }
}
