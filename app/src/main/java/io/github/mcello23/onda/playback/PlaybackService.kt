package io.github.mcello23.onda.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import io.github.mcello23.onda.MainActivity
import io.github.mcello23.onda.R
import io.github.mcello23.onda.data.AppLanguage
import io.github.mcello23.onda.data.AppSettingsStore
import io.github.mcello23.onda.data.BufferProfile
import io.github.mcello23.onda.data.Library
import io.github.mcello23.onda.data.PodcastRepo
import io.github.mcello23.onda.data.RadioBrowserApi
import io.github.mcello23.onda.data.Station
import io.github.mcello23.onda.data.StationRepo
import io.github.mcello23.onda.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    companion object {
        const val ACTION_TOGGLE_FAVORITE = "io.github.mcello23.onda.TOGGLE_FAVORITE"
        const val ACTION_RECONNECT_LIVE = "io.github.mcello23.onda.RECONNECT_LIVE"
        const val ACTION_REWIND_15 = "io.github.mcello23.onda.REWIND_15"
        const val ACTION_FORWARD_30 = "io.github.mcello23.onda.FORWARD_30"
        const val ACTION_CYCLE_SPEED = "io.github.mcello23.onda.CYCLE_SPEED"
        const val ACTION_SET_SLEEP_TIMER = "io.github.mcello23.onda.SET_SLEEP_TIMER"
        const val ACTION_SET_SPEED = "io.github.mcello23.onda.SET_SPEED"
        const val ACTION_APPLY_AUDIO_FX = "io.github.mcello23.onda.APPLY_AUDIO_FX"

        const val ACTION_WIDGET_TOGGLE = "io.github.mcello23.onda.WIDGET_TOGGLE"
        const val ACTION_WIDGET_NEXT = "io.github.mcello23.onda.WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "io.github.mcello23.onda.WIDGET_PREV"
        const val ACTION_WIDGET_PLAY_STATION = "io.github.mcello23.onda.WIDGET_PLAY_STATION"
        const val EXTRA_STATION_UUID = "station_uuid"
        const val EXTRA_TIMER_MINUTES = "minutes"
        const val EXTRA_END_OF_EPISODE = "end_of_episode"
        const val EXTRA_SPEED = "speed"

        val CMD_TOGGLE_FAVORITE = SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY)
        val CMD_RECONNECT_LIVE = SessionCommand(ACTION_RECONNECT_LIVE, Bundle.EMPTY)
        val CMD_REWIND_15 = SessionCommand(ACTION_REWIND_15, Bundle.EMPTY)
        val CMD_FORWARD_30 = SessionCommand(ACTION_FORWARD_30, Bundle.EMPTY)
        val CMD_CYCLE_SPEED = SessionCommand(ACTION_CYCLE_SPEED, Bundle.EMPTY)
        val CMD_SET_SLEEP_TIMER = SessionCommand(ACTION_SET_SLEEP_TIMER, Bundle.EMPTY)
        val CMD_SET_SPEED = SessionCommand(ACTION_SET_SPEED, Bundle.EMPTY)
        val CMD_APPLY_AUDIO_FX = SessionCommand(ACTION_APPLY_AUDIO_FX, Bundle.EMPTY)

        // Official Android Auto MediaConstants keys
        private const val KEY_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        private const val KEY_STYLE_BROWSABLE = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val KEY_STYLE_PLAYABLE = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        private const val KEY_GROUP_TITLE = "android.media.browse.CONTENT_STYLE_GROUP_TITLE"
        private const val STYLE_LIST = 1
        private const val STYLE_GRID = 2
        private const val STYLE_CATEGORY_GRID = 4

        private val SPEED_STEPS = listOf(1.0f, 1.25f, 1.5f, 2.0f)
        private const val RESUME_AT_LIVE_AFTER_MS = 3_000L
        private const val MAX_RETRIES = 30
        private const val MAX_LOADER_ERRORS = 60
        private val UNRECOVERABLE_ERRORS = setOf(
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val audioFx = AudioFxManager()
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession

    private var interruptedAt = 0L
    private var retries = 0
    private var lastMediaId: String? = null
    private var triedVariantUrls = mutableSetOf<String>()
    private var pendingPodcastSeekMs: Long = C.TIME_UNSET
    private var sleepTimerJob: Job? = null
    private var sleepEndOfEpisode = false
    private var podcastProgressJob: Job? = null

    private val retry = Runnable {
        if (player.playWhenReady && player.playbackState == Player.STATE_IDLE && player.currentMediaItem != null) {
            if (!isCurrentPodcast()) player.seekToDefaultPosition()
            player.prepare()
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppSettingsStore.init(this)
        StationRepo.init(this)
        PodcastRepo.init(this)

        player = buildExoPlayer(AppSettingsStore.current.bufferProfile)
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.addListener(playerListener)
        audioFx.attach(player.audioSessionId)

        val sessionPlayer = object : ForwardingPlayer(player) {
            override fun seekToPrevious() {
                if (isCurrentPodcast()) {
                    seekBackBy(15_000L)
                } else if (hasPreviousMediaItem()) {
                    seekToPreviousMediaItem()
                }
            }

            override fun seekToNext() {
                if (isCurrentPodcast()) {
                    seekForwardBy(30_000L)
                } else if (hasNextMediaItem()) {
                    seekToNextMediaItem()
                }
            }
        }

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaLibrarySession.Builder(this, sessionPlayer, libraryCallback)
            .setSessionActivity(openApp)
            .setCustomLayout(radioButtons(false))
            .build()

        scope.launch { runCatching { StationRepo.refreshSaved() } }
        scope.launch {
            StationRepo.favorites.collect { favorites ->
                updateCustomLayout()
                session.notifyChildrenChanged(Library.FAVORITES, favorites.size, null)
                updateWidget(refreshFavoritesGrid = true)
            }
        }
        scope.launch {
            StationRepo.recent.collect { recent ->
                session.notifyChildrenChanged(Library.RECENT, recent.size, null)
                session.notifyChildrenChanged(Library.FAVORITES, StationRepo.favorites.value.size + recent.size, null)
            }
        }
        scope.launch {
            PodcastRepo.savedEpisodes.collect { eps ->
                session.notifyChildrenChanged(Library.PODCASTS, eps.size, null)
            }
        }
        scope.launch {
            PodcastRepo.subscriptions.collect { subs ->
                session.notifyChildrenChanged(Library.PODCASTS, subs.size, null)
            }
        }
        scope.launch {
            AppSettingsStore.settings.collect { settings ->
                audioFx.applySettings(settings)
                if (isCurrentPodcast()) {
                    player.setPlaybackSpeed(settings.podcastSpeed)
                    updateCustomLayout()
                }
            }
        }
        startPodcastProgressLoop()
    }

    private fun buildExoPlayer(profile: BufferProfile): ExoPlayer {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(RadioBrowserApi.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
        val sources = DefaultMediaSourceFactory(DefaultDataSource.Factory(this, http))
            .setLoadErrorHandlingPolicy(object : DefaultLoadErrorHandlingPolicy() {
                override fun getMinimumLoadableRetryCount(dataType: Int) = Int.MAX_VALUE

                override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                    val code = (loadErrorInfo.exception as? HttpDataSource.InvalidResponseCodeException)?.responseCode
                    if (code != null && code in 400..499) return C.TIME_UNSET
                    if (loadErrorInfo.errorCount > MAX_LOADER_ERRORS) return C.TIME_UNSET
                    return super.getRetryDelayMsFor(loadErrorInfo)
                }
            })

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                profile.minBufferMs,
                profile.maxBufferMs,
                profile.playbackBufferMs,
                profile.rebufferMs,
            )
            .build()

        return ExoPlayer.Builder(this)
            .setMediaSourceFactory(sources)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_WIDGET_TOGGLE -> {
                if (player.playWhenReady) {
                    player.pause()
                } else if (player.currentMediaItem != null) {
                    if (player.playbackState == Player.STATE_IDLE) player.prepare()
                    player.play()
                } else {
                    scope.launch {
                        runCatching { resume() }.getOrNull()?.let { res ->
                            player.setMediaItems(res.mediaItems, res.startIndex, res.startPositionMs)
                            player.prepare()
                            player.play()
                        }
                    }
                }
            }
            ACTION_WIDGET_NEXT -> {
                if (isCurrentPodcast()) {
                    seekForwardBy(30_000L)
                } else if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                } else {
                    cycleFavoriteFromWidget(+1)
                }
            }
            ACTION_WIDGET_PREV -> {
                if (isCurrentPodcast()) {
                    seekBackBy(15_000L)
                } else if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem()
                } else {
                    cycleFavoriteFromWidget(-1)
                }
            }
            ACTION_WIDGET_PLAY_STATION -> {
                val uuid = intent.getStringExtra(EXTRA_STATION_UUID)
                if (!uuid.isNullOrBlank()) {
                    scope.launch {
                        runCatching { expand(Library.FAVORITES, uuid) }.getOrNull()?.let { res ->
                            player.setMediaItems(res.mediaItems, res.startIndex, res.startPositionMs)
                            player.prepare()
                            player.play()
                        }
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun cycleFavoriteFromWidget(delta: Int) {
        val favs = StationRepo.favorites.value
        if (favs.isEmpty()) return
        val curUuid = currentStation()?.uuid
        val idx = favs.indexOfFirst { it.uuid == curUuid }
        val target = if (idx < 0) 0 else (idx + delta + favs.size) % favs.size
        player.setMediaItems(favs.map { toStationMediaItem(it, Library.FAVORITES) }, target, C.TIME_UNSET)
        player.prepare()
        player.play()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveCurrentPodcastProgress()
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        saveCurrentPodcastProgress()
        sleepTimerJob?.cancel()
        podcastProgressJob?.cancel()
        SleepTimerState.clear()
        handler.removeCallbacksAndMessages(null)
        audioFx.release()
        scope.cancel()
        session.release()
        player.release()
        super.onDestroy()
    }

    private fun isOnWifi(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val net = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(net) ?: return true
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun isCurrentPodcast(): Boolean = Library.isPodcastEpisode(player.currentMediaItem?.mediaId)

    private fun currentStation(): Station? =
        player.currentMediaItem?.mediaId?.let(Library::parse)?.second?.let(StationRepo::cached)

    private fun currentPodcastEpisodeId(): String? =
        Library.parsePodcastEpisodeId(player.currentMediaItem?.mediaId)

    private fun seekBackBy(ms: Long) {
        val target = (player.currentPosition - ms).coerceAtLeast(0L)
        player.seekTo(target)
        saveCurrentPodcastProgress()
    }

    private fun seekForwardBy(ms: Long) {
        val dur = player.duration.takeIf { it > 0L }
        val target = if (dur != null) (player.currentPosition + ms).coerceAtMost(dur) else player.currentPosition + ms
        player.seekTo(target)
        saveCurrentPodcastProgress()
    }

    private fun startPodcastProgressLoop() {
        podcastProgressJob = scope.launch {
            while (isActive) {
                delay(5_000L)
                if (player.playWhenReady && isCurrentPodcast()) {
                    saveCurrentPodcastProgress()
                }
            }
        }
    }

    private fun saveCurrentPodcastProgress(completed: Boolean = false) {
        val epId = currentPodcastEpisodeId() ?: return
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.takeIf { it > 0L } ?: 0L
        PodcastRepo.saveProgress(epId, pos, dur, completed)
    }

    private fun setSleepTimer(minutes: Int, endOfEpisode: Boolean) {
        sleepTimerJob?.cancel()
        sleepEndOfEpisode = false
        player.volume = 1f

        if (endOfEpisode && isCurrentPodcast()) {
            sleepEndOfEpisode = true
            SleepTimerState.update(SleepTimerStatus(active = true, remainingMs = 0L, endOfEpisode = true))
            return
        }
        if (minutes <= 0) {
            SleepTimerState.clear()
            return
        }
        val totalMs = minutes * 60_000L
        val endAt = SystemClock.elapsedRealtime() + totalMs
        SleepTimerState.update(SleepTimerStatus(active = true, remainingMs = totalMs, endOfEpisode = false))
        sleepTimerJob = scope.launch {
            while (isActive) {
                val left = endAt - SystemClock.elapsedRealtime()
                if (left <= 0L) {
                    saveCurrentPodcastProgress()
                    player.pause()
                    player.volume = 1f
                    SleepTimerState.clear()
                    break
                }
                if (left <= 5_000L) {
                    player.volume = (left / 5_000f).coerceIn(0.05f, 1f)
                }
                SleepTimerState.update(SleepTimerStatus(active = true, remainingMs = left, endOfEpisode = false))
                delay(1_000L)
            }
        }
    }

    private fun radioButtons(isFavorite: Boolean): List<CommandButton> = listOf(
        CommandButton.Builder()
            .setDisplayName(getString(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite))
            .setIconResId(if (isFavorite) R.drawable.ic_heart else R.drawable.ic_heart_outline)
            .setSessionCommand(CMD_TOGGLE_FAVORITE)
            .build(),
        CommandButton.Builder()
            .setDisplayName(if (AppSettingsStore.current.language == AppLanguage.EN) "Reconnect live" else "Reconectar ao vivo")
            .setIconResId(R.drawable.ic_aa_refresh)
            .setSessionCommand(CMD_RECONNECT_LIVE)
            .build(),
    )

    private fun podcastButtons(): List<CommandButton> {
        val spd = AppSettingsStore.current.podcastSpeed
        val spdLabel = if (spd == spd.toInt().toFloat()) "${spd.toInt()}x" else "${spd}x"
        return listOf(
            CommandButton.Builder()
                .setDisplayName(getString(R.string.action_rewind_15))
                .setIconResId(R.drawable.ic_replay_15)
                .setSessionCommand(CMD_REWIND_15)
                .build(),
            CommandButton.Builder()
                .setDisplayName(getString(R.string.action_forward_30))
                .setIconResId(R.drawable.ic_forward_30)
                .setSessionCommand(CMD_FORWARD_30)
                .build(),
            CommandButton.Builder()
                .setDisplayName(spdLabel)
                .setIconResId(R.drawable.ic_aa_speed)
                .setSessionCommand(CMD_CYCLE_SPEED)
                .build(),
        )
    }

    private fun updateCustomLayout() {
        if (isCurrentPodcast()) {
            session.setCustomLayout(podcastButtons())
        } else {
            val isFavorite = currentStation()?.let { StationRepo.isFavorite(it.uuid) } ?: false
            session.setCustomLayout(radioButtons(isFavorite))
        }
    }

    private fun updateWidget(refreshFavoritesGrid: Boolean = false) {
        val isPod = isCurrentPodcast()
        val station = currentStation()
        val ep = currentPodcastEpisodeId()?.let { PodcastRepo.cachedEpisode(it) }
        val title = when {
            isPod && ep != null -> ep.title
            station != null -> station.displayName
            else -> getString(R.string.widget_idle_title)
        }
        val metaTitle = player.mediaMetadata.title?.toString()
        val subtitle = when {
            isPod && ep != null -> ep.podcastTitle
            station != null && metaTitle != null && metaTitle != station.displayName -> metaTitle
            station != null && player.playWhenReady -> listOf(getString(R.string.widget_live), station.qualityBadge)
                .filter { it.isNotBlank() }.joinToString(" · ")
            station != null -> station.subtitle
            else -> getString(R.string.widget_idle_subtitle)
        }
        val artUri = when {
            isPod && ep != null -> ArtworkProvider.uriFor(ep.podcastArtwork, ep.podcastTitle)
            station != null -> ArtworkProvider.uriFor(station.favicon, station.displayName, station.fallbackLogoUrl)
            else -> null
        }
        WidgetUpdater.updateAll(
            context = this,
            title = title,
            subtitle = subtitle,
            isPlaying = player.playWhenReady,
            currentStationUuid = station?.uuid,
            artUri = artUri,
            refreshFavoritesGrid = refreshFavoritesGrid,
        )
    }

    private fun tryFallbackStreamVariant(): Boolean {
        val item = player.currentMediaItem ?: return false
        val parsed = Library.parse(item.mediaId) ?: return false
        val station = StationRepo.cached(parsed.second) ?: return false
        val currentUri = item.localConfiguration?.uri?.toString().orEmpty()
        if (currentUri.isNotBlank()) triedVariantUrls.add(currentUri)
        val nextVariant = station.allStreamVariants().firstOrNull { it.url !in triedVariantUrls } ?: return false
        triedVariantUrls.add(nextVariant.url)
        val replacement = station.toMediaItem(
            category = parsed.first,
            overrideVariant = nextVariant,
        )
        player.replaceMediaItem(player.currentMediaItemIndex, replacement)
        player.prepare()
        player.play()
        return true
    }

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            audioFx.attach(audioSessionId)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) {
                handler.removeCallbacks(retry)
                saveCurrentPodcastProgress()
            }
            onInterruptionChanged()
            updateWidget()
        }

        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
            onInterruptionChanged()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                retries = 0
                if (pendingPodcastSeekMs != C.TIME_UNSET && isCurrentPodcast()) {
                    val seekTarget = pendingPodcastSeekMs
                    pendingPodcastSeekMs = C.TIME_UNSET
                    if (seekTarget > 0L) player.seekTo(seekTarget)
                }
            }
            if (playbackState == Player.STATE_ENDED && isCurrentPodcast()) {
                saveCurrentPodcastProgress(completed = true)
                if (sleepEndOfEpisode) {
                    sleepEndOfEpisode = false
                    player.pause()
                    SleepTimerState.clear()
                }
            }
            updateWidget()
        }

        override fun onPlayerError(error: PlaybackException) {
            handler.removeCallbacks(retry)
            if (!player.playWhenReady) return
            if (!isCurrentPodcast() && tryFallbackStreamVariant()) {
                return
            }
            if (error.errorCode in UNRECOVERABLE_ERRORS || retries >= MAX_RETRIES) {
                player.pause()
                updateWidget()
                return
            }
            retries++
            val delay = (1_000L shl minOf(retries - 1, 4)).coerceAtMost(10_000L)
            handler.postDelayed(retry, delay)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId
            if (id == lastMediaId) return
            if (Library.isPodcastEpisode(lastMediaId)) {
                saveCurrentPodcastProgress()
            }
            lastMediaId = id
            retries = 0
            triedVariantUrls.clear()
            handler.removeCallbacks(retry)

            if (Library.isPodcastEpisode(id)) {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.setPlaybackSpeed(AppSettingsStore.current.podcastSpeed)
                val epId = Library.parsePodcastEpisodeId(id)
                val ep = epId?.let { PodcastRepo.cachedEpisode(it) }
                pendingPodcastSeekMs = ep?.positionMs?.takeIf { it > 2_000L && !ep.completed } ?: C.TIME_UNSET
                if (id != null) StationRepo.onPlayedMediaId(id)
                updateCustomLayout()
                updateWidget(refreshFavoritesGrid = true)
                return
            }

            // Radio mode
            pendingPodcastSeekMs = C.TIME_UNSET
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.setPlaybackSpeed(1.0f)
            handler.post(retry)
            val (category, uuid) = id?.let(Library::parse) ?: return
            val station = StationRepo.cached(uuid) ?: return
            StationRepo.onPlayed(category, station)
            if (!station.isCurated && !station.isCustom) scope.launch { RadioBrowserApi.countClick(uuid) }
            updateCustomLayout()
            updateWidget(refreshFavoritesGrid = true)
        }

        override fun onMetadata(metadata: Metadata) {
            if (isCurrentPodcast()) return
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is IcyInfo) applyStreamTitle(entry.title)
            }
        }
    }

    private fun onInterruptionChanged() {
        if (isCurrentPodcast()) return
        val interrupted = !player.playWhenReady ||
            player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE
        val now = SystemClock.elapsedRealtime()
        if (interrupted) {
            if (interruptedAt == 0L) interruptedAt = now
            return
        }
        val since = interruptedAt
        interruptedAt = 0L
        if (since != 0L && now - since > RESUME_AT_LIVE_AFTER_MS &&
            player.playbackState != Player.STATE_IDLE && player.currentMediaItem != null
        ) {
            player.seekToDefaultPosition()
        }
    }

    private fun applyStreamTitle(raw: String?) {
        val item = player.currentMediaItem ?: return
        val station = currentStation() ?: return
        val song = raw?.trim()?.takeIf { it.length > 2 && !it.equals(station.displayName, ignoreCase = true) }
        if (song != null) StationRepo.recordSong(station, song)
        val title = song ?: station.displayName
        val artist = if (song != null) station.displayName else station.subtitle
        if (item.mediaMetadata.title?.toString() == title && item.mediaMetadata.artist?.toString() == artist) return
        val updated = item.buildUpon()
            .setMediaMetadata(item.mediaMetadata.buildUpon().setTitle(title).setArtist(artist).build())
            .build()
        player.replaceMediaItem(player.currentMediaItemIndex, updated)
        updateWidget()
    }

    // ==================== ANDROID AUTO v1.1 BROWSE TREE ====================

    private fun folder(
        id: String,
        title: String,
        subtitle: String? = null,
        artworkUri: Uri? = null,
        mediaType: Int = MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS,
        groupTitle: String? = null,
        browsableStyle: Int? = null,
        playableStyle: Int? = null,
    ): MediaItem {
        val extras = Bundle().apply {
            if (!groupTitle.isNullOrBlank()) putString(KEY_GROUP_TITLE, groupTitle)
            if (browsableStyle != null) putInt(KEY_STYLE_BROWSABLE, browsableStyle)
            if (playableStyle != null) putInt(KEY_STYLE_PLAYABLE, playableStyle)
        }
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(mediaType)
            .apply {
                if (!subtitle.isNullOrBlank()) setSubtitle(subtitle)
                if (artworkUri != null) setArtworkUri(artworkUri)
                if (!extras.isEmpty) setExtras(extras)
            }
            .build()
        return MediaItem.Builder().setMediaId(id).setMediaMetadata(meta).build()
    }

    private fun rootItem() = folder(Library.ROOT, getString(R.string.app_name))

    /**
     * Exactly 4 top-level tabs with crisp vector icons (zero Android Auto "More" overflow!):
     * 1. Favoritos (grid of logos + grouped Recently Played below)
     * 2. Explorar (grouped Category Grid: Countries [Spain, Brazil, US/UK] + World Genres [Rock, Classic Rock, Indie...])
     * 3. Podcasts (grouped Continue Listening with progress + Offline + Followed Shows)
     * 4. Recentes (quick grid of recent stations)
     */
    private fun rootFolders(): List<MediaItem> {
        val lang = AppSettingsStore.current.language
        return listOf(
            folder(
                id = Library.FAVORITES,
                title = if (lang == AppLanguage.EN) "Favorites" else "Favoritos",
                artworkUri = ArtworkProvider.resourceUri(this, R.drawable.ic_aa_favorites),
                playableStyle = STYLE_GRID,
            ),
            folder(
                id = Library.EXPLORE,
                title = if (lang == AppLanguage.EN) "Explore" else "Explorar",
                artworkUri = ArtworkProvider.resourceUri(this, R.drawable.ic_aa_explore),
                browsableStyle = STYLE_CATEGORY_GRID,
                playableStyle = STYLE_GRID,
            ),
            folder(
                id = Library.PODCASTS,
                title = "Podcasts",
                artworkUri = ArtworkProvider.resourceUri(this, R.drawable.ic_aa_podcasts),
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS,
                browsableStyle = STYLE_GRID,
                playableStyle = STYLE_LIST,
            ),
            folder(
                id = Library.RECENT,
                title = if (lang == AppLanguage.EN) "Recent" else "Recentes",
                artworkUri = ArtworkProvider.resourceUri(this, R.drawable.ic_aa_recent),
                playableStyle = STYLE_GRID,
            ),
        )
    }

    private fun exploreItems(): List<MediaItem> {
        val lang = AppSettingsStore.current.language
        val countriesGroup = if (lang == AppLanguage.EN) "By Country" else "Por País"
        val genresGroup = if (lang == AppLanguage.EN) "World Genres" else "Gêneros Mundiais"

        val countries = listOf(
            folder(
                id = Library.regionCategory("ES"),
                title = if (lang == AppLanguage.EN) "Spain" else "Espanha",
                subtitle = "Rock FM, LOS40, Cadena SER…",
                artworkUri = ArtworkProvider.categoryUri("🇪🇸", if (lang == AppLanguage.EN) "SPAIN" else "ESPANHA", 0xFFC60B1E.toInt(), 0xFF7F1D1D.toInt()),
                groupTitle = countriesGroup,
                playableStyle = STYLE_GRID,
            ),
            folder(
                id = Library.regionCategory("BR"),
                title = if (lang == AppLanguage.EN) "Brazil" else "Brasil",
                subtitle = "89 FM, Kiss FM, Antena 1…",
                artworkUri = ArtworkProvider.categoryUri("🇧🇷", if (lang == AppLanguage.EN) "BRAZIL" else "BRASIL", 0xFF009C3B.toInt(), 0xFF064E3B.toInt()),
                groupTitle = countriesGroup,
                playableStyle = STYLE_GRID,
            ),
            folder(
                id = Library.regionCategory("US", "GB"),
                title = if (lang == AppLanguage.EN) "USA & UK" else "EUA & Reino Unido",
                subtitle = "KEXP, BBC 6, Radio Paradise…",
                artworkUri = ArtworkProvider.categoryUri("🇺🇸", "USA · UK", 0xFF1D4ED8.toInt(), 0xFF1E3A8A.toInt()),
                groupTitle = countriesGroup,
                playableStyle = STYLE_GRID,
            ),
        )

        val genreBadges = mapOf(
            "rock" to "🎸",
            "classic_rock" to "⚡",
            "indie" to "✦",
            "pop" to "★",
            "jazz" to "♪",
            "electronic" to "◈",
            "mpb" to "☀",
            "80s" to "80s",
            "chill" to "☁",
            "classical" to "𝄞",
            "news" to "🎙",
        )
        val genres = Library.GENRES.map { g ->
            folder(
                id = Library.genreCategory(g.id),
                title = g.label(lang),
                artworkUri = ArtworkProvider.categoryUri(
                    badge = genreBadges[g.id] ?: "♪",
                    title = g.label(lang).uppercase(),
                    colorStart = g.colorStart.toInt(),
                    colorEnd = g.colorEnd.toInt(),
                ),
                groupTitle = genresGroup,
                playableStyle = STYLE_GRID,
            )
        }
        return countries + genres
    }

    private fun favoritesTabItems(): List<MediaItem> {
        val lang = AppSettingsStore.current.language
        val favGroup = if (lang == AppLanguage.EN) "Your Favorites" else "Seus Favoritos"
        val recGroup = if (lang == AppLanguage.EN) "Recently Played" else "Ouvidas Recentemente"
        val favs = StationRepo.favorites.value
        val favIds = favs.map { it.uuid }.toSet()
        val favItems = favs.map { toStationMediaItem(it, Library.FAVORITES, groupTitle = favGroup) }
        val extraRecents = StationRepo.recent.value
            .filterNot { it.uuid in favIds }
            .take(8)
            .map { toStationMediaItem(it, Library.RECENT, groupTitle = recGroup) }
        return favItems + extraRecents
    }

    private fun podcastsTabItems(): List<MediaItem> {
        val lang = AppSettingsStore.current.language
        val continueGroup = if (lang == AppLanguage.EN) "Continue Listening" else "Continuar Ouvindo"
        val offlineGroup = if (lang == AppLanguage.EN) "Downloaded (Offline)" else "Baixados (Offline)"
        val showsGroup = if (lang == AppLanguage.EN) "Your Podcasts" else "Seus Podcasts"

        val items = mutableListOf<MediaItem>()
        val inProgress = PodcastRepo.savedEpisodes.value.filter { it.positionMs > 2_000L && !it.completed }
        val inProgressIds = inProgress.map { it.id }.toSet()
        inProgress.take(8).forEach { items += it.toMediaItem(groupTitle = continueGroup) }

        val offlineOnly = PodcastRepo.downloadedEpisodes().filterNot { it.id in inProgressIds }
        offlineOnly.take(10).forEach { items += it.toMediaItem(groupTitle = offlineGroup) }

        PodcastRepo.subscriptions.value.forEach { show ->
            items += folder(
                id = Library.PODCAST_SHOW_PREFIX + show.id,
                title = show.title,
                subtitle = show.author,
                artworkUri = ArtworkProvider.uriFor(show.artworkUrl, show.title),
                mediaType = MediaMetadata.MEDIA_TYPE_PODCAST,
                groupTitle = showsGroup,
                playableStyle = STYLE_LIST,
            )
        }
        return items
    }

    private fun <T> page(items: List<T>, page: Int, pageSize: Int): List<T> {
        if (pageSize <= 0 || pageSize == Int.MAX_VALUE) return if (page == 0) items else emptyList()
        val from = page.toLong() * pageSize
        if (from >= items.size) return emptyList()
        return items.subList(from.toInt(), minOf(items.size, from.toInt() + pageSize))
    }

    private fun toStationMediaItem(station: Station, category: String, groupTitle: String? = null): MediaItem =
        station.toMediaItem(category, AppSettingsStore.current.streamQuality, isOnWifi(), groupTitle = groupTitle)

    private suspend fun expand(category: String, uuid: String): MediaItemsWithStartPosition {
        val list = runCatching { StationRepo.list(category) }.getOrDefault(emptyList())
        val index = list.indexOfFirst { it.uuid == uuid }
        if (index >= 0) return MediaItemsWithStartPosition(list.map { toStationMediaItem(it, category) }, index, C.TIME_UNSET)
        val station = StationRepo.find(uuid) ?: throw IllegalStateException("Unknown station $uuid")
        return MediaItemsWithStartPosition(listOf(toStationMediaItem(station, category)), 0, C.TIME_UNSET)
    }

    private fun resolvePodcastEpisode(epId: String): MediaItemsWithStartPosition {
        val ep = PodcastRepo.cachedEpisode(epId)
            ?: PodcastRepo.savedEpisodes.value.firstOrNull { it.id == epId }
            ?: throw IllegalStateException("Unknown episode $epId")
        val startMs = if (ep.positionMs > 2_000L && !ep.completed) ep.positionMs else 0L
        return MediaItemsWithStartPosition(listOf(ep.toMediaItem()), 0, startMs)
    }

    private suspend fun resume(): MediaItemsWithStartPosition {
        val last = StationRepo.lastPlayed
        if (Library.isPodcastEpisode(last)) {
            Library.parsePodcastEpisodeId(last)?.let { epId ->
                runCatching { return resolvePodcastEpisode(epId) }
            }
        }
        last?.let(Library::parse)?.let { (category, uuid) ->
            runCatching { return expand(category, uuid) }
        }
        val favorites = StationRepo.favorites.value
        if (favorites.isNotEmpty()) {
            return MediaItemsWithStartPosition(favorites.map { toStationMediaItem(it, Library.FAVORITES) }, 0, C.TIME_UNSET)
        }
        val category = Library.regionCategory(StationRepo.region)
        val stations = StationRepo.list(category)
        if (stations.isEmpty()) throw IllegalStateException("Nothing to play")
        return MediaItemsWithStartPosition(stations.map { toStationMediaItem(it, category) }, 0, C.TIME_UNSET)
    }

    private suspend fun playFromSearch(query: String): MediaItemsWithStartPosition {
        if (query.isBlank()) return resume()
        val favoriteMatch = StationRepo.matchFavorites(query).firstOrNull()
        if (favoriteMatch != null) return expand(Library.FAVORITES, favoriteMatch.uuid)
        val results = StationRepo.search(query)
        if (results.isEmpty()) throw IllegalStateException("No station for $query")
        return MediaItemsWithStartPosition(results.map { toStationMediaItem(it, Library.SEARCH) }, 0, C.TIME_UNSET)
    }

    private suspend fun resolveOne(item: MediaItem): MediaItem? {
        if (item.localConfiguration != null) return item
        Library.parsePodcastEpisodeId(item.mediaId)?.let { epId ->
            return PodcastRepo.cachedEpisode(epId)?.toMediaItem()
        }
        val (category, uuid) = Library.parse(item.mediaId) ?: return null
        return StationRepo.find(uuid)?.let { toStationMediaItem(it, category) }
    }

    private val libraryCallback = object : MediaLibrarySession.Callback {

        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(CMD_TOGGLE_FAVORITE)
                .add(CMD_RECONNECT_LIVE)
                .add(CMD_REWIND_15)
                .add(CMD_FORWARD_30)
                .add(CMD_CYCLE_SPEED)
                .add(CMD_SET_SLEEP_TIMER)
                .add(CMD_SET_SPEED)
                .add(CMD_APPLY_AUDIO_FX)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_TOGGLE_FAVORITE -> {
                    currentStation()?.let(StationRepo::toggleFavorite)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_RECONNECT_LIVE -> {
                    if (!isCurrentPodcast() && player.currentMediaItem != null) {
                        player.seekToDefaultPosition()
                        player.prepare()
                        player.play()
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_REWIND_15 -> {
                    seekBackBy(15_000L)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_FORWARD_30 -> {
                    seekForwardBy(30_000L)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_CYCLE_SPEED -> {
                    val cur = AppSettingsStore.current.podcastSpeed
                    val idx = SPEED_STEPS.indexOfFirst { kotlin.math.abs(it - cur) < 0.05f }
                    val next = SPEED_STEPS[(idx + 1) % SPEED_STEPS.size]
                    AppSettingsStore.update { it.copy(podcastSpeed = next) }
                    if (isCurrentPodcast()) {
                        player.setPlaybackSpeed(next)
                        updateCustomLayout()
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_SET_SLEEP_TIMER -> {
                    val mins = args.getInt(EXTRA_TIMER_MINUTES, 0)
                    val endEp = args.getBoolean(EXTRA_END_OF_EPISODE, false)
                    setSleepTimer(mins, endEp)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_SET_SPEED -> {
                    val spd = args.getFloat(EXTRA_SPEED, 1.0f).coerceIn(0.75f, 2.5f)
                    AppSettingsStore.update { it.copy(podcastSpeed = spd) }
                    if (isCurrentPodcast()) {
                        player.setPlaybackSpeed(spd)
                        updateCustomLayout()
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_APPLY_AUDIO_FX -> {
                    audioFx.applySettings(AppSettingsStore.current)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val extras = Bundle().apply {
                putBoolean("android.media.browse.SEARCH_SUPPORTED", true)
                putBoolean(KEY_STYLE_SUPPORTED, true)
                putInt(KEY_STYLE_BROWSABLE, STYLE_CATEGORY_GRID)
                putInt(KEY_STYLE_PLAYABLE, STYLE_GRID)
            }
            val root = if (params?.isRecent == true) folder(Library.RECENT, getString(R.string.folder_recent)) else rootItem()
            return Futures.immediateFuture(LibraryResult.ofItem(root, LibraryParams.Builder().setExtras(extras).build()))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            val items = when {
                parentId == Library.ROOT -> rootFolders()
                parentId == Library.FAVORITES -> favoritesTabItems()
                parentId == Library.EXPLORE -> exploreItems()
                parentId == Library.PODCASTS -> podcastsTabItems()
                parentId == Library.RECENT -> StationRepo.list(Library.RECENT).map { toStationMediaItem(it, Library.RECENT) }
                parentId.startsWith(Library.PODCAST_SHOW_PREFIX) -> {
                    val showId = parentId.removePrefix(Library.PODCAST_SHOW_PREFIX)
                    val show = PodcastRepo.subscriptions.value.firstOrNull { it.id == showId }
                    if (show != null) {
                        runCatching { PodcastRepo.episodesFor(show) }.getOrDefault(emptyList()).take(20).map { it.toMediaItem() }
                    } else emptyList()
                }
                parentId.startsWith(Library.REGION_PREFIX) || parentId.startsWith(Library.GENRE_PREFIX) ->
                    runCatching { StationRepo.list(parentId) }.getOrDefault(emptyList()).map { toStationMediaItem(it, parentId) }
                else -> emptyList()
            }
            LibraryResult.ofItemList(page(items, page, pageSize), params)
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
            val folder = (rootFolders() + exploreItems() + rootItem()).firstOrNull { it.mediaId == mediaId }
            val item = folder
                ?: Library.parsePodcastEpisodeId(mediaId)?.let { PodcastRepo.cachedEpisode(it)?.toMediaItem() }
                ?: Library.parse(mediaId)?.let { (category, uuid) -> StationRepo.find(uuid)?.let { toStationMediaItem(it, category) } }
            if (item != null) LibraryResult.ofItem(item, null) else LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            scope.launch {
                val results = runCatching { StationRepo.search(query) }.getOrDefault(emptyList())
                session.notifySearchResultChanged(browser, query, results.size, params)
            }
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            val results = runCatching { StationRepo.search(query) }.getOrDefault(emptyList())
            LibraryResult.ofItemList(page(results.map { toStationMediaItem(it, Library.SEARCH) }, page, pageSize), params)
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaItemsWithStartPosition> = scope.future {
            val first = mediaItems.firstOrNull()
            val query = first?.requestMetadata?.searchQuery
            when {
                first == null -> resume()
                mediaItems.size == 1 && query != null -> playFromSearch(query)
                mediaItems.size == 1 && Library.isPodcastEpisode(first.mediaId) -> {
                    val epId = Library.parsePodcastEpisodeId(first.mediaId)!!
                    resolvePodcastEpisode(epId)
                }
                mediaItems.size == 1 && first.localConfiguration == null && Library.parse(first.mediaId) != null -> {
                    val (category, uuid) = Library.parse(first.mediaId)!!
                    expand(category, uuid)
                }
                else -> {
                    val resolved = mediaItems.mapNotNull { resolveOne(it) }
                    if (resolved.isEmpty()) throw IllegalStateException("Nothing to play")
                    val index = if (startIndex == C.INDEX_UNSET) 0 else startIndex.coerceIn(0, resolved.size - 1)
                    MediaItemsWithStartPosition(resolved, index, startPositionMs)
                }
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = scope.future {
            mediaItems.mapNotNull { resolveOne(it) }.toMutableList()
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaItemsWithStartPosition> = scope.future { resume() }
    }
}
