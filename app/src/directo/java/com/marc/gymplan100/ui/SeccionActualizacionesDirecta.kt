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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.BuildConfig
import com.marc.gymplan100.R
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
        Text(stringResource(R.string.ini_updates_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(Space.x1))
        Text(
            stringResource(R.string.ini_upd_direct_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(Space.x3))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.ini_upd_auto), modifier = Modifier.weight(1f))
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
        ) { Text(stringResource(R.string.ini_upd_check_now)) }

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
                    stringResource(
                        R.string.ini_upd_update_to,
                        (estado as EstadoActualizacion.Disponible).info.versionName
                    )
                )
            }
        }

        EstadoComprobacionManual(estado)

        Spacer(Modifier.size(Space.x3))
        Text(
            stringResource(R.string.ini_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Traduce el estado a una línea legible bajo el botón de comprobar. */
@Composable
private fun EstadoComprobacionManual(estado: EstadoActualizacion) {
    val texto = when (estado) {
        EstadoActualizacion.Comprobando -> stringResource(R.string.ini_upd_checking)
        EstadoActualizacion.AlDia -> stringResource(R.string.ini_upd_up_to_date)
        is EstadoActualizacion.Disponible -> null       // ya lo dice el botón
        is EstadoActualizacion.Descargando -> stringResource(R.string.ini_upd_downloading, estado.porcentaje)
        EstadoActualizacion.Verificando -> stringResource(R.string.ini_upd_verifying)
        EstadoActualizacion.Instalando -> stringResource(R.string.ini_installing)
        is EstadoActualizacion.Error -> when (estado.tipo) {
            TipoError.SIN_RED -> stringResource(R.string.ini_upd_err_offline)
            TipoError.HTTP, TipoError.MANIFIESTO -> stringResource(R.string.ini_upd_err_check)
            TipoError.DESCARGA -> stringResource(R.string.ini_upd_err_download)
            TipoError.HASH -> stringResource(R.string.ini_upd_err_hash)
            // El mensaje viene del módulo :actualizador (fuera de esta app), tal cual.
            TipoError.INSTALACION -> estado.mensaje ?: stringResource(R.string.ini_upd_err_install)
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
