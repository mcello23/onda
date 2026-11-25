package io.github.mcello23.onda.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
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
class StationRepoTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        PodcastRepo.resetForTest(app)
        StationRepo.resetForTest(app)
    }

    @Test
    fun curatedStationsAssetLoadsWithValidEntries() {
        val curated = StationRepo.curated
        assertTrue("Expected at least 50 curated stations, got ${curated.size}", curated.size >= 50)

        val uuids = curated.map { it.uuid }
        assertEquals("Duplicate UUIDs in stations.json", uuids.size, uuids.toSet().size)

        curated.forEach { station ->
            assertTrue("Expected curated prefix for ${station.uuid}", station.isCurated)
            assertTrue("Blank name for ${station.uuid}", station.name.isNotBlank())
            assertTrue(
                "Invalid streamUrl for ${station.uuid}: ${station.streamUrl}",
                station.streamUrl.startsWith("http://") || station.streamUrl.startsWith("https://"),
            )
            assertEquals("Expected 2-letter ISO code for ${station.uuid}", 2, station.countrycode.length)
        }

        assertTrue("Expected Spain curated stations", curated.any { it.countrycode == "ES" })
        assertTrue("Expected Brazil curated stations", curated.any { it.countrycode == "BR" })
        assertTrue("Expected curated rock stations", StationRepo.curatedForGenre("rock").isNotEmpty())
    }

    @Test
    fun favoritesToggleAndReorderPersist() {
        val first = StationRepo.curated[0]
        val second = StationRepo.curated[1]

        assertFalse(StationRepo.isFavorite(first.uuid))
        StationRepo.toggleFavorite(first)
        StationRepo.toggleFavorite(second)
        assertEquals(listOf(first.uuid, second.uuid), StationRepo.favorites.value.map { it.uuid })
        assertTrue(StationRepo.isFavorite(first.uuid))

        StationRepo.moveFavorite(0, 1)
        assertEquals(listOf(second.uuid, first.uuid), StationRepo.favorites.value.map { it.uuid })

        // Out-of-bounds move is safely ignored
        StationRepo.moveFavorite(-1, 5)
        assertEquals(listOf(second.uuid, first.uuid), StationRepo.favorites.value.map { it.uuid })

        // Toggling removes favorite
        StationRepo.toggleFavorite(first)
        assertEquals(listOf(second.uuid), StationRepo.favorites.value.map { it.uuid })
        assertFalse(StationRepo.isFavorite(first.uuid))
    }

    @Test
    fun addCustomStationPrependsToFavoritesAndDetectsHls() {
        val hls = StationRepo.addCustomStation(
            name = "  My Custom HLS  ",
            streamUrl = "  https://example.com/live/master.m3u8  ",
            country = "Brazil",
            genre = "indie",
        )
        assertTrue(hls.isCustom)
        assertEquals("My Custom HLS", hls.displayName)
        assertEquals(1, hls.hls)
        assertEquals(hls.uuid, StationRepo.favorites.value.first().uuid)

        val fallback = StationRepo.addCustomStation(
            name = "   ",
            streamUrl = "https://example.com/audio.mp3",
        )
        assertEquals("Minha Rádio", fallback.displayName)
        assertEquals(0, fallback.hls)
        assertEquals(fallback.uuid, StationRepo.favorites.value.first().uuid)
    }

    @Test
    fun onPlayedUpdatesRecentListDeduplicatesAndCapsAtTwenty() {
        repeat(25) { i ->
            val st = Station(uuid = "custom-st-$i", name = "Station $i", url = "https://example.com/$i.mp3")
            StationRepo.onPlayed(Library.EXPLORE, st)
        }
        assertEquals(20, StationRepo.recent.value.size)
        assertEquals("custom-st-24", StationRepo.recent.value.first().uuid)
        assertEquals("explore/custom-st-24", StationRepo.lastPlayed)

        // Playing an older station moves it back to the front without duplicating
        val existing = Station(uuid = "custom-st-10", name = "Station 10", url = "https://example.com/10.mp3")
        StationRepo.onPlayed(Library.RECENT, existing)
        assertEquals(20, StationRepo.recent.value.size)
        assertEquals("custom-st-10", StationRepo.recent.value.first().uuid)
        assertEquals(1, StationRepo.recent.value.count { it.uuid == "custom-st-10" })
    }

    @Test
    fun recordSongFiltersNoiseDeduplicatesAndClears() {
        val station = StationRepo.curated.first()

        // Short strings and station's own name are ignored
        StationRepo.recordSong(station, "ab")
        StationRepo.recordSong(station, station.displayName)
        assertTrue(StationRepo.songHistory.value.isEmpty())

        StationRepo.recordSong(station, "Queen - Bohemian Rhapsody")
        StationRepo.recordSong(station, "queen - bohemian rhapsody") // consecutive dup ignored
        assertEquals(1, StationRepo.songHistory.value.size)

        StationRepo.recordSong(station, "Pink Floyd - Time")
        assertEquals(listOf("Pink Floyd - Time", "Queen - Bohemian Rhapsody"), StationRepo.songHistory.value.map { it.songTitle })

        StationRepo.clearSongHistory()
        assertTrue(StationRepo.songHistory.value.isEmpty())
    }

    @Test
    fun matchCuratedIsAccentAndCaseInsensitive() {
        val matches = StationRepo.matchCurated("radio rock")
        assertTrue("Expected accent-insensitive match for 'Rádio Rock'", matches.any { it.uuid == "onda-br-89fm" })
        assertTrue(StationRepo.matchCurated("   ").isEmpty())
    }

    @Test
    fun backupExportAndImportRoundTripsFavoritesHistoryPodcastsAndSettings() {
        val station = StationRepo.curated.first()
        StationRepo.toggleFavorite(station)
        StationRepo.recordSong(station, "Nirvana - Come As You Are")
        PodcastRepo.toggleSubscription(
            PodcastShow(id = "it-42", title = "Tech Talk", feedUrl = "https://example.com/feed.xml")
        )
        AppSettingsStore.update {
            it.copy(
                language = AppLanguage.EN,
                themeMode = ThemeMode.DARK,
                streamQuality = StreamQuality.HIGH,
                podcastSpeed = 1.5f,
            )
        }

        val exported = StationRepo.exportBackupJson()

        // Wipe local state completely and re-import
        StationRepo.resetForTest(app)
        PodcastRepo.resetForTest(app)
        assertTrue(StationRepo.favorites.value.isEmpty())
        assertEquals(AppLanguage.PT, AppSettingsStore.current.language)

        val result = StationRepo.importBackupJson(exported)
        assertTrue(result.isSuccess)
        assertEquals(listOf(station.uuid), StationRepo.favorites.value.map { it.uuid })
        assertEquals(listOf("Nirvana - Come As You Are"), StationRepo.songHistory.value.map { it.songTitle })
        assertEquals(listOf("it-42"), PodcastRepo.subscriptions.value.map { it.id })
        assertEquals(AppLanguage.EN, AppSettingsStore.current.language)
        assertEquals(ThemeMode.DARK, AppSettingsStore.current.themeMode)
        assertEquals(StreamQuality.HIGH, AppSettingsStore.current.streamQuality)
        assertEquals(1.5f, AppSettingsStore.current.podcastSpeed, 0.001f)

        assertTrue(StationRepo.importBackupJson("{not valid json").isFailure)
    }
}
