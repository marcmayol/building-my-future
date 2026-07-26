package com.marcm.actualizador

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Instalación del APK ya descargado y verificado.
 *
 * Dos niveles de fricción:
 *  1) Permiso de "instalar apps desconocidas" (REQUEST_INSTALL_PACKAGES +
 *     canRequestPackageInstalls). Si falta, hay que mandar al usuario a ajustes
 *     con [intentPermisoInstalacion] (una sola vez; ese "una vez" lo controla la fachada).
 *  2) Confirmación de la instalación: si esta app es el "installer of record" de su
 *     propio paquete y la API lo permite (31+), la sesión pide NO requerir acción del
 *     usuario y la actualización se aplica en silencio. Si no lo es (p. ej. la primera
 *     vez, cuando la instaló adb o el sistema), PackageInstaller devuelve
 *     STATUS_PENDING_USER_ACTION y mostramos la confirmación del sistema. Traducción:
 *     la 1ª auto-actualización pide confirmar; a partir de ahí, silenciosas (31+).
 *     Ojo: eso exige además el permiso UPDATE_PACKAGES_WITHOUT_USER_ACTION, que
 *     declara el manifest del módulo. Sin él el sistema ignora la petición en
 *     silencio y sigue pidiendo confirmación siempre — comprobado en dispositivo.
 *
 * Dos vías de instalación: la sesión de [PackageInstaller] es la principal; si crear
 * o confirmar la sesión falla (fabricante, política, almacenamiento), se cae al
 * intent clásico de instalación con [FileProvider], que siempre muestra el diálogo
 * del sistema. La diferencia importa para la UI: la sesión avisa del resultado por
 * [InstallResultReceiver], el intent no avisa de nada (ver [Via]).
 */
object Instalador {

    /** Camino por el que finalmente se lanzó la instalación. */
    enum class Via {
        /** Sesión de PackageInstaller: el resultado llegará a [InstallResultReceiver]. */
        SESION,

        /** Intent + FileProvider: lo gestiona el instalador del sistema, sin callback. */
        INTENT,
    }

    /** ¿Tiene la app permiso para instalar APKs de orígenes desconocidos? */
    fun puedeInstalar(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** Intent a la pantalla de ajustes del permiso, apuntando a NUESTRA app. */
    fun intentPermisoInstalacion(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )

    /**
     * Instala [apk] (ya verificado). Intenta la sesión de PackageInstaller y, si esa
     * vía falla, cae al intent con FileProvider. Solo lanza si fallan las dos.
     */
    fun instalar(context: Context, apk: File): Via = try {
        instalarPorSesion(context, apk)
        Via.SESION
    } catch (e: Exception) {
        // La sesión puede fallar por almacenamiento, política del fabricante o una
        // sesión previa colgada. El intent clásico sigue estando disponible.
        instalarPorIntent(context, apk)
        Via.INTENT
    }

    /**
     * Crea una sesión, escribe el APK y hace commit. El resultado (confirmación
     * pendiente, éxito o fallo) llega a [InstallResultReceiver] vía el IntentSender.
     */
    private fun instalarPorSesion(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL,
        ).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && somosInstalador(context)) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }

        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { entrada ->
                session.openWrite("base.apk", 0, apk.length()).use { salida ->
                    entrada.copyTo(salida)
                    session.fsync(salida)
                }
            }
            session.commit(pendingIntent(context, sessionId).intentSender)
        }
    }

    /**
     * Vía de respaldo: entrega el APK al instalador del sistema con un content:// de
     * [FileProvider] (el APK vive en cacheDir, privado; un file:// daría
     * FileUriExposedException). Siempre muestra la confirmación del sistema y no
     * devuelve resultado a la app.
     */
    private fun instalarPorIntent(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, autoridadFileProvider(context), apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Debe coincidir con el authority declarado en el manifest del módulo. */
    private fun autoridadFileProvider(context: Context): String =
        "${context.packageName}.actualizador.fileprovider"

    /** ¿Somos nosotros el instalador registrado de nuestro propio paquete? (API 30+) */
    private fun somosInstalador(context: Context): Boolean = try {
        context.packageManager
            .getInstallSourceInfo(context.packageName)
            .installingPackageName == context.packageName
    } catch (e: Exception) {
        false
    }

    private fun pendingIntent(context: Context, sessionId: Int): PendingIntent {
        val intent = Intent(context, InstallResultReceiver::class.java).apply {
            action = InstallResultReceiver.ACCION
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // El sistema rellena extras en el intent de estado: debe ser MUTABLE.
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(context, sessionId, intent, flags)
    }
}
