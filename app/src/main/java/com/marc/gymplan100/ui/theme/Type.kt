package com.marc.gymplan100.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography()

/**
 * Lo que la escala de M3 no cubre: las cifras grandes de las pantallas de planes.
 *
 * `fontFeatureSettings = "tnum"` fuerza cifras tabulares (todas del mismo ancho) para que un
 * contador no baile al cambiar de dígito.
 */
@Immutable
data class AppTextStyles(
    val bigNumber: TextStyle = TextStyle(
        fontWeight = FontWeight.W800, fontSize = 56.sp, lineHeight = 56.sp,
        fontFeatureSettings = "tnum",
    ),
    val displayCard: TextStyle = TextStyle(
        fontWeight = FontWeight.W800, fontSize = 40.sp, lineHeight = 42.sp,
    ),
    val dataMedium: TextStyle = TextStyle(
        fontWeight = FontWeight.W800, fontSize = 40.sp, lineHeight = 42.sp,
        fontFeatureSettings = "tnum",
    ),
    val tabular: TextStyle = TextStyle(
        fontWeight = FontWeight.W700, fontSize = 15.sp, fontFeatureSettings = "tnum",
    ),
)

val LocalAppTextStyles = staticCompositionLocalOf { AppTextStyles() }
