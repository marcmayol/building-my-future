@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.marc.gymplan100.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.marc.gymplan100.R

// Archivo variable (SIL OFL 1.1) en res/font/archivo_variable.ttf. Ejes: wght 100..900,
// wdth 62..125. Se eligió por sus cifras tabulares: con la fuente anterior los cronómetros
// bailaban al cambiar de dígito.
private fun archivo(weight: Int, width: Float) = Font(
    R.font.archivo_variable,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.width(width),
    ),
    weight = FontWeight(weight),
)

val Archivo = FontFamily(
    archivo(400, 100f), archivo(600, 100f), archivo(700, 100f), archivo(800, 100f),
)
val ArchivoWide = FontFamily(archivo(800, 112f))
val ArchivoWider = FontFamily(archivo(800, 118f))

/** Cifras tabulares: obligatorio en todo lo que cuenta, o el número baila. */
private const val TNUM = "tnum"

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.W800,
        fontSize = 34.sp, lineHeight = 37.sp, letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.W800,
        fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.W700,
        fontSize = 22.sp, lineHeight = 27.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.W700,
        fontSize = 22.sp, lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.W600,
        fontSize = 17.sp, lineHeight = 21.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.W600,
        fontSize = 15.sp, lineHeight = 19.sp,
    ),
    bodyLarge = TextStyle(   // M3 usa 16sp; +1 por la distancia de lectura en el gimnasio
        fontFamily = Archivo, fontWeight = FontWeight.W400,
        fontSize = 17.sp, lineHeight = 25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.W400,
        fontSize = 15.sp, lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.W400,
        fontSize = 13.sp, lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(  // botón
        fontFamily = Archivo, fontWeight = FontWeight.W700,
        fontSize = 19.sp, lineHeight = 23.sp,
    ),
    labelMedium = TextStyle( // versalitas
        fontFamily = Archivo, fontWeight = FontWeight.W600,
        fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 2.1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.W600,
        fontSize = 12.sp, lineHeight = 15.sp, letterSpacing = 0.5.sp,
    ),
)

/**
 * Lo que M3 no cubre: las cifras grandes.
 *
 * El tamaño de la cuenta atrás está a la mitad de lo que proponía el diseño (124 sp): a él le
 * resultaba excesiva y se comía el margen lateral. A 62 sp sigue leyéndose de sobra a un brazo
 * de distancia y respeta los 20 dp de margen.
 */
@Immutable
data class AppTextStyles(
    val countdown: TextStyle = TextStyle(
        fontFamily = ArchivoWider, fontWeight = FontWeight.W800,
        fontSize = 62.sp, lineHeight = 62.sp, letterSpacing = (-2.5).sp,
        fontFeatureSettings = TNUM,
    ),
    val countdownCompact: TextStyle = TextStyle(  // font_scale alto o pantalla corta
        fontFamily = ArchivoWider, fontWeight = FontWeight.W800,
        fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-1.8).sp,
        fontFeatureSettings = TNUM,
    ),
    val bigNumber: TextStyle = TextStyle(         // racha, "X de N"
        fontFamily = ArchivoWide, fontWeight = FontWeight.W800,
        fontSize = 56.sp, lineHeight = 56.sp, fontFeatureSettings = TNUM,
    ),
    val displayCard: TextStyle = TextStyle(       // "Día 42"
        fontFamily = ArchivoWide, fontWeight = FontWeight.W800,
        fontSize = 40.sp, lineHeight = 42.sp,
    ),
    val dataMedium: TextStyle = TextStyle(        // peso, métricas de tarjeta
        fontFamily = ArchivoWide, fontWeight = FontWeight.W800,
        fontSize = 40.sp, lineHeight = 42.sp, fontFeatureSettings = TNUM,
    ),
    val tabular: TextStyle = TextStyle(           // cualquier cifra en línea
        fontFamily = Archivo, fontWeight = FontWeight.W700,
        fontSize = 15.sp, fontFeatureSettings = TNUM,
    ),
)

val LocalAppTextStyles = staticCompositionLocalOf { AppTextStyles() }
