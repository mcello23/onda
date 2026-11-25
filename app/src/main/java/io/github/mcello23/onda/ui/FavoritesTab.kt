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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mcello23.onda.data.AppLanguage
import io.github.mcello23.onda.data.Library
import io.github.mcello23.onda.data.Station

@Composable
fun FavoritesTab(
    lang: AppLanguage,
    favorites: List<Station>,
    now: NowPlaying,
    onPlayStation: (Station, String) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    onMoveFavorite: (Int, Int) -> Unit,
    onOpenAddCustom: () -> Unit,
    onOpenExplore: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }

    if (favorites.size <= 1 && editing) {
        editing = false
    }

    val query = search.trim()
    val filtered = remember(favorites, query) {
        if (query.isEmpty()) favorites
        else favorites.filter { s ->
            s.displayName.contains(query, ignoreCase = true) ||
                s.tags.contains(query, ignoreCase = true) ||
                s.country.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("favorites_tab"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        L(lang, "Favoritos", "Favorites"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    if (favorites.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                "${favorites.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                if (favorites.size > 1 && query.isEmpty()) {
                    TextButton(
                        onClick = { editing = !editing },
                        modifier = Modifier.testTag("favorites_reorder_toggle"),
                    ) {
                        Icon(
                            if (editing) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (editing) L(lang, "Pronto", "Done") else L(lang, "Ordenar", "Reorder"))
                    }
                }
                IconButton(
                    onClick = onOpenAddCustom,
                    modifier = Modifier.testTag("favorites_add_custom"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = L(lang, "Adicionar rádio", "Add radio"))
                }
            }
        }

        // Filter bar when user has favorites
        if (favorites.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                        if (it.isNotBlank()) editing = false
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                    placeholder = { Text(L(lang, "Filtrar seus favoritos...", "Filter your favorites...")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(
                                onClick = { search = "" },
                                modifier = Modifier.testTag("favorites_search_clear"),
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = L(lang, "Limpar", "Clear"))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("favorites_search_input"),
                )
            }
        }

        if (favorites.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                        Text(
                            L(lang, "Nenhum favorito salvo", "No saved favorites yet"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            L(
                                lang,
                                "As estações que você favoritar aparecem aqui, no atalho da tela inicial, nos widgets e no Android Auto.",
                                "Stations you favorite show up here, in the Home shortcut, on widgets, and in Android Auto.",
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilledTonalButton(
                            onClick = onOpenExplore,
                            modifier = Modifier.testTag("favorites_empty_explore"),
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(L(lang, "Explorar estações", "Explore stations"))
                        }
                    }
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        L(lang, "Nenhum favorito encontrado para \"$query\".", "No favorites match \"$query\"."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (editing) {
            // Single-column reorder list with spacious up/down and remove controls
            itemsIndexed(favorites, key = { _, s -> "edit-${s.uuid}" }) { idx, station ->
                val isCurrent = !now.isPodcast && now.station?.uuid == station.uuid
                Surface(
                    onClick = { onPlayStation(station, Library.FAVORITES) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StationArt(station, Modifier.size(46.dp), corner = 14.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                station.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${station.flag} ${station.qualityBadge.ifBlank { station.country }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = { if (idx > 0) onMoveFavorite(idx, idx - 1) },
                            enabled = idx > 0,
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = L(lang, "Subir", "Move up"))
                        }
                        IconButton(
                            onClick = { if (idx < favorites.lastIndex) onMoveFavorite(idx, idx + 1) },
                            enabled = idx < favorites.lastIndex,
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = L(lang, "Descer", "Move down"))
                        }
                        IconButton(onClick = { onToggleFavorite(station) }) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Unfavorite ${station.displayName}",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        } else {
            // Expressive 2-column grid for browsing all favorites cleanly
            val rows = filtered.chunked(2)
            itemsIndexed(rows, key = { rowIdx, pair -> "fav-row-$rowIdx-${pair.first().uuid}" }) { _, pair ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    pair.forEach { station ->
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
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
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
