package com.marcm.actualizador

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller

/**
 * Recibe el resultado de la sesión de PackageInstaller:
 *  - STATUS_PENDING_USER_ACTION: el sistema pide confirmación → lanzamos su Intent.
 *  - STATUS_SUCCESS / resto: informamos a [EventosInstalacion] para que la UI reaccione.
 *
 * Se declara en el manifest del módulo (se mergea en la app). El broadcast es
 * explícito (PendingIntent a esta clase), así que no le afectan las restricciones
 * de broadcasts implícitos.
 */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirm != null) context.startActivity(confirm)
            }

            PackageInstaller.STATUS_SUCCESS -> {
                EventosInstalacion.emitir(exito = true, mensaje = null)
            }

            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                EventosInstalacion.emitir(exito = false, mensaje = msg)
            }
        }
    }

    companion object {
        const val ACCION = "com.marcm.actualizador.INSTALL_RESULT"
    }
}

/** Puente sencillo entre el receiver (proceso/sistema) y la fachada/UI. */
object EventosInstalacion {
    @Volatile
    var onResultado: ((exito: Boolean, mensaje: String?) -> Unit)? = null

    fun emitir(exito: Boolean, mensaje: String?) {
        onResultado?.invoke(exito, mensaje)
    }
}
