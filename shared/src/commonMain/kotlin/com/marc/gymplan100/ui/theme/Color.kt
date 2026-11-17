package com.marc.gymplan100.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─── Marca ────────────────────────────────────────────────────────────────────
// El degradado del logo es un reloj: ámbar = preparas, naranja = trabajas, magenta = esperas.
// El degradado completo se reserva a la marca y al progreso del plan; nunca va detrás de texto.
val BrandAmber = Color(0xFFFFB24D)
val BrandOrange = Color(0xFFFF6A3D)
val BrandMagenta = Color(0xFFFF2E6E)
val BrandBlack = Color(0xFF0A0708)

/** Versión "tinta" para el tema claro: más saturada, aguanta la luz del gimnasio. */
val InkAmber = Color(0xFFC87A16)
val InkOrange = Color(0xFFD4451A)
val InkMagenta = Color(0xFFB3134C)

val BrandGradient = Brush.linearGradient(listOf(BrandAmber, BrandOrange, BrandMagenta))
val InkGradient = Brush.linearGradient(listOf(InkAmber, InkOrange, InkMagenta))

// ─── ColorScheme M3 ───────────────────────────────────────────────────────────
val LightColors = lightColorScheme(
    primary = Color(0xFFC8391B),            // antes #D23A1C: no llegaba a 4.5:1 en texto pequeño
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDACE),
    onPrimaryContainer = Color(0xFF3D0B02),
    secondary = Color(0xFFB3134C),          // antes #D81B60
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF3D0616),
    background = Color(0xFFFBF6F4),
    onBackground = Color(0xFF221A17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF221A17),
    surfaceVariant = Color(0xFFF1E7E2),
    onSurfaceVariant = Color(0xFF58504B),
    outline = Color(0xFF8A7A73),
    outlineVariant = Color(0xFFE4D6D0),
    error = Color(0xFFB3134C),
    onError = Color(0xFFFFFFFF),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6A4D),
    onPrimary = Color(0xFF44100A),
    primaryContainer = Color(0xFF8A2616),
    onPrimaryContainer = Color(0xFFFFDACE),
    secondary = Color(0xFFFF5E8A),
    onSecondary = Color(0xFF3D0616),
    secondaryContainer = Color(0xFF6E0B2C),
    onSecondaryContainer = Color(0xFFFFD9E2),
    background = BrandBlack,
    onBackground = Color(0xFFF2EDEA),
    surface = Color(0xFF161013),
    onSurface = Color(0xFFF2EDEA),
    surfaceVariant = Color(0xFF2A2024),
    onSurfaceVariant = Color(0xFFCBBFB9),
    outline = Color(0xFFA2938D),
    outlineVariant = Color(0xFF2A2024),
    error = Color(0xFFFF5E8A),
    onError = Color(0xFF3D0616),
)

// ─── Tokens propios de estado (M3 no tiene rol para esto) ─────────────────────
@Immutable
data class AppColors(
    val warmup: Color,        // ámbar · preparas
    val work: Color,          // naranja · trabajas
    val rest: Color,          // magenta · esperas
    val streak: Color,
    val special: Color,
    val positive: Color,      // "+5 este mes"
    val warmupSurface: Color, // fondo teñido de la pantalla de sesión
    val workSurface: Color,
    val restSurface: Color,
    val brandGradient: Brush,
    /** Superficies elevadas SIN sombra (en oscuro, sobre negro, una sombra no se ve). */
    val surface1: Color,
    val surface2: Color,
    /** Borde de botón secundario cuando va sobre un fondo teñido. */
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

/**
 * Halo radial del estado, para el fondo de la sesión guiada. En claro devuelve un brush
 * transparente: allí el tinte va en la superficie, no en el halo.
 */
fun stateHalo(color: Color, dark: Boolean): Brush =
    if (dark) Brush.radialGradient(
        colors = listOf(color.copy(alpha = 0.32f), Color.Transparent),
        radius = 900f,
    ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
