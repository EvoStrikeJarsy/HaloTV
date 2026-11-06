package com.halotv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.halotv.app.PlayerActivity
import com.halotv.app.data.PlaylistRepository
import com.halotv.app.ui.theme.HaloColors
import kotlinx.coroutines.delay

@Composable
fun HaloApp(vm: HomeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().background(HaloColors.Background)) {
        when (val s = state) {
            UiState.Loading -> LoadingScreen()
            UiState.NeedsSetup -> SetupScreen(initialUrl = vm.playlistUrl.orEmpty(), onSubmit = vm::setPlaylist)
            is UiState.Error -> ErrorScreen(s.message, onRetry = vm::refresh, onChange = vm::changePlaylist)
            is UiState.Ready -> HomeScreen(
                groups = s.groups,
                total = s.total,
                favorites = favorites,
                playlistUrl = vm.playlistUrl.orEmpty(),
                onPlay = { queue, index ->
                    PlaylistRepository.playQueue = queue
                    context.startActivity(PlayerActivity.intent(context, index))
                },
                onToggleFavorite = vm::toggleFavorite,
                onRefresh = vm::refresh,
                onChangePlaylist = vm::changePlaylist,
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spinner()
        Spacer(Modifier.height(20.dp))
        Text("Loading your channels", style = MaterialTheme.typography.titleMedium, color = HaloColors.TextSecondary)
    }
}

@Composable
private fun SetupScreen(initialUrl: String, onSubmit: (String) -> Unit) {
    var url by rememberSaveable { mutableStateOf(initialUrl) }
    var error by remember { mutableStateOf<String?>(null) }
    val fieldFocus = remember { FocusRequester() }

    val submit: () -> Unit = {
        val u = url.trim()
        if (u.startsWith("http://", true) || u.startsWith("https://", true)) {
            onSubmit(u)
        } else {
            error = "Enter a full link starting with http:// or https://"
        }
        Unit
    }

    Row(
        Modifier.fillMaxSize().padding(horizontal = 72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 48.dp)) {
            Text("Halo", style = MaterialTheme.typography.displayLarge, color = HaloColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            Text(
                "Add your IPTV playlist to start watching.",
                style = MaterialTheme.typography.headlineSmall,
                color = HaloColors.TextSecondary,
            )
        }
        Column(Modifier.weight(1.2f)) {
            Text("Playlist link (M3U)", style = MaterialTheme.typography.titleMedium, color = HaloColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            TvTextField(
                value = url,
                onValueChange = { url = it; error = null },
                placeholder = "http://provider.com/playlist.m3u",
                keyboardType = KeyboardType.Uri,
                onDone = submit,
                modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                error ?: "Using Xtream Codes? Use http://server:port/get.php?username=USER&password=PASS&type=m3u_plus&output=ts",
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) HaloColors.Live else HaloColors.TextSecondary,
            )
            Spacer(Modifier.height(24.dp))
            HaloButton("Load playlist", onClick = submit)
        }
    }

    LaunchedEffect(Unit) {
        delay(120)
        runCatching { fieldFocus.requestFocus() }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit, onChange: () -> Unit) {
    val retryFocus = remember { FocusRequester() }
    Column(
        Modifier.fillMaxSize().padding(64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Couldn't load your playlist", style = MaterialTheme.typography.headlineMedium, color = HaloColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = HaloColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 600.dp),
        )
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HaloButton("Try again", onClick = onRetry, modifier = Modifier.focusRequester(retryFocus))
            HaloButton("Change playlist", onClick = onChange)
        }
    }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { retryFocus.requestFocus() }
    }
}
