package com.homesajja.app.ui

import androidx.compose.ui.graphics.Color
import com.homesajja.app.ui.theme.CabernetPrimary
import com.homesajja.app.ui.theme.CabernetPrimaryContainer
import com.homesajja.app.ui.theme.ErrorContainer
import com.homesajja.app.ui.theme.ErrorRed
import com.homesajja.app.ui.theme.OnCabernetPrimary
import com.homesajja.app.ui.theme.OnCabernetPrimaryContainer
import com.homesajja.app.ui.theme.OnErrorContainer
import com.homesajja.app.ui.theme.OnPearlBackground
import com.homesajja.app.ui.theme.OnPearlSurface
import com.homesajja.app.ui.theme.OnPearlSurfaceVariant
import com.homesajja.app.ui.theme.OnPoignantPinkSecondaryContainer
import com.homesajja.app.ui.theme.OnSuccessContainer
import com.homesajja.app.ui.theme.OutlineNeutral
import com.homesajja.app.ui.theme.PearlBackground
import com.homesajja.app.ui.theme.PearlSurface
import com.homesajja.app.ui.theme.PearlSurfaceVariant
import com.homesajja.app.ui.theme.PoignantPinkSecondaryContainer
import com.homesajja.app.ui.theme.RoseTertiary
import com.homesajja.app.ui.theme.SuccessContainer
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG contrast checks for the colour pairs the app actually puts on top of each other. Text needs 4.5:1, borders and icons 3:1. */
class ContrastTest {

    private fun channel(value: Float): Double = value.toDouble().let { if (it <= 0.03928) it / 12.92 else Math.pow((it + 0.055) / 1.055, 2.4) }

    private fun luminance(c: Color): Double = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertText(name: String, foreground: Color, background: Color) =
        assertTrue("$name is only %.1f:1 (needs 4.5:1)".format(ratio(foreground, background)), ratio(foreground, background) >= 4.5)

    private fun assertGraphic(name: String, foreground: Color, background: Color) =
        assertTrue("$name is only %.1f:1 (needs 3:1)".format(ratio(foreground, background)), ratio(foreground, background) >= 3.0)

    @Test
    fun bodyAndSecondaryText_areReadableOnTheWarmBackgrounds() {
        assertText("body on background", OnPearlBackground, PearlBackground)
        assertText("body on cards", OnPearlSurface, PearlSurface)
        assertText("secondary on background", OnPearlSurfaceVariant, PearlBackground)
        assertText("secondary on cards", OnPearlSurfaceVariant, PearlSurface)
        assertText("secondary on variant surfaces", OnPearlSurfaceVariant, PearlSurfaceVariant)
    }

    @Test
    fun brandColourText_isReadable() {
        assertText("cabernet on background", CabernetPrimary, PearlBackground)
        assertText("cabernet on cards", CabernetPrimary, PearlSurface)
        assertText("cabernet on its container", CabernetPrimary, CabernetPrimaryContainer)
        assertText("button label on cabernet", OnCabernetPrimary, CabernetPrimary)
        assertText("text on primary container", OnCabernetPrimaryContainer, CabernetPrimaryContainer)
    }

    @Test
    fun statusColours_areReadable() {
        assertText("status badge", OnPoignantPinkSecondaryContainer, PoignantPinkSecondaryContainer)
        assertText("success badge", OnSuccessContainer, SuccessContainer)
        assertText("error text on background", ErrorRed, PearlBackground)
        assertText("error banner", OnErrorContainer, ErrorContainer)
    }

    @Test
    fun bordersAndAccents_meetTheGraphicMinimum() {
        assertGraphic("field borders on background", OutlineNeutral, PearlBackground)
        assertGraphic("field borders on cards", OutlineNeutral, PearlSurface)
        assertGraphic("accent on background", RoseTertiary, PearlBackground)
    }
}
