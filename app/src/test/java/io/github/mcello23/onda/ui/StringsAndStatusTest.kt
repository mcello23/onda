package io.github.mcello23.onda.ui

import io.github.mcello23.onda.data.AppLanguage
import io.github.mcello23.onda.data.BufferProfile
import io.github.mcello23.onda.data.EqPreset
import io.github.mcello23.onda.data.PodcastEpisode
import io.github.mcello23.onda.data.SeedColor
import io.github.mcello23.onda.data.Station
import io.github.mcello23.onda.data.StreamQuality
import io.github.mcello23.onda.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringsAndStatusTest {

    @Test
    fun localizationHelperPicksRequestedLanguage() {
        assertEquals("Início", L(AppLanguage.PT, "Início", "Home"))
        assertEquals("Home", L(AppLanguage.EN, "Início", "Home"))
    }

    @Test
    fun formatDurationHandlesZeroMinutesAndHours() {
        assertEquals("0:00", formatDurationMs(-500L))
        assertEquals("0:00", formatDurationMs(0L))
        assertEquals("0:05", formatDurationMs(5_000L))
        assertEquals("3:09", formatDurationMs(189_000L))
        assertEquals("1:02:03", formatDurationMs((3600L + 120L + 3L) * 1_000L))
    }

    @Test
    fun allSettingEnumsHaveNonBlankLabelsInBothLanguages() {
        for (lang in AppLanguage.entries) {
            ThemeMode.entries.forEach { assertTrue(it.label(lang).isNotBlank()) }
            SeedColor.entries.forEach { assertTrue(it.label(lang).isNotBlank()) }
            StreamQuality.entries.forEach {
                assertTrue(it.label(lang).isNotBlank())
                assertTrue(it.description(lang).isNotBlank())
            }
            BufferProfile.entries.forEach {
                assertTrue(it.label(lang).isNotBlank())
                assertTrue(it.description(lang).isNotBlank())
            }
            EqPreset.entries.forEach { assertTrue(it.label(lang).isNotBlank()) }
        }
    }

    @Test
    fun statusTextCoversAllPlaybackStatesInPtAndEn() {
        val station = Station(uuid = "onda-es-rockfm", name = "Rock FM", url = "https://example.com/rock.mp3")
        val episode = PodcastEpisode(
            id = "ep-1",
            podcastId = "p-1",
            podcastTitle = "Onda Cast",
            title = "Pilot",
            audioUrl = "https://example.com/ep1.mp3",
        )

        assertEquals(
            "Reconectando…",
            statusText(NowPlaying(station = station, failed = true, playWhenReady = true), AppLanguage.PT),
        )
        assertEquals(
            "Reconnecting…",
            statusText(NowPlaying(station = station, failed = true, playWhenReady = true), AppLanguage.EN),
        )
        assertEquals(
            "Can't connect. Tap to retry",
            statusText(NowPlaying(station = station, failed = true, playWhenReady = false), AppLanguage.EN),
        )
        assertEquals(
            "Conectando…",
            statusText(NowPlaying(station = station, buffering = true, playWhenReady = true), AppLanguage.PT),
        )
        assertEquals(
            "Onda Cast",
            statusText(NowPlaying(podcastEpisode = episode, playWhenReady = true), AppLanguage.EN),
        )
        assertEquals(
            "AC/DC - Back in Black",
            statusText(NowPlaying(station = station, songTitle = "AC/DC - Back in Black", playWhenReady = true), AppLanguage.EN),
        )
        assertEquals(
            "Ao vivo",
            statusText(NowPlaying(station = station, playWhenReady = true), AppLanguage.PT),
        )
        assertEquals(
            "Stopped",
            statusText(NowPlaying(station = station, playWhenReady = false), AppLanguage.EN),
        )
    }
}
