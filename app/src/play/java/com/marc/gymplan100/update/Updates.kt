package com.marc.gymplan100.update

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.marc.gymplan100.BuildConfig
import com.marc.gymplan100.ui.theme.Space

/**
 * La auto-actualización, en la variante de Google Play: no hay ninguna.
 *
 * Play prohíbe que una app instale actualizaciones de sí misma por fuera de la tienda, así que
 * esta variante ni siquiera incluye el módulo `:actualizador` ni sus permisos
 * (`REQUEST_INSTALL_PACKAGES`, `UPDATE_PACKAGES_WITHOUT_USER_ACTION`). Aquí actualiza Play.
 *
 * Esta fachada tiene exactamente la misma forma que la de la variante `directo` y no hace
 * nada, para que el resto de la app sea el mismo código en las dos.
 */
object Updates {

    /** No hay auto-actualización en esta variante: la lleva Play. */
    const val DISPONIBLE = false

    fun onAppCreate(app: Application) = Unit

    @Suppress("RedundantSuspendModifier")
    suspend fun checkOnOpen(context: Context) = Unit

    fun onResume(context: Context) = Unit

    /** Sin banner: si hay versión nueva, la avisa la propia tienda. */
    @Composable
    fun Banner() = Unit

    /**
     * En Ajustes se dice quién actualiza, y no se deja el hueco vacío: la pregunta
     * "¿esto se actualiza solo?" se la hace todo el mundo.
     */
    @Composable
    fun SettingsSection() {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Actualizaciones", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(Space.x1))
            Text(
                "Las actualizaciones llegan por Google Play, como en cualquier otra app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(Space.x2))
            Text(
                "Versión ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
