package io.github.mcello23.onda.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mcello23.onda.data.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val lang = settings.language
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val songHistory by vm.songHistory.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val sleepTimer by vm.sleepTimer.collectAsStateWithLifecycle()
    val fxInfo by vm.audioFxInfo.collectAsStateWithLifecycle()

    val homeCountry by vm.homeCountry.collectAsStateWithLifecycle()
    val homeStations by vm.homeStations.collectAsStateWithLifecycle()
    val worldRockStations by vm.worldRockStations.collectAsStateWithLifecycle()

    val query by vm.query.collectAsStateWithLifecycle()
    val exploreCountry by vm.exploreCountry.collectAsStateWithLifecycle()
    val selectedGenreId by vm.selectedGenreId.collectAsStateWithLifecycle()
    val countries by vm.countries.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()

    val podcastQuery by vm.podcastQuery.collectAsStateWithLifecycle()
    val podcastLoading by vm.podcastLoading.collectAsStateWithLifecycle()
    val featuredPodcasts by vm.featuredPodcasts.collectAsStateWithLifecycle()
    val podcastSearchResults by vm.podcastSearchResults.collectAsStateWithLifecycle()
    val subscriptions by vm.subscriptions.collectAsStateWithLifecycle()
    val savedEpisodes by vm.savedEpisodes.collectAsStateWithLifecycle()
    val downloadProgress by vm.downloadProgress.collectAsStateWithLifecycle()
    val selectedShow by vm.selectedShow.collectAsStateWithLifecycle()
    val showEpisodes by vm.showEpisodes.collectAsStateWithLifecycle()
    val episodesLoading by vm.episodesLoading.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var showCountryPicker by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showSleepTimer by rememberSaveable { mutableStateOf(false) }
    var showEqualizer by rememberSaveable { mutableStateOf(false) }
    var showAddCustom by rememberSaveable { mutableStateOf(false) }

    val favoriteIds = favorites.map { it.uuid }.toSet()
    val continuePodcasts = savedEpisodes.filter { (it.positionMs > 2_000L && !it.completed) || it.isDownloaded }

    Scaffold(
        bottomBar = {
            Column {
                if (now.hasActiveItem) {
                    FloatingMiniPlayer(
                        now = now,
                        lang = lang,
                        onOpen = { showPlayer = true },
                        onToggle = vm::togglePlayback,
                    )
                }
                NavigationBar {
                    val items = listOf(
                        Triple(L(lang, "Início", "Home"), Icons.Filled.Home, Icons.Outlined.Home),
                        Triple(L(lang, "Explorar", "Explore"), Icons.Filled.Explore, Icons.Outlined.Explore),
                        Triple("Podcasts", Icons.Filled.Podcasts, Icons.Outlined.Podcasts),
                        Triple(L(lang, "Opções", "Settings"), Icons.Filled.Settings, Icons.Outlined.Settings),
                    )
                    items.forEachIndexed { idx, (label, filled, outlined) ->
                        NavigationBarItem(
                            selected = tab == idx,
                            onClick = { tab = idx },
                            icon = { Icon(if (tab == idx) filled else outlined, contentDescription = label) },
                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> HomeTab(
                    lang = lang,
                    favorites = favorites,
                    recent = recent,
                    continuePodcasts = continuePodcasts,
                    regions = vm.regions,
                    homeCountry = homeCountry,
                    homeCountryLabel = vm.countryLabel(homeCountry),
                    homeStations = homeStations,
                    worldRockStations = worldRockStations,
                    songHistory = songHistory,
                    now = now,
                    favoriteIds = favoriteIds,
                    onSelectCountry = vm::setHomeCountry,
                    onOpenCountryPicker = { showCountryPicker = true },
                    onPlayStation = vm::play,
                    onPlayPodcast = vm::playPodcast,
                    onToggleFavorite = vm::toggleFavorite,
                    onMoveFavorite = vm::moveFavorite,
                    onOpenExploreCountry = {
                        vm.selectGenre(null)
                        tab = 1
                    },
                    onOpenWorldRock = {
                        vm.selectGenre("rock")
                        tab = 1
                    },
                    onOpenHistory = { showHistory = true },
                    onOpenAddCustom = { showAddCustom = true },
                )
                1 -> ExploreTab(
                    lang = lang,
                    query = query,
                    countryCode = exploreCountry,
                    countryLabel = vm.countryLabel(exploreCountry),
                    selectedGenreId = selectedGenreId,
                    genres = vm.genres,
                    results = results,
                    now = now,
                    favoriteIds = favoriteIds,
                    onQuery = vm::setQuery,
                    onOpenCountryPicker = { showCountryPicker = true },
                    onSelectGenre = vm::selectGenre,
                    onRetry = vm::retryExplore,
                    onPlay = vm::play,
                    onFavorite = vm::toggleFavorite,
                )
                2 -> PodcastsTab(
                    lang = lang,
                    query = podcastQuery,
                    loading = podcastLoading,
                    featured = featuredPodcasts,
                    searchResults = podcastSearchResults,
                    subscriptions = subscriptions,
                    savedEpisodes = savedEpisodes,
                    downloadProgress = downloadProgress,
                    now = now,
                    selectedShow = selectedShow,
                    showEpisodes = showEpisodes,
                    episodesLoading = episodesLoading,
                    onQuery = vm::setPodcastQuery,
                    onOpenShow = vm::openPodcastShow,
                    onCloseShow = vm::closePodcastShow,
                    onToggleSubscribe = vm::toggleSubscription,
                    onPlayEpisode = vm::playPodcast,
                    onDownloadEpisode = vm::downloadEpisode,
                    onCancelDownload = vm::cancelDownload,
                    onDeleteDownload = vm::deleteDownload,
                )
                3 -> SettingsTab(
                    settings = settings,
                    countryLabel = vm.countryLabel(settings.defaultCountry),
                    onThemeMode = vm::setThemeMode,
                    onDynamicColor = vm::setDynamicColor,
                    onSeedColor = vm::setSeedColor,
                    onLanguage = vm::setLanguage,
                    onStreamQuality = vm::setStreamQuality,
                    onBufferProfile = vm::setBufferProfile,
                    onNormalizeVolume = vm::setNormalizeVolume,
                    onOpenEqualizer = { showEqualizer = true },
                    onOpenCountryPicker = { showCountryPicker = true },
                    onOpenAddCustom = { showAddCustom = true },
                    onExportJson = vm::exportBackupJson,
                    onImportJson = vm::importBackupJson,
                )
            }
        }
    }

    LaunchedEffect(!now.hasActiveItem) {
        if (!now.hasActiveItem) showPlayer = false
    }

    if (showPlayer && now.hasActiveItem) {
        ModalBottomSheet(
            onDismissRequest = { showPlayer = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            val speedSteps = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
            NowPlayingSheet(
                now = now,
                settings = settings,
                sleepTimer = sleepTimer,
                isFavorite = now.station?.uuid in favoriteIds,
                onToggle = vm::togglePlayback,
                onPrevious = vm::skipPrevious,
                onNext = vm::skipNext,
                onRewind15 = vm::rewind15,
                onForward30 = vm::forward30,
                onSeekTo = vm::seekTo,
                onCycleSpeed = {
                    val idx = speedSteps.indexOfFirst { kotlin.math.abs(it - settings.podcastSpeed) < 0.05f }
                    val next = speedSteps[(idx + 1) % speedSteps.size]
                    vm.setPodcastSpeed(next)
                },
                onFavorite = { now.station?.let(vm::toggleFavorite) },
                onOpenSleepTimer = { showSleepTimer = true },
                onOpenEqualizer = { showEqualizer = true },
                onOpenHistory = { showHistory = true },
            )
        }
    }

    if (showCountryPicker) {
        CountryPickerSheet(
            countries = countries,
            selectedCode = exploreCountry,
            lang = lang,
            onSelect = vm::setExploreCountry,
            onDismiss = { showCountryPicker = false },
        )
    }

    if (showHistory) {
        SongHistorySheet(
            history = songHistory,
            lang = lang,
            onOpenSpotify = { openExternalMusicSearch(context, it, spotify = true) },
            onOpenYouTubeMusic = { openExternalMusicSearch(context, it, spotify = false) },
            onClear = vm::clearSongHistory,
            onDismiss = { showHistory = false },
        )
    }

    if (showSleepTimer) {
        SleepTimerDialog(
            lang = lang,
            isPodcast = now.isPodcast,
            current = sleepTimer,
            onSelect = vm::setSleepTimer,
            onDismiss = { showSleepTimer = false },
        )
    }

    if (showEqualizer) {
        EqualizerBottomSheet(
            lang = lang,
            settings = settings,
            fxInfo = fxInfo,
            onToggleNormalize = vm::setNormalizeVolume,
            onToggleEq = vm::setEqEnabled,
            onSelectPreset = vm::setEqPreset,
            onBandChange = vm::setEqBandLevel,
            onDismiss = { showEqualizer = false },
        )
    }

    if (showAddCustom) {
        AddCustomStationDialog(
            lang = lang,
            onAdd = vm::addCustomStation,
            onDismiss = { showAddCustom = false },
        )
    }
}

@Composable
private fun FloatingMiniPlayer(
    now: NowPlaying,
    lang: AppLanguage,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    val station = now.station
    val ep = now.podcastEpisode

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onOpen),
    ) {
        Column {
            if (now.isPodcast && now.durationMs > 0L) {
                LinearProgressIndicator(
                    progress = { (now.positionMs.toFloat() / now.durationMs.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (now.isPodcast && ep != null) {
                    PodcastArt(ep.podcastArtwork, ep.podcastTitle, Modifier.size(46.dp), corner = 14.dp)
                } else if (station != null) {
                    StationArt(station, Modifier.size(46.dp), corner = 14.dp)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        text = if (now.isPodcast) ep?.title.orEmpty() else station?.displayName.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (now.playWhenReady && !now.failed) {
                            AnimatedEqualizerBars(
                                color = MaterialTheme.colorScheme.primary,
                                playing = !now.buffering,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                        Text(
                            text = statusText(now, lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Surface(
                    onClick = onToggle,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            when {
                                !now.playWhenReady -> Icons.Default.PlayArrow
                                now.isPodcast -> Icons.Default.Pause
                                else -> Icons.Default.Stop
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }
    }
}
