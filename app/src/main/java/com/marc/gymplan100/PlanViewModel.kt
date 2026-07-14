package com.marc.gymplan100

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marc.gymplan100.data.Achievements
import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.Celebration
import com.marc.gymplan100.data.ExerciseLog
import com.marc.gymplan100.data.Motivation
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.ProgressRepository
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.SessionEngine
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.SessionRecord
import com.marc.gymplan100.data.TrainingDay
import com.marc.gymplan100.data.UserProfile
import com.marc.gymplan100.data.secondsPerSetFromScheme
import com.marc.gymplan100.health.HealthConnectManager
import com.marc.gymplan100.notify.RestReminder
import com.marc.gymplan100.wear.WearBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlanViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProgressRepository(app)

    private val _progress = MutableStateFlow(ProgressState())
    val progress: StateFlow<ProgressState> = _progress.asStateFlow()

    private val _active = MutableStateFlow<ActiveSession?>(null)
    val activeSession: StateFlow<ActiveSession?> = _active.asStateFlow()

    private val _celebration = MutableStateFlow<Celebration?>(null)
    val celebration: StateFlow<Celebration?> = _celebration.asStateFlow()
    private var lastMessage: String? = null

    private val _history = MutableStateFlow<List<SessionRecord>>(emptyList())
    val history: StateFlow<List<SessionRecord>> = _history.asStateFlow()

    // --- Health Connect (Google Health) ----------------------------------
    private val healthConnect = HealthConnectManager(app)

    /** Permisos de Health Connect a solicitar desde la UI. */
    val healthPermissions: Set<String> get() = healthConnect.permissions

    /** Health Connect está disponible en este dispositivo. */
    val healthAvailable: Boolean get() = healthConnect.isAvailable

    /** Health Connect existe pero el proveedor necesita actualizarse. */
    val healthNeedsUpdate: Boolean get() = healthConnect.needsProviderUpdate

    private val _healthGranted = MutableStateFlow(false)
    /** El usuario ya concedió el permiso de escritura. */
    val healthGranted: StateFlow<Boolean> = _healthGranted.asStateFlow()

    /** Relee si tenemos el permiso (tras volver del diálogo de permisos o al abrir Resultados). */
    fun refreshHealthPermissions() {
        viewModelScope.launch { _healthGranted.value = healthConnect.hasAllPermissions() }
    }

    // --- Perfil del usuario (peso / altura / género) ----------------------
    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    /** Guarda el perfil editado con las ruedas de Configuración. */
    fun updateProfile(profile: UserProfile) {
        _profile.value = profile
        viewModelScope.launch { repo.saveProfile(profile) }
    }

    /**
     * Rellena peso y altura leyéndolos de Google Health. Llama a [onDone] con `true` si se
     * importó algún dato. Requiere que el permiso de lectura esté concedido (si no, no hay
     * datos y devuelve `false`); la UI se encarga de pedirlo antes.
     */
    fun importProfileFromHealth(onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val weight = healthConnect.latestWeightKg()
            val height = healthConnect.latestHeightCm()
            if (weight == null && height == null) {
                onDone(false)
                return@launch
            }
            val merged = _profile.value.copy(
                weightKg = weight?.let { Math.round(it).toInt() } ?: _profile.value.weightKg,
                heightCm = height?.let { Math.round(it).toInt() } ?: _profile.value.heightCm
            )
            _profile.value = merged
            repo.saveProfile(merged)
            onDone(true)
        }
    }

    fun clearCelebration() { _celebration.value = null }

    val unlockedAchievements: Int
        get() = Achievements.unlockedIds(_progress.value).size

    init {
        viewModelScope.launch {
            _progress.value = repo.progress.first()
            _history.value = repo.history.first()
            _profile.value = repo.profile.first()
        }
        refreshHealthPermissions()
        // Observa la sesión activa de forma continua: así los cambios hechos desde la
        // notificación persistente (SkipActionReceiver, en segundo plano) se reflejan al
        // volver a la app. saveActive() sigue fijando el valor al instante para respuesta
        // inmediata; el StateFlow deduplica los valores iguales.
        viewModelScope.launch {
            repo.activeSession.collect {
                _active.value = it
                // Refleja cada cambio de la sesión en el reloj (Pixel Watch). Sin sesión, le
                // damos el siguiente día pendiente para el botón "Empezar entreno" del reloj.
                WearBridge.publishState(
                    getApplication(),
                    it,
                    nextDay = if (it == null) nextDay() else 0
                )
            }
        }
    }

    private fun update(transform: (ProgressState) -> ProgressState) {
        val newState = transform(_progress.value)
        _progress.value = newState
        viewModelScope.launch { repo.save(newState) }
    }

    fun toggleDay(day: Int) {
        val before = _progress.value
        val wasCompleted = day in before.completedDays
        val set = before.completedDays.toMutableSet().apply {
            if (!add(day)) remove(day)
        }
        val after = before.copy(completedDays = set)
        _progress.value = after
        viewModelScope.launch { repo.save(after) }
        if (!wasCompleted && day in after.completedDays) {
            triggerCelebration(before, after, day)
        }
    }

    private fun triggerCelebration(before: ProgressState, after: ProgressState, day: Int) {
        val total = after.completedDays.count { it in 1..PlanData.TOTAL_DAYS }
        val finalVictory = total >= PlanData.TOTAL_DAYS
        _celebration.value = Celebration(
            dayNumber = day,
            totalCompleted = total,
            message = pickMessage(total),
            newAchievements = Achievements.newlyUnlocked(before, after),
            isFinalVictory = finalVictory
        )
        if (finalVictory) playChampionsVideo()
    }

    /** Abre en YouTube el videoclip de "We Are the Champions" de Queen. */
    fun playChampionsVideo() {
        val ctx = getApplication<Application>()
        val videoId = "04854XqcfCY" // Queen - We Are The Champions (Official Video)
        val appIntent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("vnd.youtube:$videoId")
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        val webIntent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://www.youtube.com/watch?v=$videoId")
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(appIntent) }
            .recoverCatching { ctx.startActivity(webIntent) }
    }

    /** Frase motivadora variada: en los hitos usa la específica; si no, una al azar sin repetir la anterior. */
    private fun pickMessage(total: Int): String {
        val milestone = Motivation.message(total)
        if (milestone != Motivation.genericFor(total)) {
            lastMessage = milestone
            return milestone
        }
        val pool = Motivation.generic.filter { it != lastMessage }
        val choice = pool[kotlin.random.Random.nextInt(pool.size)]
        lastMessage = choice
        return choice
    }

    fun setLog(
        day: Int,
        exerciseIndex: Int,
        weight: String? = null,
        reps: String? = null,
        done: Boolean? = null
    ) = update { state ->
        val key = "$day-$exerciseIndex"
        val current = state.logs[key] ?: ExerciseLog()
        val updated = current.copy(
            weight = weight ?: current.weight,
            reps = reps ?: current.reps,
            done = done ?: current.done
        )
        // Si se registra peso, lo guardamos también como "peso actual" de ese ejercicio.
        val name = PlanData.dayByNumber(day)?.template?.exercises?.getOrNull(exerciseIndex)?.name
        val weights = if (weight != null && !name.isNullOrBlank()) {
            state.exerciseWeights + (name to weight)
        } else state.exerciseWeights
        state.copy(logs = state.logs + (key to updated), exerciseWeights = weights)
    }

    /** Peso actual guardado para un ejercicio por su nombre. */
    fun exerciseWeight(name: String): String = _progress.value.exerciseWeights[name] ?: ""

    /** Fija el peso de referencia de un ejercicio (desde la pantalla de pesos). */
    fun setExerciseWeight(name: String, weight: String) = update { state ->
        state.copy(exerciseWeights = state.exerciseWeights + (name to weight))
    }

    fun logFor(day: Int, exerciseIndex: Int): ExerciseLog =
        _progress.value.logs["$day-$exerciseIndex"] ?: ExerciseLog()

    /** Primer día sin completar; si están todos hechos, devuelve el último. */
    fun nextDay(): Int = SessionEngine.nextDay(_progress.value.completedDays)

    // --- Entrenamiento guiado en vivo ------------------------------------

    private fun saveActive(session: ActiveSession?) {
        _active.value = session
        viewModelScope.launch { repo.saveActiveSession(session) }
    }

    /** Inicia una sesión nueva para el día indicado (descarta cualquier otra en curso). */
    fun startSession(dayNumber: Int) {
        RestReminder.cancel(getApplication())
        val now = System.currentTimeMillis()
        val session = SessionEngine.startSession(dayNumber, now)
        // El calentamiento arranca en marcha; programamos el aviso de su fin.
        RestReminder.schedule(
            getApplication(),
            now + session.warmupTargetSeconds * 1000L,
            RestReminder.KIND_WARMUP,
            dayNumber
        )
        saveActive(session)
    }

    /**
     * Inicia un entrenamiento ESPECIAL/libre para el día indicado (descarta cualquier otro en curso).
     * No hay series, pesos ni descansos: solo un cronómetro que corre hasta pulsar "Finalizar".
     */
    fun startSpecialSession(dayNumber: Int) {
        RestReminder.cancel(getApplication())
        val now = System.currentTimeMillis()
        saveActive(
            ActiveSession(
                dayNumber = dayNumber,
                startMillis = now,
                phase = SessionPhase.FREE,
                special = true
            )
        )
    }

    /**
     * Inicia un entrenamiento EXTRA (bonus): cronómetro libre que NO cuenta como día del plan.
     * Para cuando ya hiciste el entreno del día y por la tarde vas a otro (p. ej. con tu tío).
     * Devuelve el día de referencia para poder abrir la pantalla de sesión.
     */
    fun startExtraSession(): Int {
        RestReminder.cancel(getApplication())
        val now = System.currentTimeMillis()
        val refDay = nextDay()
        saveActive(
            ActiveSession(
                dayNumber = refDay,
                startMillis = now,
                phase = SessionPhase.FREE,
                special = true,
                extra = true
            )
        )
        return refDay
    }

    // --- Calentamiento -----------------------------------------------------

    /** Pausa la cuenta atrás del calentamiento y cancela su aviso. */
    fun pauseWarmup() {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.WARMUP || s.warmupPaused) return
        RestReminder.cancel(getApplication())
        saveActive(
            s.copy(
                warmupPaused = true,
                warmupElapsedBeforePause = s.warmupElapsed(System.currentTimeMillis())
            )
        )
    }

    /** Reanuda la cuenta atrás del calentamiento y reprograma su aviso. */
    fun resumeWarmup() {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.WARMUP || !s.warmupPaused) return
        val now = System.currentTimeMillis()
        val remaining = (s.warmupTargetSeconds - s.warmupElapsedBeforePause).coerceAtLeast(0)
        RestReminder.schedule(getApplication(), now + remaining * 1000L, RestReminder.KIND_WARMUP, s.dayNumber)
        saveActive(s.copy(warmupPaused = false, warmupStartMillis = now))
    }

    /** Ajusta (±segundos) el objetivo del calentamiento y reprograma el aviso si está en marcha. */
    fun adjustWarmup(deltaSeconds: Int) {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.WARMUP) return
        val newTarget = (s.warmupTargetSeconds + deltaSeconds).coerceIn(0, 1800)
        if (!s.warmupPaused) {
            val remaining = (newTarget - s.warmupElapsed(System.currentTimeMillis())).coerceAtLeast(0)
            RestReminder.schedule(
                getApplication(),
                System.currentTimeMillis() + remaining * 1000L,
                RestReminder.KIND_WARMUP,
                s.dayNumber
            )
        }
        saveActive(s.copy(warmupTargetSeconds = newTarget))
    }

    /** Termina el calentamiento (manual o al expirar) y pasa a la primera serie. */
    fun endWarmup() {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.WARMUP) return
        RestReminder.cancel(getApplication())
        saveActive(SessionEngine.endWarmup(s))
    }

    /** Máquina ocupada: pospone el ejercicio actual al final de la cola y pasa al siguiente pendiente. */
    fun skipExercise() {
        val s = _active.value ?: return
        val next = SessionEngine.skipExercise(s)
        if (next !== s) saveActive(next)
    }

    /** Peso sugerido para la serie actual: el de la serie anterior del mismo ejercicio. */
    fun suggestedWeight(session: ActiveSession): String {
        // Si durante el descanso previo se preparó un peso para esta serie, se usa ese.
        if (session.plannedWeight.isNotBlank()) return session.plannedWeight.trim()
        session.completedSets
            .lastOrNull { it.exerciseIndex == session.exerciseIndex && it.weight.isNotBlank() }
            ?.let { return it.weight }
        // Primera serie del ejercicio: usa el último peso conocido de esa máquina/ejercicio.
        val name = PlanData.dayByNumber(session.dayNumber)
            ?.template?.exercises?.getOrNull(session.exerciseIndex)?.name
        if (name != null) {
            val known = _progress.value.exerciseWeights[name]
            if (!known.isNullOrBlank()) return known
        }
        return logFor(session.dayNumber, session.exerciseIndex).weight
    }

    /**
     * Peso sugerido para la PRÓXIMA serie (la que vendrá al terminar el descanso actual), para
     * precargar el campo con el que se prepara la máquina. Mira si el siguiente es la misma
     * máquina (peso que acabas de usar) o un ejercicio nuevo (último peso conocido de esa máquina).
     */
    fun plannedWeightSuggestion(session: ActiveSession): String {
        val day = PlanData.dayByNumber(session.dayNumber) ?: return ""
        val exercise = day.template.exercises.getOrNull(session.exerciseIndex) ?: return ""
        val totalSets = com.marc.gymplan100.data.setCountFromScheme(exercise.scheme)
        if (session.setNumber < totalSets) {
            // Misma máquina: el peso que acabas de levantar en la última serie hecha.
            session.completedSets
                .lastOrNull { it.exerciseIndex == session.exerciseIndex && it.weight.isNotBlank() }
                ?.let { return it.weight }
            return _progress.value.exerciseWeights[exercise.name] ?: ""
        }
        // Siguiente ejercicio del orden: su último peso conocido, si lo hay.
        val order = session.order.ifEmpty { day.template.exercises.indices.toList() }
        val pos = order.indexOf(session.exerciseIndex)
        val nextIdx = order.getOrNull(pos + 1) ?: return ""
        val nextName = day.template.exercises.getOrNull(nextIdx)?.name ?: return ""
        return _progress.value.exerciseWeights[nextName] ?: ""
    }

    /** Guarda el peso preparado durante el descanso para dejar la máquina lista. */
    fun setPlannedWeight(weight: String) {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.RESTING) return
        saveActive(s.copy(plannedWeight = weight))
    }

    // --- Serie por tiempo (planchas, isométricos) -------------------------

    /** Arranca la cuenta atrás de una serie por tiempo y programa su aviso sonoro. */
    fun startTimedSet() {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.WORKING) return
        val day = PlanData.dayByNumber(s.dayNumber) ?: return
        val exercise = day.template.exercises.getOrNull(s.exerciseIndex) ?: return
        val target = secondsPerSetFromScheme(exercise.scheme) ?: return
        val now = System.currentTimeMillis()
        RestReminder.schedule(getApplication(), now + target * 1000L, RestReminder.KIND_TIMED_SET, s.dayNumber)
        saveActive(
            s.copy(
                phase = SessionPhase.TIMED_SET,
                timedTargetSeconds = target,
                timedStartMillis = now,
                timedElapsedBeforePause = 0,
                timedPaused = false
            )
        )
    }

    /** Pausa la cuenta atrás de la serie por tiempo y cancela su aviso. */
    fun pauseTimedSet() {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.TIMED_SET || s.timedPaused) return
        RestReminder.cancel(getApplication())
        saveActive(
            s.copy(
                timedPaused = true,
                timedElapsedBeforePause = s.timedElapsed(System.currentTimeMillis())
            )
        )
    }

    /** Reanuda la cuenta atrás de la serie por tiempo y reprograma su aviso. */
    fun resumeTimedSet() {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.TIMED_SET || !s.timedPaused) return
        val now = System.currentTimeMillis()
        val remaining = (s.timedTargetSeconds - s.timedElapsedBeforePause).coerceAtLeast(0)
        RestReminder.schedule(getApplication(), now + remaining * 1000L, RestReminder.KIND_TIMED_SET, s.dayNumber)
        saveActive(s.copy(timedPaused = false, timedStartMillis = now))
    }

    /** Ajusta (±segundos) el objetivo de la serie por tiempo y reprograma el aviso si está en marcha. */
    fun adjustTimedSet(deltaSeconds: Int) {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.TIMED_SET) return
        val newTarget = (s.timedTargetSeconds + deltaSeconds).coerceIn(1, 600)
        if (!s.timedPaused) {
            val remaining = (newTarget - s.timedElapsed(System.currentTimeMillis())).coerceAtLeast(0)
            RestReminder.schedule(
                getApplication(),
                System.currentTimeMillis() + remaining * 1000L,
                RestReminder.KIND_TIMED_SET,
                s.dayNumber
            )
        }
        saveActive(s.copy(timedTargetSeconds = newTarget))
    }

    /** Marca la serie actual como hecha y arranca el descanso (o finaliza el día). */
    fun completeSet(weight: String) {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.WORKING && s.phase != SessionPhase.TIMED_SET) return
        val day = PlanData.dayByNumber(s.dayNumber) ?: return
        val exercise = day.template.exercises.getOrNull(s.exerciseIndex) ?: return

        // El peso de referencia es un efecto propio del ViewModel; la transición de fase la
        // calcula SessionEngine (compartida con la notificación) para no divergir.
        val cleanWeight = weight.trim()
        if (cleanWeight.isNotBlank()) {
            update { it.copy(exerciseWeights = it.exerciseWeights + (exercise.name to cleanWeight)) }
        }
        val next = SessionEngine.completeSet(s, weight, System.currentTimeMillis())
        if (next.phase == SessionPhase.FINISHED) {
            RestReminder.cancel(getApplication())
        } else {
            val kind = if (next.restBetweenExercises) RestReminder.KIND_BETWEEN_EXERCISES
            else RestReminder.KIND_BETWEEN_SETS
            RestReminder.schedule(
                getApplication(),
                next.restStartMillis + next.restTargetSeconds * 1000L,
                kind,
                next.dayNumber
            )
        }
        saveActive(next)
    }

    /** Ajusta el objetivo de descanso (±segundos) durante un descanso. */
    fun adjustRest(deltaSeconds: Int) {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.RESTING) return
        val newTarget = (s.restTargetSeconds + deltaSeconds).coerceIn(0, 600)
        val kind = if (s.restBetweenExercises) RestReminder.KIND_BETWEEN_EXERCISES
        else RestReminder.KIND_BETWEEN_SETS
        RestReminder.schedule(getApplication(), s.restStartMillis + newTarget * 1000L, kind, s.dayNumber)
        saveActive(s.copy(restTargetSeconds = newTarget))
    }

    /** Termina el descanso, guarda el tiempo real descansado y avanza a la siguiente serie/ejercicio. */
    fun endRest() {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.RESTING) return
        RestReminder.cancel(getApplication())
        saveActive(SessionEngine.endRest(s, System.currentTimeMillis()))
    }

    /** Finaliza la sesión: guarda el histórico, marca el día y vuelca los pesos a los registros. */
    fun finishSession() {
        val s = _active.value ?: return
        val end = System.currentTimeMillis()
        val totalRest = s.completedSets.sumOf { it.restSeconds }
        val record = SessionRecord(
            dayNumber = s.dayNumber,
            startMillis = s.startMillis,
            endMillis = end,
            totalSets = s.completedSets.size,
            totalRestSeconds = totalRest,
            special = s.special,
            extra = s.extra
        )
        viewModelScope.launch { repo.appendHistory(record) }
        _history.value = _history.value + record

        // Vuelca el entreno a Health Connect (Google Health) si el usuario lo conectó.
        syncToHealthConnect(s, record)

        // Un entrenamiento EXTRA es un bonus: se guarda en el historial pero NO marca
        // ningún día del plan ni toca los pesos ni dispara celebración de hito.
        if (s.extra) {
            RestReminder.cancel(getApplication())
            saveActive(null)
            return
        }

        val before = _progress.value
        val wasCompleted = s.dayNumber in before.completedDays
        val logs = before.logs.toMutableMap()
        s.completedSets.groupBy { it.exerciseIndex }.forEach { (idx, setsForExercise) ->
            val lastWeight = setsForExercise.lastOrNull { it.weight.isNotBlank() }?.weight
            val key = "${s.dayNumber}-$idx"
            val current = logs[key] ?: ExerciseLog()
            logs[key] = current.copy(weight = lastWeight ?: current.weight, done = true)
        }
        val after = before.copy(completedDays = before.completedDays + s.dayNumber, logs = logs)
        _progress.value = after
        viewModelScope.launch { repo.save(after) }

        RestReminder.cancel(getApplication())
        saveActive(null)

        if (!wasCompleted) {
            triggerCelebration(before, after, s.dayNumber)
        }
    }

    /**
     * Escribe el entreno recién finalizado en Health Connect. Es seguro llamarlo siempre:
     * el manager comprueba disponibilidad y permiso y, si faltan, no hace nada.
     */
    private fun syncToHealthConnect(session: ActiveSession, record: SessionRecord) {
        if (!healthConnect.isAvailable) return
        val day = PlanData.dayByNumber(session.dayNumber)
        val title = when {
            session.extra -> "Entrenamiento extra"
            else -> "Día ${session.dayNumber} · ${day?.template?.title ?: "Entrenamiento"}"
        }
        val notes = buildWorkoutNotes(session, record, day)
        // El reloj manda: si la sesión se controló desde él (automático) o el interruptor manual
        // está activo, Google Health ya tiene calorías reales del pulso → no estimamos.
        val watchInvolved = session.watchControlled || _profile.value.usesWatch
        val kcal = if (watchInvolved) null
        else estimateActiveKcal(record.startMillis, record.endMillis)
        viewModelScope.launch {
            runCatching {
                healthConnect.writeWorkout(title, notes, record.startMillis, record.endMillis, kcal)
            }
        }
    }

    /**
     * Estima las calorías activas del entreno con la fórmula MET estándar:
     * `kcal = MET · 3,5 · peso_kg / 200 · minutos`, usando MET ≈ 5 (entreno de fuerza
     * vigoroso). Devuelve null si no hay peso definido, porque sin peso la estimación no
     * tiene base y preferimos no inventar un número.
     */
    private fun estimateActiveKcal(startMillis: Long, endMillis: Long): Double? {
        val weightKg = _profile.value.weightKg
        if (weightKg <= 0) return null
        val minutes = (endMillis - startMillis).coerceAtLeast(0L) / 60000.0
        if (minutes <= 0) return null
        val met = 5.0
        return met * 3.5 * weightKg / 200.0 * minutes
    }

    /** Construye el detalle (ejercicios + pesos + duración) que se guarda en las notas de la sesión. */
    private fun buildWorkoutNotes(
        session: ActiveSession,
        record: SessionRecord,
        day: TrainingDay?
    ): String {
        val sb = StringBuilder()
        if (session.special || day == null) {
            sb.append("Sesión libre guiada.\n")
        } else {
            val byExercise = session.completedSets.groupBy { it.exerciseIndex }
            day.template.exercises.forEachIndexed { idx, ex ->
                val sets = byExercise[idx].orEmpty()
                val detail = if (sets.isEmpty()) "—"
                else sets.joinToString(", ") { s ->
                    if (s.weight.isBlank()) "—" else "${s.weight} kg"
                }
                sb.append("• ${ex.name} (${ex.scheme}): $detail\n")
            }
        }
        val mins = record.durationSeconds / 60
        sb.append("Duración: $mins min · ${record.totalSets} series")
        return sb.toString().trim()
    }

    /** Descarta la sesión en curso sin guardar nada. */
    fun cancelSession() {
        RestReminder.cancel(getApplication())
        saveActive(null)
    }

    /** Oculta para siempre el aviso de optimización de batería. */
    fun dismissBatteryHint() = update { it.copy(batteryHintDismissed = true) }
}
