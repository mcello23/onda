package io.github.mcello23.onda.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import io.github.mcello23.onda.R
import io.github.mcello23.onda.playback.ArtworkProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetUpdater {
    private const val PREFS = "onda_widget"
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class Snapshot(
        val title: String,
        val subtitle: String,
        val isPlaying: Boolean,
        val currentStationUuid: String?,
        val artUri: String?,
    )

    fun readSnapshot(context: Context): Snapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Snapshot(
            title = prefs.getString("title", null) ?: context.getString(R.string.widget_idle_title),
            subtitle = prefs.getString("subtitle", null) ?: context.getString(R.string.widget_idle_subtitle),
            isPlaying = prefs.getBoolean("playing", false),
            currentStationUuid = prefs.getString("currentUuid", null),
            artUri = prefs.getString("artUri", null),
        )
    }

    fun updateAll(
        context: Context,
        title: String,
        subtitle: String,
        isPlaying: Boolean,
        currentStationUuid: String?,
        artUri: Uri?,
        refreshFavoritesGrid: Boolean = false,
    ) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("title", title)
            .putString("subtitle", subtitle)
            .putBoolean("playing", isPlaying)
            .putString("currentUuid", currentStationUuid)
            .putString("artUri", artUri?.toString())
            .apply()

        pushToLaunchers(app, refreshFavoritesGrid)
    }

    fun notifyFavoritesChanged(context: Context) {
        val app = context.applicationContext
        pushToLaunchers(app, refreshFavoritesGrid = true)
    }

    fun pushToLaunchers(context: Context, refreshFavoritesGrid: Boolean = false) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val favIds = manager.getAppWidgetIds(ComponentName(app, OndaWidgetProvider::class.java))
        val compactIds = manager.getAppWidgetIds(ComponentName(app, OndaCompactWidgetProvider::class.java))
        if (favIds.isEmpty() && compactIds.isEmpty()) return

        io.launch {
            val snap = readSnapshot(app)
            val artBitmap = snap.artUri?.let { Uri.parse(it) }?.let { uri ->
                ArtworkProvider.loadWidgetBitmap(app, uri, sizePx = 128, cornerPx = 28f)
            }
            if (favIds.isNotEmpty()) {
                favIds.forEach { id ->
                    manager.updateAppWidget(id, OndaWidgetProvider.buildViews(app, id, snap, artBitmap))
                }
                if (refreshFavoritesGrid) {
                    manager.notifyAppWidgetViewDataChanged(favIds, R.id.widget_fav_grid)
                }
            }
            if (compactIds.isNotEmpty()) {
                val views = OndaCompactWidgetProvider.buildViews(app, snap, artBitmap)
                compactIds.forEach { id ->
                    manager.updateAppWidget(id, views)
                }
            }
        }
    }
}
