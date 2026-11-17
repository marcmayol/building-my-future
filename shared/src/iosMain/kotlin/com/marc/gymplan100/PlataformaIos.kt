package com.marc.gymplan100

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.FreeStep
import com.marc.gymplan100.data.IosContext
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.ahoraMs
import com.marc.gymplan100.data.preferencias
import com.marc.gymplan100.plataforma.Plataforma
import com.marc.gymplan100.plataforma.Recordatorio
import com.marc.gymplan100.plataforma.RestReminder
import com.marc.gymplan100.plataforma.Salud
import com.marc.gymplan100.shared.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/** Lo que la app compartida pide al iPhone. */
object PlataformaIos : Plataforma {

    private val scope = MainScope()
    private val centro get() = UNUserNotificationCenter.currentNotificationCenter()

    /** Sube al cambiar de idioma: la pantalla entera se vuelve a pintar. */
    var reinicios by mutableIntStateOf(0)
        private set

    /** El aviso corto que se está enseñando (lo pinta MainViewController). */
    var avisoActual by mutableStateOf<String?>(null)
        private set

    override val version: String =
        NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: ""

    fun pedirPermisoAvisos() {
        centro.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { _, _ -> }
    }

    override fun abrirEnlace(url: String) {
        val u = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(u, emptyMap<Any?, Any?>(), null)
    }

    override fun abrirYoutube(videoId: String) {
        val app = NSURL.URLWithString("youtube://watch?v=$videoId")
        if (app != null && UIApplication.sharedApplication.canOpenURL(app)) {
            UIApplication.sharedApplication.openURL(app, emptyMap<Any?, Any?>(), null)
        } else abrirEnlace("https://www.youtube.com/watch?v=$videoId")
    }

    override fun avisoBreve(texto: String) {
        avisoActual = texto
        scope.launch {
            delay(3500)
            if (avisoActual == texto) avisoActual = null
        }
    }

    override fun reiniciarPantalla() {
        aplicarIdioma()
        reinicios++
    }

    // --- Aviso de fin de descanso: una notificación local a la hora en que acaba -------------

    private const val ID_DESCANSO = "descanso"

    override fun programarDescanso(alMs: Long, tipo: Int, dia: Int) {
        val segundos = (alMs - ahoraMs()) / 1000.0
        cancelarDescanso()
        if (segundos < 1) return
        scope.launch {
            val titulo = getString(
                when (tipo) {
                    RestReminder.KIND_WARMUP -> Res.string.ini_rest_done_warmup
                    RestReminder.KIND_TIMED_SET -> Res.string.ini_rest_done_timed
                    else -> Res.string.ini_rest_done_rest
                }
            )
            val cuerpo = getString(
                when (tipo) {
                    RestReminder.KIND_WARMUP -> Res.string.ini_rest_done_text_warmup
                    RestReminder.KIND_TIMED_SET -> Res.string.ini_rest_done_text_timed
                    RestReminder.KIND_BETWEEN_EXERCISES -> Res.string.ini_rest_done_text_exercises
                    else -> Res.string.ini_rest_done_text_sets
                }
            )
            val contenido = UNMutableNotificationContent().apply {
                setTitle(titulo)
                setBody(cuerpo)
                setSound(UNNotificationSound.defaultSound)
            }
            val disparo = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(segundos, false)
            centro.addNotificationRequest(
                UNNotificationRequest.requestWithIdentifier(ID_DESCANSO, contenido, disparo), null
            )
        }
    }

    override fun cancelarDescanso() {
        centro.removePendingNotificationRequestsWithIdentifiers(listOf(ID_DESCANSO))
        centro.removeDeliveredNotificationsWithIdentifiers(listOf(ID_DESCANSO))
    }

    /** Igual que en Android: según la fase, hay cuenta atrás (se programa) o no (se cancela). */
    override fun sincronizarDescanso(sesion: ActiveSession?) {
        val s = sesion ?: return cancelarDescanso()
        val now = ahoraMs()
        when (s.phase) {
            SessionPhase.WARMUP -> if (s.warmupPaused) cancelarDescanso() else programarDescanso(
                now + (s.warmupTargetSeconds - s.warmupElapsed(now)).coerceAtLeast(0) * 1000L,
                RestReminder.KIND_WARMUP, s.dayNumber
            )
            SessionPhase.TIMED_SET -> if (s.timedPaused) cancelarDescanso() else programarDescanso(
                now + (s.timedTargetSeconds - s.timedElapsed(now)).coerceAtLeast(0) * 1000L,
                RestReminder.KIND_TIMED_SET, s.dayNumber
            )
            SessionPhase.RESTING -> programarDescanso(
                s.restStartMillis + s.restTargetSeconds * 1000L,
                if (s.restBetweenExercises) RestReminder.KIND_BETWEEN_EXERCISES else RestReminder.KIND_BETWEEN_SETS,
                s.dayNumber
            )
            SessionPhase.FREE -> when (s.freeStep) {
                FreeStep.WARMUP -> programarDescanso(
                    s.warmupStartMillis + s.warmupTargetSeconds * 1000L, RestReminder.KIND_WARMUP, s.dayNumber
                )
                FreeStep.REST -> programarDescanso(
                    s.restStartMillis + s.restTargetSeconds * 1000L,
                    if (s.restBetweenExercises) RestReminder.KIND_BETWEEN_EXERCISES else RestReminder.KIND_BETWEEN_SETS,
                    s.dayNumber
                )
                FreeStep.WORK -> cancelarDescanso()
            }
            else -> cancelarDescanso()
        }
    }

