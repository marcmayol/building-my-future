@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.UserProfile
import kotlinx.coroutines.launch

private val WEIGHTS = (30..200).toList()      // kg
private val HEIGHTS = (120..220).toList()     // cm
private val GENDERS = listOf("Hombre", "Mujer", "Otro")

@Composable
fun SettingsScreen(
    viewModel: PlanViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Valores por defecto razonables cuando aún no hay perfil, para que la rueda no arranque
    // en el extremo: 75 kg y 170 cm.
    val weight = if (profile.isWeightSet) profile.weightKg else 75
    val height = if (profile.isHeightSet) profile.heightCm else 170

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.refreshHealthPermissions()
        viewModel.importProfileFromHealth { imported ->
            scope.launch {
                snackbar.showSnackbar(
                    if (imported) "Datos importados desde Google Health"
                    else "Google Health no tiene peso ni altura guardados"
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = inner.calculateTopPadding() + 8.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Tus datos personales. Sirven para estimar las calorías de cada entreno, " +
                        "que es lo que Google Health necesita para contarlo en tus objetivos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                WheelCard(title = "Peso") {
                    WheelPicker(
                        items = WEIGHTS.map { "$it kg" },
                        selectedIndex = WEIGHTS.indexOf(weight).coerceAtLeast(0),
                        onSelectedIndexChange = { i ->
                            viewModel.updateProfile(profile.copy(weightKg = WEIGHTS[i]))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                WheelCard(title = "Altura") {
                    WheelPicker(
                        items = HEIGHTS.map { "$it cm" },
                        selectedIndex = HEIGHTS.indexOf(height).coerceAtLeast(0),
                        onSelectedIndexChange = { i ->
                            viewModel.updateProfile(profile.copy(heightCm = HEIGHTS[i]))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                WheelCard(title = "Género") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GENDERS.forEach { g ->
                            FilterChip(
                                selected = profile.gender == g,
                                onClick = { viewModel.updateProfile(profile.copy(gender = g)) },
                                label = { Text(g) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Uso reloj o pulsómetro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                "Si empiezas o controlas el entreno desde el reloj, esto se aplica " +
                                    "solo: Google Health usa las calorías reales de tu pulso y la app " +
                                    "no estima (evita el doble conteo). Actívalo aquí para forzarlo " +
                                    "también en los entrenos que empieces desde el móvil.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = profile.usesWatch,
                            onCheckedChange = { viewModel.updateProfile(profile.copy(usesWatch = it)) }
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Rellenar desde Google Health",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            "Trae tu peso y altura más recientes guardados en Google Health.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.size(12.dp))
                        if (viewModel.healthAvailable) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        // Pide permisos de lectura si aún no los tenemos; el
                                        // resultado dispara la importación en el launcher.
                                        permissionLauncher.launch(viewModel.healthPermissions)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Importar de Google Health") }
                        } else {
                            Text(
                                "Google Health no está disponible en este dispositivo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.size(4.dp))
            content()
        }
    }
}
