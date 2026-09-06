package com.marc.gymplan100.review

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
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
import com.google.android.play.core.review.ReviewManagerFactory
import com.marc.gymplan100.BuildConfig
import com.marc.gymplan100.ui.theme.Space

/**
 * Valorar la app, en la variante de Google Play.
 *
 * Hay **dos caminos distintos a propósito**, y no es un capricho:
 *
 * · El de después de entrenar usa la API oficial de Play (*in-app review*), que enseña las
 *   estrellas encima de la app sin sacarte de ella. Google es explícito en que esa API **no
 *   puede colgar de un botón**: tiene cuota, decide ella si se muestra, y no avisa de nada. Un
 *   botón que la mitad de las veces no hace nada es un botón roto, así que va sola, cuando toca
 *   ([MomentoDeValorar]).
 *
 * · El de Ajustes es un enlace a la ficha de la tienda. Ese sí se puede pulsar siempre y
 *   siempre hace algo, que es lo que se espera de un botón.
 *
 * Nada de esto necesita permiso de internet: el diálogo lo pinta la app de Play Store, y el
 * enlace lo abre ella. Esta variante sigue sin pedir red, como dice su ficha.
 */
object Valoracion {

    /** Aquí sí hay dónde valorar: la tienda. */
    const val DISPONIBLE = true

    private const val ID = BuildConfig.APPLICATION_ID

    /**
     * Acaba de terminar un entreno: si toca, pide la valoración.
     *
     * Si Play dice que no —sin conexión, cuota agotada, ya valoró— no se marca nada y se
     * volverá a intentar el próximo día. No hay diálogo propio de repuesto: insistir por
     * nuestra cuenta después de que la tienda haya dicho que no es justo lo que molesta.
     */
    fun trasEntrenar(context: Context) {
        if (!MomentoDeValorar.tocaTrasEntrenar(context)) return
        val activity = context.actividad() ?: return
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            manager.launchReviewFlow(activity, task.result)
            // Se apunta al lanzarlo, no al cerrarse: Google no dice si el usuario valoró ni
            // si llegó a ver el diálogo. Lo único que sabemos es que se ha preguntado.
            MomentoDeValorar.marcaPedida(activity)
        }
    }

    /** El bloque de Ajustes: el enlace de siempre a la ficha. */
    @Composable
    fun SettingsSection() {
        val context = LocalContext.current
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Valorar la app", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(Space.x1))
            Text(
                "La app no tiene detrás ninguna empresa ni se anuncia en ningún sitio: se " +
                    "encuentra porque alguien la valora. Si te está sirviendo, dilo en la ficha.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(Space.x2))
            TextButton(onClick = { abreLaFicha(context) }) {
                Text("Valorar en Google Play", maxLines = 1)
            }
        }
    }

    /**
     * Abre la ficha en la app de la tienda y, si no está instalada, en el navegador. El
     * segundo intento no sobra: hay móviles con Play Store desactivada.
     */
    private fun abreLaFicha(context: Context) {
        val enLaTienda = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$ID"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(enLaTienda)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$ID")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * La Activity que hay detrás de este Context. En Compose lo que llega suele ser un
     * ContextWrapper, y la API de Play necesita la Activity de verdad para dibujarse encima.
     */
    private fun Context.actividad(): Activity? {
        var c: Context? = this
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }
}
