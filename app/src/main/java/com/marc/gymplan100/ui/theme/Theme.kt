package com.marc.gymplan100.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

/** Espaciado base 4, para las pantallas de planes. */
object Space {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val screen = 20.dp
    val block = 24.dp
    val inner = 12.dp
    val thumbZone = 96.dp
}

/** Alturas mínimas de toque. Con guantes y prisa, 48 dp es el suelo absoluto. */
object Touch {
    val primaryInSession = 64.dp
    val primary = 56.dp
    val stepper = 60.dp
    val min = 48.dp
}

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = OnLightPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = OnLightPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = OnLightSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = OnLightSecondaryContainer,
    background = LightBackground,
    onBackground = OnLightBackground,
    surface = LightSurface,
    onSurface = OnLightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightSurfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = OnDarkPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = OnDarkPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = OnDarkSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = OnDarkSecondaryContainer,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
)

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
            typography = Typography,
            content = content
        )
    }
}
