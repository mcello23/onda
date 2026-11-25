package io.github.mcello23.onda.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mcello23.onda.data.AppLanguage
import io.github.mcello23.onda.data.GenreInfo
import io.github.mcello23.onda.data.Library
import io.github.mcello23.onda.data.Station
import io.github.mcello23.onda.data.countryFlag

@Composable
fun ExploreTab(
    lang: AppLanguage,
    query: String,
    countryCode: String,
    countryLabel: String,
    selectedGenreId: String?,
    genres: List<GenreInfo>,
    results: Results,
    now: NowPlaying,
    favoriteIds: Set<String>,
    onQuery: (String) -> Unit,
    onOpenCountryPicker: () -> Unit,
    onSelectGenre: (String?) -> Unit,
    onRetry: () -> Unit,
    onPlay: (Station, String) -> Unit,
    onFavorite: (Station) -> Unit,
) {
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize()) {
        // Header + Search Bar
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                L(lang, "Explorar", "Explore"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().testTag("explore_search_input"),
                placeholder = { Text(L(lang, "Buscar rádio, gênero ou país...", "Search station, genre or country...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQuery("") }, modifier = Modifier.testTag("explore_search_clear")) {
                            Icon(Icons.Default.Clear, contentDescription = L(lang, "Limpar", "Clear"))
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            )
        }

        // Active Genre Banner OR Active Country Banner
        if (query.isBlank()) {
            if (selectedGenreId != null) {
                val activeGenre = Library.genreById(selectedGenreId)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { onSelectGenre(null) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L(lang, "Voltar", "Back"))
                        }
                        Text(
                            activeGenre?.label(lang) ?: selectedGenreId,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            } else {
                Surface(
                    onClick = onOpenCountryPicker,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(countryFlag(countryCode), style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${L(lang, "País ativo", "Active country")}: $countryLabel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                L(lang, "Filtrar entre mais de 190 países", "Filter across 190+ countries"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    L(lang, "Trocar", "Change"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Scrollable Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        ) {
            // Genre Visual Grid (when not searching & no specific genre open)
            if (query.isBlank() && selectedGenreId == null) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            L(lang, "Rádios por Gênero Mundial", "World Radio by Genre"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            genres.chunked(2).forEach { pair ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    pair.forEach { genre ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(68.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(genre.colorStart), Color(genre.colorEnd))
                                                    )
                                                )
                                                .clickable { onSelectGenre(genre.id) }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            contentAlignment = Alignment.BottomStart,
                                        ) {
                                            Text(
                                                text = genre.label(lang),
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Results Section
            when (results) {
                Results.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is Results.Error -> {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(results.message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = onRetry) { Text(L(lang, "Tentar novamente", "Retry")) }
                        }
                    }
                }
                is Results.Loaded -> {
                    if (results.stations.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text(L(lang, "Nenhuma rádio encontrada.", "No stations found."), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    } else {
                        item {
                            Text(
                                results.header,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                            )
                        }
                        items(results.stations, key = { "${results.category}-${it.uuid}" }) { station ->
                            val isCurrent = !now.isPodcast && now.station?.uuid == station.uuid
                            StationRow(
                                station = station,
                                isFavorite = station.uuid in favoriteIds,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && now.playWhenReady,
                                onClick = { focus.clearFocus(); onPlay(station, results.category) },
                                onFavorite = { onFavorite(station) },
                            )
                        }
                    }
                }
            }
        }
    }
}
