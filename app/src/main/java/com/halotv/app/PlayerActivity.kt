package com.halotv.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.halotv.app.data.Channel
import com.halotv.app.data.DEFAULT_USER_AGENT
import com.halotv.app.data.PlaylistRepository
import com.halotv.app.ui.ChannelArt
import com.halotv.app.ui.Spinner
import com.halotv.app.ui.theme.HaloColors
import com.halotv.app.ui.theme.HaloTheme
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_INDEX = "extra_index"
        fun intent(context: Context, index: Int): Intent =
            Intent(context, PlayerActivity::class.java).putExtra(EXTRA_INDEX, index)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var exoPlayer: ExoPlayer? = null
    private val queue: List<Channel> get() = PlaylistRepository.playQueue

    // UI state observed by Compose
    private var index by mutableIntStateOf(0)
    private var overlayVisible by mutableStateOf(true)
    private var overlayToken by mutableIntStateOf(0)
    private var buffering by mutableStateOf(true)
    private var error by mutableStateOf<String?>(null)

    private var retries = 0
    private var stopped = false

    // Start MPEG-TS streams on the first decodable frame instead of waiting for an IDR -> faster zapping
    private val extractors = DefaultExtractorsFactory().setTsExtractorFlags(
        DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
            DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS,
    )
    private val playPending = Runnable { play(index) }
    private val retryPending = Runnable { play(index, resetRetries = false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (queue.isEmpty()) {
            finish()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val start = savedInstanceState?.getInt(EXTRA_INDEX) ?: intent.getIntExtra(EXTRA_INDEX, 0)
        index = start.coerceIn(0, queue.lastIndex)
        exoPlayer = createPlayer()
        setContent { HaloTheme { PlayerScreen() } }
        play(index)
    }

    private fun createPlayer(): ExoPlayer {
        // Small start-up buffer = picture appears quickly after a channel change.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_500,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        val renderers = DefaultRenderersFactory(this).setEnableDecoderFallback(true)

        return ExoPlayer.Builder(this, renderers)
            .setLoadControl(loadControl)
            .build()
            .also { p ->
                p.playWhenReady = true
                p.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        buffering = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_READY) {
                            error = null
                            retries = 0
                        }
                    }

                    override fun onPlayerError(e: PlaybackException) = handleError(e)
                })
            }
    }

    private fun handleError(e: PlaybackException) {
        val p = exoPlayer ?: return
        if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            p.seekToDefaultPosition()
            p.prepare()
            return
        }
        if (retries < 2) {
            retries++
            buffering = true
            error = "Reconnecting…"
            handler.postDelayed(retryPending, 1_500L * retries)
            return
        }
        buffering = false
        error = "This channel isn't available right now (${e.errorCodeName})."
        showOverlay()
    }

    private fun play(i: Int, resetRetries: Boolean = true) {
        val p = exoPlayer ?: return
        val channel = queue.getOrNull(i) ?: return
        handler.removeCallbacks(playPending)
        handler.removeCallbacks(retryPending)
        if (resetRetries) {
            retries = 0
            error = null
        }
        index = i
        buffering = true

        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(channel.userAgent ?: DEFAULT_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
        channel.referrer?.let { http.setDefaultRequestProperties(mapOf("Referer" to it)) }

        val item = MediaItem.Builder()
            .setUri(channel.url)
            .apply { if (channel.url.contains("m3u8", ignoreCase = true)) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .build()

        val source = DefaultMediaSourceFactory(DefaultDataSource.Factory(this, http), extractors)
            .createMediaSource(item)

        p.setMediaSource(source)
        p.prepare()
        p.play()
    }

    private fun zap(delta: Int, isRepeat: Boolean) {
        val n = queue.size
        if (n == 0) return
        handler.removeCallbacks(playPending)
        handler.removeCallbacks(retryPending)
        index = ((index + delta) % n + n) % n
        showOverlay()
        // Holding the button scrolls through channels; only tune once you stop.
        if (isRepeat) handler.postDelayed(playPending, 400) else play(index)
    }

    private fun showOverlay() {
        overlayVisible = true
        overlayToken++
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        val repeat = event.repeatCount > 0
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                if (down) zap(+1, repeat)
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                if (down) zap(-1, repeat)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO -> {
                if (down && !repeat) {
                    if (overlayVisible) overlayVisible = false else showOverlay()
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                if (down && !repeat) {
                    exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() }
                    showOverlay()
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        super.onStart()
        if (stopped) {
            stopped = false
            play(index)
        }
    }

    override fun onStop() {
        super.onStop()
        stopped = true
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_INDEX, index)
    }

    @Composable
    private fun PlayerScreen() {
        val channel = queue.getOrNull(index) ?: return

        LaunchedEffect(overlayToken) {
            delay(4_500)
            if (error == null) overlayVisible = false
        }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        isFocusable = false
                        keepScreenOn = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        player = this@PlayerActivity.exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (buffering) Spinner(Modifier.align(Alignment.Center), diameter = 56.dp)

            AnimatedVisibility(
                visible = overlayVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut() + slideOutVertically { it / 3 },
            ) {
                ChannelOverlay(channel, index, queue.size, error)
            }
        }
    }
}

@Composable
private fun ChannelOverlay(channel: Channel, position: Int, total: Int, error: String?) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            .padding(horizontal = 48.dp, vertical = 32.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(HaloColors.Glass)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(150.dp).height(84.dp).clip(RoundedCornerShape(12.dp))) {
                ChannelArt(channel, logoPadding = 12.dp)
            }
            Spacer(Modifier.width(22.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(HaloColors.Live)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text("Live", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${position + 1} of $total  ${channel.group}",
                        style = MaterialTheme.typography.labelLarge,
                        color = HaloColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    channel.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = HaloColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    error ?: "Up and down change channel. OK hides this panel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) HaloColors.Live else HaloColors.TextSecondary,
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()),
                style = MaterialTheme.typography.titleMedium,
                color = HaloColors.TextSecondary,
                modifier = Modifier.size(width = 80.dp, height = 28.dp),
            )
        }
    }
}
