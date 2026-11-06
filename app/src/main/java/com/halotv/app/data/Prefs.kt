package com.halotv.app.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("halo", Context.MODE_PRIVATE)

    var playlistUrl: String?
        get() = sp.getString("playlist_url", null)
        set(value) = sp.edit().putString("playlist_url", value).apply()

    var favorites: Set<String>
        get() = sp.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()
        set(value) = sp.edit().putStringSet("favorites", value).apply()
}
