package io.github.mcello23.onda.data

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

object PodcastApi {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Serializable
    private data class ItunesSearchResponse(val results: List<ItunesItem> = emptyList())

    @Serializable
    private data class ItunesItem(
        val collectionId: Long = 0L,
        val collectionName: String = "",
        val artistName: String = "",
        val feedUrl: String = "",
        val artworkUrl600: String = "",
        val artworkUrl100: String = "",
        val primaryGenreName: String = "",
    )

    suspend fun search(query: String, country: String = "BR"): List<PodcastShow> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        val cc = country.ifBlank { "BR" }.lowercase()
        val url = "https://itunes.apple.com/search?media=podcast&entity=podcast&country=${enc(cc)}&limit=25&term=${enc(q)}"
        val body = fetchText(url)
        val resp = json.decodeFromString<ItunesSearchResponse>(body)
        resp.results.mapNotNull { item ->
            if (item.feedUrl.isBlank() || item.collectionName.isBlank()) return@mapNotNull null
            PodcastShow(
                id = "it-${item.collectionId}",
                title = item.collectionName.trim(),
                author = item.artistName.trim(),
                artworkUrl = item.artworkUrl600.ifBlank { item.artworkUrl100 },
                feedUrl = item.feedUrl.trim(),
                genre = item.primaryGenreName.trim(),
            )
        }.distinctBy { it.feedUrl }
    }

    suspend fun featured(countryCode: String): List<PodcastShow> = withContext(Dispatchers.IO) {
        val cc = countryCode.lowercase().takeIf { it.length == 2 } ?: "br"
        val chart = runCatching {
            val rssJson = fetchText("https://itunes.apple.com/${enc(cc)}/rss/toppodcasts/limit=20/json")
            val root = json.parseToJsonElement(rssJson).jsonObject
            val entries = root["feed"]?.jsonObject?.get("entry")?.jsonArray.orEmpty()
            val ids = entries.mapNotNull { el ->
                el.jsonObject["id"]?.jsonObject?.get("attributes")?.jsonObject?.get("im:id")?.jsonPrimitive?.content
            }
            if (ids.isEmpty()) return@runCatching emptyList<PodcastShow>()
            val lookupBody = fetchText("https://itunes.apple.com/lookup?id=${ids.joinToString(",")}")
            val resp = json.decodeFromString<ItunesSearchResponse>(lookupBody)
            resp.results.mapNotNull { item ->
                if (item.feedUrl.isBlank() || item.collectionName.isBlank()) return@mapNotNull null
                PodcastShow(
                    id = "it-${item.collectionId}",
                    title = item.collectionName.trim(),
                    author = item.artistName.trim(),
                    artworkUrl = item.artworkUrl600.ifBlank { item.artworkUrl100 },
                    feedUrl = item.feedUrl.trim(),
                    genre = item.primaryGenreName.trim(),
                )
            }.distinctBy { it.feedUrl }
        }.getOrDefault(emptyList())
        if (chart.isNotEmpty()) return@withContext chart
        runCatching { search("podcast", cc) }.getOrDefault(emptyList())
    }

    suspend fun fetchEpisodes(show: PodcastShow, limit: Int = 40): List<PodcastEpisode> = withContext(Dispatchers.IO) {
        if (show.feedUrl.isBlank()) return@withContext emptyList()
        val xml = fetchText(show.feedUrl)
        parseRss(show, xml, limit)
    }

    internal fun parseRss(show: PodcastShow, xml: String, limit: Int): List<PodcastEpisode> {
        val episodes = mutableListOf<PodcastEpisode>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.reader())

        var inItem = false
        var title = ""
        var desc = ""
        var audioUrl = ""
        var pubDate = ""
        var duration = 0L
        var guid = ""
        var itemArt = ""
        var channelArt = show.artworkUrl

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && episodes.size < limit) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase()
                    when {
                        tag == "item" -> {
                            inItem = true
                            title = ""; desc = ""; audioUrl = ""; pubDate = ""; duration = 0L; guid = ""; itemArt = ""
                        }
                        inItem && tag == "title" -> title = readTextSafe(parser)
                        inItem && (tag == "description" || tag == "itunes:summary") -> {
                            val txt = readTextSafe(parser)
                            if (desc.isBlank() || tag == "itunes:summary") desc = stripHtml(txt)
                        }
                        inItem && tag == "guid" -> guid = readTextSafe(parser)
                        inItem && tag == "pubdate" -> pubDate = formatPubDate(readTextSafe(parser))
                        inItem && tag == "itunes:duration" -> duration = parseDurationMs(readTextSafe(parser))
                        inItem && tag == "enclosure" -> {
                            val u = parser.getAttributeValue(null, "url").orEmpty().trim()
                            val type = parser.getAttributeValue(null, "type").orEmpty().lowercase()
                            if (u.startsWith("http") && (type.isBlank() || type.contains("audio") || u.contains(".mp3") || u.contains(".m4a"))) {
                                audioUrl = u
                            }
                        }
                        inItem && (tag == "itunes:image" || tag == "media:thumbnail") -> {
                            val href = parser.getAttributeValue(null, "href") ?: parser.getAttributeValue(null, "url")
                            if (!href.isNullOrBlank()) itemArt = href.trim()
                        }
                        !inItem && tag == "itunes:image" && channelArt.isBlank() -> {
                            parser.getAttributeValue(null, "href")?.takeIf { it.isNotBlank() }?.let { channelArt = it.trim() }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("item", ignoreCase = true) && inItem) {
                        inItem = false
                        if (audioUrl.isNotBlank() && title.isNotBlank()) {
                            val rawId = guid.ifBlank { audioUrl }
                            val epId = sha1Short("${show.id}|$rawId")
                            episodes += PodcastEpisode(
                                id = epId,
                                podcastId = show.id,
                                podcastTitle = show.title,
                                podcastArtwork = itemArt.ifBlank { channelArt },
                                title = stripHtml(title).trim(),
                                description = desc.take(360),
                                audioUrl = audioUrl,
                                pubDate = pubDate,
                                durationMs = duration,
                            )
                        }
                    }
                }
            }
            event = runCatching { parser.next() }.getOrDefault(XmlPullParser.END_DOCUMENT)
        }
        return episodes
    }

    private fun readTextSafe(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(parser.text.orEmpty())
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> break
            }
        }
        return sb.toString().trim()
    }

    private fun stripHtml(s: String): String = s
        .replace(Regex("<[^>]*>"), " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun formatPubDate(raw: String): String {
        // Turn "Mon, 24 Nov 2026 08:00:00 +0000" into "24 Nov 2026"
        val match = Regex("(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4})").find(raw)
        return match?.groupValues?.getOrNull(1) ?: raw.take(16)
    }

    private fun parseDurationMs(raw: String): Long {
        val clean = raw.trim()
        if (clean.isEmpty()) return 0L
        clean.toLongOrNull()?.let { return it * 1_000L }
        val parts = clean.split(':').mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1_000L
            2 -> (parts[0] * 60 + parts[1]) * 1_000L
            1 -> parts[0] * 1_000L
            else -> 0L
        }
    }

    private fun fetchText(urlStr: String): String {
        var url = URL(urlStr)
        repeat(5) {
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", RadioBrowserApi.USER_AGENT)
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location") ?: throw IOException("Redirect without location")
                    url = URL(url, loc)
                    return@repeat
                }
                if (code !in 200..299) throw IOException("HTTP $code for $url")
                return conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("Too many redirects for $urlStr")
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun sha1Short(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).take(10).joinToString("") { "%02x".format(it) }
}
