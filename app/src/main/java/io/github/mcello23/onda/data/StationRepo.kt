package io.github.mcello23.onda.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

@Serializable
data class OndaBackup(
    val version: Int = 1,
    val favorites: List<Station> = emptyList(),
    val songHistory: List<SongHistoryEntry> = emptyList(),
    val podcasts: List<PodcastShow> = emptyList(),
    val settings: AppSettings? = null,
)

object StationRepo {
    private const val MAX_RECENT = 20
    private const val MAX_SONG_HISTORY = 60
    private const val POPULAR_WAIT_MS = 7_000L

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private val cache = ConcurrentHashMap<String, Station>()
    private val popularByCountry = ConcurrentHashMap<String, List<Station>>()
    private val byGenreCache = ConcurrentHashMap<String, List<Station>>()
    private val inflightCountry = ConcurrentHashMap<String, Deferred<List<Station>>>()
    private val inflightGenre = ConcurrentHashMap<String, Deferred<List<Station>>>()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var countriesCache: List<CountryInfo>? = null

    private val _favorites = MutableStateFlow<List<Station>>(emptyList())
    val favorites: StateFlow<List<Station>> = _favorites.asStateFlow()

    private val _recent = MutableStateFlow<List<Station>>(emptyList())
    val recent: StateFlow<List<Station>> = _recent.asStateFlow()

    private val _songHistory = MutableStateFlow<List<SongHistoryEntry>>(emptyList())
    val songHistory: StateFlow<List<SongHistoryEntry>> = _songHistory.asStateFlow()

    var curated: List<Station> = emptyList()
        private set

    @Volatile private var searchResults: List<Station> = emptyList()
    @Volatile private var lastQuery: String? = null

    val lastPlayed: String?
        get() = prefs.getString("lastPlayed", null)

