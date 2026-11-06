package com.halotv.app.data

/** Many IPTV panels only accept player-like user agents. */
const val DEFAULT_USER_AGENT = "VLC/3.0.20 LibVLC/3.0.20"

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String,
    val group: String,
    val userAgent: String? = null,
    val referrer: String? = null,
)

data class ChannelGroup(val name: String, val channels: List<Channel>)
