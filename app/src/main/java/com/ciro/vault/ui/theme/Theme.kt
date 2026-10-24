package com.ciro.vault.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0D1117)
val SurfaceDark = Color(0xFF161B22)
val SurfaceBorder = Color(0xFF30363D)
val AccentCyan = Color(0xFF58A6FF)
val AccentGreen = Color(0xFF3FB950)
val AccentOrange = Color(0xFFF0883E)
val TextPrimary = Color(0xFFF0F6FC)
val TextSecondary = Color(0xFF8B949E)

val CiroVaultColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    background = BgDark,
    onBackground = TextPrimary,
    outline = SurfaceBorder
)
