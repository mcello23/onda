package io.github.mcello23.onda.ui

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import io.github.mcello23.onda.data.AppLanguage
import io.github.mcello23.onda.data.AppSettings
import io.github.mcello23.onda.data.AppSettingsStore
import io.github.mcello23.onda.data.BufferProfile
import io.github.mcello23.onda.data.CountryInfo
import io.github.mcello23.onda.data.EqPreset
import io.github.mcello23.onda.data.GenreInfo
import io.github.mcello23.onda.data.Library
import io.github.mcello23.onda.data.PodcastEpisode
import io.github.mcello23.onda.data.PodcastRepo
import io.github.mcello23.onda.data.PodcastShow
import io.github.mcello23.onda.data.Region
import io.github.mcello23.onda.data.SeedColor
import io.github.mcello23.onda.data.SongHistoryEntry
import io.github.mcello23.onda.data.Station
import io.github.mcello23.onda.data.StationRepo
import io.github.mcello23.onda.data.StreamQuality
import io.github.mcello23.onda.data.ThemeMode
import io.github.mcello23.onda.playback.AudioFxInfo
import io.github.mcello23.onda.playback.AudioFxState
import io.github.mcello23.onda.playback.PlaybackService
import io.github.mcello23.onda.playback.SleepTimerState
import io.github.mcello23.onda.playback.SleepTimerStatus
import io.github.mcello23.onda.widget.WidgetUpdater
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class NowPlaying(
    val station: Station? = null,
    val podcastEpisode: PodcastEpisode? = null,
    val songTitle: String? = null,
    val playWhenReady: Boolean = false,
    val buffering: Boolean = false,
    val failed: Boolean = false,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
) {
    val isPodcast: Boolean get() = podcastEpisode != null
    val hasActiveItem: Boolean get() = station != null || podcastEpisode != null
}

sealed interface Results {
    data object Loading : Results
    data class Error(val message: String) : Results
    data class Loaded(val header: String, val category: String, val stations: List<Station>) : Results
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val settings: StateFlow<AppSettings> = AppSettingsStore.settings
    val favorites: StateFlow<List<Station>> = StationRepo.favorites
    val recent: StateFlow<List<Station>> = StationRepo.recent
    val songHistory: StateFlow<List<SongHistoryEntry>> = StationRepo.songHistory
    val sleepTimer: StateFlow<SleepTimerStatus> = SleepTimerState.status
    val audioFxInfo: StateFlow<AudioFxInfo> = AudioFxState.info

    val subscriptions: StateFlow<List<PodcastShow>> = PodcastRepo.subscriptions
    val savedEpisodes: StateFlow<List<PodcastEpisode>> = PodcastRepo.savedEpisodes
    val downloadProgress: StateFlow<Map<String, Float>> = PodcastRepo.downloadProgress

    val regions: List<Region> = Library.REGIONS
    val genres: List<GenreInfo> = Library.GENRES

    private val _now = MutableStateFlow(NowPlaying())
    val now: StateFlow<NowPlaying> = _now.asStateFlow()

    // Home Tab state
    private val _homeCountry = MutableStateFlow(StationRepo.region)
    val homeCountry: StateFlow<String> = _homeCountry.asStateFlow()

    private val _homeStations = MutableStateFlow<List<Station>>(emptyList())
    val homeStations: StateFlow<List<Station>> = _homeStations.asStateFlow()

    private val _worldRockStations = MutableStateFlow<List<Station>>(emptyList())
    val worldRockStations: StateFlow<List<Station>> = _worldRockStations.asStateFlow()

    // Explore Tab state
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _exploreCountry = MutableStateFlow(StationRepo.region)
    val exploreCountry: StateFlow<String> = _exploreCountry.asStateFlow()

    private val _selectedGenreId = MutableStateFlow<String?>(null)
    val selectedGenreId: StateFlow<String?> = _selectedGenreId.asStateFlow()

    private val _countries = MutableStateFlow<List<CountryInfo>>(emptyList())
    val countries: StateFlow<List<CountryInfo>> = _countries.asStateFlow()

    private val retryTick = MutableStateFlow(0)

    private val _results = MutableStateFlow<Results>(Results.Loading)
    val results: StateFlow<Results> = _results.asStateFlow()

    // Podcasts Tab state
    private val _podcastQuery = MutableStateFlow("")
    val podcastQuery: StateFlow<String> = _podcastQuery.asStateFlow()

