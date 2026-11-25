package io.github.mcello23.onda.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class SeedColor { OCEAN, TERRACOTTA, EMERALD, VIOLET }

@Serializable
enum class StreamQuality { AUTO, HIGH, DATA_SAVER }

@Serializable
enum class BufferProfile(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val playbackBufferMs: Int,
    val rebufferMs: Int,
) {
    FAST(5_000, 15_000, 1_000, 2_000),
    BALANCED(15_000, 45_000, 2_500, 4_000),
    STABLE(30_000, 90_000, 4_000, 6_000),
}

@Serializable
enum class EqPreset(val gainsMb: List<Int>) {
    FLAT(listOf(0, 0, 0, 0, 0)),
    ROCK(listOf(400, 200, -100, 250, 400)),
    POP(listOf(-100, 200, 350, 200, -100)),
    JAZZ(listOf(300, 150, -150, 150, 300)),
    VOCAL(listOf(-200, 100, 400, 350, 0)),
    BASS_BOOST(listOf(550, 350, 0, 0, 100)),
    CUSTOM(listOf(0, 0, 0, 0, 0)),
}

@Serializable
enum class AppLanguage { PT, EN }

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val seedColor: SeedColor = SeedColor.OCEAN,
    val streamQuality: StreamQuality = StreamQuality.AUTO,
    val bufferProfile: BufferProfile = BufferProfile.STABLE,
    val normalizeVolume: Boolean = true,
    val eqEnabled: Boolean = false,
    val eqPreset: EqPreset = EqPreset.FLAT,
    val eqBandLevels: List<Int> = EqPreset.FLAT.gainsMb,
    val defaultCountry: String = "ES",
    val language: AppLanguage = AppLanguage.PT,
    val podcastSpeed: Float = 1.0f,
)

object AppSettingsStore {
    private const val KEY_SETTINGS = "app_settings_v1"
    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val current: AppSettings get() = _settings.value

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("onda", Context.MODE_PRIVATE)
        val loaded = prefs.getString(KEY_SETTINGS, null)?.let {
            runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull()
        }
        if (loaded != null) _settings.value = loaded
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_SETTINGS, json.encodeToString(next)).apply()
        }
    }

    fun replace(next: AppSettings) {
        _settings.value = next
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_SETTINGS, json.encodeToString(next)).apply()
        }
    }

    internal fun resetForTest(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("onda", Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SETTINGS).apply()
        _settings.value = AppSettings()
    }
}
