package io.github.mcello23.onda.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import io.github.mcello23.onda.playback.ArtworkProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI
import java.util.Locale

@Serializable
data class StreamVariant(
    val url: String,
    val codec: String = "",
    val bitrate: Int = 0,
    val hls: Int = 0,
) {
    val isHls: Boolean get() = hls == 1 || url.contains(".m3u8", ignoreCase = true)
}

@Serializable
data class Station(
    @SerialName("stationuuid") val uuid: String,
    val name: String,
    @SerialName("url_resolved") val urlResolved: String = "",
    val url: String = "",
    val favicon: String = "",
    val homepage: String = "",
    val country: String = "",
    val countrycode: String = "",
    val tags: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val hls: Int = 0,
    val variants: List<StreamVariant> = emptyList(),
) {
    companion object {
        const val CURATED_PREFIX = "onda-"
        const val CUSTOM_PREFIX = "custom-"
    }

    val isCurated: Boolean get() = uuid.startsWith(CURATED_PREFIX)
    val isCustom: Boolean get() = uuid.startsWith(CUSTOM_PREFIX)

    val streamUrl: String get() = urlResolved.ifBlank { url }.trim()

    val displayName: String get() = name.trim().ifBlank { "Estação sem nome" }

    val qualityBadge: String
        get() {
            val c = codec.takeIf { it.isNotBlank() && !it.equals("UNKNOWN", ignoreCase = true) }
            val b = bitrate.takeIf { it > 0 }?.let { "${it}k" }
            return listOfNotNull(c, b).joinToString(" ")
        }

    val subtitle: String
        get() {
            val genre = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.take(2).joinToString(", ")
            return listOf(country, genre, qualityBadge).filter { it.isNotBlank() }.joinToString(" · ")
        }

    val flag: String get() = countryFlag(countrycode)

    /**
     * Secondary logo URL derived from the station's homepage (via Google S2 high-res favicon service).
     * Used automatically when a station from Radio Browser has an empty or broken favicon.
     */
    val fallbackLogoUrl: String
        get() {
            val host = extractHost(homepage).ifBlank { extractHost(favicon) }
            if (host.isBlank() || host.contains("wikimedia.org") || host.contains("streamtheworld")) return ""
            return "https://www.google.com/s2/favicons?domain=${Uri.encode(host)}&sz=256"
        }

    fun allStreamVariants(): List<StreamVariant> {
        val primary = StreamVariant(streamUrl, codec, bitrate, hls)
        val combined = (listOf(primary) + variants).filter { it.url.startsWith("http://") || it.url.startsWith("https://") }
        return combined.distinctBy { it.url }
    }

    fun preferredVariant(quality: StreamQuality, onWifi: Boolean): StreamVariant {
        val all = allStreamVariants()
        if (all.size <= 1) return all.firstOrNull() ?: StreamVariant(streamUrl, codec, bitrate, hls)
        val preferLow = quality == StreamQuality.DATA_SAVER || (quality == StreamQuality.AUTO && !onWifi)
        return if (preferLow) {
            all.minByOrNull { v ->
                val aacBonus = if (v.codec.contains("AAC", ignoreCase = true)) -30 else 0
                val br = if (v.bitrate > 0) v.bitrate else 128
                br + aacBonus
            } ?: all.first()
        } else {
            all.maxByOrNull { v ->
                val hlsBonus = if (v.isHls) 25 else 0
                val aacBonus = if (v.codec.contains("AAC", ignoreCase = true)) 20 else 0
                v.bitrate.coerceAtMost(320) + hlsBonus + aacBonus
            } ?: all.first()
        }
    }

    fun toMediaItem(
        category: String,
        quality: StreamQuality = AppSettingsStore.current.streamQuality,
        onWifi: Boolean = true,
        overrideVariant: StreamVariant? = null,
        groupTitle: String? = null,
    ): MediaItem {
        val variant = overrideVariant ?: preferredVariant(quality, onWifi)
        val uri = variant.url.ifBlank { streamUrl }
        val isHls = variant.isHls
        val qLabel = listOfNotNull(
            variant.codec.takeIf { it.isNotBlank() && !it.equals("UNKNOWN", ignoreCase = true) },
            variant.bitrate.takeIf { it > 0 }?.let { "${it}k" }
        ).joinToString(" ").ifBlank { qualityBadge }
        val sub = listOf(country, qLabel).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { subtitle }

        val metaBuilder = MediaMetadata.Builder()
            .setTitle(displayName)
            .setArtist(sub)
            .setSubtitle(sub)
            .setStation(displayName)
            .setArtworkUri(ArtworkProvider.uriFor(favicon, displayName, fallbackLogoUrl))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
        if (!groupTitle.isNullOrBlank()) {
            metaBuilder.setExtras(android.os.Bundle().apply {
                putString("android.media.browse.CONTENT_STYLE_GROUP_TITLE", groupTitle)
            })
        }

        return MediaItem.Builder()
            .setMediaId(Library.mediaId(category, uuid))
            .setUri(uri)
            .setMimeType(if (isHls) MimeTypes.APPLICATION_M3U8 else null)
            .setMediaMetadata(metaBuilder.build())
            .build()
    }
}

@Serializable
data class CountryInfo(
    val name: String = "",
    @SerialName("iso_3166_1") val code: String = "",
    val stationcount: Int = 0,
) {
    val flag: String get() = countryFlag(code)
}

