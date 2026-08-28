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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.marc.gymplan100.data.Backup
import com.marc.gymplan100.data.BackupFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.marc.gymplan100.ui.theme.AppTheme
import com.marc.gymplan100.ui.theme.ThemeMode
import androidx.compose.runtime.saveable.rememberSaveable
import com.marc.gymplan100.notify.TrainingReminder
import com.marc.gymplan100.notify.WEEKDAY_LABELS
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.marc.gymplan100.BuildConfig
import com.marc.gymplan100.GymApp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.contar
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
                        "Sigues «${activePlan.name}» · " +
                            contar(activePlan.totalDays, "día", "días") + ". " +
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

            item { SeccionRecordatorios() }

            item {
                val context = LocalContext.current
                WheelCard(
                    title = "Tema",
                    explicacion = "Cómo se ve la app. Por defecto sigue al móvil, pero puedes " +
                        "fijarla en claro o en oscuro."
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.x2),
                        verticalArrangement = Arrangement.spacedBy(Space.x2)
                    ) {
                        ThemeMode.entries.forEach { modo ->
                            FilterChip(
                                selected = AppTheme.mode == modo,
                                onClick = { AppTheme.set(context, modo) },
                                label = { Text(modo.label, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            item { SeccionCopia(viewModel) }

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
/**
 * La copia de seguridad: un archivo tuyo con todo lo que la app sabe de ti.
 *
 * Es la unica pieza de la app que existe por lo que puede pasar y no se puede deshacer. Todo
 * vive en este telefono y no hay cuenta ni nube donde recuperarlo, asi que la copia se guarda
 * donde tu digas (Drive, el PC, la tarjeta) y se restaura desde donde tu digas.
 *
 * Restaurar sobrescribe: por eso se lee y se comprueba el archivo ANTES de tocar nada, y se
 * dice cuantos dias trae y con que se van a sustituir.
 */
/**
 * Los días y la hora a la que la app te recuerda que toca gimnasio.
 *
 * El plan sigue yendo por días numerados y no por fechas: el día 47 es el 47 lo hagas el martes
 * o el sábado. Lo que faltaba no era un calendario, sino que la app supiera cuándo sueles ir.
 * Con esto, «100 días» pasa a ser «100 días de entrenamiento» repartidos como entrenes tú.
 */
@Composable
private fun SeccionRecordatorios() {
    val context = LocalContext.current
    // El estado no necesita sobrevivir a nada: cada cambio se guarda al instante en las
    // preferencias, que son la fuente de verdad, y de ahi se relee al volver a Ajustes.
    var activo by remember { mutableStateOf(TrainingReminder.isEnabled(context)) }
    var dias by remember { mutableStateOf(TrainingReminder.days(context)) }
    var minuto by remember { mutableStateOf(TrainingReminder.minuteOfDay(context)) }

    fun guardar() = TrainingReminder.save(context, activo, dias, minuto)

    Bloque {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recordarme ir al gimnasio",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(Space.x3))
            Switch(
                checked = activo,
                onCheckedChange = { activo = it; guardar() }
            )
        }
        Spacer(Modifier.size(Space.x1))
        Text(
            "Elige qué días sueles entrenar y a qué hora quieres el aviso. Si ese día ya has " +
                "entrenado, no te molesta.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (activo) {
            Spacer(Modifier.size(Space.x3))
            // Los siete a partes iguales en una sola fila: con chips normales el domingo no
            // cabia y se caia solo a la linea de abajo, que para una semana se lee fatal.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.x1)
            ) {
                WEEKDAY_LABELS.forEach { (numero, letra) ->
                    val elegido = numero in dias
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Touch.min)
                            .clip(CircleShape)
                            .background(
                                if (elegido) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                if (elegido) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                CircleShape
                            )
                            .clickable {
                                val nuevos = dias.toMutableSet()
                                if (!nuevos.remove(numero)) nuevos.add(numero)
                                dias = nuevos
                                guardar()
                            }
                    ) {
                        Text(
                            letra,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            color = if (elegido) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(Modifier.size(Space.x3))
            Text(
                "A las ${TrainingReminder.formatTime(minuto)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            // Cuartos de hora: nadie queda con el gimnasio a las 18:07, y una rueda de 1440
            // valores es imposible de manejar con el pulgar.
            val pasos = remember { (0 until 24 * 4).map { it * 15 } }
            val indice = pasos.indexOfFirst { it >= minuto }.coerceAtLeast(0)
            CarrilDeScroll {
                WheelPicker(
                    items = pasos.map { TrainingReminder.formatTime(it) },
                    selectedIndex = indice,
                    onSelectedIndexChange = { i ->
                        minuto = pasos[i.coerceIn(0, pasos.lastIndex)]
                        guardar()
                    }
                )
            }
            if (dias.isEmpty()) {
                Text(
                    "Sin días elegidos no hay aviso que dar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SeccionCopia(viewModel: PlanViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mensaje by remember { mutableStateOf<String?>(null) }
    var porRestaurar by remember { mutableStateOf<BackupFile?>(null) }

    val guardar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            mensaje = runCatching {
                val texto = Backup.create(context, BuildConfig.VERSION_NAME, System.currentTimeMillis())
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { salida ->
                        salida.write(texto.toByteArray())
                    } ?: error("no se pudo escribir")
                }
                "Copia guardada."
            }.getOrElse { "No se ha podido guardar la copia." }
        }
    }

    val abrir = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val leido = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                }
            }.getOrNull()
            if (leido == null) {
                mensaje = "No se ha podido leer el archivo."
                return@launch
            }
            when (val resultado = Backup.parse(leido)) {
                is Backup.Result.Ok -> porRestaurar = resultado.file
                is Backup.Result.Error -> mensaje = resultado.message
            }
        }
    }

    Bloque {
        Text("Copia de seguridad", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(Space.x1))
        Text(
            "Tus días, tus pesos, tu historial y tus planes viven solo en este teléfono. " +
                "Guarda una copia donde quieras y podrás recuperarlo todo en otro móvil.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(Space.x3))
        Button(
            onClick = { guardar.launch(Backup.suggestedName(System.currentTimeMillis())) },
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
        ) { Text("Guardar una copia", maxLines = 1) }
        Spacer(Modifier.size(Space.x2))
        OutlinedButton(
            onClick = { abrir.launch(arrayOf("application/json", "text/plain", "*/*")) },
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
        ) { Text("Restaurar una copia", maxLines = 1) }
        if (mensaje != null) {
            Spacer(Modifier.size(Space.x2))
            Text(
                mensaje.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    porRestaurar?.let { copia ->
        AlertDialog(
            onDismissRequest = { porRestaurar = null },
            title = { Text("¿Restaurar esta copia?") },
            text = {
                Text(
                    "La copia trae " + copia.resumen() + ". Al restaurarla se sustituye " +
                        "TODO lo que hay ahora en la app: días, pesos, historial y planes."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val file = copia
                    porRestaurar = null
                    scope.launch {
                        mensaje = runCatching {
                            Backup.restore(context, file)
                            viewModel.reloadAfterRestore()
                            "Copia restaurada."
                        }.getOrElse { "No se ha podido restaurar la copia." }
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { porRestaurar = null }) { Text("Cancelar") }
            }
        )
    }
}

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
