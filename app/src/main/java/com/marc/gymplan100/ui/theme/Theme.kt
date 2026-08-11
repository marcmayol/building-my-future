package com.marc.gymplan100.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

// Tres pasos de radio, no cinco: los 12-14 dp del diseño anterior no se distinguían entre sí.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // chips, campos de texto
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),      // bloques de sección
    large = RoundedCornerShape(24.dp),       // héroe, hojas inferiores
    extraLarge = RoundedCornerShape(24.dp),
)

/** Espaciado base 4. */
object Space {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val screen = 20.dp      // margen lateral de pantalla
    val block = 24.dp       // entre bloques
    val inner = 12.dp       // dentro de un bloque
    val thumbZone = 96.dp   // últimos 96 dp: solo acciones, nunca información
}

/** Alturas mínimas de toque. Con guantes y prisa, 48 dp es el suelo absoluto. */
object Touch {
    val primaryInSession = 64.dp
    val primary = 56.dp
    val stepper = 60.dp
    val min = 48.dp
}

@Composable
fun GymPlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAppColors provides if (darkTheme) DarkAppColors else LightAppColors,
        LocalAppTextStyles provides AppTextStyles(),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
