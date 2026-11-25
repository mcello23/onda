package io.github.mcello23.onda.ui

import io.github.mcello23.onda.data.AppLanguage
import io.github.mcello23.onda.data.BufferProfile
import io.github.mcello23.onda.data.EqPreset
import io.github.mcello23.onda.data.SeedColor
import io.github.mcello23.onda.data.StreamQuality
import io.github.mcello23.onda.data.ThemeMode
import java.util.Calendar

fun L(lang: AppLanguage, pt: String, en: String): String = if (lang == AppLanguage.EN) en else pt

fun greetingText(lang: AppLanguage): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> L(lang, "Bom dia", "Good morning")
        hour < 18 -> L(lang, "Boa tarde", "Good afternoon")
        else -> L(lang, "Boa noite", "Good evening")
    }
}

fun ThemeMode.label(lang: AppLanguage): String = when (this) {
    ThemeMode.SYSTEM -> L(lang, "Sistema", "System")
    ThemeMode.LIGHT -> L(lang, "Claro", "Light")
    ThemeMode.DARK -> L(lang, "Escuro", "Dark")
}

fun SeedColor.label(lang: AppLanguage): String = when (this) {
    SeedColor.OCEAN -> L(lang, "Oceano", "Ocean")
    SeedColor.TERRACOTTA -> L(lang, "Terracota", "Terracotta")
    SeedColor.EMERALD -> L(lang, "Esmeralda", "Emerald")
    SeedColor.VIOLET -> L(lang, "Violeta", "Violet")
}

fun StreamQuality.label(lang: AppLanguage): String = when (this) {
    StreamQuality.AUTO -> L(lang, "Auto HD", "Auto HD")
    StreamQuality.HIGH -> L(lang, "Alta (320k)", "High (320k)")
    StreamQuality.DATA_SAVER -> L(lang, "Economia 4G", "Data Saver")
}

fun StreamQuality.description(lang: AppLanguage): String = when (this) {
    StreamQuality.AUTO -> L(lang, "Prioriza maior bitrate no Wi-Fi e AAC+ econômico no 4G/5G", "High bitrate on Wi-Fi, efficient AAC+ on cellular")
    StreamQuality.HIGH -> L(lang, "Sempre escolhe o stream de maior qualidade disponível", "Always chooses the highest quality stream available")
    StreamQuality.DATA_SAVER -> L(lang, "Menor consumo de dados móveis priorizando AAC+", "Lower mobile data usage prioritizing AAC+")
}

fun BufferProfile.label(lang: AppLanguage): String = when (this) {
    BufferProfile.FAST -> L(lang, "Rápido", "Fast")
    BufferProfile.BALANCED -> L(lang, "Equilibrado", "Balanced")
    BufferProfile.STABLE -> L(lang, "Estável", "Stable")
}

fun BufferProfile.description(lang: AppLanguage): String = when (this) {
    BufferProfile.FAST -> L(lang, "Abre mais rápido (~1s), ideal para Wi-Fi estável", "Starts faster (~1s), ideal for strong Wi-Fi")
    BufferProfile.BALANCED -> L(lang, "Bom equilíbrio entre início rápido e estabilidade", "Good balance between fast start and stability")
    BufferProfile.STABLE -> L(lang, "Buffer maior anti-travamento para metrô e estrada", "Larger anti-stutter buffer for transit & roads")
}

fun EqPreset.label(lang: AppLanguage): String = when (this) {
    EqPreset.FLAT -> "Flat"
    EqPreset.ROCK -> "Rock"
    EqPreset.POP -> "Pop"
    EqPreset.JAZZ -> "Jazz"
    EqPreset.VOCAL -> L(lang, "Voz / Podcast", "Voice / Podcast")
    EqPreset.BASS_BOOST -> L(lang, "Graves +", "Bass Boost")
    EqPreset.CUSTOM -> L(lang, "Personalizado", "Custom")
}

fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000L
    val hours = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, mins, secs) else "%d:%02d".format(mins, secs)
}
