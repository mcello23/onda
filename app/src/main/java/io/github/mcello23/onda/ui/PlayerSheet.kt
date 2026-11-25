package io.github.mcello23.onda.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mcello23.onda.data.AppLanguage
import io.github.mcello23.onda.data.AppSettings
import io.github.mcello23.onda.data.EqPreset
import io.github.mcello23.onda.playback.AudioFxInfo
import io.github.mcello23.onda.playback.SleepTimerStatus

fun statusText(now: NowPlaying, lang: AppLanguage): String = when {
    now.failed && now.playWhenReady -> L(lang, "Reconectando…", "Reconnecting…")
    now.failed -> L(lang, "Sem conexão. Toque para tentar", "Can't connect. Tap to retry")
    now.buffering && now.playWhenReady -> L(lang, "Conectando…", "Connecting…")
    now.isPodcast -> now.podcastEpisode?.podcastTitle.orEmpty()
    now.songTitle != null -> now.songTitle
    now.playWhenReady -> L(lang, "Ao vivo", "Live")
    else -> L(lang, "Parado", "Stopped")
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NowPlayingSheet(
    now: NowPlaying,
    settings: AppSettings,
    sleepTimer: SleepTimerStatus,
    isFavorite: Boolean,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRewind15: () -> Unit,
    onForward30: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onFavorite: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val context = LocalContext.current
    val lang = settings.language
    val station = now.station
    val episode = now.podcastEpisode

    val bgBrush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.surface,
        )
    )

    Column(
        Modifier
            .fillMaxWidth()
            .semantics { testTagsAsResourceId = true }
            .testTag("now_playing_sheet")
            .background(bgBrush)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top Technical Status Pill
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            val topLabel = if (now.isPodcast && episode != null) {
                if (episode.isDownloaded) "PODCAST · OFFLINE" else "PODCAST"
            } else {
                listOf(
                    L(lang, "AO VIVO", "LIVE"),
                    station?.qualityBadge.orEmpty(),
                    settings.bufferProfile.label(lang),
                ).filter { it.isNotBlank() }.joinToString(" · ")
            }
            Text(
                topLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        // Expressive Rounded Artwork
        val maxArt = minOf(280f, LocalConfiguration.current.screenHeightDp * 0.34f).dp
        val artMod = Modifier
            .widthIn(max = maxArt)
            .fillMaxWidth(0.76f)
            .aspectRatio(1f)
            .shadow(14.dp, RoundedCornerShape(28.dp))

        if (now.isPodcast && episode != null) {
            PodcastArt(episode.podcastArtwork, episode.podcastTitle, artMod, corner = 28.dp)
        } else if (station != null) {
            StationArt(station, artMod, corner = 28.dp)
        }

        Spacer(Modifier.height(22.dp))

        // Title & Subtitle / ICY Song
        Text(
            text = if (now.isPodcast) episode?.title.orEmpty() else station?.displayName.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = statusText(now, lang),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!now.isPodcast && station != null) {
            Text(
                text = "${station.flag} ${station.subtitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Spotify & YouTube Music 1-tap buttons when ICY song detected!
        if (!now.isPodcast && !now.songTitle.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { openExternalMusicSearch(context, now.songTitle, spotify = true) },
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Spotify", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Surface(
                    onClick = { openExternalMusicSearch(context, now.songTitle, spotify = false) },
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("YouTube Music", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }

        // Podcast Seek Bar
        if (now.isPodcast) {
            Spacer(Modifier.height(14.dp))
            val dur = now.durationMs.coerceAtLeast(1L)
            var draggingValue by remember { mutableStateOf<Float?>(null) }
            val progress = draggingValue ?: (now.positionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            Slider(
                value = progress,
                onValueChange = { draggingValue = it },
                onValueChangeFinished = {
                    draggingValue?.let { onSeekTo((it * dur).toLong()) }
                    draggingValue = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDurationMs((progress * dur).toLong()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDurationMs(now.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(22.dp))

        // Main Transport Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            FilledTonalIconButton(
                onClick = if (now.isPodcast) onRewind15 else onPrevious,
                enabled = now.isPodcast || now.hasPrevious,
                modifier = Modifier.size(58.dp),
            ) {
                Icon(
                    if (now.isPodcast) Icons.Default.Replay10 else Icons.Default.SkipPrevious,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
            }

            Surface(
                onClick = onToggle,
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(84.dp).testTag("player_toggle_btn"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when {
                            !now.playWhenReady -> Icons.Default.PlayArrow
                            now.isPodcast -> Icons.Default.Pause
                            else -> Icons.Default.Stop
                        },
                        contentDescription = when {
                            !now.playWhenReady -> L(lang, "Tocar", "Play")
                            now.isPodcast -> L(lang, "Pausar", "Pause")
                            else -> L(lang, "Parar", "Stop")
                        },
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }

            FilledTonalIconButton(
                onClick = if (now.isPodcast) onForward30 else onNext,
                enabled = now.isPodcast || now.hasNext,
                modifier = Modifier.size(58.dp),
            ) {
                Icon(
                    if (now.isPodcast) Icons.Default.Forward30 else Icons.Default.SkipNext,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Expressive Quick Action Bar (Timer, EQ, Favorite/Speed, History/Share)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Sleep Timer
            val timerLabel = when {
                sleepTimer.endOfEpisode -> L(lang, "Fim ep.", "End ep.")
                sleepTimer.active -> formatDurationMs(sleepTimer.remainingMs)
                else -> "Timer"
            }
            PlayerActionTile(
                icon = Icons.Default.Bedtime,
                label = timerLabel,
                active = sleepTimer.active,
                onClick = onOpenSleepTimer,
                modifier = Modifier.weight(1f),
            )

            // Equalizer
            PlayerActionTile(
                icon = Icons.Default.Equalizer,
                label = if (settings.eqEnabled) settings.eqPreset.label(lang) else "EQ",
                active = settings.eqEnabled || settings.normalizeVolume,
                onClick = onOpenEqualizer,
                modifier = Modifier.weight(1f),
            )

            if (now.isPodcast) {
                PlayerActionTile(
                    icon = Icons.Default.Speed,
                    label = "${settings.podcastSpeed}x",
                    active = settings.podcastSpeed != 1.0f,
                    onClick = onCycleSpeed,
                    modifier = Modifier.weight(1f),
                )
            } else {
                PlayerActionTile(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = if (isFavorite) L(lang, "Favorito", "Saved") else L(lang, "Favoritar", "Favorite"),
                    active = isFavorite,
                    onClick = onFavorite,
                    modifier = Modifier.weight(1f),
                )
            }

            PlayerActionTile(
                icon = if (now.isPodcast) Icons.Default.Share else Icons.Default.History,
                label = if (now.isPodcast) L(lang, "Enviar", "Share") else L(lang, "Histórico", "History"),
                active = false,
                onClick = {
                    if (now.isPodcast && episode != null) {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${episode.podcastTitle} — ${episode.title}\n${episode.audioUrl}")
                        }
                        context.startActivity(Intent.createChooser(share, null))
                    } else {
                        onOpenHistory()
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlayerActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SleepTimerDialog(
    lang: AppLanguage,
    isPodcast: Boolean,
    current: SleepTimerStatus,
    onSelect: (minutes: Int, endOfEpisode: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L(lang, "Temporizador para dormir", "Sleep Timer")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (current.active) {
                    Text(
                        if (current.endOfEpisode) L(lang, "Ativo: parar ao fim do episódio", "Active: stop at end of episode")
                        else L(lang, "Tempo restante: ${formatDurationMs(current.remainingMs)}", "Remaining: ${formatDurationMs(current.remainingMs)}"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                listOf(15, 30, 45, 60).forEach { mins ->
                    Surface(
                        onClick = { onSelect(mins, false); onDismiss() },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            L(lang, "$mins minutos", "$mins minutes"),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
                if (isPodcast) {
                    Surface(
                        onClick = { onSelect(0, true); onDismiss() },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            L(lang, "Fim do episódio atual", "End of current episode"),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (current.active) {
                TextButton(onClick = { onSelect(0, false); onDismiss() }) {
                    Text(L(lang, "Desativar Timer", "Turn off timer"))
                }
            } else {
                TextButton(onClick = onDismiss) { Text(L(lang, "Fechar", "Close")) }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EqualizerBottomSheet(
    lang: AppLanguage,
    settings: AppSettings,
    fxInfo: AudioFxInfo,
    onToggleNormalize: (Boolean) -> Unit,
    onToggleEq: (Boolean) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onBandChange: (Int, Int) -> Unit,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                L(lang, "Áudio & Equalizador", "Audio & Equalizer"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            // Volume Normalization Switch
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(L(lang, "Normalizar Volume", "Volume Normalization"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(L(lang, "Equilibra o volume entre rádios diferentes", "Balances loudness across different stations"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.normalizeVolume, onCheckedChange = onToggleNormalize)
                }
            }

            // Equalizer Switch
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(L(lang, "Equalizador", "Equalizer"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(L(lang, "Ajuste de frequências em tempo real", "Real-time frequency shaping"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.eqEnabled, onCheckedChange = onToggleEq)
                }
            }

            // Presets
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EqPreset.entries.filter { it != EqPreset.CUSTOM }.forEach { preset ->
                    FilterChip(
                        selected = settings.eqEnabled && settings.eqPreset == preset,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset.label(lang)) },
                        shape = RoundedCornerShape(999.dp),
                    )
                }
            }

            // 5-band sliders
            val minMb = fxInfo.minLevelMb.coerceAtMost(-600).toFloat()
            val maxMb = fxInfo.maxLevelMb.coerceAtLeast(600).toFloat()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                fxInfo.bandFrequenciesHz.take(5).forEachIndexed { idx, freqHz ->
                    val currentMb = settings.eqBandLevels.getOrElse(idx) { 0 }.toFloat().coerceIn(minMb, maxMb)
                    val freqLabel = if (freqHz >= 1000) "${freqHz / 1000} kHz" else "$freqHz Hz"
                    val db = currentMb / 100f
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(freqLabel, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(56.dp))
                        Slider(
                            value = currentMb,
                            onValueChange = { onBandChange(idx, it.toInt()) },
                            valueRange = minMb..maxMb,
                            enabled = settings.eqEnabled,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "%+.1fdB".format(db),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(46.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}
