package com.example.rimembranze.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Rimembranze has a single fixed dark/amber look — every screen already
// hardcodes these same colors (see ui/components/SharedComponents.kt) rather
// than reading MaterialTheme.colorScheme. This scheme exists so that
// default-styled M3 elements not explicitly overridden (dialog chrome,
// ripples, selection handles, edge-to-edge status bar contrast) match the
// app's real look instead of falling back to the Compose template purple
// scheme, or — worse — a wallpaper-derived Material You color that clashes
// with the fixed brand.
private val RimembranzeDarkColorScheme = darkColorScheme(
    primary        = RimAmber,
    onPrimary      = RimOnAmber,
    secondary      = RimBlue,
    onSecondary    = RimTextPrimary,
    tertiary       = RimGreen,
    onTertiary     = RimOnAmber,
    background     = RimBackground,
    onBackground   = RimTextPrimary,
    surface        = RimSurface,
    onSurface      = RimTextPrimary,
    surfaceVariant = RimSurfaceElevated,
    onSurfaceVariant = RimTextSecondary,
    error          = RimError,
    onError        = RimTextPrimary,
    outline        = RimOutline
)

@Composable
fun RimembranzeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RimembranzeDarkColorScheme,
        typography = Typography,
        content = content
    )
}