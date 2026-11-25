package io.github.mcello23.onda.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import io.github.mcello23.onda.R
import io.github.mcello23.onda.data.StationRepo

/**
 * Compact 4x1 Home Screen Widget: current station/podcast artwork, live title/song,
 * and Prev / Play-Pause / Next controls with zero empty whitespace.
 */
class OndaCompactWidgetProvider : AppWidgetProvider() {

    companion object {
        fun buildViews(
            context: Context,
            snap: WidgetUpdater.Snapshot,
            artBitmap: Bitmap?,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_onda_compact)
            views.setTextViewText(R.id.widget_compact_title, snap.title)
            views.setTextViewText(R.id.widget_compact_subtitle, snap.subtitle)
            views.setImageViewResource(R.id.widget_compact_play, if (snap.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)

            if (artBitmap != null) {
                views.setImageViewBitmap(R.id.widget_compact_art, artBitmap)
            } else {
                views.setImageViewResource(R.id.widget_compact_art, R.drawable.ic_radio)
            }

            views.setOnClickPendingIntent(R.id.widget_compact_info, OndaWidgetProvider.openAppPendingIntent(context, 300))
            views.setOnClickPendingIntent(R.id.widget_compact_prev, OndaWidgetProvider.actionPendingIntent(context, OndaWidgetProvider.ACTION_PREV, 301))
            views.setOnClickPendingIntent(R.id.widget_compact_play, OndaWidgetProvider.actionPendingIntent(context, OndaWidgetProvider.ACTION_TOGGLE, 302))
            views.setOnClickPendingIntent(R.id.widget_compact_next, OndaWidgetProvider.actionPendingIntent(context, OndaWidgetProvider.ACTION_NEXT, 303))
            return views
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        StationRepo.init(context)
        WidgetUpdater.pushToLaunchers(context, refreshFavoritesGrid = false)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        WidgetUpdater.pushToLaunchers(context, refreshFavoritesGrid = false)
    }
}
