package com.example.rimembranze.ui.theme

import androidx.compose.ui.graphics.Color

// Rimembranze's actual fixed dark/amber palette — same values used ad hoc
// across the screens (see ui/components/SharedComponents.kt). Centralized
// here so MaterialTheme's default-styled elements (dialogs, ripples, status
// bar contrast, etc.) match instead of falling back to the Compose template
// purple scheme or a wallpaper-derived Material You color.
val RimBackground     = Color(0xFF0F0F13)
val RimSurface        = Color(0xFF1A1A22)
val RimSurfaceElevated = Color(0xFF23232E)
val RimAmber          = Color(0xFFE8A020)
val RimOnAmber        = Color(0xFF1A1100) // matches the dark text used on amber buttons throughout the app
val RimBlue           = Color(0xFF5B8DEF)
val RimGreen          = Color(0xFF5BEF9A)
val RimError          = Color(0xFFE05858)
val RimTextPrimary    = Color(0xFFF0EEE8)
val RimTextSecondary  = Color(0xFF8A8898)
val RimOutline        = Color(0xFF2C2C3A)