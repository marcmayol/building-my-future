package com.marc.gymplan100.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.BuildConfig
import com.marc.gymplan100.ui.theme.Space
import com.marc.gymplan100.ui.theme.Touch
import com.marcm.actualizador.EstadoActualizacion
import com.marcm.actualizador.Modo
import com.marcm.actualizador.TipoError
import kotlinx.coroutines.launch

/**
 * El bloque de actualizaciones de Ajustes, solo en la variante que se reparte fuera de Play.
 *
 * Vive en el flavor `directo` porque habla con el modulo `:actualizador`, que la variante de
 * Play no incluye: alli este bloque lo sustituye un texto diciendo que actualiza la tienda.
 */
@Composable
fun SeccionActualizacionesDirecta(actualizador: com.marcm.actualizador.Actualizador) {
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
