package io.github.mcello23.onda.data

import java.text.Normalizer
import java.util.Locale

object Monogram {
    // Rich gradient pairs (start, end) for polished artwork fallback tiles
    private val gradients = arrayOf(
        0xFF0E7490.toInt() to 0xFF164E63.toInt(),
        0xFFBE185D.toInt() to 0xFF500724.toInt(),
        0xFF15803D.toInt() to 0xFF052E16.toInt(),
        0xFF6D28D9.toInt() to 0xFF2E1065.toInt(),
        0xFFC2410C.toInt() to 0xFF431407.toInt(),
        0xFF0F766E.toInt() to 0xFF042F2E.toInt(),
        0xFF1D4ED8.toInt() to 0xFF172554.toInt(),
        0xFFB45309.toInt() to 0xFF451A03.toInt(),
        0xFF4338CA.toInt() to 0xFF1E1B4B.toInt(),
        0xFF9F1239.toInt() to 0xFF4C0519.toInt(),
    )

    private val filler = setOf("radio", "rádio", "fm", "am", "the", "web", "online", "la", "el", "de", "da", "do")

    fun label(name: String): String {
        val words = Normalizer.normalize(name, Normalizer.Form.NFC)
            .split(Regex("[^\\p{L}\\p{M}\\p{N}]+"))
            .filter { it.isNotBlank() }
        val meaningful = words.filterNot { it.lowercase(Locale.ROOT) in filler }.ifEmpty { words }
        val first = meaningful.firstOrNull() ?: return "♪"
        if (isWideScript(first)) return codePoints(first, 2)
        if (first.codePointCount(0, first.length) <= 4) return first.uppercase(Locale.ROOT)
        return meaningful.take(2).joinToString("") { codePoints(it, 1) }.uppercase(Locale.ROOT)
    }

    private fun codePoints(s: String, n: Int) =
        s.substring(0, s.offsetByCodePoints(0, minOf(n, s.codePointCount(0, s.length))))

    private fun isWideScript(s: String) = s.codePoints().anyMatch {
        Character.UnicodeScript.of(it) in setOf(
            Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA, Character.UnicodeScript.HANGUL,
        )
    }

    fun gradient(name: String): Pair<Int, Int> =
        gradients[Math.floorMod(name.trim().lowercase(Locale.ROOT).hashCode(), gradients.size)]

    fun color(name: String): Int = gradient(name).first
}
