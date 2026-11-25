package io.github.mcello23.onda.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import io.github.mcello23.onda.R
import io.github.mcello23.onda.data.Station
import io.github.mcello23.onda.data.StationRepo
import io.github.mcello23.onda.playback.ArtworkProvider
import io.github.mcello23.onda.playback.PlaybackService

/**
 * Feeds the scrollable GridView of the 4x2/4x3 Onda widget with ALL favorite station icons.
 */
class OndaWidgetGridService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = FavoritesGridFactory(applicationContext)
}

private class FavoritesGridFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var stations: List<Station> = emptyList()
    private var currentUuid: String? = null

    override fun onCreate() {
        StationRepo.init(context)
    }

    override fun onDataSetChanged() {
        StationRepo.init(context)
        val favs = StationRepo.favorites.value
        stations = favs.ifEmpty {
            // Show curated Spain & Brazil picks until the user stars their own favorites
            StationRepo.curated.filter { it.countrycode == "ES" || it.countrycode == "BR" }.take(8)
        }
        currentUuid = WidgetUpdater.readSnapshot(context).currentStationUuid
    }

    override fun onDestroy() {
        stations = emptyList()
    }

    override fun getCount(): Int = stations.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_fav_grid_item)
        val station = stations.getOrNull(position) ?: return views

        views.setTextViewText(R.id.fav_tile_name, station.displayName)
        val isActive = station.uuid == currentUuid
        views.setInt(
            R.id.fav_tile_root,
            "setBackgroundResource",
            if (isActive) R.drawable.widget_tile_active_bg else android.R.color.transparent,
        )

        val uri = ArtworkProvider.uriFor(station.favicon, station.displayName, station.fallbackLogoUrl)
        val bitmap = ArtworkProvider.loadWidgetBitmap(context, uri, sizePx = 112, cornerPx = 24f)
        if (bitmap != null) {
            views.setImageViewBitmap(R.id.fav_tile_icon, bitmap)
        } else {
            views.setImageViewResource(R.id.fav_tile_icon, R.drawable.ic_radio)
        }

        val fillIn = Intent().apply {
            putExtra(PlaybackService.EXTRA_STATION_UUID, station.uuid)
        }
        views.setOnClickFillInIntent(R.id.fav_tile_root, fillIn)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = stations.getOrNull(position)?.uuid?.hashCode()?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
