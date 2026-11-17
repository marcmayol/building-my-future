package com.marc.gymplan100

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.health.HealthConnectManager
import com.marc.gymplan100.notify.RestReminder
import com.marc.gymplan100.notify.TrainingReminder
import com.marc.gymplan100.plataforma.Plataforma
import com.marc.gymplan100.plataforma.Recordatorio
import com.marc.gymplan100.plataforma.Salud
import com.marc.gymplan100.review.Valoracion
import com.marc.gymplan100.update.Updates
import com.marc.gymplan100.wear.WearBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.lang.ref.WeakReference

/**
 * Lo que la app compartida (:shared) pide a Android: es lo mismo que hacían antes las
 * pantallas por su cuenta (AlarmManager, Health Connect, el reloj, el selector de
 * documentos…), reunido aquí.
 */
class PlataformaAndroid(private val app: Application) : Plataforma {

    /** La actividad en primer plano: hace falta para recrearla o para la valoración de Play. */
    private var actividad: WeakReference<Activity>? = null

    init {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) { actividad = WeakReference(a) }
            override fun onActivityResumed(a: Activity) { actividad = WeakReference(a) }
            override fun onActivityStarted(a: Activity) = Unit
            override fun onActivityPaused(a: Activity) = Unit
            override fun onActivityStopped(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
            override fun onActivityDestroyed(a: Activity) {
                if (actividad?.get() === a) actividad = null
            }
        })
    }

    override val version: String = BuildConfig.VERSION_NAME

    override fun abrirEnlace(url: String) {
        runCatching {
            app.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun abrirYoutube(videoId: String) {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { app.startActivity(appIntent) }
            .recoverCatching { app.startActivity(webIntent) }
    }

    override fun avisoBreve(texto: String) {
        Toast.makeText(actividad?.get() ?: app, texto, Toast.LENGTH_LONG).show()
    }

    // Recrear la actividad vuelve a pasar por attachBaseContext, que es donde se aplica el idioma.
    override fun reiniciarPantalla() {
        actividad?.get()?.recreate()
    }

    override fun programarDescanso(alMs: Long, tipo: Int, dia: Int) = RestReminder.schedule(app, alMs, tipo, dia)
    override fun cancelarDescanso() = RestReminder.cancel(app)
    override fun sincronizarDescanso(sesion: ActiveSession?) = RestReminder.syncForSession(app, sesion)
    override fun puedeAvisarExacto(): Boolean = RestReminder.canScheduleExact(app)
    override fun abrirAjustesAvisoExacto() = RestReminder.openExactAlarmSettings(actividad?.get() ?: app)

    override fun ignoraAhorroBateria(): Boolean {
        val pm = app.getSystemService(Application.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(app.packageName)
    }

    override fun abrirAjustesBateria() {
        val ctx = actividad?.get() ?: return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${ctx.packageName}")
        }
        runCatching { ctx.startActivity(intent) }.onFailure {
            runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }
    }

    override fun publicarEnReloj(sesion: ActiveSession?, siguienteDia: Int) =
        WearBridge.publishState(app, sesion, nextDay = siguienteDia)

    private val healthConnect = HealthConnectManager(app)

    override val salud: Salud = object : Salud {
        override val disponible: Boolean get() = healthConnect.isAvailable
        override val necesitaActualizar: Boolean get() = healthConnect.needsProviderUpdate
        override suspend fun tienePermisos(): Boolean = healthConnect.hasAllPermissions()
        override suspend fun escribirEntreno(titulo: String, notas: String, inicioMs: Long, finMs: Long, kcal: Double?) =
            healthConnect.writeWorkout(titulo, notas, inicioMs, finMs, kcal)
        override suspend fun ultimoPesoKg(): Double? = healthConnect.latestWeightKg()
    }

    override val recordatorio: Recordatorio = object : Recordatorio {
        override fun activo(): Boolean = TrainingReminder.isEnabled(app)
        override fun dias(): Set<Int> = TrainingReminder.days(app)
        override fun minutoDelDia(): Int = TrainingReminder.minuteOfDay(app)
        override fun pausaHasta(): LocalDate? = TrainingReminder.pausedUntil(app)?.toKotlinLocalDate()
        override fun guardar(activo: Boolean, dias: Set<Int>, minutoDelDia: Int) =
            TrainingReminder.save(app, activo, dias, minutoDelDia)
        override fun pausar(hasta: LocalDate?) = TrainingReminder.setPause(app, hasta?.toJavaLocalDate())
    }

    override fun trasEntrenar() {
        Valoracion.trasEntrenar(actividad?.get() ?: app)
    }

    // Se abre el selector con los tipos que pida quien llama; para importar un plan, todos:
    // muchos gestores no etiquetan los .json como application/json y los dejarían en gris.
    @Composable
    override fun rememberAbrirTexto(tipos: List<String>, alLeer: (String?) -> Unit): () -> Unit {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val texto = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    }
                }.getOrNull()
                alLeer(texto)
            }
        }
        return { launcher.launch(tipos.toTypedArray()) }
    }

    @Composable
    override fun rememberGuardarTexto(alTerminar: (Boolean) -> Unit): (String, suspend () -> String) -> Unit {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var pendiente by remember { mutableStateOf<(suspend () -> String)?>(null) }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            val contenido = pendiente
            pendiente = null
            if (uri == null || contenido == null) return@rememberLauncherForActivityResult
            scope.launch {
                val ok = runCatching {
                    val texto = contenido()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { salida ->
                            salida.write(texto.toByteArray())
                        } ?: error("no se pudo escribir")
                    }
                }.isSuccess
                alTerminar(ok)
            }
        }
        return { nombre, contenido ->
            pendiente = contenido
            launcher.launch(nombre)
        }
    }

    @Composable
    override fun rememberPedirPermisosSalud(alVolver: () -> Unit): () -> Unit {
        val launcher = rememberLauncherForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { alVolver() }
        return { launcher.launch(healthConnect.permissions) }
    }

    @Composable
    override fun SeccionActualizaciones() = Updates.SettingsSection()

    @Composable
    override fun BannerActualizacion() = Updates.Banner()

    @Composable
    override fun SeccionValoracion() = Valoracion.SettingsSection()
}
