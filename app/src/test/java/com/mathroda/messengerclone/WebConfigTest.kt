package com.mathroda.messengerclone

import com.mathroda.messengerclone.web.WebConfig
import com.mathroda.messengerclone.web.WebConfig.Navigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebConfigTest {

    @Test
    fun facebookPagesStayInsideTheWebView() {
        assertEquals(Navigation.Allow, WebConfig.classify("https://www.facebook.com/messages/t/123"))
        assertEquals(Navigation.Allow, WebConfig.classify("https://m.facebook.com/login/?next=x"))
        assertEquals(Navigation.Allow, WebConfig.classify("https://www.messenger.com/"))
        assertEquals(Navigation.Allow, WebConfig.classify("https://accountscenter.facebook.com/password_and_security"))
    }

    @Test
    fun lookAlikeHostsAreNotTreatedAsFacebook() {
        assertEquals(
            Navigation.External("https://facebook.com.evil.example/login"),
            WebConfig.classify("https://facebook.com.evil.example/login")
        )
        assertEquals(
            Navigation.External("https://notfacebook.com/"),
            WebConfig.classify("https://notfacebook.com/")
        )
        // user-info trick: the real host here is evil.example
        assertEquals(
            Navigation.External("https://www.facebook.com@evil.example/"),
            WebConfig.classify("https://www.facebook.com@evil.example/")
        )
    }

    @Test
    fun outsideLinksGoToACustomTab() {
        assertEquals(Navigation.External("https://example.org/page"), WebConfig.classify("https://example.org/page"))
        assertEquals(Navigation.External("http://example.org/"), WebConfig.classify("http://example.org/"))
        // Cleartext facebook.com is not loaded inside the app either.
        assertEquals(Navigation.External("http://www.facebook.com/"), WebConfig.classify("http://www.facebook.com/"))
    }

    @Test
    fun linkShimIsUnwrapped() {
        assertEquals(
            Navigation.External("https://example.org/a?b=1"),
            WebConfig.classify("https://l.facebook.com/l.php?u=https%3A%2F%2Fexample.org%2Fa%3Fb%3D1&h=AT0xyz")
        )
        // A shim that points at a non-web scheme is not followed; the shim page itself is a Facebook host.
        assertEquals(
            Navigation.Allow,
            WebConfig.classify("https://l.facebook.com/l.php?u=javascript%3Aalert(1)")
        )
    }

    @Test
    fun mailAndPhoneLinksUseTheMatchingApp() {
        assertEquals(Navigation.ViewIntent("mailto:a@b.example"), WebConfig.classify("mailto:a@b.example"))
        assertEquals(Navigation.ViewIntent("tel:+10000000"), WebConfig.classify("tel:+10000000"))
    }

    @Test
    fun otherSchemesAreBlocked() {
        listOf(
            "intent://scan/#Intent;scheme=zxing;end",
            "fb://profile/1",
            "market://details?id=com.facebook.orca",
            "javascript:alert(1)",
            "file:///sdcard/x.html",
            "data:text/html,hi",
            "not a url at all"
        ).forEach { assertEquals(it, Navigation.Block, WebConfig.classify(it)) }
    }

    @Test
    fun aboutBlankIsAllowed() {
        assertEquals(Navigation.Allow, WebConfig.classify("about:blank"))
    }

    @Test
    fun hostCheck() {
        assertTrue(WebConfig.isAllowedHost("facebook.com"))
        assertTrue(WebConfig.isAllowedHost("WWW.Facebook.com"))
        assertFalse(WebConfig.isAllowedHost("evilfacebook.com"))
        assertFalse(WebConfig.isAllowedHost(null))
    }

    @Test
    fun desktopUserAgentDropsTheMobileMarkers() {
        val mobile = "Mozilla/5.0 (Linux; Android 10; RMX1911) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36"
        val desktop = WebConfig.desktopUserAgent(mobile)
        assertFalse(desktop.contains("Android"))
        assertFalse(desktop.contains("Mobile"))
        assertTrue(desktop.contains("X11; Linux x86_64"))
        assertTrue(desktop.contains("Chrome/120.0.0.0"))
    }
}
