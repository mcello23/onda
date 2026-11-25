package io.github.mcello23.onda.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object RadioBrowserApi {
    const val USER_AGENT = "Onda/1.0 (https://github.com/mcello23/onda)"

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val fallbackHosts = listOf("de1.api.radio-browser.info", "de2.api.radio-browser.info", "fi1.api.radio-browser.info")

    @Volatile
    private var host: String? = null

    @Serializable
    private data class Server(val name: String)

    suspend fun search(name: String): List<Station> =
        stations("/json/stations/search?name=${enc(name)}&limit=70&hidebroken=true&order=clickcount&reverse=true")

    suspend fun popular(countryCode: String): List<Station> =
        if (countryCode.isBlank()) stations("/json/stations/topclick/70?hidebroken=true")
        else stations("/json/stations/search?countrycode=${enc(countryCode)}&limit=80&hidebroken=true&order=clickcount&reverse=true")

    suspend fun byTag(tag: String, limit: Int = 80): List<Station> =
        stations("/json/stations/search?tag=${enc(tag)}&limit=$limit&hidebroken=true&order=clickcount&reverse=true")

    suspend fun countries(): List<CountryInfo> = withContext(Dispatchers.IO) {
        json.decodeFromString<List<CountryInfo>>(get("/json/countries?hidebroken=true&order=stationcount&reverse=true"))
            .filter { it.code.length == 2 && it.stationcount >= 5 && it.name.isNotBlank() }
            .distinctBy { it.code.uppercase() }
    }

    suspend fun byUuid(uuid: String): Station? = stations("/json/stations/byuuid/${enc(uuid)}").firstOrNull()

    suspend fun byUuids(uuids: List<String>): List<Station> =
        stations("/json/stations/byuuid?uuids=${enc(uuids.joinToString(","))}")

    suspend fun countClick(uuid: String) {
        withContext(Dispatchers.IO) { runCatching { get("/json/url/${enc(uuid)}") } }
    }

    private suspend fun stations(path: String): List<Station> = withContext(Dispatchers.IO) {
        json.decodeFromString<List<Station>>(get(path))
            .filter { it.streamUrl.startsWith("http://") || it.streamUrl.startsWith("https://") }
            .distinctBy { it.streamUrl }
    }

    private fun get(path: String): String {
        host?.let { h ->
            try {
                return fetch("https://$h$path")
            } catch (_: IOException) {
                host = null
            }
        }
        var last: IOException? = null
        for (h in discoverHosts()) {
            try {
                return fetch("https://$h$path").also { host = h }
            } catch (e: IOException) {
                last = e
            }
        }
        throw last ?: IOException("No radio-browser server reachable")
    }

    private fun discoverHosts(): List<String> {
        val discovered = try {
            json.decodeFromString<List<Server>>(fetch("https://all.api.radio-browser.info/json/servers"))
                .map { it.name }
                .shuffled()
        } catch (_: Exception) {
            emptyList()
        }
        return (discovered + fallbackHosts).distinct()
    }

    private fun fetch(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code for $url")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}