@Serializable
data class SongHistoryEntry(
    val id: String,
    val songTitle: String,
    val stationName: String,
    val stationUuid: String,
    val timestampMs: Long,
)

data class Region(val code: String, val labelPt: String, val labelEn: String) {
    val flag: String get() = countryFlag(code)
    fun label(lang: AppLanguage): String = if (lang == AppLanguage.EN) labelEn else labelPt
}

data class GenreInfo(
    val id: String,
    val tag: String,
    val labelPt: String,
    val labelEn: String,
    val colorStart: Long,
    val colorEnd: Long,
    val matchTerms: List<String> = listOf(tag),
) {
    fun label(lang: AppLanguage): String = if (lang == AppLanguage.EN) labelEn else labelPt
}

fun countryFlag(isoCode: String): String {
    val clean = isoCode.trim().uppercase(Locale.ROOT)
    if (clean.length != 2 || !clean[0].isLetter() || !clean[1].isLetter()) return "🌍"
    val first = Character.toChars(0x1F1E6 + (clean[0] - 'A'))
    val second = Character.toChars(0x1F1E6 + (clean[1] - 'A'))
    return String(first) + String(second)
}

private fun extractHost(raw: String): String {
    if (raw.isBlank()) return ""
    return runCatching { URI(raw.trim()).host?.removePrefix("www.").orEmpty() }.getOrDefault("")
}

object Library {
    const val ROOT = "root"
    const val FAVORITES = "fav"
    const val EXPLORE = "explore"
    const val RECENT = "rec"
    const val SEARCH = "search"
    const val PODCASTS = "pod"
    const val PODCAST_SHOW_PREFIX = "podshow:"
    const val GENRES_ROOT = "genres"
    const val REGION_PREFIX = "cc:"
    const val GENRE_PREFIX = "genre:"
    const val PODCAST_EP_PREFIX = "ep:"

    val REGIONS = listOf(
        Region("ES", "Espanha", "Spain"),
        Region("BR", "Brasil", "Brazil"),
        Region("US", "EUA", "USA"),
        Region("GB", "Reino Unido", "UK"),
    )

    val GENRES = listOf(
        GenreInfo("rock", "rock", "Rock Mundial", "World Rock", 0xFFD93025, 0xFF7A1414, listOf("rock", "hard rock", "punk")),
        GenreInfo("classic_rock", "classic rock", "Classic Rock", "Classic Rock", 0xFFE67E22, 0xFF7D3C0E, listOf("classic rock", "70s", "classic hits")),
        GenreInfo("indie", "indie", "Indie & Alternativo", "Indie & Alternative", 0xFF0F9D58, 0xFF094D2B, listOf("indie", "alternative", "eclectic")),
        GenreInfo("pop", "pop", "Pop & Hits", "Pop & Hits", 0xFF2962FF, 0xFF142E7A, listOf("pop", "hits", "adult contemporary")),
        GenreInfo("jazz", "jazz", "Jazz & Blues", "Jazz & Blues", 0xFF5C6BC0, 0xFF283066, listOf("jazz", "blues", "soul")),
        GenreInfo("electronic", "electronic", "Eletrônica & Dance", "Electronic & Dance", 0xFF8E24AA, 0xFF420D54, listOf("electronic", "dance", "house", "techno")),
        GenreInfo("mpb", "mpb", "MPB & Brasileira", "MPB & Brazilian", 0xFF00897B, 0xFF00473E, listOf("mpb", "brazilian", "bossa")),
        GenreInfo("80s", "80s", "Anos 80 & Retrô", "80s & Retro", 0xFFD81B60, 0xFF6B0B2E, listOf("80s", "90s", "retro", "classic hits")),
        GenreInfo("chill", "chillout", "Chill & Lo-Fi", "Chill & Lo-Fi", 0xFF00ACC1, 0xFF005662, listOf("chill", "chillout", "ambient", "downtempo", "lofi")),
        GenreInfo("classical", "classical", "Música Clássica", "Classical", 0xFF6D4C41, 0xFF36241E, listOf("classical", "film scores", "opera")),
        GenreInfo("news", "news", "Notícias & Talk", "News & Talk", 0xFF455A64, 0xFF202B30, listOf("news", "talk", "sports")),
    )

    fun regionCategory(vararg codes: String) = REGION_PREFIX + codes.joinToString("+")

    fun regionCodes(category: String) = category.removePrefix(REGION_PREFIX).split('+').filter { it.isNotBlank() }

    fun genreCategory(genreId: String) = GENRE_PREFIX + genreId

    fun genreById(genreId: String): GenreInfo? = GENRES.firstOrNull { it.id == genreId }

    fun podcastEpisodeMediaId(episodeId: String) = "$PODCAST_EP_PREFIX$episodeId"

    fun isPodcastEpisode(mediaId: String?) = mediaId?.startsWith(PODCAST_EP_PREFIX) == true

    fun parsePodcastEpisodeId(mediaId: String?): String? =
        mediaId?.takeIf { it.startsWith(PODCAST_EP_PREFIX) }?.removePrefix(PODCAST_EP_PREFIX)?.takeIf { it.isNotBlank() }

    fun mediaId(category: String, uuid: String) = "$category/$uuid"

    fun parse(mediaId: String): Pair<String, String>? {
        if (isPodcastEpisode(mediaId)) return null
        val slash = mediaId.indexOf('/')
        if (slash <= 0 || slash == mediaId.length - 1) return null
        return mediaId.substring(0, slash) to mediaId.substring(slash + 1)
    }
}
