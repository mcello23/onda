package io.github.mcello23.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MonogramTest {

    @Test
    fun labelStripsFillerWordsAndKeepsShortNames() {
        assertEquals("BBC", Monogram.label("BBC Radio 1"))
        assertEquals("KEXP", Monogram.label("KEXP 90.3 FM"))
        assertEquals("89", Monogram.label("89 FM A Rádio Rock"))
        assertEquals("FIP", Monogram.label("Radio FIP"))
        assertEquals("SER", Monogram.label("Rádio SER"))
    }

    @Test
    fun labelUsesTwoInitialsForLongerNames() {
        assertEquals("CS", Monogram.label("Cadena SER"))
        assertEquals("P", Monogram.label("Radio Paradise"))
        assertEquals("VI", Monogram.label("Virgin Radio Italy"))
        assertEquals("AM", Monogram.label("Antena Music"))
    }

    @Test
    fun labelHandlesOnlyFillerWordsAndEmptyStrings() {
        assertEquals("FM", Monogram.label("FM"))
        assertEquals("♪", Monogram.label(""))
        assertEquals("♪", Monogram.label("   ---   "))
    }

    @Test
    fun labelHandlesWideScripts() {
        assertEquals("東京", Monogram.label("東京FM"))
        assertEquals("한국", Monogram.label("한국방송"))
    }

    @Test
    fun gradientIsDeterministicAndCaseInsensitive() {
        val a = Monogram.gradient("Rock FM")
        val b = Monogram.gradient("  rock fm  ")
        assertEquals(a, b)
        assertEquals(a.first, Monogram.color("Rock FM"))
        assertNotEquals(a.first, a.second)
    }
}
