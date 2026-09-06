package com.marc.gymplan100.review

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.marc.gymplan100.BuildConfig
import com.marc.gymplan100.ui.theme.Space

/**
 * Valorar la app, en la variante que se reparte fuera de Play.
 *
 * Aquí no hay ficha ni estrellas: esta app se instala desde DracApps y no está en ninguna
 * tienda, así que no hay nada que puntuar. Lo que sí hay —y es lo que de verdad servía— es una
 * forma de contar qué falla, con la versión ya escrita en el asunto para no tener que
 * preguntarla después.
 *
 * Y **nunca interrumpe**: [trasEntrenar] no hace nada. La única razón de existir de esa función
 * es que la variante de Play tenga la misma forma que esta y el resto del código sea idéntico
 * en las dos, igual que pasa con [com.marc.gymplan100.update.Updates].
 */
object Valoracion {

    /** No hay tienda donde valorar, pero el bloque de Ajustes sigue teniendo algo que ofrecer. */
    const val DISPONIBLE = false

    private const val CORREO = "marcmayolorell@gmail.com"

    /** Fuera de Play no se pide ninguna nota: no hay dónde dejarla. */
    fun trasEntrenar(context: Context) = Unit

    /** El bloque de Ajustes: contar qué tal va, no puntuar. */
    @Composable
    fun SettingsSection() {
        val context = LocalContext.current
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("¿Qué tal te va?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(Space.x1))
            Text(
                "Esta copia no viene de ninguna tienda, así que no hay estrellas que poner. " +
                    "Si algo falla o echas algo en falta, escríbeme: el correo ya lleva puesta " +
                    "la versión que tienes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(Space.x2))
            TextButton(onClick = { escribeme(context) }) { Text("Escribirme", maxLines = 1) }
        }
    }

    private fun escribeme(context: Context) {
        val asunto = "Building My Future ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$CORREO")
            putExtra(Intent.EXTRA_SUBJECT, asunto)
        }
        // Un móvil sin ningún cliente de correo configurado existe: mejor no hacer nada que
        // reventar al volver de Ajustes.
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Unit
        }
    }
}
