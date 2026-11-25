package io.github.mcello23.onda.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object PodcastRepo {
    private const val KEY_SUBSCRIPTIONS = "podcast_subscriptions_v1"
    private const val KEY_EPISODES = "podcast_episodes_state_v1"

    private lateinit var prefs: SharedPreferences
    private lateinit var downloadDir: File
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val episodeCache = ConcurrentHashMap<String, PodcastEpisode>()
    private val feedCache = ConcurrentHashMap<String, List<PodcastEpisode>>()
    private val downloadJobs = ConcurrentHashMap<String, Job>()

    private val _subscriptions = MutableStateFlow<List<PodcastShow>>(emptyList())
    val subscriptions: StateFlow<List<PodcastShow>> = _subscriptions.asStateFlow()

    private val _savedEpisodes = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val savedEpisodes: StateFlow<List<PodcastEpisode>> = _savedEpisodes.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        val app = context.applicationContext
        prefs = app.getSharedPreferences("onda", Context.MODE_PRIVATE)
        downloadDir = File(app.filesDir, "podcasts").apply { mkdirs() }
        _subscriptions.value = prefs.getString(KEY_SUBSCRIPTIONS, null)?.let {
            runCatching { json.decodeFromString<List<PodcastShow>>(it) }.getOrNull()
        } ?: emptyList()
        val eps = prefs.getString(KEY_EPISODES, null)?.let {
            runCatching { json.decodeFromString<List<PodcastEpisode>>(it) }.getOrNull()
        }?.map { ep ->
            if (!ep.localFilePath.isNullOrBlank() && !File(ep.localFilePath).exists()) ep.copy(localFilePath = null) else ep
        } ?: emptyList()
        _savedEpisodes.value = eps
        eps.forEach { episodeCache[it.id] = it }
    }

    fun isSubscribed(showId: String): Boolean = _subscriptions.value.any { it.id == showId }

    fun toggleSubscription(show: PodcastShow) {
        val current = _subscriptions.value
        val next = if (current.any { it.id == show.id }) current.filterNot { it.id == show.id } else listOf(show) + current
        _subscriptions.value = next
        saveSubscriptions(next)
    }

    fun importSubscriptions(shows: List<PodcastShow>) {
        val merged = (shows + _subscriptions.value).distinctBy { it.feedUrl.ifBlank { it.id } }
        _subscriptions.value = merged
        saveSubscriptions(merged)
    }

    suspend fun search(query: String, country: String): List<PodcastShow> = PodcastApi.search(query, country)

    suspend fun featured(country: String): List<PodcastShow> = PodcastApi.featured(country)

    suspend fun episodesFor(show: PodcastShow, forceRefresh: Boolean = false): List<PodcastEpisode> {
        if (!forceRefresh) {
            feedCache[show.id]?.let { cached -> return cached.map { mergeSavedState(it) } }
        }
        val fetched = PodcastApi.fetchEpisodes(show).map { mergeSavedState(it) }
        fetched.forEach { episodeCache[it.id] = it }
        feedCache[show.id] = fetched
        return fetched
    }

    fun cachedEpisode(id: String): PodcastEpisode? = episodeCache[id]

    fun rememberEpisode(ep: PodcastEpisode) {
        episodeCache[ep.id] = mergeSavedState(ep)
    }

    fun continueListening(): List<PodcastEpisode> =
        _savedEpisodes.value.filter { (it.positionMs > 2_000L && !it.completed) || it.isDownloaded }

    fun downloadedEpisodes(): List<PodcastEpisode> =
        _savedEpisodes.value.filter { it.isDownloaded }

    fun saveProgress(episodeId: String, positionMs: Long, durationMs: Long, completed: Boolean = false) {
        val base = episodeCache[episodeId] ?: _savedEpisodes.value.firstOrNull { it.id == episodeId } ?: return
        val effectiveDuration = if (durationMs > 0L) durationMs else base.durationMs
        val isDone = completed || (effectiveDuration > 15_000L && positionMs >= effectiveDuration - 5_000L)
        val updated = base.copy(
            positionMs = if (isDone) 0L else positionMs.coerceAtLeast(0L),
            durationMs = effectiveDuration,
            completed = isDone,
        )
        upsertSavedEpisode(updated)
    }

    fun downloadEpisode(episode: PodcastEpisode) {
        if (episode.isDownloaded || downloadJobs.containsKey(episode.id)) return
        episodeCache[episode.id] = episode
        val job = io.launch {
            _downloadProgress.update { it + (episode.id to 0.01f) }
            val dest = File(downloadDir, "${episode.id}.audio")
            val tmp = File(downloadDir, "${episode.id}.tmp")
            try {
                downloadToFile(episode.audioUrl, tmp) { pct ->
                    _downloadProgress.update { it + (episode.id to pct.coerceIn(0.01f, 0.99f)) }
                }
                if (!isActive) {
                    tmp.delete()
                    return@launch
                }
                if (dest.exists()) dest.delete()
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                val latest = episodeCache[episode.id] ?: episode
                upsertSavedEpisode(latest.copy(localFilePath = dest.absolutePath))
            } catch (_: Exception) {
                tmp.delete()
            } finally {
                downloadJobs.remove(episode.id)
                _downloadProgress.update { it - episode.id }
            }
        }
        downloadJobs[episode.id] = job
    }

    fun cancelDownload(episodeId: String) {
        downloadJobs.remove(episodeId)?.cancel()
        _downloadProgress.update { it - episodeId }
    }

    fun deleteDownload(episodeId: String) {
        cancelDownload(episodeId)
        val ep = episodeCache[episodeId] ?: _savedEpisodes.value.firstOrNull { it.id == episodeId } ?: return
        ep.localFilePath?.let { runCatching { File(it).delete() } }
        val updated = ep.copy(localFilePath = null)
        episodeCache[episodeId] = updated
        if (updated.positionMs <= 0L) {
            _savedEpisodes.update { list -> list.filterNot { it.id == episodeId } }
            persistEpisodes()
        } else {
            upsertSavedEpisode(updated)
        }
    }

    private fun mergeSavedState(fresh: PodcastEpisode): PodcastEpisode {
        val saved = _savedEpisodes.value.firstOrNull { it.id == fresh.id } ?: return fresh
        return fresh.copy(
            positionMs = saved.positionMs,
            durationMs = if (saved.durationMs > 0L) saved.durationMs else fresh.durationMs,
            completed = saved.completed,
            localFilePath = saved.localFilePath?.takeIf { File(it).exists() },
        )
    }

    private fun upsertSavedEpisode(updated: PodcastEpisode) {
        episodeCache[updated.id] = updated
        _savedEpisodes.update { list ->
            (listOf(updated) + list.filterNot { it.id == updated.id }).take(60)
        }
        persistEpisodes()
    }

    private fun saveSubscriptions(list: List<PodcastShow>) {
        prefs.edit().putString(KEY_SUBSCRIPTIONS, json.encodeToString(list)).apply()
    }

    private fun persistEpisodes() {
        prefs.edit().putString(KEY_EPISODES, json.encodeToString(_savedEpisodes.value)).apply()
    }

    internal fun resetForTest(context: Context) {
        val app = context.applicationContext
        prefs = app.getSharedPreferences("onda", Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SUBSCRIPTIONS).remove(KEY_EPISODES).apply()
        downloadDir = File(app.filesDir, "podcasts").apply { mkdirs() }
        episodeCache.clear()
        feedCache.clear()
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        _subscriptions.value = emptyList()
        _savedEpisodes.value = emptyList()
        _downloadProgress.value = emptyMap()
    }

    private fun downloadToFile(urlStr: String, target: File, onProgress: (Float) -> Unit) {
        var url = URL(urlStr)
        repeat(6) {
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 12_000
                conn.readTimeout = 20_000
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", RadioBrowserApi.USER_AGENT)
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location") ?: throw IOException("Redirect without location")
                    url = URL(url, loc)
                    return@repeat
                }
                if (code !in 200..299) throw IOException("HTTP $code")
                val total = conn.contentLengthLong
                var downloaded = 0L
                conn.inputStream.use { input ->
                    FileOutputStream(target).use { out ->
                        val buf = ByteArray(32 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            downloaded += n
                            if (total > 0L) onProgress(downloaded.toFloat() / total.toFloat())
                        }
                    }
                }
                return
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }
}
