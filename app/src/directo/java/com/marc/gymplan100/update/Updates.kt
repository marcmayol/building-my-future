package com.marc.gymplan100.update

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.marc.gymplan100.BuildConfig
import com.marc.gymplan100.ui.BannerActualizacion
import com.marc.gymplan100.ui.pintaBanner
import com.marc.gymplan100.ui.SeccionActualizacionesDirecta
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.ActualizadorConfig
import com.marcm.actualizador.Modo

/**
 * La auto-actualización, en la variante que se reparte FUERA de Play.
 *
 * Toda la app habla con este objeto y no con el módulo `:actualizador` directamente. El motivo
 * es que la variante de Play no lo lleva —la tienda prohíbe que una app se actualice por su
 * cuenta— y necesita una fachada con la misma forma que no haga nada. Así el resto del código
 * (portada, ajustes, MainActivity) es idéntico en las dos y no se llena de condicionales.
 */
object Updates {

    /** Existe la auto-actualización en esta variante. */
    const val DISPONIBLE = true

    private var actualizador: Actualizador? = null

    private fun of(context: Context): Actualizador {
        val app = context.applicationContext as Application
        return actualizador ?: Actualizador(
            app = app,
            config = ActualizadorConfig(
                manifiestoUrl = "https://marcmayol.com/building-my-future/updates.json",
                versionCodeActual = BuildConfig.VERSION_CODE,
                checkHorasPorDefecto = 24,
            ),
        ).also { actualizador = it }
    }

    /** Al arrancar la app: deja programada la comprobación periódica (WorkManager, con red). */
    fun onAppCreate(app: Application) {
        of(app).programarPeriodica()
    }

    /**
     * Unos segundos después de abrir, en silencio: si falla (sin red, manifiesto roto…) no se
     * entera nadie. La única comprobación que informa de errores es la manual de Ajustes.
     */
    suspend fun checkOnOpen(context: Context) {
        of(context).comprobar(Modo.AUTOMATICO)
    }

    /** Al volver a la app: reanuda si acaban de conceder el permiso de instalación. */
    fun onResume(context: Context) {
        of(context).onPermisoQuizaConcedido()
    }

    /** El aviso de versión nueva en la portada. Solo aparece si hay algo que decir. */
    @Composable
    fun Banner() {
        val context = LocalContext.current
        val act = remember(context) { of(context) }
        val estado by act.estado.collectAsState()
        if (estado.pintaBanner) {
            BannerActualizacion(estado = estado, onActualizar = { act.actualizarAhora() })
        }
    }

    /** El bloque de Ajustes: interruptor de comprobación automática y "Buscar ahora". */
    @Composable
    fun SettingsSection() {
        val context = LocalContext.current
        SeccionActualizacionesDirecta(remember(context) { of(context) })
    }
}
