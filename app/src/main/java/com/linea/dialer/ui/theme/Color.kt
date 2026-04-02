package com.linea.dialer.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand Gradient ──────────────────────────────────────────────────────────
val BrandOrange = Color(0xFFFF7D31)
val BrandRed    = Color(0xFFFE3E4D)

// Gradient pair used throughout the app
val GradientStart = BrandOrange
val GradientEnd   = BrandRed

// ── Dark Theme Surfaces ─────────────────────────────────────────────────────
val Dark900  = Color(0xFF080808) // deepest background
val Dark800  = Color(0xFF0F0F0F) // scaffold background
val Dark700  = Color(0xFF161616) // card / bottom sheet
val Dark600  = Color(0xFF1E1E1E) // elevated surface
val Dark500  = Color(0xFF252525) // input / pressed state
val Dark400  = Color(0xFF2E2E2E) // dividers / borders
val Dark300  = Color(0xFF3A3A3A) // subtle border
val Dark200  = Color(0xFF555555) // disabled text
val Dark100  = Color(0xFF888888) // secondary text
val Dark050  = Color(0xFFBBBBBB) // tertiary text

// ── Light Theme Surfaces ────────────────────────────────────────────────────
val Light900  = Color(0xFFFFFFFF) // scaffold
val Light800  = Color(0xFFF7F7F8) // card
val Light700  = Color(0xFFEEEEF0) // elevated surface
val Light600  = Color(0xFFE4E4E7) // input
val Light500  = Color(0xFFD4D4D8) // divider
val Light400  = Color(0xFFA1A1AA) // border
val Light300  = Color(0xFF71717A) // disabled
val Light200  = Color(0xFF52525B) // secondary
val Light100  = Color(0xFF27272A) // primary text

// ── Semantic ────────────────────────────────────────────────────────────────
val CallIncoming  = Color(0xFF34D399) // green
val CallOutgoing  = Color(0xFF60A5FA) // blue
val CallMissed    = Color(0xFFFE3E4D) // brand red (missed = brand accent)
val CallActive    = Color(0xFF34D399)

// Tag palette
val TagClient   = Color(0xFF34D399)
val TagLead     = Color(0xFF60A5FA)
val TagPartner  = Color(0xFFC084FC)
val TagSupplier = Color(0xFFFBBF24)
val TagPersonal = Color(0xFF22D3EE)
val TagUnknown  = Color(0xFF6B7280)

// Gradient overlay for onboarding cards
val GradientCardStart = Color(0xFFFF7D31).copy(alpha = 0.12f)
val GradientCardEnd   = Color(0xFFFE3E4D).copy(alpha = 0.05f)
