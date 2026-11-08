@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.marc.gymplan100.R
import com.marc.gymplan100.ui.theme.Space

/** Un apartado de la ayuda: qué es, cómo se ve y qué hacer, paso a paso. Los textos son recursos. */
private data class HelpSection(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    val image: Int?,
    @ArrayRes val steps: Int
)

private val SECCIONES = listOf(
    HelpSection(
        R.string.ayu_help_inicio_title,
        R.string.ayu_help_inicio_summary,
        R.drawable.ayuda_inicio,
        R.array.ayu_help_inicio_steps
    ),
    HelpSection(
        R.string.ayu_help_dia_title,
        R.string.ayu_help_dia_summary,
        R.drawable.ayuda_dia,
        R.array.ayu_help_dia_steps
    ),
    HelpSection(
        R.string.ayu_help_expres_title,
        R.string.ayu_help_expres_summary,
        R.drawable.ayuda_expres,
        R.array.ayu_help_expres_steps
    ),
    HelpSection(
        R.string.ayu_help_serie_title,
        R.string.ayu_help_serie_summary,
        R.drawable.ayuda_serie,
        R.array.ayu_help_serie_steps
    ),
    HelpSection(
        R.string.ayu_help_descanso_title,
        R.string.ayu_help_descanso_summary,
        R.drawable.ayuda_descanso,
        R.array.ayu_help_descanso_steps
    ),
    HelpSection(
        R.string.ayu_help_maquina_title,
        R.string.ayu_help_maquina_summary,
        R.drawable.ayuda_ficha,
        R.array.ayu_help_maquina_steps
    ),
    HelpSection(
        R.string.ayu_help_libre_title,
        R.string.ayu_help_libre_summary,
        R.drawable.ayuda_libre,
        R.array.ayu_help_libre_steps
    ),
    HelpSection(
        R.string.ayu_help_salir_title,
        R.string.ayu_help_salir_summary,
        R.drawable.ayuda_salir,
        R.array.ayu_help_salir_steps
    ),
    HelpSection(
        R.string.ayu_help_resultados_title,
        R.string.ayu_help_resultados_summary,
        R.drawable.ayuda_resultados,
        R.array.ayu_help_resultados_steps
    ),
    HelpSection(
        R.string.ayu_help_estadisticas_title,
        R.string.ayu_help_estadisticas_summary,
        R.drawable.ayuda_estadisticas,
        R.array.ayu_help_estadisticas_steps
    ),
    HelpSection(
        R.string.ayu_help_pesos_title,
        R.string.ayu_help_pesos_summary,
        R.drawable.ayuda_pesos,
        R.array.ayu_help_pesos_steps
    ),
    HelpSection(
        R.string.ayu_help_planes_title,
        R.string.ayu_help_planes_summary,
        R.drawable.ayuda_planes,
        R.array.ayu_help_planes_steps
    ),
    HelpSection(
        R.string.ayu_help_ajustes_title,
        R.string.ayu_help_ajustes_summary,
        R.drawable.ayuda_ajustes,
        R.array.ayu_help_ajustes_steps
    ),
    HelpSection(
        R.string.ayu_help_reloj_title,
        R.string.ayu_help_reloj_summary,
        null,
        R.array.ayu_help_reloj_steps
    ),
)

/**
 * La ayuda de toda la app, con capturas de verdad.
 *
 * Es una pantalla aparte y de solo lectura: no lee ni escribe el progreso, así que se puede
 * abrir en mitad de un descanso y volver exactamente donde estabas. Va por apartados plegados
 * (el índice es la propia lista) para no repetir lo que la gente ya ve agobiante: una pared de
 * texto.
 */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    var abierto by rememberSaveable { mutableStateOf<Int?>(null) }
    var ampliada by rememberSaveable { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ayu_help_titulo), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ayu_help_volver))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Space.screen,
                end = Space.screen,
                top = inner.calculateTopPadding() + Space.x2,
                bottom = 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x2)
        ) {
            item {
                Text(
                    stringResource(R.string.ayu_help_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Space.x2)
                )
            }
            items(SECCIONES, key = { it.title }) { s ->
                val open = abierto == s.title
                val titulo = stringResource(s.title)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { abierto = if (open) null else s.title }
                            .padding(Space.x4),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(titulo, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(s.summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (open) {
                        Column(
                            modifier = Modifier.padding(start = Space.x4, end = Space.x4, bottom = Space.x4),
                            verticalArrangement = Arrangement.spacedBy(Space.x2)
                        ) {
                            s.image?.let { img ->
                                // La captura, estrecha y centrada (es un móvil dentro del
                                // móvil); un toque la amplía.
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(img),
                                        contentDescription = stringResource(R.string.ayu_help_captura, titulo),
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .widthIn(max = 220.dp)
                                            .heightIn(max = 420.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                            .clickable { ampliada = img }
                                    )
                                }
                            }
                            stringArrayResource(s.steps).forEachIndexed { i, paso ->
                                Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                                    Text(
                                        "${i + 1}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(paso, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ampliada?.let { img ->
        Dialog(
            onDismissRequest = { ampliada = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { ampliada = null }
                    .padding(Space.x4),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(img),
                    contentDescription = stringResource(R.string.ayu_help_captura_ampliada),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
    }
}

/**
 * Abre la ayuda desde donde haga falta sin pasar el navegador por media app. Lo provee el
 * NavHost; fuera de él (vistas previas, tests) vale null y el botón no se pinta.
 */
val LocalOpenHelp = androidx.compose.runtime.staticCompositionLocalOf<(() -> Unit)?> { null }
