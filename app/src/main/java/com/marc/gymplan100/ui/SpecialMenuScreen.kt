@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.ui.theme.Space

/**
 * Sección de entrenamientos especiales: tres opciones seleccionables.
 *  1. Entrenamiento libre (el "extra" de siempre, cronómetro sin cambios en su flujo).
 *  2. Rutina Militar (secuencia guiada de 13 pasos).
 *  3. Rutina Quema Grasa (catálogo de ejercicios).
 */
@Composable
fun SpecialMenuScreen(
    viewModel: PlanViewModel,
    onBack: () -> Unit,
    onFreeWorkout: () -> Unit,
    onOpenMilitary: () -> Unit,
    onOpenFatburn: () -> Unit,
    onOpenPosture: () -> Unit
) {
    val militar = viewModel.specialWorkouts.militar
    val altura = viewModel.specialWorkouts.altura
    // Aviso no bloqueante: si ya se cumplió la frecuencia recomendada, se muestra antes de empezar.
    var militaryWarning by remember { mutableStateOf<String?>(null) }
    var postureWarning by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Especiales", style = MaterialTheme.typography.headlineSmall)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(
                start = Space.screen,
                end = Space.screen,
                top = Space.x2,
                bottom = 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3)
        ) {
            item {
                Text(
                    "NINGUNO CUENTA COMO DÍA DEL PLAN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.x1))
                Text("Elige qué entrenar hoy", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(Space.x1))
                Text(
                    "Son extras que se guardan aparte.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.x2))
            }
            item {
                OptionCard(
                    emoji = "⏱️",
                    title = "Entrenamiento libre (extra)",
                    subtitle = "El entreno extra de siempre: cronómetro simple, tú marcas el ritmo.",
                    onClick = onFreeWorkout
                )
            }
            item {
                OptionCard(
                    emoji = "🎖️",
                    title = militar?.nombre ?: "Rutina Militar",
                    subtitle = militar?.descripcion
                        ?: "Secuencia guiada de fuerza, cardio y resistencia sin parar.",
                    onClick = {
                        val status = viewModel.militaryFrequency()
                        if (status.reached && militar != null) militaryWarning = militar.aviso_frecuencia
                        else onOpenMilitary()
                    }
                )
            }
            item {
                OptionCard(
                    emoji = "🔥",
                    title = "Rutina Quema Grasa",
                    subtitle = "Catálogo de ejercicios de alta intensidad, cada uno con su protocolo.",
                    onClick = onOpenFatburn
                )
            }
            item {
                OptionCard(
                    emoji = "🧘",
                    title = altura?.nombre ?: "Rutina Altura y Postura",
                    subtitle = altura?.descripcion
                        ?: "Cinco ejercicios de postura y descompresión de columna. Baja intensidad.",
                    onClick = {
                        val status = viewModel.postureFrequency()
                        if (status.reached && altura != null) postureWarning = altura.aviso_frecuencia
                        else onOpenPosture()
                    }
                )
            }
        }
    }

    militaryWarning?.let { text ->
        FrequencyWarningDialog(
            text = text,
            onContinue = { militaryWarning = null; onOpenMilitary() },
            onCancel = { militaryWarning = null }
        )
    }

    postureWarning?.let { text ->
        FrequencyWarningDialog(
            text = text,
            onContinue = { postureWarning = null; onOpenPosture() },
            onCancel = { postureWarning = null }
        )
    }
}

@Composable
private fun OptionCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(Space.x4)
    ) {
        // El emoji en su propia línea: pegado al título le robaba el ancho a la primera línea
        // en cuanto el texto del sistema crecía.
        Text(emoji, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Space.x2))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Diálogo informativo (no bloqueante) del sistema de avisos de frecuencia: el usuario SIEMPRE
 * puede entrenar; solo se le recuerda que ya cumplió la recomendación semanal.
 */
@Composable
fun FrequencyWarningDialog(
    text: String,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Ya has cumplido esta semana") },
        text = { Text(text) },
        confirmButton = { Button(onClick = onContinue) { Text("Entrenar igualmente") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Mejor no") } }
    )
}
