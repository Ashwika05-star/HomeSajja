package com.homesajja.app.ui.theme

import androidx.compose.ui.graphics.Color

// HomeSajja brand palette — modern, elegant, premium. Light theme only for now;
// dark theme is a later phase.

// Primary — Cabernet: buttons, headings, navigation, important accents
val CabernetPrimary = Color(0xFF6D1F2B)
val OnCabernetPrimary = Color(0xFFFFFFFF)
val CabernetPrimaryContainer = Color(0xFFF3DCE0)
val OnCabernetPrimaryContainer = Color(0xFF4A121A)

// Secondary — Poignant Pink: secondary accents, highlights, selected states
val PoignantPinkSecondary = Color(0xFFE29AA8)
val OnPoignantPinkSecondary = Color(0xFF4A121A)
val PoignantPinkSecondaryContainer = Color(0xFFF8E3E7)
val OnPoignantPinkSecondaryContainer = Color(0xFF5C1E29)

// Tertiary — a deeper rose between Cabernet and Poignant Pink; the palette only
// specifies two accent colors, so this fills the M3 tertiary role without
// introducing an unrelated hue.
val RoseTertiary = Color(0xFFB5677A)
val OnRoseTertiary = Color(0xFFFFFFFF)
val RoseTertiaryContainer = Color(0xFFF8E3E7)
val OnRoseTertiaryContainer = Color(0xFF5C1E29)

// Background / surface — Mother of Pearl
val PearlBackground = Color(0xFFE8D4C8)
val OnPearlBackground = Color(0xFF2B211E)
val PearlSurface = Color(0xFFEFE1D8)
val OnPearlSurface = Color(0xFF2B211E)
val PearlSurfaceVariant = Color(0xFFDCC5B7)
val OnPearlSurfaceVariant = Color(0xFF5B4A42)

// Status semantics — a standard success/pending/error pattern for StatusBadge,
// kept separate from the three brand colors above since the palette doesn't
// define a "success" hue and reusing Cabernet for that would read as an error.
val SuccessContainer = Color(0xFFDCEAD9)
val OnSuccessContainer = Color(0xFF1E4620)

// Error
val ErrorRed = Color(0xFFB3261E)
val OnErrorRed = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFF9DEDC)
val OnErrorContainer = Color(0xFF410E0B)

// Outline
val OutlineNeutral = Color(0xFF8A7A72)
val OutlineVariantNeutral = Color(0xFFD9C7BB)

// Surface container family — used by menus, sheets and dialogs. Left unset,
// Material3 falls back to its baseline purple palette (visible e.g. in the
// AppDropdownField menu), so these are filled in as tonal steps of Mother of
// Pearl to keep every surface consistent with the brand palette.
val PearlSurfaceDim = Color(0xFFD9C4B7)
val PearlSurfaceBright = Color(0xFFFBF2EC)
val PearlSurfaceContainerLowest = Color(0xFFFFFBF8)
val PearlSurfaceContainerLow = Color(0xFFF3E7DE)
val PearlSurfaceContainer = Color(0xFFEFE1D8)
val PearlSurfaceContainerHigh = Color(0xFFE9D9CC)
val PearlSurfaceContainerHighest = Color(0xFFE3D0C0)

// Inverse roles — used by components like Snackbar
val InverseSurface = Color(0xFF362F2A)
val InverseOnSurface = Color(0xFFF5E9E0)
val InversePrimary = Color(0xFFF0A8B4)