    // En el iPhone una notificación local llega a su hora: no hay alarmas exactas ni ahorro
    // de batería que pedir.
    override fun puedeAvisarExacto(): Boolean = true
    override fun abrirAjustesAvisoExacto() = Unit
    override fun ignoraAhorroBateria(): Boolean = true
    override fun abrirAjustesBateria() = Unit

    // Sin reloj de Wear OS.
    override fun publicarEnReloj(sesion: ActiveSession?, siguienteDia: Int) = Unit

    // HealthKit: pendiente. Mientras, la app dice que no está disponible.
    override val salud: Salud = object : Salud {
        override val disponible: Boolean = false
        override val necesitaActualizar: Boolean = false
        override suspend fun tienePermisos(): Boolean = false
        override suspend fun escribirEntreno(titulo: String, notas: String, inicioMs: Long, finMs: Long, kcal: Double?) = Unit
        override suspend fun ultimoPesoKg(): Double? = null
    }

    // --- Recordatorio para entrenar ---------------------------------------------------------

    override val recordatorio: Recordatorio = RecordatorioIos

    override fun trasEntrenar() = Unit

    // Selector de ficheros: pendiente (copias de seguridad e importar planes).
    @Composable
    override fun rememberAbrirTexto(tipos: List<String>, alLeer: (String?) -> Unit): () -> Unit = {}

    @Composable
    override fun rememberGuardarTexto(alTerminar: (Boolean) -> Unit): (String, suspend () -> String) -> Unit =
        { _, _ -> }

    @Composable
    override fun rememberPedirPermisosSalud(alVolver: () -> Unit): () -> Unit = {}

    @Composable
    override fun SeccionActualizaciones() = Unit

    @Composable
    override fun BannerActualizacion() = Unit

    @Composable
    override fun SeccionValoracion() = Unit
}

/**
 * El recordatorio en el iPhone: mismas claves que en Android y, en vez de una alarma que se
 * reprograma sola, avisos sueltos para las próximas dos semanas (se rehacen al abrir la app).
 */
private object RecordatorioIos : Recordatorio {
    private const val PREFS = "gym_recordatorios"
    private const val KEY_ON = "activo"
    private const val KEY_DAYS = "dias"
    private const val KEY_MINUTE = "minuto"
    private const val KEY_PAUSE = "pausa_hasta"
    private const val PREFIJO = "recordatorio-"
    private val prefs get() = IosContext.preferencias(PREFS)

    override fun activo(): Boolean = prefs.getBoolean(KEY_ON, false)

    override fun dias(): Set<Int> =
        prefs.getString(KEY_DAYS)?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet()
            ?: setOf(1, 2, 3, 4, 5)

    override fun minutoDelDia(): Int = prefs.getString(KEY_MINUTE)?.toIntOrNull() ?: (18 * 60)

    override fun pausaHasta(): LocalDate? {
        val fecha = prefs.getString(KEY_PAUSE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        if (fecha < hoy) {
            prefs.editar { remove(KEY_PAUSE) }
            return null
        }
        return fecha
    }

    override fun guardar(activo: Boolean, dias: Set<Int>, minutoDelDia: Int) {
        prefs.editar {
            putBoolean(KEY_ON, activo)
            putString(KEY_DAYS, dias.sorted().joinToString(","))
            putString(KEY_MINUTE, minutoDelDia.toString())
            if (!activo) remove(KEY_PAUSE)
        }
        if (activo) PlataformaIos.pedirPermisoAvisos()
        reprogramar()
    }

    override fun pausar(hasta: LocalDate?) {
        prefs.editar { if (hasta == null) remove(KEY_PAUSE) else putString(KEY_PAUSE, hasta.toString()) }
        reprogramar()
    }

    fun reprogramar() {
        val centro = UNUserNotificationCenter.currentNotificationCenter()
        centro.removePendingNotificationRequestsWithIdentifiers((0 until 15).map { "$PREFIJO$it" })
        if (!activo()) return
        val zona = TimeZone.currentSystemDefault()
        val ahora = Clock.System.now()
        val hoy = ahora.toLocalDateTime(zona).date
        val pausa = pausaHasta()
        val dias = dias()
        val minuto = minutoDelDia()
        MainScope().launch {
            // Solo el título: el día que toca depende de lo que se entrene antes del aviso.
            val titulo = getString(Res.string.ini_reminder_title)
            for (salto in 0 until 15) {
                val fecha = hoy.plus(salto, DateTimeUnit.DAY)
                if (fecha.dayOfWeek.isoDayNumber !in dias) continue
                if (pausa != null && fecha <= pausa) continue
                val cuando = LocalDateTime(fecha.year, fecha.month, fecha.dayOfMonth, minuto / 60, minuto % 60)
                    .toInstant(zona)
                val segundos = (cuando - ahora).inWholeSeconds
                if (segundos < 60) continue
                val contenido = UNMutableNotificationContent().apply {
                    setTitle(titulo)
                    setSound(UNNotificationSound.defaultSound)
                }
                centro.addNotificationRequest(
                    UNNotificationRequest.requestWithIdentifier(
                        "$PREFIJO$salto", contenido,
                        UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(segundos.toDouble(), false)
                    ), null
                )
            }
        }
    }
}

/** Al abrir la app se rehacen los avisos del recordatorio (solo cubren dos semanas). */
fun reprogramarRecordatorio() = RecordatorioIos.reprogramar()
