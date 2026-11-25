package io.github.mcello23.onda.data

import android.app.Application
import androidx.media3.common.MediaMetadata
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PodcastTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        PodcastRepo.resetForTest(app)
    }

    @Test
    fun parseRssExtractsEpisodesStripsHtmlAndParsesDurations() {
        val show = PodcastShow(
            id = "it-999",
            title = "Onda Cast",
            author = "Marcelo",
            artworkUrl = "https://example.com/channel.jpg",
            feedUrl = "https://example.com/feed.xml",
        )
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Onda Cast</title>
                <item>
                  <title><![CDATA[Episode <b>1</b> &amp; Intro]]></title>
                  <guid>ep-guid-1</guid>
                  <pubDate>Mon, 24 Nov 2026 08:00:00 +0000</pubDate>
                  <itunes:duration>01:02:03</itunes:duration>
                  <itunes:summary><![CDATA[<p>Hello&nbsp;&quot;world&quot;&#39;s best!</p>]]></itunes:summary>
                  <itunes:image href="https://example.com/ep1.jpg" />
                  <enclosure url="https://cdn.example.com/ep1.mp3" type="audio/mpeg" />
                </item>
                <item>
                  <title>Episode 2</title>
                  <pubDate>Tue, 25 Nov 2026 09:00:00 +0000</pubDate>
                  <itunes:duration>45:30</itunes:duration>
                  <enclosure url="https://cdn.example.com/ep2.m4a" type="audio/x-m4a" />
                </item>
                <item>
                  <title>Episode 3 Seconds Only</title>
                  <itunes:duration>180</itunes:duration>
                  <enclosure url="https://cdn.example.com/ep3.mp3" />
                </item>
                <item>
                  <title>Video Item Ignored</title>
                  <enclosure url="https://cdn.example.com/video.mp4" type="video/mp4" />
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val episodes = PodcastApi.parseRss(show, xml, limit = 10)
        assertEquals(3, episodes.size)

        val ep1 = episodes[0]
        assertEquals("Episode 1 & Intro", ep1.title)
        assertEquals("24 Nov 2026", ep1.pubDate)
        assertEquals((3600L + 120L + 3L) * 1_000L, ep1.durationMs)
        assertEquals("Hello \"world\"'s best!", ep1.description)
        assertEquals("https://example.com/ep1.jpg", ep1.podcastArtwork)
        assertEquals("https://cdn.example.com/ep1.mp3", ep1.audioUrl)

        val ep2 = episodes[1]
        assertEquals((45L * 60L + 30L) * 1_000L, ep2.durationMs)
        assertEquals("https://example.com/channel.jpg", ep2.podcastArtwork)

        val ep3 = episodes[2]
        assertEquals(180_000L, ep3.durationMs)

        // Limit is respected and episode IDs are deterministic
        val limited = PodcastApi.parseRss(show, xml, limit = 1)
        assertEquals(1, limited.size)
        assertEquals(ep1.id, limited[0].id)
    }

    @Test
    fun episodeProgressPlaybackUriAndMediaItemExtras() {
        val localAudio = File(app.cacheDir, "offline-ep.audio").apply { writeText("fake-audio") }
        val episode = PodcastEpisode(
            id = "ep-1",
            podcastId = "it-1",
            podcastTitle = "Tech Daily",
            title = "Kotlin Testing",
            audioUrl = "https://cdn.example.com/ep.mp3",
            durationMs = 100_000L,
            positionMs = 40_000L,
            completed = false,
            localFilePath = localAudio.absolutePath,
        )

        assertTrue(episode.isDownloaded)
        assertEquals(0.4f, episode.progressFraction, 0.001f)
        assertEquals("file", episode.playbackUri.scheme)

        val mediaItem = episode.toMediaItem(groupTitle = "Offline")
        assertEquals("ep:ep-1", mediaItem.mediaId)
        assertEquals(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE, mediaItem.mediaMetadata.mediaType)
        assertEquals("Tech Daily · Offline", mediaItem.mediaMetadata.subtitle.toString())
        val extras = mediaItem.mediaMetadata.extras
        assertNotNull(extras)
        assertEquals("Offline", extras!!.getString("android.media.browse.CONTENT_STYLE_GROUP_TITLE"))
        assertEquals(1, extras.getInt("android.media.extra.PLAYBACK_STATUS"))
        assertEquals(0.4, extras.getDouble("android.media.extra.COMPLETION_PERCENTAGE"), 0.001)
        assertEquals(2L, extras.getLong("android.media.extra.DOWNLOAD_STATUS"))

        localAudio.delete()
        assertFalse(episode.isDownloaded)
        assertEquals("https", episode.playbackUri.scheme)
    }

    @Test
    fun podcastRepoSubscriptionsProgressAndDeleteDownload() {
        val show1 = PodcastShow(id = "it-1", title = "Show One", feedUrl = "https://example.com/1.xml")
        val show2 = PodcastShow(id = "it-2", title = "Show Two", feedUrl = "https://example.com/2.xml")

        PodcastRepo.toggleSubscription(show1)
        assertTrue(PodcastRepo.isSubscribed(show1.id))

        // Importing duplicate feedUrl deduplicates cleanly
        PodcastRepo.importSubscriptions(listOf(show1.copy(id = "it-1-dup"), show2))
        assertEquals(2, PodcastRepo.subscriptions.value.size)

        val ep = PodcastEpisode(
            id = "ep-progress",
            podcastId = show1.id,
            podcastTitle = show1.title,
            title = "Deep Dive",
            audioUrl = "https://cdn.example.com/deep.mp3",
            durationMs = 60_000L,
        )
        PodcastRepo.rememberEpisode(ep)

        // Partial progress shows up in continueListening
        PodcastRepo.saveProgress(ep.id, positionMs = 20_000L, durationMs = 60_000L)
        assertEquals(1, PodcastRepo.continueListening().size)
        assertFalse(PodcastRepo.cachedEpisode(ep.id)!!.completed)

        // Reaching within 5s of end auto-marks completed and resets position
        PodcastRepo.saveProgress(ep.id, positionMs = 56_000L, durationMs = 60_000L)
        assertTrue(PodcastRepo.cachedEpisode(ep.id)!!.completed)
        assertEquals(0L, PodcastRepo.cachedEpisode(ep.id)!!.positionMs)
        assertTrue(PodcastRepo.continueListening().isEmpty())

        // Delete download removes local file and cleans up zero-position entry
        val downloadedFile = File(app.filesDir, "podcasts/ep-dl.audio").apply {
            parentFile?.mkdirs()
            writeText("bytes")
        }
        val dlEp = ep.copy(id = "ep-dl", positionMs = 0L, localFilePath = downloadedFile.absolutePath)
        PodcastRepo.rememberEpisode(dlEp)
        PodcastRepo.saveProgress(dlEp.id, positionMs = 0L, durationMs = 60_000L)
        assertEquals(1, PodcastRepo.downloadedEpisodes().size)

        PodcastRepo.deleteDownload(dlEp.id)
        assertFalse(downloadedFile.exists())
        assertTrue(PodcastRepo.downloadedEpisodes().isEmpty())
    }
}
