package com.halotv.app.data

import java.io.InputStream

/**
 * Streaming M3U / M3U8 (extended) parser. Reads line-by-line so even
 * 50k+ channel playlists parse quickly without loading the file into memory.
 */
object M3uParser {

    private val attrRegex = Regex("""([A-Za-z0-9_-]+)\s*=\s*"([^"]*)"""")

    fun parse(input: InputStream): List<Channel> {
        val out = ArrayList<Channel>(4096)
        val seenIds = HashSet<String>(8192)

        var name: String? = null
        var logo = ""
        var group = ""
        var tvgId = ""
        var extGroup = ""
        var userAgent: String? = null
        var referrer: String? = null

        input.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                when {
                    line.isEmpty() -> Unit

                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        val split = nameSeparator(line)
                        val head = if (split >= 0) line.substring(0, split) else line
                        val attrs = HashMap<String, String>()
                        for (m in attrRegex.findAll(head)) attrs[m.groupValues[1].lowercase()] = m.groupValues[2]
                        val title = if (split >= 0) line.substring(split + 1).trim() else ""
                        name = title.ifEmpty { attrs["tvg-name"].orEmpty() }.ifEmpty { "Channel" }
                        logo = attrs["tvg-logo"].orEmpty()
                        group = attrs["group-title"].orEmpty()
                        tvgId = attrs["tvg-id"].orEmpty()
                        attrs["user-agent"]?.let { userAgent = it }
                        attrs["http-user-agent"]?.let { userAgent = it }
                    }

                    line.startsWith("#EXTGRP:", ignoreCase = true) -> extGroup = line.substring(8).trim()

                    line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                        val opt = line.substring(11).trim()
                        when {
                            opt.startsWith("http-user-agent=", true) -> userAgent = opt.substringAfter('=')
                            opt.startsWith("http-referrer=", true) -> referrer = opt.substringAfter('=')
                        }
                    }

                    line.startsWith("#") -> Unit

                    else -> {
                        val channelName = name
                        if (channelName != null) {
                            // Kodi-style headers: http://url|User-Agent=x&Referer=y
                            var url = line
                            val pipe = line.indexOf('|')
                            if (pipe > 0) {
                                url = line.substring(0, pipe)
                                line.substring(pipe + 1).split('&').forEach { kv ->
                                    val k = kv.substringBefore('=').trim()
                                    val v = kv.substringAfter('=', "").trim()
                                    when {
                                        k.equals("User-Agent", true) -> userAgent = v
                                        k.equals("Referer", true) || k.equals("Referrer", true) -> referrer = v
                                    }
                                }
                            }
                            val base = Integer.toHexString((tvgId + url).hashCode()) +
                                Integer.toHexString(channelName.hashCode())
                            var id = base
                            var n = 1
                            while (!seenIds.add(id)) id = "$base-${n++}"

                            out += Channel(
                                id = id,
                                name = channelName,
                                url = url,
                                logo = logo,
                                group = group.ifEmpty { extGroup }.ifEmpty { "Other" },
                                userAgent = userAgent,
                                referrer = referrer,
                            )
                        }
                        name = null; logo = ""; group = ""; tvgId = ""; extGroup = ""
                        userAgent = null; referrer = null
                    }
                }
            }
        }
        return out
    }

    /** First comma that is not inside a quoted attribute value. */
    private fun nameSeparator(line: String): Int {
        var inQuotes = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '"') inQuotes = !inQuotes
            else if (c == ',' && !inQuotes) return i
        }
        return -1
    }
}
