package com.homesajja.app.ui

import androidx.compose.ui.graphics.Color
import com.homesajja.app.ui.theme.DarkBackground
import com.homesajja.app.ui.theme.DarkError
import com.homesajja.app.ui.theme.DarkOnBackground
import com.homesajja.app.ui.theme.DarkOnError
import com.homesajja.app.ui.theme.DarkOnPrimary
import com.homesajja.app.ui.theme.DarkOnSurface
import com.homesajja.app.ui.theme.DarkOnSurfaceVariant
import com.homesajja.app.ui.theme.DarkOutline
import com.homesajja.app.ui.theme.DarkPrimary
import com.homesajja.app.ui.theme.DarkSurface
import com.homesajja.app.ui.theme.LightBackground
import com.homesajja.app.ui.theme.LightError
import com.homesajja.app.ui.theme.LightErrorContainer
import com.homesajja.app.ui.theme.LightOnBackground
import com.homesajja.app.ui.theme.LightOnError
import com.homesajja.app.ui.theme.LightOnErrorContainer
import com.homesajja.app.ui.theme.LightOnPrimary
import com.homesajja.app.ui.theme.LightOnSurface
import com.homesajja.app.ui.theme.LightOnSurfaceVariant
import com.homesajja.app.ui.theme.LightOutline
import com.homesajja.app.ui.theme.LightPrimary
import com.homesajja.app.ui.theme.LightSurface
import com.homesajja.app.ui.theme.StatusAgreedDarkBg
import com.homesajja.app.ui.theme.StatusAgreedDarkText
import com.homesajja.app.ui.theme.StatusAgreedLightBg
import com.homesajja.app.ui.theme.StatusAgreedLightText
import com.homesajja.app.ui.theme.StatusInProgressDarkBg
import com.homesajja.app.ui.theme.StatusInProgressDarkText
import com.homesajja.app.ui.theme.StatusInProgressLightBg
import com.homesajja.app.ui.theme.StatusInProgressLightText
import com.homesajja.app.ui.theme.StatusQuotedDarkBg
import com.homesajja.app.ui.theme.StatusQuotedDarkText
import com.homesajja.app.ui.theme.StatusQuotedLightBg
import com.homesajja.app.ui.theme.StatusQuotedLightText
import com.homesajja.app.ui.theme.StatusReservedDarkBg
import com.homesajja.app.ui.theme.StatusReservedDarkText
import com.homesajja.app.ui.theme.StatusReservedLightBg
import com.homesajja.app.ui.theme.StatusReservedLightText
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG contrast checks for the Stitch colour pairs the app actually puts on top of each other. Text needs 4.5:1, borders and icons 3:1. */
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
    fun lightTheme_textAndBrandContrast() {
        assertText("body on background", LightOnBackground, LightBackground)
        assertText("body on cards", LightOnSurface, LightSurface)
        assertText("secondary on background", LightOnSurfaceVariant, LightBackground)
        assertText("primary terracotta on background", LightPrimary, LightBackground)
        assertText("button label on primary", LightOnPrimary, LightPrimary)
        assertText("error text on error", LightOnError, LightError)
        assertText("error text on error container", LightOnErrorContainer, LightErrorContainer)
    }

    @Test
    fun darkTheme_textAndBrandContrast() {
        assertText("body on dark background", DarkOnBackground, DarkBackground)
        assertText("body on dark surface", DarkOnSurface, DarkSurface)
        assertText("secondary on dark background", DarkOnSurfaceVariant, DarkBackground)
        assertText("dark text on ember primary", DarkOnPrimary, DarkPrimary)
        assertText("error on dark error", DarkOnError, DarkError)
    }

    @Test
    fun statusColours_areReadableAcrossBothThemes() {
        assertText("status agreed light", StatusAgreedLightText, StatusAgreedLightBg)
        assertText("status in-progress light", StatusInProgressLightText, StatusInProgressLightBg)
        assertText("status quoted light", StatusQuotedLightText, StatusQuotedLightBg)
        assertText("status reserved light", StatusReservedLightText, StatusReservedLightBg)

        assertText("status agreed dark", StatusAgreedDarkText, StatusAgreedDarkBg)
        assertText("status in-progress dark", StatusInProgressDarkText, StatusInProgressDarkBg)
        assertText("status quoted dark", StatusQuotedDarkText, StatusQuotedDarkBg)
        assertText("status reserved dark", StatusReservedDarkText, StatusReservedDarkBg)
    }

    @Test
    fun bordersAndOutlines_meetGraphicMinimum() {
        assertGraphic("light outline on canvas", LightOutline, LightBackground)
        assertGraphic("dark outline on dark canvas", DarkOutline, DarkBackground)
    }
}
