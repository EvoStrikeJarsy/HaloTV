package com.halotv.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.halotv.app.data.Channel
import com.halotv.app.data.ChannelGroup
import com.halotv.app.data.PlaylistRepository
import com.halotv.app.ui.theme.HaloColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private val EdgePad = 48.dp

private enum class HomeTab(val title: String) {
    Live("Live TV"), Favorites("Favorites"), Search("Search"), Settings("Settings")
}

@Composable
fun HomeScreen(
    groups: List<ChannelGroup>,
    total: Int,
    favorites: Set<String>,
    playlistUrl: String,
    onPlay: (List<Channel>, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRefresh: () -> Unit,
    onChangePlaylist: () -> Unit,
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = HomeTab.entries[tabIndex]
    var focused by remember { mutableStateOf(groups.firstOrNull()?.channels?.firstOrNull()) }
    val firstCard = remember { FocusRequester() }
    val favoriteChannels = remember(groups, favorites) {
        PlaylistRepository.channels.filter { it.id in favorites }
    }

    BackHandler(enabled = tab != HomeTab.Live) { tabIndex = 0 }

    Box(Modifier.fillMaxSize()) {
        if (tab == HomeTab.Live) Backdrop(focused)

        Column(Modifier.fillMaxSize()) {
            TopBar(selected = tabIndex, onSelect = { tabIndex = it })

            when (tab) {
                HomeTab.Live -> BrowseRows(
                    groups = groups,
                    favorites = favorites,
                    focused = focused,
                    firstCard = firstCard,
                    onFocus = { focused = it },
                    onPlay = onPlay,
                    onToggleFavorite = onToggleFavorite,
                )

                HomeTab.Favorites ->
                    if (favoriteChannels.isEmpty()) {
                        EmptyMessage(
                            "No favorites yet",
                            "Hold OK on any channel to add it here.",
                        )
                    } else {
                        ChannelGrid(favoriteChannels, favorites, onPlay, onToggleFavorite)
                    }

                HomeTab.Search -> SearchPane(favorites, onPlay, onToggleFavorite)

                HomeTab.Settings -> SettingsPane(
                    playlistUrl = playlistUrl,
                    total = total,
                    groupCount = groups.size,
                    onRefresh = onRefresh,
                    onChangePlaylist = onChangePlaylist,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(150)
        runCatching { firstCard.requestFocus() }
    }
}

// ---------------------------------------------------------------- top bar

@Composable
private fun TopBar(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = EdgePad, end = EdgePad, top = 22.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Halo", style = MaterialTheme.typography.titleLarge, color = HaloColors.TextPrimary)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            TabRow(
                selectedTabIndex = selected,
                modifier = Modifier.focusRestorer(),
                separator = { Spacer(Modifier.width(6.dp)) },
            ) {
                HomeTab.entries.forEachIndexed { i, t ->
                    Tab(
                        selected = i == selected,
                        onFocus = { onSelect(i) },
                        onClick = { onSelect(i) },
                    ) {
                        Text(
                            t.title,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
        Clock()
    }
}

@Composable
private fun Clock() {
    var now by remember { mutableStateOf(timeNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = timeNow()
            delay(15_000)
        }
    }
    Text(now, style = MaterialTheme.typography.titleMedium, color = HaloColors.TextSecondary)
}

private fun timeNow(): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())

// ---------------------------------------------------------------- live tv

/** Big dimmed logo of the focused channel, faded into black – the "top shelf" look. */
@Composable
private fun Backdrop(channel: Channel?) {
    // Debounce so fast scrolling doesn't trigger an image load for every tile passed.
    var shown by remember { mutableStateOf(channel) }
    LaunchedEffect(channel) {
        delay(220)
        shown = channel
    }
    val context = LocalContext.current
    val logo = shown?.logo

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp)
                .width(560.dp)
                .height(320.dp)
                .background(
                    Brush.radialGradient(
                        listOf(HaloColors.Accent.copy(alpha = 0.16f), Color.Transparent),
                    ),
                ),
        )
        if (!logo.isNullOrBlank()) {
            AsyncImage(
                model = remember(logo) {
                    ImageRequest.Builder(context).data(logo).size(320, 180).crossfade(350).build()
                },
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alpha = 0.55f,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 92.dp, end = 96.dp)
                    .width(340.dp)
                    .height(150.dp),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.42f to Color.Transparent,
                    0.62f to HaloColors.Background,
                ),
            ),
        )
    }
}

