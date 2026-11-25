package io.github.mcello23.onda.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import io.github.mcello23.onda.playback.ArtworkProvider
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class PodcastShow(
    val id: String,
    val title: String,
    val author: String = "",
    val artworkUrl: String = "",
    val feedUrl: String = "",
    val genre: String = "",
    val description: String = "",
)

@Serializable
data class PodcastEpisode(
    val id: String,
    val podcastId: String,
    val podcastTitle: String,
    val podcastArtwork: String = "",
    val title: String,
    val description: String = "",
    val audioUrl: String,
    val pubDate: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val completed: Boolean = false,
    val localFilePath: String? = null,
) {
    val isDownloaded: Boolean
        get() = !localFilePath.isNullOrBlank() && File(localFilePath).exists()

    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val playbackUri: Uri
        get() = if (isDownloaded) Uri.fromFile(File(localFilePath!!)) else Uri.parse(audioUrl)

    fun toMediaItem(groupTitle: String? = null): MediaItem {
        val subtitle = buildString {
            append(podcastTitle)
            if (isDownloaded) append(" · Offline")
        }
        val extras = android.os.Bundle().apply {
            if (!groupTitle.isNullOrBlank()) {
                putString("android.media.browse.CONTENT_STYLE_GROUP_TITLE", groupTitle)
            }
            val status = when {
                completed -> 2 // STATUS_PLAYED
                positionMs > 2_000L -> 1 // STATUS_PARTIALLY_PLAYED
                else -> 0 // STATUS_NOT_PLAYED
            }
            putInt("android.media.extra.PLAYBACK_STATUS", status)
            if (durationMs > 0L && positionMs > 0L && !completed) {
                putDouble("android.media.extra.COMPLETION_PERCENTAGE", progressFraction.toDouble())
            }
            if (isDownloaded) {
                putLong("android.media.extra.DOWNLOAD_STATUS", 2L) // STATUS_DOWNLOADED
            }
        }
        return MediaItem.Builder()
            .setMediaId(Library.podcastEpisodeMediaId(id))
            .setUri(playbackUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(podcastTitle)
                    .setAlbumTitle(podcastTitle)
                    .setSubtitle(subtitle)
                    .setArtworkUri(ArtworkProvider.uriFor(podcastArtwork, podcastTitle))
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }
}
