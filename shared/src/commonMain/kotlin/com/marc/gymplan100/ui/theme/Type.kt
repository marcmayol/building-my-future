@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.marc.gymplan100.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import org.jetbrains.compose.resources.Font
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.marc.gymplan100.shared.*

// Archivo variable (SIL OFL 1.1) en composeResources/font/archivo_variable.ttf. Ejes: wght
// 100..900, wdth 62..125. Se eligió por sus cifras tabulares: con la fuente anterior los
// cronómetros bailaban al cambiar de dígito.
@Composable
private fun archivo(weight: Int, width: Float) = Font(
    Res.font.archivo_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.width(width),
    ),
)

/** Las tres anchuras de Archivo que usa la app. En Compose Multiplatform se cargan al pintar. */
@Immutable
class Fuentes(val archivo: FontFamily, val wide: FontFamily, val wider: FontFamily) {
    companion object {
        /** Hasta que se cargan (y en las vistas previas): la del sistema. */
        val Defecto = Fuentes(FontFamily.Default, FontFamily.Default, FontFamily.Default)
    }
}

@Composable
fun fuentes(): Fuentes = Fuentes(
    archivo = FontFamily(archivo(400, 100f), archivo(600, 100f), archivo(700, 100f), archivo(800, 100f)),
    wide = FontFamily(archivo(800, 112f)),
    wider = FontFamily(archivo(800, 118f)),
)

/** Cifras tabulares: obligatorio en todo lo que cuenta, o el número baila. */
private const val TNUM = "tnum"

fun appTypography(f: Fuentes) = Typography(
    headlineLarge = TextStyle(
        fontFamily = f.archivo, fontWeight = FontWeight.W800,
        fontSize = 34.sp, lineHeight = 37.sp, letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = f.archivo, fontWeight = FontWeight.W800,
        fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = f.archivo, fontWeight = FontWeight.W700,
        fontSize = 22.sp, lineHeight = 27.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = f.archivo, fontWeight = FontWeight.W700,
        fontSize = 22.sp, lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = f.archivo, fontWeight = FontWeight.W600,
        fontSize = 17.sp, lineHeight = 21.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = f.archivo, fontWeight = FontWeight.W600,
        fontSize = 15.sp, lineHeight = 19.sp,
    ),
    bodyLarge = TextStyle(   // M3 usa 16sp; +1 por la distancia de lectura en el gimnasio
        fontFamily = f.archivo, fontWeight = FontWeight.W400,
        fontSize = 17.sp, lineHeight = 25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = f.archivo, fontWeight = FontWeight.W400,
        fontSize = 15.sp, lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = f.archivo, fontWeight = FontWeight.W400,
        fontSize = 13.sp, lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(  // botón
        fontFamily = f.archivo, fontWeight = FontWeight.W700,
        fontSize = 19.sp, lineHeight = 23.sp,
    ),
    labelMedium = TextStyle( // versalitas
        fontFamily = f.archivo, fontWeight = FontWeight.W600,
        fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 2.1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = f.archivo, fontWeight = FontWeight.W600,
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
    val f: Fuentes = Fuentes.Defecto,
    val countdown: TextStyle = TextStyle(
        fontFamily = f.wider, fontWeight = FontWeight.W800,
        fontSize = 62.sp, lineHeight = 62.sp, letterSpacing = (-2.5).sp,
        fontFeatureSettings = TNUM,
    ),
    val countdownCompact: TextStyle = TextStyle(  // font_scale alto o pantalla corta
        fontFamily = f.wider, fontWeight = FontWeight.W800,
        fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-1.8).sp,
        fontFeatureSettings = TNUM,
    ),
    val bigNumber: TextStyle = TextStyle(         // racha, "X de N"
        fontFamily = f.wide, fontWeight = FontWeight.W800,
        fontSize = 56.sp, lineHeight = 56.sp, fontFeatureSettings = TNUM,
    ),
    val displayCard: TextStyle = TextStyle(       // "Día 42"
        fontFamily = f.wide, fontWeight = FontWeight.W800,
        fontSize = 40.sp, lineHeight = 42.sp,
    ),
    val dataMedium: TextStyle = TextStyle(        // peso, métricas de tarjeta
        fontFamily = f.wide, fontWeight = FontWeight.W800,
        fontSize = 40.sp, lineHeight = 42.sp, fontFeatureSettings = TNUM,
    ),
    val tabular: TextStyle = TextStyle(           // cualquier cifra en línea
        fontFamily = f.archivo, fontWeight = FontWeight.W700,
        fontSize = 15.sp, fontFeatureSettings = TNUM,
    ),
)

val LocalAppTextStyles = staticCompositionLocalOf { AppTextStyles() }
