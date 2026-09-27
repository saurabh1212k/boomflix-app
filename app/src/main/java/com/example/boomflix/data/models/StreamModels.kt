package com.example.boomflix.data.models

data class ServerItem(
    val id: String,
    val name: String
)

data class StreamResult(
    val server: String,
    val streams: List<StreamInfo>,
    val subtitles: List<SubtitleInfo>
)

data class StreamInfo(
    val url: String,
    val type: String,
    val quality: String,
    val server: String,
    val headers: Map<String, String> = emptyMap()
)

data class SubtitleInfo(
    val url: String,
    val lang: String,
    val label: String
)
