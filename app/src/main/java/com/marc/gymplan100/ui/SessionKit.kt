package com.marc.gymplan100.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch
import com.marc.gymplan100.ui.theme.stateHalo
import kotlinx.coroutines.delay

/**
 * Piezas comunes de la sesión guiada.
 *
 * La idea del rediseño: **el estado tiñe la pantalla**. Ámbar preparas, naranja trabajas,
 * magenta esperas. Así, a un brazo de distancia y de reojo, el color ya dice si toca darle o
 * estás descansando, sin leer una palabra. Cada estado tiene un único gesto principal, siempre
 * en el mismo sitio: lo demás vive encima y se lee como secundario.
 */

/** Los cuatro ambientes de la sesión. */
enum class SessionTint { WARMUP, WORK, REST, NEUTRAL }

@Composable
private fun tintSurface(tint: SessionTint): Color {
    val c = LocalAppColors.current
    return when (tint) {
        SessionTint.WARMUP -> c.warmupSurface
        SessionTint.WORK -> c.workSurface
        SessionTint.REST -> c.restSurface
        SessionTint.NEUTRAL -> MaterialTheme.colorScheme.background
    }
}

@Composable
fun tintAccent(tint: SessionTint): Color {
    val c = LocalAppColors.current
    return when (tint) {
        SessionTint.WARMUP -> c.warmup
        SessionTint.WORK -> c.work
        SessionTint.REST -> c.rest
        SessionTint.NEUTRAL -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Armazón de un estado de la sesión: fondo teñido, cabecera, contenido y barra de acción.
 *
 * [content] recibe el espacio que sobra: la cifra o el ejercicio van ahí, alineados a la
 * izquierda. La barra inferior no se mueve nunca de sitio; entre estados solo cambia de
 * etiqueta y de color, para que el pulgar no tenga que buscarla.
 */
@Composable
fun SessionShell(
    tint: SessionTint,
    dark: Boolean,
    stateLabel: String,
    context: String,
    onExit: () -> Unit,
    secondaries: @Composable RowScope.() -> Unit = {},
    contextCard: @Composable ColumnScope.() -> Unit = {},
    primary: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surface by animateColorAsState(
        targetValue = tintSurface(tint),
        animationSpec = tween(260),
        label = "tinte de estado"
    )
    val accent = tintAccent(tint)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surface)
    ) {
        // El halo es un degradado radial, no una imagen: en oscuro da profundidad al color de
        // estado sin ensuciar el texto. En claro el tinte ya vive en la superficie.
        if (dark) {
            // El alto tiene que superar el radio del degradado (900 px): si se queda corto, el
            // halo se corta en seco a media pantalla y se ve la costura.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(700.dp)
                    .background(stateHalo(accent, true))
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = Space.screen)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.x3, bottom = Space.x2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stateLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    modifier = Modifier.weight(1f)
                )
                if (context.isNotBlank()) {
                    Text(
                        context,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onExit, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Salir del entrenamiento",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // El contenido se apoya en la parte baja: así la cifra queda a la altura de los
            // ojos cuando el móvil está en el banco, y el aire sobrante se va arriba.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = Space.block),
                verticalArrangement = Arrangement.Bottom,
                content = content
            )

            contextCard()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.x3, bottom = Space.x3),
                horizontalArrangement = Arrangement.spacedBy(Space.x2),
                content = secondaries
            )

            primary()
        }
    }
}

/** Botón secundario en píldora: vive sobre el fondo teñido, así que va con borde, no relleno. */
@Composable
fun RowScope.SessionSecondary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outline = LocalAppColors.current.onTintOutline
    Box(
        modifier = modifier
            .weight(1f)
            .heightIn(min = Touch.min)
            .clip(CircleShape)
            .border(1.5.dp, outline, CircleShape)
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.x2, vertical = Space.x3),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * El gesto principal del estado. En oscuro va en blanco (máximo contraste sobre el tinte);
 * en claro, en el color del estado. Con [alert] pulsa para avisar de que el descanso ya acabó.
 */
@Composable
fun SessionPrimary(
    text: String,
    onClick: () -> Unit,
    dark: Boolean,
    tint: SessionTint,
    alert: Boolean = false,
    enabled: Boolean = true,
    gradient: Brush? = null,
) {
    val accent = tintAccent(tint)
    val container = if (dark) Color.White else accent
    val onContainer = if (dark) Color(0xFF1A1013) else Color.White

    val pulse = if (alert) {
        val transition = rememberInfiniteTransition(label = "aviso")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label = "halo"
        ).value
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Space.x3)
    ) {
        if (alert) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Touch.primaryInSession)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.25f + 0.25f * pulse))
            )
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (gradient == null) container else Color.Transparent,
                contentColor = onContainer
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Touch.primaryInSession)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Touch.primaryInSession)
                    .then(if (gradient != null) Modifier.background(gradient, CircleShape) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                    color = onContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Space.x4, vertical = Space.x3)
                )
            }
        }
    }
}

/**
 * La cifra del cronómetro. Tabular para que no baile al cambiar de dígito, y a la mitad del
 * tamaño que proponía el diseño: a él le resultaba excesiva y se comía el margen lateral.
 */
@Composable
fun Countdown(
    text: String,
    caption: String,
    captionColor: Color,
    modifier: Modifier = Modifier
) {
    val styles = LocalAppTextStyles.current
    val fontScale = LocalDensity.current.fontScale
    Column(modifier = modifier) {
        Text(
            text,
            style = if (fontScale > 1.3f) styles.countdownCompact else styles.countdown,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Space.x1))
        Text(
            caption,
            style = MaterialTheme.typography.titleMedium,
            color = captionColor,
        )
    }
}

/** Barra de progreso del descanso: 8 dp, color del estado. */
@Composable
fun SessionProgress(fraction: Float, tint: SessionTint) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        color = tintAccent(tint),
        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
    )
}

/** Tarjeta de contexto: lo que toca, lo que viene, sin robarle protagonismo a la cifra. */
@Composable
fun SessionCard(
    label: String,
    dark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val bg = if (dark) Color.White.copy(alpha = 0.08f)
    else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(Space.x4)
    ) {
        if (label.isNotBlank()) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Space.x2))
        }
        content()
    }
}

// El botón de stepper vive en Steppers.kt: lo usan también el editor de planes y la pantalla
// de planes, que no dependen de este armazón de sesión.

