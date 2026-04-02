package com.linea.dialer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = BrandOrange,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF3D1A00),
    onPrimaryContainer = Color(0xFFFFD0B5),

    secondary        = BrandRed,
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFF3D0008),
    onSecondaryContainer = Color(0xFFFFD2D4),

    tertiary         = CallIncoming,
    onTertiary       = Color.Black,

    background       = Dark800,
    onBackground     = Color.White,

    surface          = Dark700,
    onSurface        = Color.White,
    surfaceVariant   = Dark600,
    onSurfaceVariant = Dark050,

    outline          = Dark400,
    outlineVariant   = Dark300,

    error            = CallMissed,
    onError          = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary          = BrandOrange,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFFFE8D6),
    onPrimaryContainer = Color(0xFF3D1A00),

    secondary        = BrandRed,
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFFFFDFE1),
    onSecondaryContainer = Color(0xFF3D0008),

    tertiary         = Color(0xFF0F9E68),
    onTertiary       = Color.White,

    background       = Light900,
    onBackground     = Light100,

    surface          = Light900,
    onSurface        = Light100,
    surfaceVariant   = Light800,
    onSurfaceVariant = Light200,

    outline          = Light500,
    outlineVariant   = Light600,

    error            = BrandRed,
    onError          = Color.White,
)

@Composable
fun LineaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor     = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = LineaTypography,
        content     = content,
    )
}
