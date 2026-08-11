package com.marc.gymplan100.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Colores de marca (degradado del logo): ámbar -> naranja -> magenta.
val BrandAmber = Color(0xFFFFB24D)
val BrandOrange = Color(0xFFFF6A3D)
val BrandMagenta = Color(0xFFFF2E6E)

// --- Tema claro ---
val LightPrimary = Color(0xFFD23A1C)
val OnLightPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFDACE)
val OnLightPrimaryContainer = Color(0xFF3C0D02)

val LightSecondary = Color(0xFFD81B60)
val OnLightSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFD9E2)
val OnLightSecondaryContainer = Color(0xFF3E0418)

val LightBackground = Color(0xFFFBF6F4)
val OnLightBackground = Color(0xFF221A17)
val LightSurface = Color(0xFFFFFFFF)
val OnLightSurface = Color(0xFF221A17)
val LightSurfaceVariant = Color(0xFFF1E7E2)
val OnLightSurfaceVariant = Color(0xFF58504B)

// --- Tema oscuro (negro premium del logo) ---
val DarkPrimary = Color(0xFFFF6A4D)
val OnDarkPrimary = Color(0xFF44100A)
val DarkPrimaryContainer = Color(0xFF8A2616)
val OnDarkPrimaryContainer = Color(0xFFFFDAD0)

val DarkSecondary = Color(0xFFFF5E8A)
val OnDarkSecondary = Color(0xFF45041E)
val DarkSecondaryContainer = Color(0xFF6E1334)
val OnDarkSecondaryContainer = Color(0xFFFFD9E2)

val DarkBackground = Color(0xFF0A0708)
val OnDarkBackground = Color(0xFFF2EDEA)
val DarkSurface = Color(0xFF161013)
val OnDarkSurface = Color(0xFFF2EDEA)
val DarkSurfaceVariant = Color(0xFF2A2024)
val OnDarkSurfaceVariant = Color(0xFFCBBFB9)

// --- Tokens de estado ---------------------------------------------------------
// M3 no tiene un rol para "esto es calentamiento" o "esto es descanso", y esos significados
// los necesitan las pantallas de planes y el editor. Van aparte, por CompositionLocal.

/** Versión "tinta" del degradado para fondos claros. */
val InkAmber = Color(0xFFC87A16)
val InkOrange = Color(0xFFD4451A)
val InkMagenta = Color(0xFFB3134C)

val BrandGradient = Brush.linearGradient(listOf(BrandAmber, BrandOrange, BrandMagenta))
val InkGradient = Brush.linearGradient(listOf(InkAmber, InkOrange, InkMagenta))

@Immutable
data class AppColors(
    val warmup: Color,        // ámbar · preparas
    val work: Color,          // naranja · trabajas
    val rest: Color,          // magenta · esperas
    val streak: Color,
    val special: Color,
    val positive: Color,
    val warmupSurface: Color,
    val workSurface: Color,
    val restSurface: Color,
    val brandGradient: Brush,
    val surface1: Color,
    val surface2: Color,
    /** Borde de un control sobre fondo teñido. */
    val onTintOutline: Color,
)

val LightAppColors = AppColors(
    warmup = InkAmber, work = InkOrange, rest = InkMagenta,
    streak = InkAmber, special = InkAmber, positive = Color(0xFF1D7A4C),
    warmupSurface = Color(0xFFFFF6E8),
    workSurface = Color(0xFFFFF1EA),
    restSurface = Color(0xFFFFEBF1),
    brandGradient = InkGradient,
    surface1 = Color(0xFFFFFFFF), surface2 = Color(0xFFF1E7E2),
    onTintOutline = Color(0xFFC89380),
)

val DarkAppColors = AppColors(
    warmup = BrandAmber, work = BrandOrange, rest = BrandMagenta,
    streak = BrandAmber, special = BrandAmber, positive = Color(0xFF57D398),
    warmupSurface = Color(0xFF1A1204),
    workSurface = Color(0xFF1B0A04),
    restSurface = Color(0xFF1C0410),
    brandGradient = BrandGradient,
    surface1 = Color(0xFF1E1619), surface2 = Color(0xFF2A2024),
    onTintOutline = Color(0x52FFFFFF),
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }
