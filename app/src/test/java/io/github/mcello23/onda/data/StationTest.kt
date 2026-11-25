package io.github.mcello23.onda.data

import android.app.Application
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StationTest {

    @Test
    fun streamVariantDetectsHlsByFlagOrExtension() {
        assertTrue(StreamVariant("https://example.com/live", hls = 1).isHls)
        assertTrue(StreamVariant("https://example.com/live/stream.M3U8?token=1", hls = 0).isHls)
        assertFalse(StreamVariant("https://example.com/live.aac", codec = "AAC", bitrate = 128, hls = 0).isHls)
    }

    @Test
    fun stationComputedPropertiesFormatCleanly() {
        val station = Station(
            uuid = "onda-es-rockfm",
            name = "  Rock FM  ",
            urlResolved = " https://stream.example.com/rock.mp3 ",
            url = "https://fallback.example.com/rock.mp3",
            homepage = "https://www.rockfm.fm/live",
            country = "Spain",
            countrycode = "es",
            tags = " rock , classic rock , guitar ",
            codec = "MP3",
            bitrate = 128,
        )

        assertTrue(station.isCurated)
        assertFalse(station.isCustom)
        assertEquals("https://stream.example.com/rock.mp3", station.streamUrl)
        assertEquals("Rock FM", station.displayName)
        assertEquals("MP3 128k", station.qualityBadge)
        assertEquals("Spain · rock, classic rock · MP3 128k", station.subtitle)
        assertEquals("🇪🇸", station.flag)
        assertEquals("https://www.google.com/s2/favicons?domain=rockfm.fm&sz=256", station.fallbackLogoUrl)
    }

    @Test
    fun stationFallbacksHandleBlankAndUnknownValues() {
        val blank = Station(
            uuid = "custom-12345678",
            name = "   ",
            url = "https://stream.example.com/radio.aac",
            favicon = "https://upload.wikimedia.org/logo.png",
            codec = "UNKNOWN",
            bitrate = 0,
        )

        assertFalse(blank.isCurated)
        assertTrue(blank.isCustom)
        assertEquals("Estação sem nome", blank.displayName)
        assertEquals("https://stream.example.com/radio.aac", blank.streamUrl)
        assertEquals("", blank.qualityBadge)
        assertEquals("🌍", blank.flag)
        // Wikimedia hosts should be excluded from Google S2 favicon fallback
        assertEquals("", blank.fallbackLogoUrl)
    }

    @Test
    fun preferredVariantRespectsQualityAndNetwork() {
        val lowAac = StreamVariant("https://cdn.example.com/low.aac", codec = "AAC+", bitrate = 64)
        val midMp3 = StreamVariant("https://cdn.example.com/mid.mp3", codec = "MP3", bitrate = 128)
        val highHls = StreamVariant("https://cdn.example.com/high.m3u8", codec = "AAC", bitrate = 320, hls = 1)
        val invalid = StreamVariant("rtsp://ignored.example.com/live", codec = "MP3", bitrate = 999)

        val station = Station(
            uuid = "onda-test-1",
            name = "Multi Variant FM",
            urlResolved = midMp3.url,
            codec = midMp3.codec,
            bitrate = midMp3.bitrate,
            variants = listOf(lowAac, midMp3, highHls, invalid),
        )

        // Filters invalid protocols and deduplicates URLs
        assertEquals(3, station.allStreamVariants().size)

        // Data Saver or Auto on cellular picks efficient low AAC+
        assertEquals(lowAac.url, station.preferredVariant(StreamQuality.DATA_SAVER, onWifi = true).url)
        assertEquals(lowAac.url, station.preferredVariant(StreamQuality.AUTO, onWifi = false).url)

        // Auto on Wi-Fi or High (even on cellular) picks best quality
        assertEquals(highHls.url, station.preferredVariant(StreamQuality.AUTO, onWifi = true).url)
        assertEquals(highHls.url, station.preferredVariant(StreamQuality.HIGH, onWifi = false).url)
    }

    @Test
    fun toMediaItemPopulatesMetadataMimeTypeAndGroupTitle() {
        val station = Station(
            uuid = "onda-uk-bbc6",
            name = "BBC Radio 6 Music",
            urlResolved = "https://a.files.bbci.co.uk/media/live/manifesto/audio/simulcast/hls/uk/sbr_high/ak/bbc_6music.m3u8",
            country = "United Kingdom",
            countrycode = "GB",
            codec = "AAC",
            bitrate = 320,
            hls = 1,
        )

        val item = station.toMediaItem(
            category = Library.FAVORITES,
            quality = StreamQuality.HIGH,
            onWifi = true,
            groupTitle = "Favorites",
        )

        assertEquals("fav/onda-uk-bbc6", item.mediaId)
        assertEquals(MimeTypes.APPLICATION_M3U8, item.localConfiguration?.mimeType)
        assertEquals("BBC Radio 6 Music", item.mediaMetadata.title.toString())
        assertEquals(MediaMetadata.MEDIA_TYPE_RADIO_STATION, item.mediaMetadata.mediaType)
        assertEquals(true, item.mediaMetadata.isPlayable)
        assertEquals(false, item.mediaMetadata.isBrowsable)
        assertEquals(
            "Favorites",
            item.mediaMetadata.extras?.getString("android.media.browse.CONTENT_STYLE_GROUP_TITLE"),
        )
    }

    @Test
    fun countryFlagProducesRegionalIndicatorsOrGlobe() {
        assertEquals("🇧🇷", countryFlag("BR"))
        assertEquals("🇪🇸", countryFlag("es"))
        assertEquals("🇺🇸", countryFlag(" US "))
        assertEquals("🌍", countryFlag(""))
        assertEquals("🌍", countryFlag("USA"))
        assertEquals("🌍", countryFlag("12"))
    }

    @Test
    fun libraryIdHelpersRoundTripAndValidate() {
        val regionCat = Library.regionCategory("ES", "BR")
        assertEquals("cc:ES+BR", regionCat)
        assertEquals(listOf("ES", "BR"), Library.regionCodes(regionCat))

        assertEquals("genre:rock", Library.genreCategory("rock"))
        assertNotNull(Library.genreById("rock"))
        assertNull(Library.genreById("nonexistent"))

        val epMediaId = Library.podcastEpisodeMediaId("abc123")
        assertEquals("ep:abc123", epMediaId)
        assertTrue(Library.isPodcastEpisode(epMediaId))
        assertEquals("abc123", Library.parsePodcastEpisodeId(epMediaId))
        assertNull(Library.parse(epMediaId))

        val stationMediaId = Library.mediaId(Library.FAVORITES, "onda-br-89fm")
        assertEquals("fav/onda-br-89fm", stationMediaId)
        assertEquals(Library.FAVORITES to "onda-br-89fm", Library.parse(stationMediaId))

        assertNull(Library.parse("noslash"))
        assertNull(Library.parse("/leading"))
        assertNull(Library.parse("trailing/"))
    }
}
