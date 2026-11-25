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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mcello23.onda.data.AppLanguage
import io.github.mcello23.onda.data.PodcastEpisode
import io.github.mcello23.onda.data.PodcastShow

@Composable
fun PodcastsTab(
    lang: AppLanguage,
    query: String,
    loading: Boolean,
    featured: List<PodcastShow>,
    searchResults: List<PodcastShow>,
    subscriptions: List<PodcastShow>,
    savedEpisodes: List<PodcastEpisode>,
    downloadProgress: Map<String, Float>,
    now: NowPlaying,
    selectedShow: PodcastShow?,
    showEpisodes: List<PodcastEpisode>,
    episodesLoading: Boolean,
    onQuery: (String) -> Unit,
    onOpenShow: (PodcastShow) -> Unit,
    onCloseShow: () -> Unit,
    onToggleSubscribe: (PodcastShow) -> Unit,
    onPlayEpisode: (PodcastEpisode) -> Unit,
    onDownloadEpisode: (PodcastEpisode) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
) {
    val focus = LocalFocusManager.current
    var section by rememberSaveable { mutableIntStateOf(0) } // 0 = Discover, 1 = Subscriptions, 2 = Offline
    val downloaded = savedEpisodes.filter { it.isDownloaded }
    val continueList = savedEpisodes.filter { it.positionMs > 2_000L && !it.completed }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Podcasts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                if (downloaded.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.OfflinePin, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${downloaded.size} offline",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(L(lang, "Buscar podcasts...", "Search podcasts...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Clear, contentDescription = null) }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            )
            if (query.isBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        L(lang, "Descobrir", "Discover"),
                        "${L(lang, "Seguindo", "Following")} (${subscriptions.size})",
                        "${L(lang, "Baixados", "Offline")} (${downloaded.size})",
                    ).forEachIndexed { idx, label ->
                        FilterChip(
                            selected = section == idx,
                            onClick = { section = idx },
                            label = { Text(label, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(999.dp),
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (query.isNotBlank()) {
                if (loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text(L(lang, "Nenhum podcast encontrado.", "No podcasts found."))
                        }
                    }
                } else {
                    items(searchResults, key = { it.id }) { show ->
                        PodcastShowRow(
                            show = show,
                            isSubscribed = subscriptions.any { it.id == show.id },
                            onClick = { focus.clearFocus(); onOpenShow(show) },
                            onToggleSubscribe = { onToggleSubscribe(show) },
                        )
                    }
                }
            } else {
                when (section) {
                    0 -> {
                        if (continueList.isNotEmpty()) {
                            item {
                                Text(
                                    L(lang, "Continuar Ouvindo", "Continue Listening"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                            items(continueList.take(3), key = { "cont-${it.id}" }) { ep ->
                                EpisodeRow(
                                    ep = ep,
                                    lang = lang,
                                    isCurrent = now.podcastEpisode?.id == ep.id,
                                    isPlaying = now.podcastEpisode?.id == ep.id && now.playWhenReady,
                                    progress = downloadProgress[ep.id],
                                    onPlay = { onPlayEpisode(ep) },
                                    onDownload = { onDownloadEpisode(ep) },
                                    onCancelDownload = { onCancelDownload(ep.id) },
                                    onDeleteDownload = { onDeleteDownload(ep.id) },
                                )
                            }
                        }

                        if (subscriptions.isNotEmpty()) {
                            item {
                                Column {
                                    Text(
                                        L(lang, "Seus Podcasts", "Your Podcasts"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(subscriptions, key = { "sub-${it.id}" }) { show ->
                                            Surface(
                                                onClick = { onOpenShow(show) },
                                                shape = RoundedCornerShape(20.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                modifier = Modifier.width(126.dp),
                                            ) {
                                                Column(Modifier.padding(10.dp)) {
                                                    PodcastArt(show.artworkUrl, show.title, Modifier.size(106.dp), corner = 16.dp)
                                                    Spacer(Modifier.height(6.dp))
                                                    Text(show.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                L(lang, "Em Destaque", "Featured Podcasts"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
                            )
                        }
                        if (loading && featured.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else {
                            items(featured, key = { "feat-${it.id}" }) { show ->
                                PodcastShowRow(
                                    show = show,
                                    isSubscribed = subscriptions.any { it.id == show.id },
                                    onClick = { onOpenShow(show) },
                                    onToggleSubscribe = { onToggleSubscribe(show) },
                                )
                            }
                        }
                    }

                    1 -> {
                        if (subscriptions.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        L(lang, "Você ainda não segue nenhum podcast.\nBusque ou explore em Descobrir.", "You aren't following any podcasts yet.\nSearch or browse Discover."),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            items(subscriptions, key = { "mysub-${it.id}" }) { show ->
                                PodcastShowRow(
                                    show = show,
                                    isSubscribed = true,
                                    onClick = { onOpenShow(show) },
                                    onToggleSubscribe = { onToggleSubscribe(show) },
                                )
                            }
                        }
                    }

                    2 -> {
                        if (downloaded.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        L(lang, "Nenhum episódio baixado para ouvir offline.", "No downloaded offline episodes yet."),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            items(downloaded, key = { "dl-${it.id}" }) { ep ->
                                EpisodeRow(
                                    ep = ep,
                                    lang = lang,
                                    isCurrent = now.podcastEpisode?.id == ep.id,
                                    isPlaying = now.podcastEpisode?.id == ep.id && now.playWhenReady,
                                    progress = downloadProgress[ep.id],
                                    onPlay = { onPlayEpisode(ep) },
                                    onDownload = { onDownloadEpisode(ep) },
                                    onCancelDownload = { onCancelDownload(ep.id) },
                                    onDeleteDownload = { onDeleteDownload(ep.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedShow != null) {
        PodcastShowDetailSheet(
            show = selectedShow,
            lang = lang,
            isSubscribed = subscriptions.any { it.id == selectedShow.id },
            episodes = showEpisodes,
            loading = episodesLoading,
            downloadProgress = downloadProgress,
            savedEpisodes = savedEpisodes,
            now = now,
            onToggleSubscribe = { onToggleSubscribe(selectedShow) },
            onPlayEpisode = onPlayEpisode,
            onDownloadEpisode = onDownloadEpisode,
            onCancelDownload = onCancelDownload,
            onDeleteDownload = onDeleteDownload,
            onDismiss = onCloseShow,
        )
    }
}

@Composable
private fun PodcastShowRow(
    show: PodcastShow,
    isSubscribed: Boolean,
    onClick: () -> Unit,
    onToggleSubscribe: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PodcastArt(show.artworkUrl, show.title, Modifier.size(60.dp), corner = 16.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(show.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (show.author.isNotBlank()) {
                    Text(show.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (show.genre.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    QualityPill(show.genre)
                }
            }
            IconButton(onClick = onToggleSubscribe) {
                Icon(
                    if (isSubscribed) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    ep: PodcastEpisode,
    lang: AppLanguage,
    isCurrent: Boolean,
    isPlaying: Boolean,
    progress: Float?,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
) {
    Surface(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PodcastArt(ep.podcastArtwork, ep.podcastTitle, Modifier.size(50.dp), corner = 14.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(ep.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val meta = listOf(
                        ep.pubDate,
                        ep.durationMs.takeIf { it > 0L }?.let { formatDurationMs(it) } ?: "",
                        if (ep.isDownloaded) "Offline" else "",
                    ).filter { it.isNotBlank() }.joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                // Download / Offline button
                when {
                    progress != null -> {
                        IconButton(onClick = onCancelDownload) {
                            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        }
                    }
                    ep.isDownloaded -> {
                        IconButton(onClick = onDeleteDownload) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = L(lang, "Remover download", "Remove download"), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    else -> {
                        IconButton(onClick = onDownload) {
                            Icon(Icons.Default.Download, contentDescription = L(lang, "Baixar offline", "Download offline"))
                        }
                    }
                }
                Surface(
                    onClick = onPlay,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            if (ep.positionMs > 2_000L && ep.durationMs > 0L && !ep.completed) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { ep.progressFraction },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastShowDetailSheet(
    show: PodcastShow,
    lang: AppLanguage,
    isSubscribed: Boolean,
    episodes: List<PodcastEpisode>,
    loading: Boolean,
    downloadProgress: Map<String, Float>,
    savedEpisodes: List<PodcastEpisode>,
    now: NowPlaying,
    onToggleSubscribe: () -> Unit,
    onPlayEpisode: (PodcastEpisode) -> Unit,
    onDownloadEpisode: (PodcastEpisode) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val savedById = savedEpisodes.associateBy { it.id }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(600.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PodcastArt(show.artworkUrl, show.title, Modifier.size(76.dp), corner = 20.dp)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(show.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (show.author.isNotBlank()) {
                        Text(show.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = onToggleSubscribe) {
                        Icon(if (isSubscribed) Icons.Default.Check else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isSubscribed) L(lang, "Seguindo", "Following") else L(lang, "Seguir podcast", "Follow podcast"))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (episodes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(L(lang, "Não foi possível carregar os episódios deste feed.", "Could not load episodes for this feed."))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(episodes, key = { it.id }) { rawEp ->
                        val ep = savedById[rawEp.id] ?: rawEp
                        EpisodeRow(
                            ep = ep,
                            lang = lang,
                            isCurrent = now.podcastEpisode?.id == ep.id,
                            isPlaying = now.podcastEpisode?.id == ep.id && now.playWhenReady,
                            progress = downloadProgress[ep.id],
                            onPlay = { onPlayEpisode(ep) },
                            onDownload = { onDownloadEpisode(ep) },
                            onCancelDownload = { onCancelDownload(ep.id) },
                            onDeleteDownload = { onDeleteDownload(ep.id) },
                        )
                    }
                }
            }
        }
    }
}