    private val _featuredPodcasts = MutableStateFlow<List<PodcastShow>>(emptyList())
    val featuredPodcasts: StateFlow<List<PodcastShow>> = _featuredPodcasts.asStateFlow()

    private val _podcastSearchResults = MutableStateFlow<List<PodcastShow>>(emptyList())
    val podcastSearchResults: StateFlow<List<PodcastShow>> = _podcastSearchResults.asStateFlow()

    private val _podcastLoading = MutableStateFlow(false)
    val podcastLoading: StateFlow<Boolean> = _podcastLoading.asStateFlow()

    private val _selectedShow = MutableStateFlow<PodcastShow?>(null)
    val selectedShow: StateFlow<PodcastShow?> = _selectedShow.asStateFlow()

    private val _showEpisodes = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val showEpisodes: StateFlow<List<PodcastEpisode>> = _showEpisodes.asStateFlow()

    private val _episodesLoading = MutableStateFlow(false)
    val episodesLoading: StateFlow<Boolean> = _episodesLoading.asStateFlow()

    private var controller: MediaController? = null
    private val controllerFuture = MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java)))
        .buildAsync()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh()
    }

    init {
        AppSettingsStore.init(app)
        StationRepo.init(app)
        PodcastRepo.init(app)

        controllerFuture.addListener({
            runCatching { controllerFuture.get() }.getOrNull()?.let {
                controller = it
                it.addListener(listener)
                refresh()
            }
        }, ContextCompat.getMainExecutor(app))

        // Curated world rock immediately + online enrichment
        _worldRockStations.value = StationRepo.curatedForGenre("rock")
        viewModelScope.launch {
            runCatching { StationRepo.genreStations("rock") }.getOrNull()?.let { _worldRockStations.value = it }
        }

        // Load Home country stations whenever homeCountry changes
        viewModelScope.launch {
            _homeCountry.collectLatest { code ->
                _homeStations.value = StationRepo.curated.filter { it.countrycode.equals(code, ignoreCase = true) }
                runCatching { StationRepo.regionStations(listOf(code)) }.getOrNull()?.let {
                    _homeStations.value = it
                }
            }
        }

        // Load Explore results
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            combine(_query.debounce(350), _exploreCountry, _selectedGenreId, retryTick) { q, c, g, _ ->
                Triple(q, c, g)
            }.collectLatest { (q, c, g) ->
                loadExplore(q, c, g)
            }
        }

        // Load all countries list in background
        viewModelScope.launch {
            runCatching { StationRepo.countries() }.getOrNull()?.let { _countries.value = it }
        }

        // Featured & search Podcasts (refreshes top charts when country changes!)
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            combine(_podcastQuery.debounce(400), _homeCountry) { q, c -> q to c }.collectLatest { (q, cc) ->
                if (q.isBlank()) {
                    _podcastSearchResults.value = emptyList()
                    _podcastLoading.value = true
                    _featuredPodcasts.value = runCatching { PodcastRepo.featured(cc) }.getOrDefault(emptyList())
                    _podcastLoading.value = false
                } else {
                    _podcastLoading.value = true
                    _podcastSearchResults.value = runCatching { PodcastRepo.search(q, cc) }.getOrDefault(emptyList())
                    _podcastLoading.value = false
                }
            }
        }

        // Live ticker for podcast seek bar
        viewModelScope.launch {
            while (isActive) {
                delay(500L)
                val c = controller
                if (c != null && c.playWhenReady && Library.isPodcastEpisode(c.currentMediaItem?.mediaId)) {
                    refresh()
                }
            }
        }
    }

    fun setHomeCountry(code: String) {
        val cc = code.uppercase()
        StationRepo.region = cc
        _homeCountry.value = cc
        _exploreCountry.value = cc
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun setExploreCountry(code: String) {
        val cc = code.uppercase()
        StationRepo.region = cc
        _exploreCountry.value = cc
        _homeCountry.value = cc
        _selectedGenreId.value = null
    }

    fun selectGenre(genreId: String?) {
        _selectedGenreId.value = genreId
        if (genreId != null) _query.value = ""
    }

    fun retryExplore() {
        retryTick.value++
    }

    fun countryLabel(code: String): String {
        val lang = settings.value.language
        regions.firstOrNull { it.code.equals(code, ignoreCase = true) }?.let { return it.label(lang) }
        _countries.value.firstOrNull { it.code.equals(code, ignoreCase = true) }?.let { return it.name }
        return code
    }

    private suspend fun loadExplore(q: String, countryCode: String, genreId: String?) {
        val lang = settings.value.language
        _results.value = Results.Loading
        _results.value = try {
            when {
                q.isNotBlank() -> Results.Loaded(
                    L(lang, "Resultados para \"${q.trim()}\"", "Results for \"${q.trim()}\""),
                    Library.SEARCH,
                    StationRepo.search(q),
                )
                genreId != null -> {
                    val g = Library.genreById(genreId)
                    val title = g?.label(lang) ?: genreId
                    Results.Loaded(
                        L(lang, "Melhores rádios · $title", "Top stations · $title"),
                        Library.genreCategory(genreId),
                        StationRepo.genreStations(genreId),
                    )
                }
                else -> {
                    val label = countryLabel(countryCode)
                    val category = Library.regionCategory(countryCode)
                    Results.Loaded(
                        L(lang, "Top estações · $label", "Top stations · $label"),
                        category,
                        StationRepo.list(category),
                    )
                }
            }
        } catch (_: Exception) {
            Results.Error(L(lang, "Não foi possível conectar ao diretório. Verifique sua internet.", "Couldn't reach the station directory. Check your connection."))
        }
    }

    // --- Podcasts ---
    fun setPodcastQuery(q: String) {
        _podcastQuery.value = q
    }

    fun openPodcastShow(show: PodcastShow) {
        _selectedShow.value = show
        _showEpisodes.value = emptyList()
        _episodesLoading.value = true
        viewModelScope.launch {
            _showEpisodes.value = runCatching { PodcastRepo.episodesFor(show) }.getOrDefault(emptyList())
            _episodesLoading.value = false
        }
    }

    fun closePodcastShow() {
        _selectedShow.value = null
    }

    fun toggleSubscription(show: PodcastShow) = PodcastRepo.toggleSubscription(show)

    fun downloadEpisode(ep: PodcastEpisode) = PodcastRepo.downloadEpisode(ep)

    fun cancelDownload(epId: String) = PodcastRepo.cancelDownload(epId)

    fun deleteDownload(epId: String) = PodcastRepo.deleteDownload(epId)

    // --- Playback Controls ---
    private fun withController(block: (MediaController) -> Unit) {
        controller?.let(block) ?: controllerFuture.addListener({
            runCatching { controllerFuture.get() }.getOrNull()?.let(block)
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    fun play(station: Station, category: String) = withController { c ->
        val currentId = c.currentMediaItem?.mediaId
        if (currentId?.let(Library::parse)?.second == station.uuid && c.playWhenReady && c.playerError == null) return@withController
        c.setMediaItem(MediaItem.Builder().setMediaId(Library.mediaId(category, station.uuid)).build())
        c.prepare()
        c.play()
    }

    fun playPodcast(episode: PodcastEpisode) = withController { c ->
        PodcastRepo.rememberEpisode(episode)
        val targetId = Library.podcastEpisodeMediaId(episode.id)
        if (c.currentMediaItem?.mediaId == targetId) {
            if (!c.playWhenReady) {
                if (c.playbackState == Player.STATE_IDLE) c.prepare()
                c.play()
            }
            return@withController
        }
        c.setMediaItem(MediaItem.Builder().setMediaId(targetId).build())
        c.prepare()
        c.play()
    }

    fun togglePlayback() {
        val c = controller ?: return
        if (c.playWhenReady) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun skipNext() {
        controller?.seekToNext()
    }

    fun skipPrevious() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        refresh()
    }

    fun rewind15() = sendCommand(PlaybackService.ACTION_REWIND_15)

    fun forward30() = sendCommand(PlaybackService.ACTION_FORWARD_30)

    fun setPodcastSpeed(speed: Float) {
        AppSettingsStore.update { it.copy(podcastSpeed = speed) }
        sendCommand(PlaybackService.ACTION_SET_SPEED, Bundle().apply { putFloat(PlaybackService.EXTRA_SPEED, speed) })
        refresh()
    }

    fun setSleepTimer(minutes: Int, endOfEpisode: Boolean = false) {
        sendCommand(
            PlaybackService.ACTION_SET_SLEEP_TIMER,
            Bundle().apply {
                putInt(PlaybackService.EXTRA_TIMER_MINUTES, minutes)
                putBoolean(PlaybackService.EXTRA_END_OF_EPISODE, endOfEpisode)
            }
        )
    }

    private fun sendCommand(action: String, args: Bundle = Bundle.EMPTY) = withController { c ->
        c.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args)
    }

    // --- Favorites, Custom Stations, History ---
    fun toggleFavorite(station: Station) {
        StationRepo.toggleFavorite(station)
        WidgetUpdater.notifyFavoritesChanged(getApplication())
    }

    fun moveFavorite(from: Int, to: Int) {
        StationRepo.moveFavorite(from, to)
        WidgetUpdater.notifyFavoritesChanged(getApplication())
    }

    fun addCustomStation(name: String, url: String, country: String, genre: String) {
        val added = StationRepo.addCustomStation(name, url, country, genre)
        WidgetUpdater.notifyFavoritesChanged(getApplication())
        play(added, Library.FAVORITES)
    }

    fun clearSongHistory() = StationRepo.clearSongHistory()

    // --- Settings ---
    fun setThemeMode(mode: ThemeMode) = AppSettingsStore.update { it.copy(themeMode = mode) }

    fun setDynamicColor(enabled: Boolean) = AppSettingsStore.update { it.copy(useDynamicColor = enabled) }

    fun setSeedColor(seed: SeedColor) = AppSettingsStore.update { it.copy(seedColor = seed) }

    fun setStreamQuality(quality: StreamQuality) = AppSettingsStore.update { it.copy(streamQuality = quality) }

    fun setBufferProfile(profile: BufferProfile) = AppSettingsStore.update { it.copy(bufferProfile = profile) }

    fun setNormalizeVolume(enabled: Boolean) {
        AppSettingsStore.update { it.copy(normalizeVolume = enabled) }
        sendCommand(PlaybackService.ACTION_APPLY_AUDIO_FX)
    }

    fun setEqEnabled(enabled: Boolean) {
        AppSettingsStore.update { it.copy(eqEnabled = enabled) }
        sendCommand(PlaybackService.ACTION_APPLY_AUDIO_FX)
    }

    fun setEqPreset(preset: EqPreset) {
        AppSettingsStore.update { it.copy(eqEnabled = true, eqPreset = preset, eqBandLevels = preset.gainsMb) }
        sendCommand(PlaybackService.ACTION_APPLY_AUDIO_FX)
    }

    fun setEqBandLevel(bandIndex: Int, levelMb: Int) {
        AppSettingsStore.update { cur ->
            val list = cur.eqBandLevels.toMutableList()
            while (list.size <= bandIndex) list.add(0)
            list[bandIndex] = levelMb
            cur.copy(eqEnabled = true, eqPreset = EqPreset.CUSTOM, eqBandLevels = list)
        }
        sendCommand(PlaybackService.ACTION_APPLY_AUDIO_FX)
    }

    fun setLanguage(lang: AppLanguage) {
        AppSettingsStore.update { it.copy(language = lang) }
        retryTick.value++
    }

    fun setHomeFavoritesCollapsed(collapsed: Boolean) {
        AppSettingsStore.update { it.copy(homeFavoritesCollapsed = collapsed) }
    }

    fun exportBackupJson(): String = StationRepo.exportBackupJson()

    fun importBackupJson(raw: String): Boolean = StationRepo.importBackupJson(raw).isSuccess

    private fun refresh() {
        val c = controller ?: return
        val mediaId = c.currentMediaItem?.mediaId
        val podId = Library.parsePodcastEpisodeId(mediaId)
        val podcastEp = podId?.let { PodcastRepo.cachedEpisode(it) ?: PodcastRepo.savedEpisodes.value.firstOrNull { e -> e.id == it } }
        val station = if (podcastEp == null) mediaId?.let(Library::parse)?.second?.let(StationRepo::cached) else null
        val title = c.mediaMetadata.title?.toString()
        val pos = c.currentPosition.coerceAtLeast(0L)
        val dur = c.duration.takeIf { it > 0L } ?: podcastEp?.durationMs ?: 0L

        _now.value = NowPlaying(
            station = station,
            podcastEpisode = podcastEp,
            songTitle = if (station != null) title?.takeIf { it != station.displayName } else null,
            playWhenReady = c.playWhenReady,
            buffering = c.playbackState == Player.STATE_BUFFERING,
            failed = c.playerError != null,
            hasPrevious = c.hasPreviousMediaItem(),
            hasNext = c.hasNextMediaItem(),
            positionMs = pos,
            durationMs = dur,
            playbackSpeed = settings.value.podcastSpeed,
        )
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
    }
}
