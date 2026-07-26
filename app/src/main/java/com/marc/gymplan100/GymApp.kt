package com.marc.gymplan100

import android.app.Application
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.ActualizadorConfig

class GymApp : Application() {

    /**
     * Auto-actualización: la app se distribuye fuera de Play Store, así que consulta
     * un manifiesto estático publicado en GitHub Pages (nunca la API de GitHub).
     */
    val actualizador: Actualizador by lazy {
        Actualizador(
            app = this,
            config = ActualizadorConfig(
                manifiestoUrl = "https://marcmayol.com/building-my-future/updates.json",
                versionCodeActual = BuildConfig.VERSION_CODE,
                checkHorasPorDefecto = 24,
            ),
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Comprobación periódica en segundo plano (WorkManager, solo con red).
        actualizador.programarPeriodica()
    }
}
