package com.marcm.actualizador

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Comprobación periódica en segundo plano. Solo comprueba y persiste el resultado
 * (el banner aparecerá al abrir la app); nunca descarga, notifica ni molesta. Como
 * es AUTOMATICO, cualquier error muere en silencio dentro de la fachada.
 */
class ComprobacionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val actualizador = Actualizador.instancia() ?: return Result.success()
        return try {
            actualizador.comprobar(Modo.AUTOMATICO)
            actualizador.programarPeriodica() // re-programa con el check_horas más fresco
            Result.success()
        } catch (e: Exception) {
            // Tolerancia a fallos: nunca reintentar de forma ruidosa.
            Result.success()
        }
    }

    companion object {
        private const val NOMBRE = "actualizador_comprobacion_periodica"

        /** (Re)programa el trabajo periódico con constraint de red conectada. */
        fun programar(context: Context, horas: Long) {
            val request = PeriodicWorkRequestBuilder<ComprobacionWorker>(
                horas.coerceAtLeast(1), TimeUnit.HOURS,
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NOMBRE,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
