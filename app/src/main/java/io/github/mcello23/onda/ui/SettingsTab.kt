package io.github.mcello23.onda.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mcello23.onda.BuildConfig
import io.github.mcello23.onda.data.AppLanguage
import io.github.mcello23.onda.data.AppSettings
import io.github.mcello23.onda.data.BufferProfile
import io.github.mcello23.onda.data.SeedColor
import io.github.mcello23.onda.data.StreamQuality
import io.github.mcello23.onda.data.ThemeMode
import io.github.mcello23.onda.data.countryFlag

private const val PRIVACY_POLICY_URL = "https://github.com/mcello23/onda/blob/main/docs/privacy-policy.md"

@Composable
fun SettingsTab(
    settings: AppSettings,
    countryLabel: String,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onSeedColor: (SeedColor) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onStreamQuality: (StreamQuality) -> Unit,
    onBufferProfile: (BufferProfile) -> Unit,
    onNormalizeVolume: (Boolean) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenCountryPicker: () -> Unit,
    onOpenAddCustom: () -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Boolean,
) {
    val context = LocalContext.current
    val lang = settings.language
    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(onExportJson().toByteArray()) }
            }.isSuccess
            feedbackMsg = if (ok) L(lang, "Backup exportado com sucesso!", "Backup exported successfully!")
            else L(lang, "Erro ao exportar backup.", "Failed to export backup.")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val raw = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            val ok = raw != null && onImportJson(raw)
            feedbackMsg = if (ok) L(lang, "Backup restaurado com sucesso!", "Backup restored successfully!")
            else L(lang, "Arquivo de backup inválido.", "Invalid backup file.")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                L(lang, "Opções", "Settings"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        if (feedbackMsg != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        feedbackMsg!!,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }

        // APPEARANCE (Material You + Optional Dark Mode)
        item {
            SettingsCard(title = L(lang, "Aparência · Material You", "Appearance · Material You")) {
                Text(L(lang, "Tema", "Theme"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onThemeMode(mode) },
                            label = { Text(mode.label(lang)) },
                            shape = RoundedCornerShape(999.dp),
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(L(lang, "Cores Dinâmicas (Wallpaper)", "Dynamic Colors (Wallpaper)"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(L(lang, "Adapta o app às cores do seu Android", "Matches your Android wallpaper colors"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.useDynamicColor, onCheckedChange = onDynamicColor)
                    }
                }

                if (!settings.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(L(lang, "Paleta de Cor", "Color Palette"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SeedColor.entries.forEach { seed ->
                            val selected = settings.seedColor == seed
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSeedColor(seed) }
                                    .padding(4.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(seed.swatchColor()),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(seed.label(lang), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(L(lang, "Idioma do Aplicativo", "App Language"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.language == AppLanguage.PT,
                        onClick = { onLanguage(AppLanguage.PT) },
                        label = { Text("🇧🇷 Português") },
                        shape = RoundedCornerShape(999.dp),
                    )
                    FilterChip(
                        selected = settings.language == AppLanguage.EN,
                        onClick = { onLanguage(AppLanguage.EN) },
                        label = { Text("🇺🇸 English") },
                        shape = RoundedCornerShape(999.dp),
                    )
                }
            }
        }

        // STREAMING & AUDIO
        item {
            SettingsCard(title = L(lang, "Streaming & Qualidade de Áudio", "Streaming & Audio Quality")) {
                Text(L(lang, "Qualidade do Stream", "Stream Quality"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(settings.streamQuality.description(lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StreamQuality.entries.forEach { q ->
                        FilterChip(
                            selected = settings.streamQuality == q,
                            onClick = { onStreamQuality(q) },
                            label = { Text(q.label(lang)) },
                            shape = RoundedCornerShape(999.dp),
                        )
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(L(lang, "Buffer Anti-travamento", "Anti-Stutter Buffer"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(settings.bufferProfile.description(lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BufferProfile.entries.forEach { b ->
                        FilterChip(
                            selected = settings.bufferProfile == b,
                            onClick = { onBufferProfile(b) },
                            label = { Text(b.label(lang)) },
                            shape = RoundedCornerShape(999.dp),
                        )
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(L(lang, "Normalizar Volume", "Normalize Volume"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(L(lang, "Iguala o volume entre rádios altas e baixas", "Evens out volume across stations"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.normalizeVolume, onCheckedChange = onNormalizeVolume)
                }

                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onOpenEqualizer).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Equalizer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(L(lang, "Equalizador", "Equalizer"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (settings.eqEnabled) "${L(lang, "Ativo", "On")} · ${settings.eqPreset.label(lang)}"
                            else L(lang, "Desativado · Toque para configurar", "Off · Tap to configure"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // REGION & CUSTOM STATIONS
        item {
            SettingsCard(title = L(lang, "Região & Catálogo", "Region & Stations")) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onOpenCountryPicker).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(L(lang, "País Padrão", "Default Country"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${countryFlag(settings.defaultCountry)} $countryLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onOpenAddCustom).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(L(lang, "Adicionar Rádio por URL", "Add Radio via Stream URL"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(L(lang, "Adicione qualquer stream MP3, AAC ou HLS (.m3u8)", "Add any MP3, AAC or HLS (.m3u8) stream"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // LOCAL BACKUP
        item {
            SettingsCard(title = L(lang, "Backup Local", "Local Backup")) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { exportLauncher.launch("onda-backup.json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(6.dp))
                            Text(L(lang, "Exportar", "Export"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Surface(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(6.dp))
                            Text(L(lang, "Importar", "Import"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }

        // PRIVACY & LEGAL
        item {
            SettingsCard(title = L(lang, "Privacidade & Legal", "Privacy & Legal")) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                            }
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(L(lang, "Política de Privacidade", "Privacy Policy"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            L(lang, "Sem anúncios, sem contas e sem rastreamento. Seus dados ficam apenas no aparelho.", "No ads, no accounts, and no tracking. Your data stays on your device."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = L(lang, "Abrir política de privacidade", "Open privacy policy"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        item {
            Text(
                "Onda v${BuildConfig.VERSION_NAME} · Material You · Android Auto",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
