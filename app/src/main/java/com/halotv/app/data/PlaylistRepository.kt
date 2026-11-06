package com.halotv.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object PlaylistRepository {

    @Volatile var channels: List<Channel> = emptyList(); private set
    @Volatile var groups: List<ChannelGroup> = emptyList(); private set

    /** The list the player zaps through with channel up/down. */
    @Volatile var playQueue: List<Channel> = emptyList()

    private const val MAX_CACHE_AGE_MS = 24L * 60 * 60 * 1000

    private fun cacheFile(context: Context) = File(context.filesDir, "playlist.m3u")

    /**
     * Loads from the on-disk cache when it is fresh (instant start-up),
     * otherwise downloads. Falls back to a stale cache if the network fails.
     */
    suspend fun load(context: Context, url: String, forceRefresh: Boolean): Int =
        withContext(Dispatchers.IO) {
            val file = cacheFile(context)
            val hasCache = file.exists() && file.length() > 0
            val stale = !hasCache || System.currentTimeMillis() - file.lastModified() > MAX_CACHE_AGE_MS
            if (forceRefresh || stale) {
                try {
                    download(url, file)
                } catch (e: IOException) {
                    if (forceRefresh || !hasCache) throw e
                }
            }
            val list = file.inputStream().use { M3uParser.parse(it) }
            if (list.isEmpty()) throw IOException("The playlist loaded but has no channels. Check the URL.")
            channels = list
            // groupBy keeps the playlist's own category order
            groups = list.groupBy { it.group }.map { (name, items) -> ChannelGroup(name, items) }
            list.size
        }

    fun clearCache(context: Context) {
        cacheFile(context).delete()
    }

    private fun download(url: String, dest: File) {
        var current = url
        for (attempt in 0 until 6) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
            }
            try {
                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                        ?: throw IOException("Server redirected without a location")
                    current = URL(URL(current), location).toString()
                    continue
                }
                if (code !in 200..299) throw IOException("The server answered with HTTP $code. Check the playlist URL.")
                val tmp = File(dest.parentFile, dest.name + ".tmp")
                conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it, 64 * 1024) } }
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                return
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }
}
