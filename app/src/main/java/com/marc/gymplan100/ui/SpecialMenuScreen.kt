@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.PlanViewModel

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
    onOpenFatburn: () -> Unit
) {
    val militar = viewModel.specialWorkouts.militar
    // Aviso no bloqueante: si ya se cumplió la frecuencia semanal, se muestra antes de empezar.
    var militaryWarning by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrenamientos especiales") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Elige qué entrenar hoy. Ninguno cuenta como día del plan de 100 días: son " +
                        "extras que se guardan aparte.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                OptionCard(
                    emoji = "⏱️",
                    title = "Entrenamiento libre",
                    subtitle = "Cronómetro simple, como hasta ahora. Tú marcas el ritmo.",
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
        }
    }

    militaryWarning?.let { text ->
        FrequencyWarningDialog(
            text = text,
            onContinue = { militaryWarning = null; onOpenMilitary() },
            onCancel = { militaryWarning = null }
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
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("$emoji  $title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
