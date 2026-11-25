package io.github.mcello23.onda.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import io.github.mcello23.onda.data.AppLanguage
import io.github.mcello23.onda.data.CountryInfo
import io.github.mcello23.onda.data.Monogram
import io.github.mcello23.onda.data.SongHistoryEntry
import io.github.mcello23.onda.data.Station
import java.net.URLEncoder

@Composable
fun StationArt(station: Station, modifier: Modifier, corner: Dp = 16.dp) {
    val urls = remember(station.favicon, station.fallbackLogoUrl) {
        listOf(station.favicon.trim(), station.fallbackLogoUrl.trim())
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()
    }
    ResilientArtwork(urls = urls, name = station.displayName, modifier = modifier, corner = corner)
}

@Composable
fun PodcastArt(artworkUrl: String, title: String, modifier: Modifier, corner: Dp = 16.dp) {
    val urls = remember(artworkUrl) {
        listOf(artworkUrl.trim()).filter { it.startsWith("http://") || it.startsWith("https://") }
    }
    ResilientArtwork(urls = urls, name = title, modifier = modifier, corner = corner)
}

@Composable
private fun ResilientArtwork(urls: List<String>, name: String, modifier: Modifier, corner: Dp) {
    var attempt by remember(urls) { mutableIntStateOf(0) }
    var showLogo by remember(urls) { mutableStateOf(false) }
    val currentUrl = urls.getOrNull(attempt)
    val label = remember(name) { Monogram.label(name) }
    val (cStart, cEnd) = remember(name) { Monogram.gradient(name) }

    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(
                if (showLogo) Brush.linearGradient(listOf(Color.White, Color(0xFFF8FAFC)))
                else Brush.linearGradient(listOf(Color(cStart), Color(cEnd)))
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!showLogo) {
            // Subtle ambient highlights so fallback art looks crisp and intentional
            Box(
                Modifier
                    .size(maxWidth * 0.65f)
                    .align(Alignment.TopEnd)
                    .offset(x = maxWidth * 0.22f, y = -maxHeight * 0.22f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            )
            Box(
                Modifier
                    .size(maxWidth * 0.55f)
                    .align(Alignment.BottomStart)
                    .offset(x = -maxWidth * 0.2f, y = maxHeight * 0.22f)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.18f))
            )
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = with(LocalDensity.current) { (maxWidth * if (label.length <= 2) 0.36f else 0.25f).toSp() },
                maxLines = 1,
                softWrap = false,
            )
        }
        if (currentUrl != null) {
            AsyncImage(
                model = currentUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(maxWidth * 0.06f)
                    .alpha(if (showLogo) 1f else 0f),
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> {
                            val okSize = state.painter.intrinsicSize.let {
                                it.isUnspecified || it.width >= 48f || it.height >= 48f
                            }
                            if (okSize) {
                                showLogo = true
                            } else if (attempt + 1 < urls.size) {
                                attempt++
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            showLogo = false
                            if (attempt + 1 < urls.size) attempt++
                        }
                        else -> Unit
                    }
                },
            )
        }
    }
}

@Composable
fun AnimatedEqualizerBars(
    color: Color = MaterialTheme.colorScheme.primary,
    playing: Boolean = true,
    modifier: Modifier = Modifier.size(16.dp),
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val b1 by transition.animateFloat(
        initialValue = 0.25f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val b2 by transition.animateFloat(
        initialValue = 0.9f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val b3 by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(490, easing = LinearEasing), RepeatMode.Reverse),
        label = "b3"
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(if (playing) b1 else 0.3f, if (playing) b2 else 0.5f, if (playing) b3 else 0.3f).forEach { frac ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(frac)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun QualityPill(text: String, modifier: Modifier = Modifier) {
    if (text.isBlank()) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun StationRow(
    station: Station,
    isFavorite: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationArt(station, Modifier.size(56.dp), corner = 14.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isCurrent && isPlaying) {
                        AnimatedEqualizerBars(color = MaterialTheme.colorScheme.primary, playing = true, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = station.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${station.flag} ${station.subtitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerSheet(
    countries: List<CountryInfo>,
    selectedCode: String,
    lang: AppLanguage,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(countries, search) {
        val q = search.trim().lowercase()
        val list = if (countries.isEmpty()) {
            listOf(
                CountryInfo("Spain", "ES", 1800),
                CountryInfo("Brazil", "BR", 2400),
                CountryInfo("United States", "US", 6500),
                CountryInfo("United Kingdom", "GB", 1500),
                CountryInfo("Portugal", "PT", 450),
                CountryInfo("Argentina", "AR", 900),
                CountryInfo("France", "FR", 1900),
                CountryInfo("Germany", "DE", 3200),
                CountryInfo("Italy", "IT", 1600),
            )
        } else countries
        if (q.isEmpty()) list else list.filter { it.name.lowercase().contains(q) || it.code.lowercase().contains(q) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .height(520.dp)
        ) {
            Text(
                L(lang, "Escolher País", "Choose Country"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(L(lang, "Buscar país...", "Search country...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) { Icon(Icons.Default.Clear, contentDescription = null) }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filtered, key = { it.code }) { c ->
                    val active = c.code.equals(selectedCode, ignoreCase = true)
                    Surface(
                        onClick = { onSelect(c.code); onDismiss() },
                        shape = RoundedCornerShape(16.dp),
                        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(c.flag, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                c.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${c.stationcount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongHistorySheet(
    history: List<SongHistoryEntry>,
    lang: AppLanguage,
    onOpenSpotify: (String) -> Unit,
    onOpenYouTubeMusic: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .height(480.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    L(lang, "Músicas Descobertas", "Discovered Songs"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(L(lang, "Limpar", "Clear"))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (history.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        L(lang, "As músicas exibidas pelas rádios ao vivo aparecem aqui para você abrir no Spotify ou YouTube Music.", "Songs broadcast by live stations will appear here so you can open them in Spotify or YouTube Music."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(history, key = { it.id }) { entry ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        entry.songTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    entry.stationName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 26.dp, top = 2.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 26.dp)) {
                                    Surface(
                                        onClick = { onOpenSpotify(entry.songTitle) },
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("Spotify", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                    Surface(
                                        onClick = { onOpenYouTubeMusic(entry.songTitle) },
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("YouTube Music", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCustomStationDialog(
    lang: AppLanguage,
    onAdd: (name: String, url: String, country: String, genre: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    val valid = name.isNotBlank() && (url.trim().startsWith("http://") || url.trim().startsWith("https://"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L(lang, "Adicionar Rádio Manual", "Add Custom Radio")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(L(lang, "Nome da estação", "Station name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(L(lang, "URL do stream (MP3/AAC/M3U8)", "Stream URL (MP3/AAC/M3U8)")) },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text(L(lang, "País (opcional)", "Country (optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(L(lang, "Gênero (opcional)", "Genre (optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onAdd(name, url, country, genre); onDismiss() }) {
                Text(L(lang, "Adicionar", "Add"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(L(lang, "Cancelar", "Cancel")) }
        },
    )
}

fun openExternalMusicSearch(context: Context, songQuery: String, spotify: Boolean) {
    val encoded = URLEncoder.encode(songQuery.trim(), "UTF-8")
    val url = if (spotify) {
        "https://open.spotify.com/search/$encoded"
    } else {
        "https://music.youtube.com/search?q=$encoded"
    }
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
