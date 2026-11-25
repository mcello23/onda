package io.github.mcello23.onda.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mcello23.onda.data.AppLanguage
import io.github.mcello23.onda.data.Library
import io.github.mcello23.onda.data.PodcastEpisode
import io.github.mcello23.onda.data.Region
import io.github.mcello23.onda.data.SongHistoryEntry
import io.github.mcello23.onda.data.Station
import io.github.mcello23.onda.data.countryFlag

@Composable
fun HomeTab(
    lang: AppLanguage,
    favorites: List<Station>,
    favoritesCollapsed: Boolean,
    recent: List<Station>,
    continuePodcasts: List<PodcastEpisode>,
    regions: List<Region>,
    homeCountry: String,
    homeCountryLabel: String,
    homeStations: List<Station>,
    worldRockStations: List<Station>,
    songHistory: List<SongHistoryEntry>,
    now: NowPlaying,
    favoriteIds: Set<String>,
    onSelectCountry: (String) -> Unit,
    onOpenCountryPicker: () -> Unit,
    onPlayStation: (Station, String) -> Unit,
    onPlayPodcast: (PodcastEpisode) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    onToggleFavoritesCollapsed: () -> Unit,
    onOpenFavoritesTab: () -> Unit,
    onOpenExploreCountry: () -> Unit,
    onOpenWorldRock: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAddCustom: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (favoritesCollapsed) -90f else 0f,
        label = "fav_chevron",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Header
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        greetingText(lang),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Onda",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                IconButton(onClick = onOpenAddCustom, modifier = Modifier.testTag("home_add_custom")) {
                    Icon(Icons.Default.Add, contentDescription = L(lang, "Adicionar rádio", "Add radio"))
                }
                IconButton(onClick = onOpenHistory, modifier = Modifier.testTag("home_song_history")) {
                    Icon(Icons.Default.History, contentDescription = L(lang, "Histórico de músicas", "Song history"))
                }
            }
        }

        // Quick Country Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(regions, key = { it.code }) { r ->
                    val selected = homeCountry.equals(r.code, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectCountry(r.code) },
                        label = { Text("${r.flag} ${r.label(lang)}", fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(999.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
                item {
                    val isCustomCountry = regions.none { it.code.equals(homeCountry, ignoreCase = true) }
                    FilterChip(
                        selected = isCustomCountry,
                        onClick = onOpenCountryPicker,
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = {
                            Text(
                                if (isCustomCountry) "${countryFlag(homeCountry)} $homeCountryLabel"
                                else L(lang, "+ Países", "+ Countries"),
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        shape = RoundedCornerShape(999.dp),
                    )
                }
            }
        }

        // Collapsible Favorites Strip (Option A) — 1 compact horizontal row + See all → Favorites tab
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Column(Modifier.padding(vertical = 10.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onToggleFavoritesCollapsed)
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .testTag("home_favorites_toggle"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = if (favoritesCollapsed) {
                                    L(lang, "Expandir favoritos", "Expand favorites")
                                } else {
                                    L(lang, "Recolher favoritos", "Collapse favorites")
                                },
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp).rotate(chevronRotation),
                            )
                            Text(
                                L(lang, "Seus Favoritos", "Your Favorites"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (favorites.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    Text(
                                        "${favorites.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }

                        if (favorites.isNotEmpty()) {
                            TextButton(
                                onClick = onOpenFavoritesTab,
                                modifier = Modifier.testTag("home_favorites_see_all"),
                            ) {
                                Text(L(lang, "Ver todos", "See all"))
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(end = 6.dp),
                            ) {
                                Text(
                                    "Android Auto ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = !favoritesCollapsed,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        if (favorites.isEmpty()) {
                            Row(
                                Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        L(lang, "Sem favoritos ainda", "No favorites yet"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        L(
                                            lang,
                                            "Toque no coração em qualquer rádio para acesso rápido aqui, no Widget e no Android Auto.",
                                            "Tap the heart on any station for instant access here, on the Widget and Android Auto.",
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.testTag("home_favorites_strip"),
                            ) {
                                items(favorites, key = { "fav-strip-${it.uuid}" }) { station ->
                                    val isCurrent = !now.isPodcast && now.station?.uuid == station.uuid
                                    val isPlaying = isCurrent && now.playWhenReady
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(74.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onPlayStation(station, Library.FAVORITES) }
                                            .padding(vertical = 2.dp),
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                        ) {
                                            Box(Modifier.padding(if (isCurrent) 2.dp else 0.dp)) {
                                                StationArt(station, Modifier.size(60.dp), corner = 16.dp)
                                                if (isPlaying) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                                                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                                                    ) {
                                                        AnimatedEqualizerBars(
                                                            modifier = Modifier.padding(4.dp).size(11.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            station.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Continue Listening (Podcasts in progress / downloaded)
        if (continuePodcasts.isNotEmpty()) {
            item {
                Column {
                    Text(
                        L(lang, "Continuar Ouvindo · Podcasts", "Continue Listening · Podcasts"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(continuePodcasts.take(6), key = { it.id }) { ep ->
                            Surface(
                                onClick = { onPlayPodcast(ep) },
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.width(264.dp),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        PodcastArt(ep.podcastArtwork, ep.podcastTitle, Modifier.size(52.dp), corner = 14.dp)
                                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                            Text(
                                                ep.podcastTitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                ep.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { ep.progressFraction.coerceAtLeast(0.04f) },
                                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)),
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        if (ep.isDownloaded) {
                                            Icon(Icons.Default.OfflinePin, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Offline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.weight(1f))
                                        if (ep.durationMs > 0L) {
                                            val rem = (ep.durationMs - ep.positionMs).coerceAtLeast(0L)
                                            Text(
                                                L(lang, "Restam ${formatDurationMs(rem)}", "${formatDurationMs(rem)} left"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top Stations for Active Country — 2 rows × 3 columns (6 stations) + See all
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${L(lang, "Top Estações", "Top Stations")} · $homeCountryLabel ${countryFlag(homeCountry)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onOpenExploreCountry,
                        modifier = Modifier.testTag("home_top_stations_see_all"),
                    ) {
                        Text(L(lang, "Ver todas", "See all"))
                    }
                }
                Spacer(Modifier.height(6.dp))
                val cat = Library.regionCategory(homeCountry)
                val rows = homeStations.take(6).chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { rowStations ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowStations.forEach { station ->
                                val isCurrent = !now.isPodcast && now.station?.uuid == station.uuid
                                Surface(
                                    onClick = { onPlayStation(station, cat) },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                                            StationArt(station, Modifier.fillMaxSize(), corner = 14.dp)
                                            if (station.qualityBadge.isNotBlank()) {
                                                QualityPill(
                                                    station.qualityBadge,
                                                    modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            station.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            station.tags.split(',').firstOrNull()?.trim().orEmpty().ifBlank { station.country },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            repeat(3 - rowStations.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Best Rock Stations in the World
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    L(lang, "🎸 Melhores de Rock do Mundo", "🎸 Best Rock Stations Worldwide"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpenWorldRock) {
                    Text(L(lang, "Ver mais", "More"))
                }
            }
        }

        items(worldRockStations.take(6), key = { "rock-${it.uuid}" }) { station ->
            val isCurrent = !now.isPodcast && now.station?.uuid == station.uuid
            StationRow(
                station = station,
                isFavorite = station.uuid in favoriteIds,
                isCurrent = isCurrent,
                isPlaying = isCurrent && now.playWhenReady,
                onClick = { onPlayStation(station, Library.genreCategory("rock")) },
                onFavorite = { onToggleFavorite(station) },
            )
        }

        // Recent Stations Carousel
        if (recent.isNotEmpty()) {
            item {
                Column {
                    Text(
                        L(lang, "Ouvidas Recentemente", "Recently Played"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(recent.take(10), key = { "rec-${it.uuid}" }) { station ->
                            Surface(
                                onClick = { onPlayStation(station, Library.RECENT) },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    StationArt(station, Modifier.size(36.dp), corner = 10.dp)
                                    Text(
                                        station.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Discovered Songs Banner
        if (songHistory.isNotEmpty()) {
            item {
                val latest = songHistory.first()
                Surface(
                    onClick = onOpenHistory,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                L(lang, "Última música identificada", "Last identified song"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                latest.songTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${latest.stationName} · ${L(lang, "Abrir no Spotify / YT Music", "Open in Spotify / YT Music")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
