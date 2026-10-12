package com.homesajja.app.ui.theme

import androidx.compose.ui.graphics.Color

// HomeSajja palettes, taken from the Stitch design systems:
//   light: "Warm Artisanal Living" (warm linen canvas, terracotta burgundy, aged amber)
//   dark:  "Nocturnal Craft Atelier" (smoked walnut canvas, terracotta ember, muted brass)
// Where a Material role has no value in the design system, the nearest tonal step of the same family is used.

// ---------------------------------------------------------------------------------------------------------------------
// Light
// ---------------------------------------------------------------------------------------------------------------------

// Primary: terracotta burgundy (actions, selected states, price text)
val LightPrimary = Color(0xFF7A2021)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFDF2F2)
val LightOnPrimaryContainer = Color(0xFF7A2021)

// Secondary: aged amber / polished brass. Dark text sits on it, since white on amber is too faint to read.
val LightSecondary = Color(0xFFD97706)
val LightOnSecondary = Color(0xFF2C221E)
val LightSecondaryContainer = Color(0xFFFEF3C7)
val LightOnSecondaryContainer = Color(0xFF92400E)

// Tertiary: warm madder root
val LightTertiary = Color(0xFF9E2A2B)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFFBEB)
val LightOnTertiaryContainer = Color(0xFFB45309)

// Canvas and tonal layers (canvas, layer 1 cards, layer 2 elevated, hairlines)
val LightBackground = Color(0xFFFBF9F5)
val LightOnBackground = Color(0xFF2C221E)
// "surface" is the card layer (layer 1); "background" is the canvas. Screens not yet revamped still draw cards with surface.
val LightSurface = Color(0xFFF4EFEB)
val LightOnSurface = Color(0xFF2C221E)
val LightSurfaceVariant = Color(0xFFEDE6DD)
val LightOnSurfaceVariant = Color(0xFF5C4A42)
val LightOutline = Color(0xFF8C776D)
val LightOutlineVariant = Color(0xFFE6DFD5)
val LightSurfaceDim = Color(0xFFE6DFD5)
val LightSurfaceBright = Color(0xFFFBF9F5)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF4EFEB)
val LightSurfaceContainer = Color(0xFFF4EFEB)
val LightSurfaceContainerHigh = Color(0xFFEDE6DD)
val LightSurfaceContainerHighest = Color(0xFFE6DFD5)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF93000A)

val LightInverseSurface = Color(0xFF392E2A)
val LightInverseOnSurface = Color(0xFFFFEDE7)
val LightInversePrimary = Color(0xFFFFB3AE)

// ---------------------------------------------------------------------------------------------------------------------
// Dark
// ---------------------------------------------------------------------------------------------------------------------

// Primary: terracotta ember. The design system puts linen text on it; dark text is used instead because it is the one that
// reaches a readable contrast on this orange.
val DarkPrimary = Color(0xFFD96B50)
val DarkOnPrimary = Color(0xFF1E1815)
val DarkPrimaryContainer = Color(0xFF381414)
val DarkOnPrimaryContainer = Color(0xFFFFB4A2)

// Secondary: muted brass
val DarkSecondary = Color(0xFFD4A373)
val DarkOnSecondary = Color(0xFF1E1815)
val DarkSecondaryContainer = Color(0xFF33220A)
val DarkOnSecondaryContainer = Color(0xFFFBBF24)

val DarkTertiary = Color(0xFFE07A5F)
val DarkOnTertiary = Color(0xFF1E1815)
val DarkTertiaryContainer = Color(0xFF2D1E0C)
val DarkOnTertiaryContainer = Color(0xFFFCD34D)

val DarkBackground = Color(0xFF161210)
val DarkOnBackground = Color(0xFFF8F4EE)
// "surface" is the card layer (layer 1); "background" is the canvas.
val DarkSurface = Color(0xFF28201C)
val DarkOnSurface = Color(0xFFF8F4EE)
val DarkSurfaceVariant = Color(0xFF28201C)
val DarkOnSurfaceVariant = Color(0xFFA89B91)
val DarkOutline = Color(0xFF8C7E74)
val DarkOutlineVariant = Color(0xFF3B2F28)
val DarkSurfaceDim = Color(0xFF161210)
val DarkSurfaceBright = Color(0xFF3E322C)
val DarkSurfaceContainerLowest = Color(0xFF1E1815)
val DarkSurfaceContainerLow = Color(0xFF28201C)
val DarkSurfaceContainer = Color(0xFF322823)
val DarkSurfaceContainerHigh = Color(0xFF3E322C)
val DarkSurfaceContainerHighest = Color(0xFF473931)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkInverseSurface = Color(0xFFF5EFEB)
val DarkInverseOnSurface = Color(0xFF181412)
val DarkInversePrimary = Color(0xFF7A2021)

// ---------------------------------------------------------------------------------------------------------------------
// Status pills ("Agreed", "Quoted", "In progress", "Reserved"): a tinted background with a strong text colour, per theme.
// ---------------------------------------------------------------------------------------------------------------------

val StatusInProgressLightBg = Color(0xFFFEF3C7)
val StatusInProgressLightText = Color(0xFF92400E)
val StatusInProgressDarkBg = Color(0xFF33220A)
val StatusInProgressDarkText = Color(0xFFFBBF24)

val StatusAgreedLightBg = Color(0xFFECFDF5)
val StatusAgreedLightText = Color(0xFF065F46)
val StatusAgreedDarkBg = Color(0xFF0C2B1D)
val StatusAgreedDarkText = Color(0xFF34D399)

val StatusQuotedLightBg = Color(0xFFFFFBEB)
val StatusQuotedLightText = Color(0xFFB45309)
val StatusQuotedDarkBg = Color(0xFF2D1E0C)
val StatusQuotedDarkText = Color(0xFFFCD34D)

val StatusReservedLightBg = Color(0xFFFDF2F2)
val StatusReservedLightText = Color(0xFF7A2021)
val StatusReservedDarkBg = Color(0xFF381414)
val StatusReservedDarkText = Color(0xFFF87171)

/** The small amber dot that marks the selected destination in the bottom bar. */
val AmberDot = Color(0xFFD97706)
