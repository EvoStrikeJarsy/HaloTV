package com.halotv.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halotv.app.data.ChannelGroup
import com.halotv.app.data.PlaylistRepository
import com.halotv.app.data.Prefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Loading : UiState
    data object NeedsSetup : UiState
    data class Ready(val groups: List<ChannelGroup>, val total: Int) : UiState
    data class Error(val message: String) : UiState
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private var loadJob: Job? = null

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _favorites = MutableStateFlow(prefs.favorites)
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    val playlistUrl: String? get() = prefs.playlistUrl

    init {
        load(forceRefresh = false)
    }

    private fun load(forceRefresh: Boolean) {
        val url = prefs.playlistUrl
        if (url.isNullOrBlank()) {
            _state.value = UiState.NeedsSetup
            return
        }
        loadJob?.cancel()
        _state.value = UiState.Loading
        loadJob = viewModelScope.launch {
            _state.value = try {
                val total = PlaylistRepository.load(getApplication(), url, forceRefresh)
                UiState.Ready(PlaylistRepository.groups, total)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Couldn't load the playlist (${e.javaClass.simpleName}).")
            }
        }
    }

    fun setPlaylist(url: String) {
        PlaylistRepository.clearCache(getApplication())
        prefs.playlistUrl = url.trim()
        load(forceRefresh = true)
    }

    fun refresh() = load(forceRefresh = true)

    fun changePlaylist() {
        loadJob?.cancel()
        _state.value = UiState.NeedsSetup
    }

    fun toggleFavorite(channelId: String) {
        val updated = _favorites.value.toMutableSet()
        if (!updated.add(channelId)) updated.remove(channelId)
        _favorites.value = updated
        prefs.favorites = updated
    }
}
