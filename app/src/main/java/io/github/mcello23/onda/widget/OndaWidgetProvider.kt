package io.github.mcello23.onda.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import io.github.mcello23.onda.MainActivity
import io.github.mcello23.onda.R
import io.github.mcello23.onda.data.StationRepo
import io.github.mcello23.onda.playback.PlaybackService

class OndaWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_FAVORITE = "io.github.mcello23.onda.widget.ACTION_PLAY_FAVORITE"
        const val ACTION_TOGGLE = "io.github.mcello23.onda.widget.ACTION_TOGGLE"
        const val ACTION_NEXT = "io.github.mcello23.onda.widget.ACTION_NEXT"
        const val ACTION_PREV = "io.github.mcello23.onda.widget.ACTION_PREV"

        fun actionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, OndaWidgetProvider::class.java).apply { this.action = action }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        fun openAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        fun buildViews(
            context: Context,
            appWidgetId: Int,
            snap: WidgetUpdater.Snapshot,
            artBitmap: Bitmap?,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_onda)
            views.setTextViewText(R.id.widget_title, snap.title)
            views.setTextViewText(R.id.widget_subtitle, snap.subtitle)
            views.setImageViewResource(R.id.widget_play_btn, if (snap.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)

            if (artBitmap != null) {
                views.setImageViewBitmap(R.id.widget_art, artBitmap)
            } else {
                views.setImageViewResource(R.id.widget_art, R.drawable.ic_radio)
            }

            views.setOnClickPendingIntent(R.id.widget_header, openAppPendingIntent(context, 100))
            views.setOnClickPendingIntent(R.id.widget_play_btn, actionPendingIntent(context, ACTION_TOGGLE, 101))

            // Bind scrollable GridView of ALL favorite station icons
            val svcIntent = Intent(context, OndaWidgetGridService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            @Suppress("DEPRECATION")
            views.setRemoteAdapter(R.id.widget_fav_grid, svcIntent)
            views.setEmptyView(R.id.widget_fav_grid, R.id.widget_empty)

            // Explicit mutable PendingIntent template so GridView item fill-in extras merge cleanly on Android 12-15
            val itemTemplate = Intent(context, OndaWidgetProvider::class.java).apply {
                action = ACTION_PLAY_FAVORITE
            }
            val itemPending = PendingIntent.getBroadcast(
                context,
                200 + appWidgetId,
                itemTemplate,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            views.setPendingIntentTemplate(R.id.widget_fav_grid, itemPending)
            return views
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> forwardToService(context, PlaybackService.ACTION_WIDGET_TOGGLE)
            ACTION_NEXT -> forwardToService(context, PlaybackService.ACTION_WIDGET_NEXT)
            ACTION_PREV -> forwardToService(context, PlaybackService.ACTION_WIDGET_PREV)
            ACTION_PLAY_FAVORITE -> {
                val uuid = intent.getStringExtra(PlaybackService.EXTRA_STATION_UUID)
                if (!uuid.isNullOrBlank()) {
                    val svc = Intent(context, PlaybackService::class.java).apply {
                        action = PlaybackService.ACTION_WIDGET_PLAY_STATION
                        putExtra(PlaybackService.EXTRA_STATION_UUID, uuid)
                    }
                    ContextCompat.startForegroundService(context, svc)
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        StationRepo.init(context)
        WidgetUpdater.pushToLaunchers(context, refreshFavoritesGrid = true)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        WidgetUpdater.pushToLaunchers(context, refreshFavoritesGrid = true)
    }

    private fun forwardToService(context: Context, serviceAction: String) {
        val svc = Intent(context, PlaybackService::class.java).apply { action = serviceAction }
        ContextCompat.startForegroundService(context, svc)
    }
}
