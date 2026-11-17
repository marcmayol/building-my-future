package com.marc.gymplan100

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.marc.gymplan100.data.IosContext
import com.marc.gymplan100.data.ahoraMs
import com.marc.gymplan100.data.preferencias
import com.marc.gymplan100.shared.Res
import com.marc.gymplan100.shared.ini_rate_app_store
import com.marc.gymplan100.shared.ini_rate_body
import com.marc.gymplan100.shared.ini_rate_title
import com.marc.gymplan100.ui.theme.Space
import org.jetbrains.compose.resources.stringResource
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindowScene

/**
 * Valorar la app en el iPhone. La misma regla que en Android (MomentoDeValorar): después del
 * tercer entreno terminado y, si se pregunta, no otra vez en 120 días. Apple también decide
 * por su cuenta si enseña las estrellas (como mucho tres veces al año).
 */
object ValoracionIos {

    /**
     * El id de la app en el App Store («1234567890»). Hasta que esté publicada no hay ficha a
     * la que mandar, y Ajustes no enseña el bloque.
     */
    private val ID_APP_STORE: String? = null

    val hayFicha: Boolean get() = ID_APP_STORE != null

    private const val ENTRENOS_MINIMOS = 3
    private const val DIAS_ENTRE_PETICIONES = 120
    private const val UN_DIA_MS = 24L * 60 * 60 * 1000
    private val prefs get() = IosContext.preferencias("gym_valoracion")

    fun trasEntrenar() {
        val entrenos = (prefs.getString("entrenos_terminados")?.toIntOrNull() ?: 0) + 1
        prefs.editar { putString("entrenos_terminados", entrenos.toString()) }
        if (entrenos < ENTRENOS_MINIMOS) return
        val ultima = prefs.getString("ultima_peticion")?.toLongOrNull() ?: 0L
        val ahora = ahoraMs()
        if (ultima > 0 && ahora - ultima < DIAS_ENTRE_PETICIONES * UN_DIA_MS) return
        val escena = UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene
            ?: return
        SKStoreReviewController.requestReviewInScene(escena)
        prefs.editar { putString("ultima_peticion", ahora.toString()) }
    }

    @Composable
    fun SeccionAjustes() {
        val id = ID_APP_STORE ?: return
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.ini_rate_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(Space.x1))
            Text(
                stringResource(Res.string.ini_rate_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(Space.x2))
            TextButton(onClick = {
                PlataformaIos.abrirEnlace("itms-apps://apps.apple.com/app/id$id?action=write-review")
            }) { Text(stringResource(Res.string.ini_rate_app_store), maxLines = 1) }
        }
    }
}