@Composable
private fun BrowseRows(
    groups: List<ChannelGroup>,
    favorites: Set<String>,
    focused: Channel?,
    firstCard: FocusRequester,
    onFocus: (Channel) -> Unit,
    onPlay: (List<Channel>, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // Hero info for the focused tile
        Column(
            Modifier
                .padding(start = EdgePad, top = 18.dp)
                .fillMaxWidth(0.55f)
                .height(128.dp),
        ) {
            Text(
                focused?.group.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = HaloColors.Accent,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                focused?.name.orEmpty(),
                style = MaterialTheme.typography.displaySmall,
                color = HaloColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Press OK to watch. Hold OK to add to Favorites.",
                style = MaterialTheme.typography.bodySmall,
                color = HaloColors.TextSecondary,
            )
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 60.dp),
        ) {
            itemsIndexed(groups, key = { _, g -> g.name }) { gi, group ->
                CategoryRow(
                    group = group,
                    favorites = favorites,
                    firstCard = if (gi == 0) firstCard else null,
                    onFocus = onFocus,
                    onPlay = onPlay,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    group: ChannelGroup,
    favorites: Set<String>,
    firstCard: FocusRequester?,
    onFocus: (Channel) -> Unit,
    onPlay: (List<Channel>, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = EdgePad, top = 4.dp), verticalAlignment = Alignment.Bottom) {
            Text(group.name, style = MaterialTheme.typography.titleMedium, color = HaloColors.TextPrimary)
            Spacer(Modifier.width(10.dp))
            Text(
                "${group.channels.size}",
                style = MaterialTheme.typography.labelMedium,
                color = HaloColors.TextSecondary,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = EdgePad, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            itemsIndexed(group.channels, key = { _, c -> c.id }) { index, channel ->
                ChannelCard(
                    channel = channel,
                    isFavorite = channel.id in favorites,
                    onClick = { onPlay(group.channels, index) },
                    onLongClick = { onToggleFavorite(channel.id) },
                    onFocused = { onFocus(channel) },
                    focusRequester = if (index == 0) firstCard else null,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- grid / search / settings

@Composable
private fun ChannelGrid(
    channels: List<Channel>,
    favorites: Set<String>,
    onPlay: (List<Channel>, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(horizontal = EdgePad, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(channels, key = { _, c -> c.id }) { index, channel ->
            ChannelCard(
                channel = channel,
                isFavorite = channel.id in favorites,
                onClick = { onPlay(channels, index) },
                onLongClick = { onToggleFavorite(channel.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SearchPane(
    favorites: Set<String>,
    onPlay: (List<Channel>, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Channel>>(emptyList()) }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isEmpty()) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        results = withContext(Dispatchers.Default) {
            PlaylistRepository.channels.asSequence()
                .filter { it.name.contains(q, ignoreCase = true) }
                .take(400)
                .toList()
        }
    }

    Column(Modifier.fillMaxSize().padding(top = 18.dp)) {
        TvTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search channels",
            modifier = Modifier.padding(horizontal = EdgePad).width(560.dp),
        )
        when {
            query.isBlank() -> EmptyMessage("Find a channel", "Select the search field and press OK to type.")
            results.isEmpty() -> EmptyMessage("No matches", "Try a shorter or different name.")
            else -> ChannelGrid(results, favorites, onPlay, onToggleFavorite)
        }
    }
}

@Composable
private fun SettingsPane(
    playlistUrl: String,
    total: Int,
    groupCount: Int,
    onRefresh: () -> Unit,
    onChangePlaylist: () -> Unit,
) {
    val masked = remember(playlistUrl) {
        playlistUrl.replace(Regex("(password=)[^&]*", RegexOption.IGNORE_CASE), "\$1••••••")
    }
    Column(Modifier.fillMaxSize().padding(horizontal = EdgePad, vertical = 28.dp)) {
        Text("Playlist", style = MaterialTheme.typography.headlineSmall, color = HaloColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            masked,
            style = MaterialTheme.typography.bodyMedium,
            color = HaloColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.8f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$total channels in $groupCount categories. Refreshes automatically every 24 hours.",
            style = MaterialTheme.typography.bodyMedium,
            color = HaloColors.TextSecondary,
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HaloButton("Refresh now", onClick = onRefresh)
            HaloButton("Change playlist", onClick = onChangePlaylist)
        }
    }
}

@Composable
private fun EmptyMessage(title: String, hint: String) {
    Column(
        Modifier.fillMaxSize().padding(bottom = 60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = HaloColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = HaloColors.TextSecondary)
    }
}
