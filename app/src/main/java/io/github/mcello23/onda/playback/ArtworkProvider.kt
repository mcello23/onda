package io.github.mcello23.onda.playback

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.caverock.androidsvg.SVG
import io.github.mcello23.onda.BuildConfig
import io.github.mcello23.onda.data.Monogram
import io.github.mcello23.onda.data.RadioBrowserApi
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class ArtworkProvider : ContentProvider() {

    companion object {
        private const val MAX_BYTES = 2 * 1024 * 1024
        private const val MAX_SIZE = 512
        private const val PERMANENT_TTL_MS = 24 * 3600_000L
        private const val TRANSIENT_TTL_MS = 60_000L
        private const val MIN_LOGO_SIZE = 48
        private const val VERSION = "v5"

        fun uriFor(favicon: String, name: String, fallbackUrl: String = ""): Uri {
            val src = favicon.trim()
            val fb = fallbackUrl.trim()
            val builder = Uri.Builder()
                .scheme("content")
                .authority("${BuildConfig.APPLICATION_ID}.art")
                .appendPath("img")
                .appendQueryParameter("n", name)
            if (isHttp(src)) builder.appendQueryParameter("u", src)
            if (isHttp(fb) && fb != src) builder.appendQueryParameter("f", fb)
            return builder.build()
        }

        /**
         * Generates a crisp 512x512 gradient category tile for Android Auto folders
         * (e.g. Spain, Brazil, World Rock, Jazz, etc.).
         */
        fun categoryUri(badge: String, title: String, colorStart: Int, colorEnd: Int): Uri =
            Uri.Builder()
                .scheme("content")
                .authority("${BuildConfig.APPLICATION_ID}.art")
                .appendPath("cat")
                .appendQueryParameter("b", badge)
                .appendQueryParameter("t", title)
                .appendQueryParameter("s", colorStart.toString())
                .appendQueryParameter("e", colorEnd.toString())
                .build()

        fun resourceUri(context: Context, drawableResId: Int): Uri =
            Uri.parse("android.resource://${context.packageName}/$drawableResId")

        /**
         * Decodes a rounded-square bitmap from ArtworkProvider for Home Screen Widgets,
         * kept small (~128px) to stay well within RemoteViews Binder limits.
         */
        fun loadWidgetBitmap(context: Context, uri: Uri, sizePx: Int = 128, cornerPx: Float = 26f): Bitmap? =
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.use { input ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = (MAX_SIZE / sizePx).coerceAtLeast(1) }
                    BitmapFactory.decodeStream(input, null, opts)
                } ?: return@runCatching null
                val scaled = Bitmap.createScaledBitmap(raw, sizePx, sizePx, true)
                if (scaled !== raw) raw.recycle()
                val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(out)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val rect = Rect(0, 0, sizePx, sizePx)
                val rectF = RectF(rect)
                canvas.drawRoundRect(rectF, cornerPx, cornerPx, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(scaled, rect, rect, paint)
                scaled.recycle()
                out
            }.getOrNull()

        private fun isHttp(s: String) = s.startsWith("http://") || s.startsWith("https://")
    }

    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val dir = File(context!!.cacheDir, "art").apply { mkdirs() }
        if (uri.pathSegments.firstOrNull() == "cat") {
            val badge = uri.getQueryParameter("b").orEmpty()
            val title = uri.getQueryParameter("t").orEmpty()
            val start = uri.getQueryParameter("s")?.toIntOrNull() ?: 0xFF006399.toInt()
            val end = uri.getQueryParameter("e")?.toIntOrNull() ?: 0xFF001D32.toInt()
            val file = categoryTile(dir, badge, title, start, end)
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        val name = uri.getQueryParameter("n").orEmpty()
        val src = uri.getQueryParameter("u")?.takeIf(::isHttp)
        val fallback = uri.getQueryParameter("f")?.takeIf(::isHttp)
        val file = src?.let { logo(dir, it) }
            ?: fallback?.let { logo(dir, it) }
            ?: monogram(dir, name)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun categoryTile(dir: File, badge: String, title: String, startColor: Int, endColor: Int): File {
        val key = sha1("$VERSION:cat:$badge:$title:$startColor:$endColor")
        val file = File(dir, "$key.png")
        if (file.exists()) return file
        val bitmap = Bitmap.createBitmap(MAX_SIZE, MAX_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, MAX_SIZE.toFloat(), MAX_SIZE.toFloat(), startColor, endColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, MAX_SIZE.toFloat(), MAX_SIZE.toFloat(), bgPaint)

        val decorLight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(34, 255, 255, 255) }
        val decorDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(46, 0, 0, 0) }
        canvas.drawCircle(MAX_SIZE * 0.84f, MAX_SIZE * 0.16f, MAX_SIZE * 0.36f, decorLight)
        canvas.drawCircle(MAX_SIZE * 0.15f, MAX_SIZE * 0.86f, MAX_SIZE * 0.30f, decorDark)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = MAX_SIZE * if (badge.length <= 2) 0.34f else 0.24f
        }
        val bw = badgePaint.measureText(badge)
        if (bw > MAX_SIZE * 0.78f) badgePaint.textSize *= (MAX_SIZE * 0.78f) / bw
        canvas.drawText(badge, MAX_SIZE / 2f, MAX_SIZE * 0.50f, badgePaint)

        if (title.isNotBlank()) {
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(235, 255, 255, 255)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                textSize = MAX_SIZE * 0.105f
            }
            val tw = titlePaint.measureText(title)
            if (tw > MAX_SIZE * 0.84f) titlePaint.textSize *= (MAX_SIZE * 0.84f) / tw
            canvas.drawText(title, MAX_SIZE / 2f, MAX_SIZE * 0.76f, titlePaint)
        }
        return write(dir, key, file, encode(bitmap))
    }

    private fun logo(dir: File, src: String): File? {
        val key = sha1("$VERSION:$src")
        val file = File(dir, "$key.png")
        if (file.exists()) return file
        val bad = File(dir, "$key.bad")
        if (bad.exists()) {
            val ttl = if (bad.readText() == "p") PERMANENT_TTL_MS else TRANSIENT_TTL_MS
            if (System.currentTimeMillis() - bad.lastModified() < ttl) return null
        }
        return try {
            val png = toPng(download(src)) ?: throw PermanentFailure("Undecodable or tiny image")
            write(dir, key, file, png)
        } catch (_: PermanentFailure) {
            bad.writeText("p")
            null
        } catch (_: IOException) {
            bad.writeText("t")
            null
        }
    }

    private fun monogram(dir: File, name: String): File {
        val key = sha1("$VERSION:tile:$name")
        val file = File(dir, "$key.png")
        if (file.exists()) return file
        val bitmap = Bitmap.createBitmap(MAX_SIZE, MAX_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val (startColor, endColor) = Monogram.gradient(name)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, MAX_SIZE.toFloat(), MAX_SIZE.toFloat(), startColor, endColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, MAX_SIZE.toFloat(), MAX_SIZE.toFloat(), bgPaint)

        val decorLight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(30, 255, 255, 255) }
        val decorDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(45, 0, 0, 0) }
        canvas.drawCircle(MAX_SIZE * 0.84f, MAX_SIZE * 0.16f, MAX_SIZE * 0.34f, decorLight)
        canvas.drawCircle(MAX_SIZE * 0.14f, MAX_SIZE * 0.88f, MAX_SIZE * 0.28f, decorDark)

        val label = Monogram.label(name)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = MAX_SIZE * if (label.length <= 2) 0.40f else 0.28f
        }
        val width = paint.measureText(label)
        val maxWidth = MAX_SIZE * 0.80f
        if (width > maxWidth) paint.textSize *= maxWidth / width
        val y = MAX_SIZE / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(label, MAX_SIZE / 2f, y, paint)
        return write(dir, key, file, encode(bitmap))
    }

    private fun write(dir: File, key: String, file: File, png: ByteArray): File {
        val tmp = File.createTempFile(key, ".tmp", dir)
        tmp.writeBytes(png)
        if (!tmp.renameTo(file)) {
            tmp.delete()
            if (!file.exists()) file.writeBytes(png)
        }
        return file
    }

    private fun download(src: String): ByteArray {
        var url = URL(src)
        repeat(5) {
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 6_000
                conn.readTimeout = 8_000
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", RadioBrowserApi.USER_AGENT)
                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location") ?: throw PermanentFailure("Redirect without location")
                    url = URL(url, location)
                    if (url.protocol != "http" && url.protocol != "https") throw PermanentFailure("Bad redirect")
                    return@repeat
                }
                if (code in 400..499 && code != 408 && code != 429) throw PermanentFailure("HTTP $code")
                if (code !in 200..299) throw IOException("HTTP $code")
                conn.inputStream.use { input ->
                    val out = ByteArrayOutputStream()
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        if (out.size() > MAX_BYTES) throw PermanentFailure("Image too large")
                    }
                    return out.toByteArray()
                }
            } finally {
                conn.disconnect()
            }
        }
        throw PermanentFailure("Too many redirects")
    }

    private fun toPng(bytes: ByteArray): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return svgToPng(bytes)
        if (bounds.outWidth < MIN_LOGO_SIZE && bounds.outHeight < MIN_LOGO_SIZE) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIZE && bounds.outHeight / (sample * 2) >= MAX_SIZE) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        return encode(onTile(bitmap))
    }

    private fun onTile(logo: Bitmap): Bitmap {
        val tile = Bitmap.createBitmap(MAX_SIZE, MAX_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tile)
        canvas.drawColor(Color.WHITE)
        val inner = MAX_SIZE * 0.86f
        val scale = minOf(inner / logo.width, inner / logo.height)
        val w = logo.width * scale
        val h = logo.height * scale
        val left = (MAX_SIZE - w) / 2f
        val top = (MAX_SIZE - h) / 2f
        canvas.drawBitmap(logo, null, RectF(left, top, left + w, top + h), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        logo.recycle()
        return tile
    }

    private fun encode(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun svgToPng(bytes: ByteArray): ByteArray? = try {
        val svg = SVG.getFromInputStream(bytes.inputStream())
        if (svg.documentViewBox == null && svg.documentWidth > 0 && svg.documentHeight > 0) {
            svg.setDocumentViewBox(0f, 0f, svg.documentWidth, svg.documentHeight)
        }
        svg.setDocumentWidth("100%")
        svg.setDocumentHeight("100%")
        val bitmap = Bitmap.createBitmap(MAX_SIZE, MAX_SIZE, Bitmap.Config.ARGB_8888)
        svg.renderToCanvas(Canvas(bitmap))
        encode(onTile(bitmap))
    } catch (_: Exception) {
        null
    }

    private class PermanentFailure(message: String) : IOException(message)

    private fun sha1(s: String) = MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    override fun getType(uri: Uri) = "image/png"
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
