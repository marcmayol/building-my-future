@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.marc.gymplan100.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch
import androidx.health.connect.client.PermissionController
import com.marc.gymplan100.BuildConfig
import com.marc.gymplan100.GymApp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.UserProfile
import com.marcm.actualizador.EstadoActualizacion
import com.marcm.actualizador.Modo
import com.marcm.actualizador.TipoError
import kotlinx.coroutines.launch

private val WEIGHTS = (30..200).toList()      // kg
private val GENDERS = listOf("Hombre", "Mujer", "Otro")

@Composable
fun SettingsScreen(
    viewModel: PlanViewModel,
    onBack: () -> Unit,
    onOpenPlans: () -> Unit = {}
) {
    val profile by viewModel.profile.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Valor por defecto razonable cuando aún no hay perfil, para que la rueda no arranque
    // en el extremo.
    val weight = if (profile.isWeightSet) profile.weightKg else 75

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.refreshHealthPermissions()
        viewModel.importProfileFromHealth { imported ->
            scope.launch {
                snackbar.showSnackbar(
                    if (imported) "Peso importado desde Google Health"
                    else "Google Health no tiene ningún peso guardado"
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Space.screen,
                end = Space.screen,
                top = inner.calculateTopPadding() + Space.x2,
                bottom = 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x3)
        ) {
            item {
                Bloque {
                    Text("Plan de entrenamiento", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(Space.x1))
                    Text(
                        "Sigues «${activePlan.name}» · ${activePlan.totalDays} días. " +
                            "Puedes traerte tu propio plan y alternar entre ellos sin " +
                            "perder el progreso de ninguno.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(Space.x3))
                    Button(
                        onClick = onOpenPlans,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Touch.primary)
                    ) { Text("Mis planes") }
                }
            }

            item {
                Text(
                    "TUS DATOS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.x2)
                )
                Spacer(Modifier.size(Space.x1))
                Text(
                    "Solo se guarda lo que la app usa de verdad. Cada uno dice para qué sirve.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                WheelCard(
                    title = "Peso",
                    explicacion = "Con él se estiman las calorías de cada entreno, que es lo " +
                        "que Google Health necesita para contarlo en tus objetivos."
                ) {
                    CarrilDeScroll {
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
            }

            item {
                WheelCard(
                    title = "Género",
                    explicacion = "Elige si las ilustraciones de los ejercicios son de hombre " +
                        "o de mujer. No entra en ningún cálculo."
                ) {
                    // Cada chip ocupa lo que necesita y salta de línea si no cabe: repartidos
                    // a tercios, "Hombre" se partía por la mitad con el texto del sistema grande.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.x2),
                        verticalArrangement = Arrangement.spacedBy(Space.x2)
                    ) {
                        GENDERS.forEach { g ->
                            FilterChip(
                                selected = profile.gender == g,
                                onClick = { viewModel.updateProfile(profile.copy(gender = g)) },
                                label = { Text(g, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            item {
                Bloque {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Uso reloj o pulsómetro",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.size(Space.x3))
                        Switch(
                            checked = profile.usesWatch,
                            onCheckedChange = {
                                viewModel.updateProfile(profile.copy(usesWatch = it))
                            }
                        )
                    }
                    Spacer(Modifier.size(Space.x1))
                    Text(
                        "Si empiezas o controlas el entreno desde el reloj, esto se aplica " +
                            "solo: Google Health usa las calorías reales de tu pulso y la app " +
                            "no estima (evita el doble conteo). Actívalo aquí para forzarlo " +
                            "también en los entrenos que empieces desde el móvil.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { SeccionActualizaciones() }

            item {
                Bloque {
                    Text(
                        "Rellenar desde Google Health",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.size(Space.x1))
                    Text(
                        "Trae el peso más reciente que tengas guardado en Google Health.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(Space.x3))
                    if (viewModel.healthAvailable) {
                        Button(
                            onClick = {
                                scope.launch {
                                    // Pide permisos de lectura si aún no los tenemos; el
                                    // resultado dispara la importación en el launcher.
                                    permissionLauncher.launch(viewModel.healthPermissions)
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Touch.primary)
                        ) { Text("Importar de Google Health") }
                    } else {
                        Text(
                            "Google Health no está disponible en este dispositivo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Actualizaciones y "acerca de". Este es el único sitio donde la comprobación
 * informa de errores o del "estás al día": el usuario la ha pedido a mano.
 */
@Composable
private fun SeccionActualizaciones() {
    val context = LocalContext.current
    val actualizador = remember(context) { (context.applicationContext as GymApp).actualizador }
    val estado by actualizador.estado.collectAsState()
    var buscarAuto by remember { mutableStateOf(actualizador.buscarAutomaticamente) }
    val scope = rememberCoroutineScope()

    Bloque {
        Text("Actualizaciones", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(Space.x1))
        Text(
            "La app se instala fuera de Play Store, así que se actualiza sola desde " +
                "su página de versiones.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(Space.x3))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Buscar actualizaciones", modifier = Modifier.weight(1f))
            Spacer(Modifier.size(Space.x3))
            Switch(
                checked = buscarAuto,
                onCheckedChange = {
                    buscarAuto = it
                    actualizador.buscarAutomaticamente = it
                }
            )
        }
        Spacer(Modifier.size(Space.x2))
        OutlinedButton(
            onClick = { scope.launch { actualizador.comprobar(Modo.MANUAL) } },
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Touch.primary)
        ) { Text("Buscar ahora") }

        if (estado is EstadoActualizacion.Disponible) {
            Spacer(Modifier.size(Space.x2))
            Button(
                onClick = { actualizador.actualizarAhora() },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Touch.primary)
            ) {
                Text(
                    "Actualizar a la " +
                        (estado as EstadoActualizacion.Disponible).info.versionName
                )
            }
        }

        EstadoComprobacionManual(estado)

        Spacer(Modifier.size(Space.x3))
        Text(
            "Versión ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Traduce el estado a una línea legible bajo el botón de comprobar. */
@Composable
private fun EstadoComprobacionManual(estado: EstadoActualizacion) {
    val texto = when (estado) {
        EstadoActualizacion.Comprobando -> "Buscando…"
        EstadoActualizacion.AlDia -> "Estás al día ✓"
        is EstadoActualizacion.Disponible -> null       // ya lo dice el botón
        is EstadoActualizacion.Descargando -> "Descargando… ${estado.porcentaje}%"
        EstadoActualizacion.Verificando -> "Comprobando la copia…"
        EstadoActualizacion.Instalando -> "Instalando…"
        is EstadoActualizacion.Error -> when (estado.tipo) {
            TipoError.SIN_RED -> "Sin conexión. Inténtalo más tarde."
            TipoError.HTTP, TipoError.MANIFIESTO -> "No se pudo consultar si hay versión nueva."
            TipoError.DESCARGA -> "Falló la descarga."
            TipoError.HASH -> "La descarga llegó corrupta y se ha borrado."
            TipoError.INSTALACION -> estado.mensaje ?: "No se pudo instalar."
        }
        EstadoActualizacion.Inactivo, EstadoActualizacion.PidiendoPermiso -> null
    }
    if (texto != null) {
        Spacer(Modifier.size(8.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodySmall,
            color = if (estado is EstadoActualizacion.Error) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Un ajuste = un bloque de color. Sustituye a las tarjetas del diseño anterior. */
@Composable
private fun Bloque(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Space.x4)
    ) { content() }
}

/** Un dato del perfil: su nombre, para qué sirve, y el control para cambiarlo. */
@Composable
private fun WheelCard(
    title: String,
    explicacion: String = "",
    content: @Composable () -> Unit
) {
    Bloque {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (explicacion.isNotBlank()) {
            Spacer(Modifier.size(2.dp))
            Text(
                explicacion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(Space.x2))
        content()
    }
}

/**
 * Carril de scroll alrededor de una rueda.
 *
 * La rueda va estrecha y centrada a propósito: a todo el ancho se comía cualquier arrastre
 * vertical, así que bajar por la pantalla te cambiaba el dato sin querer. Con los laterales
 * libres queda sitio para desplazar la lista.
 */
@Composable
private fun CarrilDeScroll(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.width(200.dp)) { content() }
    }
}
