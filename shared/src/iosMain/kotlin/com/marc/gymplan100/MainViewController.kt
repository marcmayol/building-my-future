package com.marc.gymplan100

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.marc.gymplan100.data.Contenido
import com.marc.gymplan100.data.Datos
import com.marc.gymplan100.data.ExerciseCatalog
import com.marc.gymplan100.data.IosContext
import com.marc.gymplan100.data.PlanStore
import com.marc.gymplan100.plataforma.localeApp
import com.marc.gymplan100.plataforma.plataforma
import com.marc.gymplan100.ui.GymNavHost
import com.marc.gymplan100.ui.theme.AppLanguage
import com.marc.gymplan100.ui.theme.AppTheme
import com.marc.gymplan100.ui.theme.GymPlanTheme
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages
import platform.Foundation.stringWithContentsOfFile
import platform.UIKit.UIViewController

/** Lo llama la app de Swift al arrancar, antes de pintar nada. */
@OptIn(ExperimentalForeignApi::class)
fun iniciarApp() {
    // Los mismos ficheros de datos que Android (app/src/main/assets), copiados al bundle.
    val carpeta = NSBundle.mainBundle.resourcePath + "/assets/"
    Datos.lector = { ruta ->
        NSString.stringWithContentsOfFile(carpeta + ruta, NSUTF8StringEncoding, null)
            ?: error("No está en el bundle: $ruta")
    }
    plataforma = PlataformaIos
    aplicarIdioma()
    // Lo que se pregunta desde sitios sin contexto (el mapa muscular, el plan activo…).
    ExerciseCatalog.load(runCatching { Datos.leer("catalogo.json") }.getOrElse { "[]" })
    PlanStore.load(IosContext)
    AppTheme.load(IosContext)
    reprogramarRecordatorio()
}

/**
 * El idioma elegido en Ajustes. Compose Multiplatform y el resto del iPhone leen el idioma de
 * AppleLanguages; en automático se deja el del sistema.
 */
fun aplicarIdioma() {
    val defaults = NSUserDefaults.standardUserDefaults
    // Se quita lo que guardó la app para leer la lista de idiomas del sistema (dominio global).
    defaults.removeObjectForKey("AppleLanguages")
    val sistema = (defaults.arrayForKey("AppleLanguages") ?: NSLocale.preferredLanguages)
        .mapNotNull { it as? String }
    val modo = AppLanguage.mode(IosContext)
    // Como en Android: con el móvil en [catalán, español] la app sale en español, no en inglés.
    // Se elige el primer idioma de la lista que la app tiene (es o en) y se pone delante, porque
    // Compose y sus recursos solo miran el primero de NSLocale.preferredLanguages.
    val preferido = modo.tag
        ?: sistema.firstOrNull { it.startsWith("es") || it.startsWith("en") }
        ?: "en"
    defaults.setObject(listOf(preferido) + sistema.filter { it != preferido }, "AppleLanguages")
    // Cualquier variante de español ve la app en español; cualquier otro idioma, en inglés.
    val idioma = if (preferido.startsWith("es")) "es" else "en"
    localeApp = NSLocale(localeIdentifier = preferido)
    Contenido.sync(idioma == "en")
}

fun MainViewController(): UIViewController = ComposeUIViewController {
    key(PlataformaIos.reinicios) {
        GymPlanTheme(darkTheme = AppTheme.isDark()) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize()) {
                    GymNavHost(openSessionDay = null, onSessionConsumed = {})
                    // En el iPhone no hay Toast: el aviso corto sale abajo y se va solo.
                    val aviso = PlataformaIos.avisoActual
                    AnimatedVisibility(
                        visible = aviso != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface
                        ) {
                            Text(aviso.orEmpty(), modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                        }
                    }
                }
            }
        }
    }
}
