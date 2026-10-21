@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.marc.gymplan100.R
import com.marc.gymplan100.ui.theme.Space

/** Un apartado de la ayuda: qué es, cómo se ve y qué hacer, paso a paso. */
private data class HelpSection(
    val title: String,
    val summary: String,
    val image: Int?,
    val steps: List<String>
)

private val SECCIONES = listOf(
    HelpSection(
        "Inicio",
        "Lo que te toca hoy y cómo vas.",
        R.drawable.ayuda_inicio,
        listOf(
            "Arriba, el plan que sigues y la fase. «Ver otros planes» lleva a la lista: hay planes de fuerza, de músculo, para casa, para empezar desde cero…",
            "El bloque grande es el día que toca. «Empezar día» arranca el entreno guiado; «Ver el día antes de empezar» enseña los ejercicios sin empezar nada.",
            "Si tienes un entreno a medias, ese bloque dice «En curso» y el botón lo reanuda donde lo dejaste.",
            "«Entreno libre» es para apuntar lo que hagas sin seguir el plan. Los «Entrenamientos especiales» son rutinas sueltas que no cuentan como día.",
            "Abajo, la barra: Logros, Mis pesos, Resultados y Estadísticas."
        )
    ),
    HelpSection(
        "La ficha del día",
        "Los ejercicios de un día, antes o después de hacerlo.",
        R.drawable.ayuda_dia,
        listOf(
            "Cada ejercicio con su dibujo, las series y repeticiones («3 x 12») y el descanso entre series.",
            "«Cómo se hace» abre la ficha: músculos, técnica, errores típicos, alternativas y un vídeo.",
            "«Apuntar a mano» sirve para anotar un día que entrenaste sin la app. Lo hecho con el entreno guiado se apunta solo al terminar.",
            "Si tienes el entreno de ese día a medias, el botón dice «Continuar entrenamiento»."
        )
    ),
    HelpSection(
        "Antes de empezar: el exprés",
        "Cuánto dura el día y cómo recortarlo si vas justo.",
        R.drawable.ayuda_expres,
        listOf(
            "Al empezar ves cuánto suele durar el día. La cuenta aprende de lo que tú tardas de verdad.",
            "Si vas justo, elige 45 o 30 minutos: todos los ejercicios bajan a 2 series y, si aún no cabe, se quitan los del final. La app te dice cuáles.",
            "Un exprés cuenta como día hecho igual que uno entero."
        )
    ),
    HelpSection(
        "La serie",
        "Lo que haces con la máquina delante.",
        R.drawable.ayuda_serie,
        listOf(
            "Pon el peso con − y + (de medio en medio kilo; manteniendo pulsado corre), con «Rueda» o tocando la cifra para escribirla.",
            "Las repeticiones vienen puestas con las del plan: tócalas solo si hoy han salido otras.",
            "«Serie de calentamiento» apunta una serie de aproximación sin gastar serie del plan.",
            "«Máquina ocupada» deja ese ejercicio para el final y pasa al siguiente. «Plan» enseña todo el día y por dónde vas.",
            "Cuando acabes, «Serie hecha»."
        )
    ),
    HelpSection(
        "El descanso",
        "La cuenta atrás entre series.",
        R.drawable.ayuda_descanso,
        listOf(
            "El descanso depende del ejercicio: 1:30 en series normales, 3 minutos en series pesadas (8 repeticiones o menos en un básico) o lo que diga el plan. Debajo de la cifra pone de dónde sale.",
            "−30 s y +30 s lo ajustan. Si lo cambias entre series, la próxima vez ese ejercicio ya empieza con tu descanso.",
            "Suena y vibra al acabar aunque tengas la pantalla apagada (y el reloj, si lo usas).",
            "«¿Cómo ha ido?» apunta si la serie fue fácil, justa o al fallo. En «Después» puedes dejar preparado el peso de la siguiente."
        )
    ),
    HelpSection(
        "Si no tienes la máquina",
        "Hacer otro ejercicio y que quede apuntado como lo que fue.",
        R.drawable.ayuda_ficha,
        listOf(
            "En la serie, toca «Cómo se hace» y baja a «Si está ocupada o no la tienes».",
            "Las alternativas que la app conoce salen con su dibujo: toca «La hago» y el ejercicio de hoy pasa a ser ese, con sus propios pesos.",
            "«He hecho otro ejercicio…» busca entre 151 ejercicios o deja escribir el tuyo.",
            "Con «Recordarlo para los próximos días» marcado, los días siguientes ya empiezan con el cambio. «Volver al del plan» lo deshace."
        )
    ),
    HelpSection(
        "Salir sin perder nada",
        "El entreno se guarda solo mientras lo haces.",
        R.drawable.ayuda_salir,
        listOf(
            "El botón atrás del móvil te lleva a Inicio con el entreno en marcha: lo reanudas desde el bloque «En curso».",
            "La X de arriba pregunta qué quieres: «Terminar y guardar» da el día por hecho con lo que lleves, «Reanudar luego» lo deja como está y «Descartar» lo borra.",
            "Mirar esta ayuda, la ficha o el plan nunca toca el entreno."
        )
    ),
    HelpSection(
        "Resultados",
        "Lo que hiciste cada día.",
        R.drawable.ayuda_resultados,
        listOf(
            "Cada entreno con su duración y, por ejercicio, los kilos de cada serie.",
            "«Recientes / Antiguos» cambia el orden.",
            "Si hiciste otro ejercicio en lugar del del plan, sale con su nombre y «en lugar de…»."
        )
    ),
    HelpSection(
        "Estadísticas",
        "Tu racha, tus números y tu progreso.",
        R.drawable.ayuda_estadisticas,
        listOf(
            "Resumen: racha, días hechos, tiempo total, series y kilos movidos.",
            "Pesos: cómo ha subido cada ejercicio. Músculos: qué has trabajado los últimos días. Constancia: qué días entrenas."
        )
    ),
    HelpSection(
        "Mis pesos",
        "El peso con el que trabajas en cada ejercicio.",
        R.drawable.ayuda_pesos,
        listOf(
            "Agrupados por músculo. Se rellenan solos al entrenar y los puedes corregir.",
            "Es el peso de tu última sesión; tu récord (PR) sale debajo de cada ejercicio."
        )
    ),
    HelpSection(
        "Planes",
        "Cambiar de plan, importar uno o crear el tuyo.",
        R.drawable.ayuda_planes,
        listOf(
            "Cada plan guarda su propio progreso: cambiar de plan no borra nada.",
            "Debajo de cada uno: su objetivo (fuerza, músculo…), los días y cuántos llevas.",
            "«Crear un plan» abre el editor; «Importar» trae uno en JSON o Markdown."
        )
    ),
    HelpSection(
        "Ajustes",
        "Tus datos, avisos, tema y copia de seguridad.",
        R.drawable.ayuda_ajustes,
        listOf(
            "Peso (para estimar calorías), género de los dibujos y si usas reloj.",
            "Recordatorios: elige días y hora y la app te avisa de que toca entrenar.",
            "Tema claro u oscuro, y copia de seguridad para llevarte el progreso a otro móvil."
        )
    ),
    HelpSection(
        "Con el reloj",
        "Si tienes un reloj con Wear OS.",
        null,
        listOf(
            "Instala la app del reloj y el reloj hará de mando: «Serie hecha», saltar el descanso o cambiar de máquina sin sacar el móvil.",
            "El reloj vibra al acabar cada descanso y, mientras entrenas, enseña en la esfera el tiempo que queda."
        )
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
    var abierto by rememberSaveable { mutableStateOf<String?>(null) }
    var ampliada by rememberSaveable { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                    "Toca un apartado para verlo. Mirar la ayuda no cambia nada: si tienes un " +
                        "entreno en marcha, sigue exactamente igual al volver.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Space.x2)
                )
            }
            items(SECCIONES, key = { it.title }) { s ->
                val open = abierto == s.title
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
                            Text(s.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                s.summary,
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
                                        contentDescription = "Captura: ${s.title}",
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
                            s.steps.forEachIndexed { i, paso ->
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
                    contentDescription = "Captura ampliada. Toca para cerrar.",
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