    var region: String
        get() = prefs.getString("region", null)?.takeIf { it.length == 2 } ?: AppSettingsStore.current.defaultCountry
        set(value) {
            val code = value.uppercase(Locale.ROOT)
            prefs.edit().putString("region", code).apply()
            AppSettingsStore.update { it.copy(defaultCountry = code) }
        }

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        AppSettingsStore.init(context)
        prefs = context.applicationContext.getSharedPreferences("onda", Context.MODE_PRIVATE)
        curated = runCatching {
            context.assets.open("stations.json").bufferedReader().use { json.decodeFromString<List<Station>>(it.readText()) }
        }.getOrDefault(emptyList())
        val byId = curated.associateBy { it.uuid }
        _favorites.value = loadStations("favorites").map { byId[it.uuid] ?: it }
        _recent.value = loadStations("recent").map { byId[it.uuid] ?: it }
        _songHistory.value = prefs.getString("songHistory", null)?.let {
            runCatching { json.decodeFromString<List<SongHistoryEntry>>(it) }.getOrNull()
        } ?: emptyList()
        remember(curated + _favorites.value + _recent.value)
    }

    fun cached(uuid: String): Station? = cache[uuid]

    fun isFavorite(uuid: String) = _favorites.value.any { it.uuid == uuid }

    fun toggleFavorite(station: Station) {
        remember(listOf(station))
        val current = _favorites.value
        _favorites.value = if (current.any { it.uuid == station.uuid }) {
            current.filterNot { it.uuid == station.uuid }
        } else {
            current + station
        }
        saveStations("favorites", _favorites.value)
    }

    fun moveFavorite(fromIndex: Int, toIndex: Int) {
        val list = _favorites.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _favorites.value = list
        saveStations("favorites", list)
    }

    fun addCustomStation(name: String, streamUrl: String, country: String = "", genre: String = ""): Station {
        val cleanUrl = streamUrl.trim()
        val station = Station(
            uuid = Station.CUSTOM_PREFIX + UUID.randomUUID().toString().take(8),
            name = name.trim().ifBlank { "Minha Rádio" },
            urlResolved = cleanUrl,
            url = cleanUrl,
            country = country.trim(),
            tags = genre.trim(),
            hls = if (cleanUrl.contains(".m3u8", ignoreCase = true)) 1 else 0,
        )
        remember(listOf(station))
        _favorites.update { listOf(station) + it }
        saveStations("favorites", _favorites.value)
        return station
    }

    fun onPlayed(category: String, station: Station) {
        remember(listOf(station))
        _recent.value = (listOf(station) + _recent.value.filterNot { it.uuid == station.uuid }).take(MAX_RECENT)
        saveStations("recent", _recent.value)
        prefs.edit().putString("lastPlayed", Library.mediaId(category, station.uuid)).apply()
    }

    fun onPlayedMediaId(mediaId: String) {
        prefs.edit().putString("lastPlayed", mediaId).apply()
    }

    fun recordSong(station: Station, rawTitle: String) {
        val clean = rawTitle.trim()
        if (clean.length < 3 || clean.equals(station.displayName, ignoreCase = true)) return
        val current = _songHistory.value
        val latest = current.firstOrNull()
        if (latest != null && latest.songTitle.equals(clean, ignoreCase = true) && latest.stationUuid == station.uuid) {
            return
        }
        val entry = SongHistoryEntry(
            id = "${System.currentTimeMillis()}-${clean.hashCode()}",
            songTitle = clean,
            stationName = station.displayName,
            stationUuid = station.uuid,
            timestampMs = System.currentTimeMillis(),
        )
        val updated = (listOf(entry) + current.filterNot {
            it.songTitle.equals(clean, ignoreCase = true) && it.stationUuid == station.uuid
        }).take(MAX_SONG_HISTORY)
        _songHistory.value = updated
        prefs.edit().putString("songHistory", json.encodeToString(updated)).apply()
    }

    fun clearSongHistory() {
        _songHistory.value = emptyList()
        prefs.edit().remove("songHistory").apply()
    }

    suspend fun refreshSaved() {
        val uuids = (_favorites.value + _recent.value)
            .map { it.uuid }
            .filterNot { it.startsWith(Station.CURATED_PREFIX) || it.startsWith(Station.CUSTOM_PREFIX) }
            .distinct()
        if (uuids.isEmpty()) return
        val fresh = RadioBrowserApi.byUuids(uuids).associateBy { it.uuid }
        if (fresh.isEmpty()) return
        remember(fresh.values.toList())
        _favorites.update { list -> list.map { fresh[it.uuid] ?: it } }
        saveStations("favorites", _favorites.value)
        _recent.update { list -> list.map { fresh[it.uuid] ?: it } }
        saveStations("recent", _recent.value)
    }

    suspend fun countries(): List<CountryInfo> {
        countriesCache?.let { return it }
        return RadioBrowserApi.countries().also { countriesCache = it }
    }

    suspend fun regionStations(codes: List<String>): List<Station> {
        val normalized = codes.map { it.uppercase(Locale.ROOT) }
        val picks = curated.filter { it.countrycode.uppercase(Locale.ROOT) in normalized }
        val jobs = normalized.map(::popularAsync)
        val fetched = withTimeoutOrNull(POPULAR_WAIT_MS) {
            jobs.map { job ->
                try {
                    job.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }.orEmpty().flatten()
        val taken = picks.map(::dedupeKey).toHashSet()
        val popular = fetched.filter { taken.add(dedupeKey(it)) }
        if (picks.isEmpty() && popular.isEmpty()) throw IOException("No stations available")
        return picks + popular
    }

    fun curatedForGenre(genreId: String): List<Station> {
        val genre = Library.genreById(genreId) ?: return emptyList()
        return curated.filter { station ->
            val tagsLower = station.tags.lowercase(Locale.ROOT)
            genre.matchTerms.any { term -> tagsLower.contains(term) }
        }.sortedByDescending { it.bitrate }
    }

    suspend fun genreStations(genreId: String): List<Station> {
        val genre = Library.genreById(genreId) ?: return emptyList()
        val picks = curatedForGenre(genreId)
        val fetched = withTimeoutOrNull(POPULAR_WAIT_MS) {
            try {
                genreAsync(genre).await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
        }.orEmpty()
        val taken = picks.map(::dedupeKey).toHashSet()
        val extras = fetched.filter { taken.add(dedupeKey(it)) }
        if (picks.isEmpty() && extras.isEmpty()) throw IOException("No stations available")
        return picks + extras
    }

    private fun popularAsync(code: String): Deferred<List<Station>> {
        popularByCountry[code]?.let { return CompletableDeferred(it) }
        return inflightCountry.computeIfAbsent(code) {
            io.async(start = CoroutineStart.LAZY) {
                try {
                    RadioBrowserApi.popular(code).let(::bestVariants).also {
                        remember(it)
                        popularByCountry[code] = it
                    }
                } finally {
                    inflightCountry.remove(code)
                }
            }
        }.also { it.start() }
    }

    private fun genreAsync(genre: GenreInfo): Deferred<List<Station>> {
        byGenreCache[genre.id]?.let { return CompletableDeferred(it) }
        return inflightGenre.computeIfAbsent(genre.id) {
            io.async(start = CoroutineStart.LAZY) {
                try {
                    RadioBrowserApi.byTag(genre.tag).let(::bestVariants).also {
                        remember(it)
                        byGenreCache[genre.id] = it
                    }
                } finally {
                    inflightGenre.remove(genre.id)
                }
            }
        }.also { it.start() }
    }

    suspend fun search(query: String): List<Station> {
        val q = query.trim()
        if (q == lastQuery) return searchResults
        val picks = matchCurated(q)
        val pickKeys = picks.map(::dedupeKey).toSet()
        val api = try {
            bestVariants(RadioBrowserApi.search(q))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (picks.isEmpty()) throw e
            null
        }
        val results = picks + api.orEmpty().filter { dedupeKey(it) !in pickKeys }
        remember(results)
        searchResults = results
        lastQuery = if (api != null) q else null
        return results
    }

    suspend fun list(category: String): List<Station> = when {
        category == Library.FAVORITES -> _favorites.value
        category == Library.RECENT -> _recent.value
        category == Library.SEARCH -> searchResults
        category.startsWith(Library.REGION_PREFIX) -> regionStations(Library.regionCodes(category))
        category.startsWith(Library.GENRE_PREFIX) -> genreStations(category.removePrefix(Library.GENRE_PREFIX))
        else -> emptyList()
    }

    suspend fun find(uuid: String): Station? =
        cache[uuid] ?: if (uuid.startsWith(Station.CURATED_PREFIX) || uuid.startsWith(Station.CUSTOM_PREFIX)) null
        else runCatching { RadioBrowserApi.byUuid(uuid) }.getOrNull()?.also { remember(listOf(it)) }

    fun matchFavorites(query: String): List<Station> = match(_favorites.value, query)

    fun matchCurated(query: String): List<Station> = match(curated, query)

    fun exportBackupJson(): String {
        val backup = OndaBackup(
            version = 1,
            favorites = _favorites.value,
            songHistory = _songHistory.value,
            podcasts = PodcastRepo.subscriptions.value,
            settings = AppSettingsStore.current,
        )
        return json.encodeToString(backup)
    }

    fun importBackupJson(raw: String): Result<OndaBackup> = runCatching {
        val parsed = json.decodeFromString<OndaBackup>(raw)
        if (parsed.favorites.isNotEmpty()) {
            val merged = (parsed.favorites + _favorites.value).distinctBy { it.uuid }
            remember(merged)
            _favorites.value = merged
            saveStations("favorites", merged)
        }
        if (parsed.songHistory.isNotEmpty()) {
            val mergedHistory = (parsed.songHistory + _songHistory.value)
                .distinctBy { "${it.stationUuid}|${it.songTitle.lowercase(Locale.ROOT)}" }
                .take(MAX_SONG_HISTORY)
            _songHistory.value = mergedHistory
            prefs.edit().putString("songHistory", json.encodeToString(mergedHistory)).apply()
        }
        if (parsed.podcasts.isNotEmpty()) {
            PodcastRepo.importSubscriptions(parsed.podcasts)
        }
        parsed.settings?.let { AppSettingsStore.replace(it) }
        parsed
    }

    private fun match(stations: List<Station>, query: String): List<Station> {
        val q = compact(query)
        if (q.isEmpty()) return emptyList()
        return stations.filter { compact(it.displayName + " " + it.tags + " " + it.country).contains(q) }
    }

    private fun bestVariants(stations: List<Station>): List<Station> {
        val groups = LinkedHashMap<String, MutableList<Station>>()
        stations.forEach { groups.getOrPut(dedupeKey(it)) { mutableListOf() } += it }
        return groups.values.map { variants ->
            val best = variants.maxBy {
                (if (it.favicon.isNotBlank()) 1000 else 0) +
                    (if (it.homepage.isNotBlank()) 200 else 0) +
                    it.bitrate.coerceAtMost(320)
            }
            val bestFavicon = best.favicon.ifBlank { variants.firstOrNull { it.favicon.isNotBlank() }?.favicon.orEmpty() }
            val bestHomepage = best.homepage.ifBlank { variants.firstOrNull { it.homepage.isNotBlank() }?.homepage.orEmpty() }
            val allStreams = variants
                .map { StreamVariant(it.streamUrl, it.codec, it.bitrate, it.hls) }
                .distinctBy { it.url }
                .sortedByDescending { it.bitrate }
            best.copy(
                favicon = bestFavicon,
                homepage = bestHomepage,
                variants = allStreams,
            )
        }
    }

    private fun dedupeKey(station: Station) = station.countrycode.uppercase(Locale.ROOT) + "|" + compact(
        station.displayName.lowercase(Locale.ROOT)
            .replace(Regex("\\b\\d{2,3}[.,]\\d{1,2}\\b"), " ")
            .replace(Regex("\\b(fm|am|hd)\\b"), " ")
    )

    private fun compact(s: String) =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")

    private fun remember(stations: List<Station>) {
        stations.forEach { cache[it.uuid] = it }
    }

    private fun loadStations(key: String): List<Station> =
        prefs.getString(key, null)?.let { runCatching { json.decodeFromString<List<Station>>(it) }.getOrNull() } ?: emptyList()

    private fun saveStations(key: String, stations: List<Station>) {
        prefs.edit().putString(key, json.encodeToString(stations)).apply()
    }

    internal fun resetForTest(context: Context) {
        AppSettingsStore.resetForTest(context)
        prefs = context.applicationContext.getSharedPreferences("onda", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        cache.clear()
        popularByCountry.clear()
        byGenreCache.clear()
        countriesCache = null
        searchResults = emptyList()
        lastQuery = null
        curated = runCatching {
            context.assets.open("stations.json").bufferedReader().use { json.decodeFromString<List<Station>>(it.readText()) }
        }.getOrDefault(emptyList())
        _favorites.value = emptyList()
        _recent.value = emptyList()
        _songHistory.value = emptyList()
        remember(curated)
    }
}
