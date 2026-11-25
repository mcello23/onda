package io.github.mcello23.onda

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import io.github.mcello23.onda.data.AppSettingsStore
import io.github.mcello23.onda.data.PodcastRepo
import io.github.mcello23.onda.data.RadioBrowserApi
import io.github.mcello23.onda.data.StationRepo
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class OndaApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        AppSettingsStore.init(this)
        StationRepo.init(this)
        PodcastRepo.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", RadioBrowserApi.USER_AGENT)
                    .header("Accept", "image/webp,image/png,image/jpeg,image/svg+xml,image/*,*/*;q=0.8")
                    .build()
                chain.proceed(req)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttp)
            .components { add(SvgDecoder.Factory()) }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_art_cache"))
                    .maxSizeBytes(40L * 1024 * 1024)
                    .build()
            }
            .crossfade(200)
            .build()
    }
}
