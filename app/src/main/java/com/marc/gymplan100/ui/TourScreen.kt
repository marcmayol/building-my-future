package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marc.gymplan100.R
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch
import kotlinx.coroutines.launch

/**
 * El recorrido de la primera vez: qué hace la app y dónde está cada cosa.
 *
 * Sale **después** de elegir plan, no antes: con la portada ya llena, cada pantalla que se
 * nombra existe de verdad y se puede ir a mirarla al cerrar. Antes de elegir no había nada
 * detrás y era contar una película.
 *
 * Cada paso lleva una línea **Dónde** con el sitio exacto —el nombre que se lee en la
 * pantalla, no un "en el menú"—, porque el problema de quien estrena una app no es que no
 * entienda para qué sirve: es que no encuentra la mitad.
 *
 * Se sale en cualquier momento, y se puede volver a ver desde *Ajustes*: un recorrido que solo
 * existe una vez es un recorrido que nadie recuerda.
 */
private data class PasoDelTour(
    val titulo: String,
    val texto: String,
    val donde: String,
    val icono: Int?,
    val color: Color
)

@Composable
fun TourScreen(onDone: () -> Unit) {
    val app = LocalAppColors.current
    val pasos = listOf(
        PasoDelTour(
            titulo = stringResource(R.string.ayu_tour_1_titulo),
            texto = stringResource(R.string.ayu_tour_1_texto),
            donde = stringResource(R.string.ayu_tour_1_donde),
            icono = null,
            color = app.warmup
        ),
        PasoDelTour(
            titulo = stringResource(R.string.ayu_tour_2_titulo),
            texto = stringResource(R.string.ayu_tour_2_texto),
            donde = stringResource(R.string.ayu_tour_2_donde),
            icono = null,
            color = app.rest
        ),
        PasoDelTour(
            titulo = stringResource(R.string.ayu_tour_3_titulo),
            texto = stringResource(R.string.ayu_tour_3_texto),
            donde = stringResource(R.string.ayu_tour_3_donde),
            icono = R.drawable.ic_pesos,
            color = app.work
        ),
        PasoDelTour(
            titulo = stringResource(R.string.ayu_tour_4_titulo),
            texto = stringResource(R.string.ayu_tour_4_texto),
            donde = stringResource(R.string.ayu_tour_4_donde),
            icono = R.drawable.ic_estadisticas,
            color = app.streak
        ),
        PasoDelTour(
            titulo = stringResource(R.string.ayu_tour_5_titulo),
            texto = stringResource(R.string.ayu_tour_5_texto),
            donde = stringResource(R.string.ayu_tour_5_donde),
            icono = R.drawable.ic_logros,
            color = app.special
        )
    )

    val estado = rememberPagerState { pasos.size }
    val scope = rememberCoroutineScope()
    val ultimo = estado.currentPage == pasos.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Se dibuja encima del NavHost y no hereda los márgenes del sistema de nadie: sin
            // esto el titular se mete debajo del reloj.
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Cabecera: la marca y la salida. "Saltar" está desde el primer paso y a la vista,
        // porque un recorrido del que no se puede salir deja de ser un recorrido.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screen, vertical = Space.x2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(app.brandGradient)
            )
            Spacer(Modifier.size(Space.x2))
            Text(
                stringResource(R.string.ayu_tour_cabecera),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDone) { Text(stringResource(R.string.ayu_tour_saltar), maxLines = 1) }
        }

        HorizontalPager(
            state = estado,
            modifier = Modifier.weight(1f),
            pageSpacing = Space.x4,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = Space.screen
            )
        ) { pagina ->
            Paso(pasos[pagina])
        }

        // Los puntos dicen cuánto queda: cinco pasos se aguantan, y saberlo es justo lo que
        // evita que se salte por si acaso son veinte.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screen),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pasos.indices.forEach { i ->
                val activo = i == estado.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = Space.x1)
                        .size(if (activo) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (activo) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Spacer(Modifier.height(Space.block))

        Button(
            onClick = {
                if (ultimo) onDone()
                else scope.launch { estado.animateScrollToPage(estado.currentPage + 1) }
            },
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screen)
                .heightIn(min = Touch.primary)
        ) { Text(
            stringResource(if (ultimo) R.string.ayu_tour_empezar else R.string.ayu_tour_siguiente)
        ) }

        Spacer(Modifier.height(Space.block))
    }
}

@Composable
private fun Paso(paso: PasoDelTour) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Con la letra del sistema grande estos textos no caben: que se puedan desplazar
            // es la diferencia entre leerlos y verlos cortados.
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(paso.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (paso.icono != null) {
                Icon(
                    painter = painterResource(paso.icono),
                    contentDescription = null,
                    tint = paso.color,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(paso.color)
                )
            }
        }

        Spacer(Modifier.height(Space.block))

        Text(
            paso.titulo,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp)
        )

        Spacer(Modifier.height(Space.x4))

        Text(
            paso.texto,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(Space.block))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(Space.x4)
        ) {
            Text(
                stringResource(R.string.ayu_tour_donde),
                style = MaterialTheme.typography.labelMedium,
                color = paso.color,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Space.x1))
            Text(
                paso.donde,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
