package io.github.mcello23.onda.playback

import android.app.Application
import android.net.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ArtworkProviderTest {

    @Test
    fun signedStationAndCategoryUrisVerify() {
        val stationUri = ArtworkProvider.uriFor(
            favicon = "https://example.com/logo.png",
            name = "Rock FM",
            fallbackUrl = "https://www.google.com/s2/favicons?domain=example.com&sz=256",
        )
        assertTrue(ArtworkProvider.hasValidSignature(stationUri))

        val catUri = ArtworkProvider.categoryUri("ES", "SPAIN", 0xFFC60B1E.toInt(), 0xFF7F1D1D.toInt())
        assertTrue(ArtworkProvider.hasValidSignature(catUri))
    }

    @Test
    fun unsignedOrTamperedUrisAreRejected() {
        val unsigned = Uri.parse("content://io.github.mcello23.onda.art/img?n=Test&u=https://evil.example/a.png")
        assertFalse(ArtworkProvider.hasValidSignature(unsigned))

        val valid = ArtworkProvider.uriFor("https://example.com/logo.png", "Test")
        val sig = valid.getQueryParameter("sig")!!
        val tampered = Uri.Builder()
            .scheme("content")
            .authority("io.github.mcello23.onda.art")
            .appendPath("img")
            .appendQueryParameter("n", "Test")
            .appendQueryParameter("u", "https://attacker.example/other.png")
            .appendQueryParameter("sig", sig)
            .build()
        assertFalse(ArtworkProvider.hasValidSignature(tampered))
    }

    @Test
    fun nonPublicIpAddressesAreBlocked() {
        assertFalse(ArtworkProvider.isPublicAddress(InetAddress.getByName("127.0.0.1")))
        assertFalse(ArtworkProvider.isPublicAddress(InetAddress.getByName("10.0.0.1")))
        assertFalse(ArtworkProvider.isPublicAddress(InetAddress.getByName("192.168.1.10")))
        assertFalse(ArtworkProvider.isPublicAddress(InetAddress.getByName("169.254.169.254")))
        assertFalse(ArtworkProvider.isPublicAddress(InetAddress.getByName("100.64.0.1")))
        assertFalse(ArtworkProvider.isPublicAddress(InetAddress.getByName("::1")))
        assertFalse(ArtworkProvider.isPublicAddress(InetAddress.getByName("fc00::1")))
        assertTrue(ArtworkProvider.isPublicAddress(InetAddress.getByName("8.8.8.8")))
    }

    @Test
    fun localhostAndNonHttpUrlsAreBlocked() {
        assertFalse(ArtworkProvider.isSafePublicUrl(URL("http://localhost:8080/test.png")))
        assertFalse(ArtworkProvider.isSafePublicUrl(URL("http://127.0.0.1/test.png")))
        assertFalse(ArtworkProvider.isSafePublicUrl(URL("file:///etc/hosts")))
    }
}
