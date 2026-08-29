package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.ui.theme.BrandAmber
import com.marc.gymplan100.ui.theme.BrandMagenta
import com.marc.gymplan100.ui.theme.BrandOrange
import com.marcm.actualizador.EstadoActualizacion

/**
 * Estados que el banner sí pinta. Se consulta antes de crear el item de la lista para
 * no dejar un hueco vacío en Home cuando no hay nada que anunciar.
 */
val EstadoActualizacion.pintaBanner: Boolean
    get() = this is EstadoActualizacion.Disponible ||
        this is EstadoActualizacion.Descargando ||
        this == EstadoActualizacion.Verificando ||
        this == EstadoActualizacion.Instalando

/**
 * Banner no bloqueante en la cabecera de Home: anuncia una versión nueva y muestra el
 * progreso de la actualización. Nunca aparece por errores ni por el "estás al día" —
 * eso vive en Configuración, donde el usuario ha pedido la comprobación.
 */
@Composable
fun BannerActualizacion(
    estado: EstadoActualizacion,
    onActualizar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (estado) {
        is EstadoActualizacion.Disponible -> MarcoBanner(modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Versión ${estado.info.versionName} disponible",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (estado.info.notas.isNotBlank()) {
                        Text(
                            text = estado.info.notas,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                Button(
                    onClick = onActualizar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = BrandMagenta
                    )
                ) {
                    Text("Actualizar", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        is EstadoActualizacion.Descargando -> MarcoBanner(modifier) {
            Progreso("Descargando la nueva versión… ${estado.porcentaje}%", estado.porcentaje / 100f)
        }

        EstadoActualizacion.Verificando -> MarcoBanner(modifier) {
            Progreso("Comprobando que la copia es íntegra…", null)
        }

        EstadoActualizacion.Instalando -> MarcoBanner(modifier) {
            Progreso("Instalando…", null)
        }

        // Inactivo, Comprobando, AlDia, PidiendoPermiso y Error no pintan nada.
        else -> Unit
    }
}

/** Tarjeta con el degradado de marca del logo (ámbar -> naranja -> magenta). */
@Composable
private fun MarcoBanner(modifier: Modifier, contenido: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(BrandAmber, BrandOrange, BrandMagenta)))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            contenido()
        }
    }
}

@Composable
private fun Progreso(texto: String, progreso: Float?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(Modifier.height(0.dp))
        if (progreso != null) {
            LinearProgressIndicator(
                progress = { progreso },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LinearProgressIndicator(
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
