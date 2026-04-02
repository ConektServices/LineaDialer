package com.linea.dialer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.linea.dialer.R

// ── Font Family ─────────────────────────────────────────────────────────────
// Add these font files to res/font/:
//   plus_jakarta_sans_light.ttf      (300)
//   plus_jakarta_sans_regular.ttf    (400)
//   plus_jakarta_sans_medium.ttf     (500)
//   plus_jakarta_sans_semibold.ttf   (600)
//   plus_jakarta_sans_bold.ttf       (700)
//   plus_jakarta_sans_extrabold.ttf  (800)
// Download from: https://fonts.google.com/specimen/Plus+Jakarta+Sans
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_light,     FontWeight.Light),
    Font(R.font.plus_jakarta_sans_regular,   FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium,    FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold,  FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold,      FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

val LineaTypography = Typography(
    // Hero / screen titles
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.ExtraBold,
        fontSize    = 48.sp,
        lineHeight  = 52.sp,
        letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.Bold,
        fontSize    = 36.sp,
        lineHeight  = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.Bold,
        fontSize    = 28.sp,
        lineHeight  = 34.sp,
    ),
    // Screen headings
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.ExtraBold,
        fontSize    = 32.sp,
        lineHeight  = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.Bold,
        fontSize    = 24.sp,
        lineHeight  = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = 20.sp,
        lineHeight  = 26.sp,
    ),
    // Titles (nav, section labels)
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = 18.sp,
        lineHeight  = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = 15.sp,
        lineHeight  = 20.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.Medium,
        fontSize    = 13.sp,
        lineHeight  = 18.sp,
        letterSpacing = 0.1.sp
    ),
    // Body
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.Normal,
        fontSize    = 16.sp,
        lineHeight  = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.Normal,
        fontSize    = 14.sp,
        lineHeight  = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.Normal,
        fontSize    = 12.sp,
        lineHeight  = 16.sp,
    ),
    // Labels (tags, chips, caps)
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.Medium,
        fontSize    = 13.sp,
        lineHeight  = 18.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.Medium,
        fontSize    = 11.sp,
        lineHeight  = 14.sp,
        letterSpacing = 0.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight  = FontWeight.Medium,
        fontSize    = 9.sp,
        lineHeight  = 12.sp,
        letterSpacing = 1.sp
    ),
)
