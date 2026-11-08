@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.marc.gymplan100.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.marc.gymplan100.R
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
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
import com.marc.gymplan100.update.Updates
import com.marc.gymplan100.review.Valoracion
import com.marc.gymplan100.data.Backup
import com.marc.gymplan100.data.BackupFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.marc.gymplan100.ui.theme.AppLanguage
import com.marc.gymplan100.ui.theme.AppTheme
import com.marc.gymplan100.ui.theme.LanguageMode
import com.marc.gymplan100.ui.theme.ThemeMode
import androidx.compose.runtime.saveable.rememberSaveable
import com.marc.gymplan100.notify.TrainingReminder
import com.marc.gymplan100.notify.WEEKDAY_LABELS
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.marc.gymplan100.BuildConfig
import com.marc.gymplan100.GymApp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.data.UserProfile
import com.marc.gymplan100.data.RestDefaults
import kotlinx.coroutines.launch

private val WEIGHTS = (30..200).toList()      // kg
// El valor se guarda en el perfil y se compara («Mujer» elige las ilustraciones): no se
// traduce. Lo que se enseña es la etiqueta de al lado.
private val GENDERS = listOf(
    "Hombre" to R.string.ini_gender_male,
    "Mujer" to R.string.ini_gender_female,
    "Otro" to R.string.ini_gender_other
)

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
                    context.getString(
                        if (imported) R.string.ini_set_weight_imported
                        else R.string.ini_set_weight_none
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ini_set_title), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ini_back))
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
                    Text(stringResource(R.string.ini_set_plan_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(Space.x1))
                    Text(
                        stringResource(
                            R.string.ini_set_plan_body,
                            activePlan.name,
                            pluralStringResource(R.plurals.ini_days, activePlan.totalDays, activePlan.totalDays)
                        ),
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
                    ) { Text(stringResource(R.string.ini_set_my_plans)) }
                    // La ayuda también aquí, que es donde se busca primero.
                    LocalOpenHelp.current?.let { abrir ->
                        Spacer(Modifier.size(Space.x2))
                        OutlinedButton(
                            onClick = abrir,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Touch.primary)
                        ) { Text(stringResource(R.string.ini_set_help)) }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.ini_set_your_data),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.x2)
                )
                Spacer(Modifier.size(Space.x1))
                Text(
                    stringResource(R.string.ini_set_your_data_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                WheelCard(
                    title = stringResource(R.string.ini_set_weight),
                    explicacion = stringResource(R.string.ini_set_weight_body)
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
                    title = stringResource(R.string.ini_set_gender),
                    explicacion = stringResource(R.string.ini_set_gender_body)
                ) {
                    // Cada chip ocupa lo que necesita y salta de línea si no cabe: repartidos
                    // a tercios, "Hombre" se partía por la mitad con el texto del sistema grande.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.x2),
                        verticalArrangement = Arrangement.spacedBy(Space.x2)
                    ) {
                        GENDERS.forEach { (g, etiqueta) ->
                            FilterChip(
                                selected = profile.gender == g,
                                onClick = { viewModel.updateProfile(profile.copy(gender = g)) },
                                label = { Text(stringResource(etiqueta), maxLines = 1) }
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
                            stringResource(R.string.ini_set_watch),
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
                        stringResource(R.string.ini_set_watch_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { SeccionDescansos(profile.restBase) { viewModel.setRestBase(it) } }

            item { SeccionRecordatorios() }

            item {
                val context = LocalContext.current
                WheelCard(
                    title = stringResource(R.string.ini_set_theme),
                    explicacion = stringResource(R.string.ini_set_theme_body)
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
                                label = { Text(stringResource(modo.label), maxLines = 1) }
                            )
                        }
                    }
                }
            }

            item {
                val context = LocalContext.current
                val actual = remember { AppLanguage.mode(context) }
                WheelCard(
                    title = stringResource(R.string.idioma_titulo),
                    explicacion = stringResource(R.string.idioma_explicacion)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.x2),
                        verticalArrangement = Arrangement.spacedBy(Space.x2)
                    ) {
                        LanguageMode.entries.forEach { modo ->
                            FilterChip(
                                selected = actual == modo,
                                onClick = {
                                    if (modo != actual) {
                                        AppLanguage.set(context, modo)
                                        // Recrear la actividad vuelve a pasar por
                                        // attachBaseContext, que es donde se aplica.
                                        context.findActivity()?.recreate()
                                    }
                                },
                                label = {
                                    Text(
                                        when (modo) {
                                            LanguageMode.SISTEMA -> stringResource(R.string.idioma_sistema)
                                            LanguageMode.ES -> stringResource(R.string.idioma_es)
                                            LanguageMode.EN -> stringResource(R.string.idioma_en)
                                        },
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item { SeccionCopia(viewModel) }

            item { Bloque { Updates.SettingsSection() } }

            item {
                Bloque {
                    Text(stringResource(R.string.ini_set_how), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(Space.x1))
                    Text(
                        stringResource(R.string.ini_set_how_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(Space.x2))
                    TextButton(onClick = {
                        // El recorrido se dibuja encima de todo desde el NavHost, así que hay
                        // que salir de Ajustes o quedaría esta pantalla debajo esperando.
                        onBack()
                        viewModel.showTourAgain()
                    }) { Text(stringResource(R.string.ini_set_tour), maxLines = 1) }
                }
            }

            item { Bloque { Valoracion.SettingsSection() } }

            item { SeccionCreditos() }

            item {
                Bloque {
                    Text(
                        stringResource(R.string.ini_set_health_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.size(Space.x1))
                    Text(
                        stringResource(R.string.ini_set_health_body),
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
                        ) { Text(stringResource(R.string.ini_set_health_import)) }
                    } else {
                        Text(
                            stringResource(R.string.ini_set_health_unavailable),
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
 * Tus descansos de siempre: los que pone la cuenta atrás cuando nada dice otra cosa.
 *
 * Lo que dice el plan (una nota de «3 min de descanso»), lo que tú hayas puesto con ±30 s en un
 * ejercicio y el descanso largo de las series pesadas siguen mandando por encima: esto es el
 * suelo, no un tope. Pasos de 15 s, que es lo que se distingue con el reloj en la mano.
 */
@Composable
private fun SeccionDescansos(base: RestDefaults.Base, onChange: (RestDefaults.Base) -> Unit) {
    Bloque {
        Text(stringResource(R.string.ini_set_rests), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(Space.x1))
        Text(
            stringResource(R.string.ini_set_rests_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(Space.x3))
        FilaDescanso(stringResource(R.string.ini_set_rest_sets), base.sets) { onChange(base.copy(sets = it)) }
        Spacer(Modifier.size(Space.x2))
        FilaDescanso(stringResource(R.string.ini_set_rest_exercises), base.exercises) { onChange(base.copy(exercises = it)) }
        if (base != RestDefaults.Base()) {
            Spacer(Modifier.size(Space.x1))
            TextButton(onClick = { onChange(RestDefaults.Base()) }) {
                Text(stringResource(R.string.ini_set_rest_reset), maxLines = 1)
            }
        }
    }
}

@Composable
private fun FilaDescanso(titulo: String, segundos: Int, onChange: (Int) -> Unit) {
    // La etiqueta encima y no al lado: «Entre ejercicios» y dos botones no caben en una fila
    // con el texto del sistema grande.
    Text(titulo, style = MaterialTheme.typography.bodyLarge)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { onChange((segundos - REST_STEP).coerceAtLeast(REST_MIN)) },
            enabled = segundos > REST_MIN,
            shape = CircleShape,
            modifier = Modifier.heightIn(min = Touch.min)
        ) { Text("−15 s", maxLines = 1) }
        Text(
            "%d:%02d".format(segundos / 60, segundos % 60),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            onClick = { onChange((segundos + REST_STEP).coerceAtMost(REST_MAX)) },
            enabled = segundos < REST_MAX,
            shape = CircleShape,
            modifier = Modifier.heightIn(min = Touch.min)
        ) { Text("+15 s", maxLines = 1) }
    }
}

private const val REST_STEP = 15
private const val REST_MIN = 30
private const val REST_MAX = 300

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
                stringResource(R.string.ini_set_remind),
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
            stringResource(R.string.ini_set_remind_body),
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
                            stringResource(letra),
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
                stringResource(R.string.ini_set_remind_at, TrainingReminder.formatTime(minuto)),
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
                    stringResource(R.string.ini_set_remind_no_days),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * De dónde salen las ilustraciones y con qué licencia.
 *
 * No es un adorno: las 166 imágenes de los ejercicios son de everkinetic y están bajo
 * CC BY-SA 4.0, que permite usarlas —también comercialmente— pero **exige citar al autor**.
 * Hasta ahora el crédito solo estaba en el README del repositorio, donde no lo ve nadie que
 * use la app. Aquí sí.
 */
@Composable
private fun SeccionCreditos() {
    val context = LocalContext.current
    Bloque {
        Text(stringResource(R.string.ini_set_credits), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(Space.x1))
        Text(
            stringResource(R.string.ini_set_credits_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(Space.x2))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
            TextButton(onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/everkinetic/data"))
                )
            }) { Text(stringResource(R.string.ini_set_credits_set), maxLines = 1) }
            TextButton(onClick = {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(context.getString(R.string.ini_set_license_url))
                    )
                )
            }) { Text(stringResource(R.string.ini_set_credits_license), maxLines = 1) }
        }
        Spacer(Modifier.size(Space.x2))
        TextButton(onClick = {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://marcmayol.com/building-my-future/privacidad.html")
                )
            )
        }) { Text(stringResource(R.string.ini_set_privacy), maxLines = 1) }
    }
}

@Composable
private fun SeccionCopia(viewModel: PlanViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // El recurso del mensaje y no el texto: así se pinta en el idioma de la app.
    var mensaje by remember { mutableStateOf<Int?>(null) }
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
                R.string.ini_backup_saved
            }.getOrElse { R.string.ini_backup_save_failed }
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
                mensaje = R.string.ini_backup_read_failed
                return@launch
            }
            when (val resultado = Backup.parse(leido)) {
                is Backup.Result.Ok -> porRestaurar = resultado.file
                is Backup.Result.Error -> mensaje = resultado.message
            }
        }
    }

    Bloque {
        Text(stringResource(R.string.ini_backup_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(Space.x1))
        Text(
            stringResource(R.string.ini_backup_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(Space.x3))
        Button(
            onClick = { guardar.launch(Backup.suggestedName(System.currentTimeMillis())) },
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
        ) { Text(stringResource(R.string.ini_backup_save), maxLines = 1) }
        Spacer(Modifier.size(Space.x2))
        OutlinedButton(
            onClick = { abrir.launch(arrayOf("application/json", "text/plain", "*/*")) },
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().heightIn(min = Touch.primary)
        ) { Text(stringResource(R.string.ini_backup_restore_button), maxLines = 1) }
        mensaje?.let { recurso ->
            Spacer(Modifier.size(Space.x2))
            Text(
                stringResource(recurso),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    porRestaurar?.let { copia ->
        AlertDialog(
            onDismissRequest = { porRestaurar = null },
            title = { Text(stringResource(R.string.ini_backup_confirm_title)) },
            text = {
                val dias = copia.diasCompletados()
                Text(
                    stringResource(
                        R.string.ini_backup_confirm_body,
                        pluralStringResource(R.plurals.ini_backup_completed_days, dias, dias)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val file = copia
                    porRestaurar = null
                    scope.launch {
                        val idiomaAntes = AppLanguage.mode(context)
                        mensaje = runCatching {
                            Backup.restore(context, file)
                            viewModel.reloadAfterRestore()
                            R.string.ini_backup_restored
                        }.getOrElse { R.string.ini_backup_restore_failed }
                        // La copia puede traer otro idioma: solo se ve al recrear la actividad.
                        if (AppLanguage.mode(context) != idiomaAntes) context.findActivity()?.recreate()
                    }
                }) { Text(stringResource(R.string.ini_backup_restore)) }
            },
            dismissButton = {
                TextButton(onClick = { porRestaurar = null }) { Text(stringResource(R.string.ini_backup_cancel)) }
            }
        )
    }
}

@Composable
internal fun Bloque(content: @Composable () -> Unit) {
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

/** La actividad que hay debajo de un contexto de Compose (puede venir envuelto). */
private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
