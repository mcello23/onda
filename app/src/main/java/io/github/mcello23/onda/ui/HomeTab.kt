package io.github.mcello23.onda.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    onMoveFavorite: (Int, Int) -> Unit,
    onOpenExploreCountry: () -> Unit,
    onOpenWorldRock: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAddCustom: () -> Unit,
) {
    var editingFavs by remember { mutableStateOf(false) }

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

        // Favorites Grid (Expressive Material You 2-column squircle tiles)
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        L(lang, "Seus Favoritos", "Your Favorites"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (favorites.size > 1) {
                        TextButton(onClick = { editingFavs = !editingFavs }) {
                            Icon(
                                if (editingFavs) Icons.Default.Close else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (editingFavs) L(lang, "Pronto", "Done") else L(lang, "Ordenar", "Reorder"))
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
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
                Spacer(Modifier.height(8.dp))

                if (favorites.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    L(lang, "Sem favoritos ainda", "No favorites yet"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    L(lang, "Toque no coração em qualquer rádio para acesso rápido aqui, no Widget e no Android Auto.", "Tap the heart on any station for instant access here, on the Widget and Android Auto."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        favorites.chunked(2).forEachIndexed { rowIdx, pair ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                pair.forEachIndexed { colIdx, station ->
                                    val idx = rowIdx * 2 + colIdx
                                    val isCurrent = !now.isPodcast && now.station?.uuid == station.uuid
                                    val isPlaying = isCurrent && now.playWhenReady
                                    Surface(
                                        onClick = { onPlayStation(station, Library.FAVORITES) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer,
                                        border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    ) {
                                        Row(
                                            Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            StationArt(station, Modifier.size(46.dp), corner = 14.dp)
                                            Column(Modifier.weight(1f).padding(start = 10.dp, end = 4.dp)) {
                                                Text(
                                                    station.displayName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    if (isPlaying) {
                                                        AnimatedEqualizerBars(modifier = Modifier.size(11.dp))
                                                    }
                                                    Text(
                                                        "${station.flag} ${station.qualityBadge.ifBlank { station.country }}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                            if (editingFavs) {
                                                Column {
                                                    if (idx > 0) {
                                                        IconButton(onClick = { onMoveFavorite(idx, idx - 1) }, modifier = Modifier.size(24.dp)) {
                                                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                    if (idx < favorites.lastIndex) {
                                                        IconButton(onClick = { onMoveFavorite(idx, idx + 1) }, modifier = Modifier.size(24.dp)) {
                                                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
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

        // Top Stations for Active Country (Spain by default!)
        item {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${L(lang, "Top Estações", "Top Stations")} · $homeCountryLabel ${countryFlag(homeCountry)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onOpenExploreCountry) {
                        Text(L(lang, "Ver todas", "See all"))
                    }
                }
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val cat = Library.regionCategory(homeCountry)
                    items(homeStations.take(15), key = { it.uuid }) { station ->
                        val isCurrent = !now.isPodcast && now.station?.uuid == station.uuid
                        Surface(
                            onClick = { onPlayStation(station, cat) },
                            shape = RoundedCornerShape(22.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.width(136.dp),
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Box {
                                    StationArt(station, Modifier.size(116.dp), corner = 16.dp)
                                    if (station.qualityBadge.isNotBlank()) {
                                        QualityPill(
                                            station.qualityBadge,
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    station.displayName,
                                    style = MaterialTheme.typography.titleSmall,
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
