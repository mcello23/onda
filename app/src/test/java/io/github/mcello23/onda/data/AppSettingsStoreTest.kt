package io.github.mcello23.onda.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppSettingsStoreTest {

    private lateinit var app: Application
    private val lenientJson = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        AppSettingsStore.resetForTest(app)
    }

    @Test
    fun defaultsMatchExpectedConfiguration() {
        val s = AppSettingsStore.current
        assertEquals(ThemeMode.SYSTEM, s.themeMode)
        assertTrue(s.useDynamicColor)
        assertEquals(SeedColor.OCEAN, s.seedColor)
        assertEquals(StreamQuality.AUTO, s.streamQuality)
        assertEquals(BufferProfile.STABLE, s.bufferProfile)
        assertTrue(s.normalizeVolume)
        assertFalse(s.eqEnabled)
        assertEquals(EqPreset.FLAT, s.eqPreset)
        assertEquals("ES", s.defaultCountry)
        assertEquals(AppLanguage.PT, s.language)
        assertEquals(1.0f, s.podcastSpeed, 0.001f)
    }

    @Test
    fun updateAndReplacePersistSettings() {
        AppSettingsStore.update {
            it.copy(
                themeMode = ThemeMode.DARK,
                useDynamicColor = false,
                seedColor = SeedColor.EMERALD,
                eqEnabled = true,
                eqPreset = EqPreset.ROCK,
                eqBandLevels = EqPreset.ROCK.gainsMb,
                language = AppLanguage.EN,
            )
        }

        val savedRaw = app.getSharedPreferences("onda", Application.MODE_PRIVATE)
            .getString("app_settings_v1", null)
        assertTrue(!savedRaw.isNullOrBlank())

        val decoded = lenientJson.decodeFromString<AppSettings>(savedRaw!!)
        assertEquals(ThemeMode.DARK, decoded.themeMode)
        assertEquals(SeedColor.EMERALD, decoded.seedColor)
        assertEquals(EqPreset.ROCK.gainsMb, decoded.eqBandLevels)
        assertEquals(AppLanguage.EN, decoded.language)

        val custom = AppSettings(themeMode = ThemeMode.LIGHT, defaultCountry = "BR")
        AppSettingsStore.replace(custom)
        assertEquals(custom, AppSettingsStore.current)
    }

    @Test
    fun serializationIgnoresUnknownFields() {
        val encoded = lenientJson.encodeToString(AppSettings(language = AppLanguage.EN))
        val withExtra = encoded.dropLast(1) + ""","futureFlag":true}"""
        val parsed = lenientJson.decodeFromString<AppSettings>(withExtra)
        assertEquals(AppLanguage.EN, parsed.language)
    }
}
