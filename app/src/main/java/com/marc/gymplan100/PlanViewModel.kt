package com.marc.gymplan100

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marc.gymplan100.data.Achievements
import com.marc.gymplan100.data.ActiveSession
import com.marc.gymplan100.data.Celebration
import com.marc.gymplan100.data.ExerciseLog
import com.marc.gymplan100.data.LoggedExercise
import com.marc.gymplan100.data.Motivation
import com.marc.gymplan100.data.DiaDto
import com.marc.gymplan100.data.EjercicioDto
import com.marc.gymplan100.data.FaseDto
import com.marc.gymplan100.data.BuiltinPlan
import com.marc.gymplan100.data.PlanAdvisor
import com.marc.gymplan100.data.PlanEquipment
import com.marc.gymplan100.data.PlanGoal
import com.marc.gymplan100.data.PlanCodec
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.data.PlanDto
import com.marc.gymplan100.data.PlanImport
import com.marc.gymplan100.data.PlanMarkdownParser
import com.marc.gymplan100.data.PlanStore
import com.marc.gymplan100.data.contar
import com.marc.gymplan100.data.Progression
import com.marc.gymplan100.data.ExerciseAliases
import com.marc.gymplan100.data.ProgressRepository
import com.marc.gymplan100.data.ProgressState
import com.marc.gymplan100.data.EjercicioCatalogo
import com.marc.gymplan100.data.Protocolo
import com.marc.gymplan100.data.Rutina
import com.marc.gymplan100.data.SessionEngine
import com.marc.gymplan100.data.SessionPhase
import com.marc.gymplan100.data.SessionRecord
import com.marc.gymplan100.data.SetLog
import com.marc.gymplan100.data.heaviestWeight
import com.marc.gymplan100.data.SpecialSessionEngine
import com.marc.gymplan100.data.SpecialWorkoutsData
import com.marc.gymplan100.data.SpecialWorkoutsLoader
import com.marc.gymplan100.data.TrainingDay
import com.marc.gymplan100.data.TrainingPlan
import com.marc.gymplan100.data.UserProfile
import com.marc.gymplan100.data.WeeklyFrequency
import com.marc.gymplan100.data.secondsPerSetFromScheme
import com.marc.gymplan100.health.HealthConnectManager
import com.marc.gymplan100.notify.RestReminder
import com.marc.gymplan100.wear.WearBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlanViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProgressRepository(app)

    private val _progress = MutableStateFlow(ProgressState())
    val progress: StateFlow<ProgressState> = _progress.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    /**
     * El progreso guardado ya está leído.
     *
     * Hasta que lo esté, [progress] es un estado vacío, y pintar la portada con él significa
     * enseñar "PLAN NUEVO · empieza cuando quieras · día 1" a alguien que lleva cuarenta días
     * hechos. Dura un parpadeo, pero es un parpadeo que dice justo lo contrario de la verdad.
     */
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

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

    // --- Perfil del usuario (peso / género / reloj) -----------------------
    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    /** Guarda el perfil editado con las ruedas de Configuración. */
    fun updateProfile(profile: UserProfile) {
        _profile.value = profile
        viewModelScope.launch { repo.saveProfile(profile) }
    }

    /**
     * Rellena el peso leyéndolo de Google Health. Llama a [onDone] con `true` si se importó.
     * Requiere que el permiso de lectura esté concedido (si no, no hay datos y devuelve
     * `false`); la UI se encarga de pedirlo antes.
     */
    fun importProfileFromHealth(onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val weight = healthConnect.latestWeightKg()
            if (weight == null) {
                onDone(false)
                return@launch
            }
            val merged = _profile.value.copy(weightKg = Math.round(weight).toInt())
            _profile.value = merged
            repo.saveProfile(merged)
            onDone(true)
        }
    }

    // --- Planes de entrenamiento -----------------------------------------

    private val _activePlan = MutableStateFlow(PlanData.active)
    /** El plan que se está siguiendo. La navegación se rehace cuando cambia. */
    val activePlan: StateFlow<TrainingPlan> = _activePlan.asStateFlow()

    private val _plans = MutableStateFlow(PlanStore.allPlans(app))
    /** El plan que viene con la app más los que haya importado el usuario. */
    val plans: StateFlow<List<TrainingPlan>> = _plans.asStateFlow()

    private val _planProgress = MutableStateFlow<Map<String, ProgressState>>(emptyMap())
    /**
     * Progreso de cada plan (por id), para enseñarlo en la lista sin activarlos: días hechos,
     * si está terminado y por qué vuelta va.
     *
     * Los de los planes que no sigues se leen de disco al cambiar de plan, pero **el activo
     * sale del progreso en vivo**: si no, la lista enseñaba como "sin empezar" el plan que
     * acabas de terminar, porque el mapa era de cuando lo activaste.
     */
    val planProgress: StateFlow<Map<String, ProgressState>> =
        combine(_planProgress, _progress, _activePlan) { guardados, actual, plan ->
            guardados + (plan.id to actual)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _needsWelcome = MutableStateFlow(false)
    /**
     * Primera vez de verdad: no se ha elegido plan nunca y no hay nada entrenado. Quien ya
     * venía usando la app no lo ve, aunque nunca haya tocado la pantalla de planes.
     */
    val needsWelcome: StateFlow<Boolean> = _needsWelcome.asStateFlow()

    /** Ya ha elegido (o ha pasado del asistente): no volver a dar la bienvenida. */
    fun welcomeDone() {
        PlanStore.marcarPlanElegido(getApplication())
        _needsWelcome.value = false
    }

    private fun refreshWelcome() {
        val hayProgreso = _progress.value.completedDays.isNotEmpty() || _history.value.isNotEmpty()
        _needsWelcome.value = PlanStore.necesitaBienvenida(getApplication(), hayProgreso)
    }

    private fun refreshPlans() {
        _plans.value = PlanStore.allPlans(getApplication())
        _activePlan.value = PlanData.active
        viewModelScope.launch {
            _planProgress.value = _plans.value.associate { it.id to repo.progressOf(it.id) }
        }
    }

    /** Cambiar de plan a mitad de un entreno dejaría la sesión apuntando a otro día. */
    val canSwitchPlan: Boolean get() = _active.value == null

    /**
     * Lee el archivo que ha elegido el usuario y lo guarda como plan nuevo, sin activarlo:
     * primero lo ve en la lista y luego decide. Devuelve el resultado para poder contarle
     * qué ha fallado si el archivo no vale.
     */
    fun importPlan(uri: Uri, onResult: (PlanImport) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val text = runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    }
                }.getOrNull()
                when {
                    text == null -> PlanImport.Error("No se pudo abrir el archivo.")
                    !PlanCodec.isProbablyText(text) -> PlanImport.Error(
                        "Ese archivo no es un plan: elige un .md o un .json."
                    )
                    // El formato se deduce del contenido y no de la extensión: los archivos
                    // llegan del selector del sistema y muchos gestores no conservan el .md.
                    text.trimStart().startsWith("{") -> PlanCodec.parseJson(text)
                    else -> PlanMarkdownParser.parse(text)
                }
            }
            if (result is PlanImport.Ok) {
                PlanStore.save(getApplication(), result.dto)
                refreshPlans()
            }
            onResult(result)
        }
    }

    /** Pasa a seguir otro plan y recarga su progreso (el del plan anterior se queda guardado). */
    fun activatePlan(id: String) {
        if (!canSwitchPlan) return
        _activePlan.value = PlanStore.setActive(getApplication(), id)
        viewModelScope.launch {
            val cargado = repo.progress.first()
            // Un plan sin empezar arranca hoy: es la fecha que se enseña al terminarlo.
            _progress.value = if (cargado.startedAt == 0L && cargado.completedDays.isEmpty()) {
                cargado.copy(startedAt = System.currentTimeMillis()).also { repo.save(it) }
            } else {
                cargado
            }
            _history.value = repo.history.first()
            refreshPlans()
            // El reloj enseña el siguiente día pendiente: ahora es otro.
            WearBridge.publishState(getApplication(), null, nextDay = nextDay())
        }
    }

    /**
     * Vuelve a leerlo todo despues de restaurar una copia.
     *
     * Restaurar cambia el almacen por debajo: el plan activo puede ser otro, los dias otros y
     * el historial otro. Sin esto la pantalla seguiria ensenando lo de antes hasta reiniciar,
     * que es la clase de duda que hace pensar que la copia no ha entrado.
     */
    fun reloadAfterRestore() {
        _activePlan.value = PlanStore.activePlan(getApplication())
        viewModelScope.launch {
            _progress.value = repo.progress.first()
            _history.value = repo.history.first()
            _profile.value = repo.profile.first()
            _active.value = repo.activeSession.first()
            refreshPlans()
            WearBridge.publishState(getApplication(), null, nextDay = nextDay())
        }
    }

    // --- Editor de planes -------------------------------------------------

    private val _draft = MutableStateFlow<PlanDto?>(null)
    /** Plan que se está editando. Vive aquí y no en la pantalla para no perderlo al girar. */
    val draft: StateFlow<PlanDto?> = _draft.asStateFlow()

    /**
     * Abre el editor: con [planId] nulo empieza un plan en blanco con un día de ejemplo (una
     * pantalla vacía del todo no dice por dónde empezar); con un id, edita ese plan CONSERVANDO
     * su identidad, así que al guardar mantiene el progreso que ya llevabas en él.
     */
    fun startDraft(planId: String?) {
        _draft.value = if (planId == null) {
            PlanDto(
                nombre = "",
                fases = listOf(
                    FaseDto(
                        nombre = "Fase 1",
                        semanas = 4,
                        dias = listOf(DiaDto(dia = "Lunes", ejercicios = listOf(EjercicioDto())))
                    )
                ),
                origen = "editor"
            )
        } else {
            // Se conserva el origen: un plan importado sigue diciendo de dónde vino aunque
            // luego se retoque aquí.
            PlanStore.storedDto(getApplication(), planId)
        }
    }

    fun discardDraft() { _draft.value = null }

    /** Cambia el borrador. Todas las ediciones de la pantalla pasan por aquí. */
    fun editDraft(transform: (PlanDto) -> PlanDto) {
        _draft.value = _draft.value?.let(transform)
    }

    /**
     * Valida y guarda el borrador. Devuelve el resultado para que la pantalla enseñe el error
     * concreto sin cerrarse: lo escrito no se pierde por una coma mal puesta.
     */
    fun saveDraft(onResult: (PlanImport) -> Unit) {
        val dto = _draft.value ?: return
        val result = PlanCodec.fromDto(dto)
        if (result is PlanImport.Ok) {
            PlanStore.save(getApplication(), result.dto)
            // Si se ha editado el plan que se está siguiendo, la app tiene que verlo ya.
            if (result.plan.id == PlanData.active.id) {
                _activePlan.value = PlanStore.setActive(getApplication(), result.plan.id)
            }
            _draft.value = null
            refreshPlans()
        }
        onResult(result)
    }

    /** Borra un plan del usuario y su progreso. Si era el activo, se vuelve al de la app. */
    fun deletePlan(id: String) {
        viewModelScope.launch {
            PlanStore.delete(getApplication(), id)
            repo.clearPlanData(id)
            _progress.value = repo.progress.first()
            _history.value = repo.history.first()
            refreshPlans()
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
            // Después de leer el progreso: es lo que distingue "primera vez" de "ya venías".
            refreshWelcome()
            _loaded.value = true
        }
        refreshPlans()
        refreshHealthPermissions()
        // Los alias («este ejercicio mio es como este del catalogo») viven en el progreso pero
        // los consultan el catalogo de imagenes, las fichas y los objetivos musculares, que son
        // objetos puros. Se mantiene una ventana global al dia, igual que con el plan activo.
        viewModelScope.launch {
            _progress.collect { ExerciseAliases.set(it.exerciseAliases) }
        }
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
        // Si el plan venía sin fecha de arranque (o se marcó el primer día antes de tenerla),
        // el primer día completado la fija: así el resumen final siempre puede decir desde cuándo.
        val conInicio = if (before.startedAt == 0L && set.isNotEmpty()) {
            before.copy(startedAt = System.currentTimeMillis())
        } else {
            before
        }
        val after = conInicio.copy(completedDays = set)
        _progress.value = after
        viewModelScope.launch { repo.save(after) }
        if (!wasCompleted && day in after.completedDays) {
            triggerCelebration(before, after, day)
        }
    }

    private fun triggerCelebration(before: ProgressState, after: ProgressState, day: Int) {
        val total = after.completedDays.count { it in 1..PlanData.TOTAL_DAYS }
        val finalVictory = total >= PlanData.TOTAL_DAYS
        val esElReto = PlanData.active.id == BuiltinPlan.ID
        _celebration.value = Celebration(
            dayNumber = day,
            totalCompleted = total,
            message = pickMessage(total),
            newAchievements = Achievements.newlyUnlocked(before, after),
            isFinalVictory = finalVictory,
            offerAnthem = finalVictory && esElReto
        )
        if (finalVictory) {
            // Queda apuntado para poder enseñar el cierre del plan al salir del diálogo.
            val terminado = after.copy(
                finishedAt = System.currentTimeMillis(),
                finishedSeen = false
            )
            _progress.value = terminado
            viewModelScope.launch { repo.save(terminado) }
            // El himno es del reto de 100 días: en un bloque de 12 sesiones no pega.
            if (esElReto) playChampionsVideo()
        }
    }

    /**
     * Otra vuelta al mismo plan (Mantenimiento y Movilidad se hacen así, en bloques que se
     * repiten). Los días se vacían para empezar de cero, pero la vuelta queda contada y **el
     * historial de entrenos no se toca**: el tiempo entrenado sigue sumando.
     */
    fun startNewRound() {
        val nueva = _progress.value.restartedFromScratch(System.currentTimeMillis())
        _progress.value = nueva
        viewModelScope.launch {
            repo.save(nueva)
            WearBridge.publishState(getApplication(), null, nextDay = nextDay())
        }
    }

    /**
     * Repetir solo la última fase, en vez del plan entero.
     *
     * Es lo que pide un plan largo: después de 100 días, volver al día 1 es retroceder, pero
     * repetir la fase final mantiene el nivel al que has llegado. Los días de esa fase se
     * borran y el resto del progreso se queda como está.
     */
    fun repeatLastPhase() {
        val fase = PlanData.active.phases.lastOrNull() ?: return
        val dias = PlanData.daysOfPhase(fase.number).map { it.number }.toSet()
        if (dias.isEmpty()) return
        val nueva = _progress.value.restartedDays(dias, System.currentTimeMillis())
        _progress.value = nueva
        viewModelScope.launch {
            repo.save(nueva)
            WearBridge.publishState(getApplication(), null, nextDay = nextDay())
        }
    }

    /** Vuelve a preguntar las cuatro cosas para elegir otro plan (el mismo asistente del inicio). */
    fun askAdvisorAgain() {
        _needsWelcome.value = true
    }

    /**
     * Vuelve a abrir el resumen del plan terminado (desde la portada o la lista).
     *
     * Solo levanta la marca de "ya visto": la fecha de fin no se toca, porque es la del día
     * que se terminó el plan y no la de la última vez que se miró el resumen.
     */
    fun showPlanSummaryAgain() {
        val actual = _progress.value
        if (!actual.everFinished || actual.isFinished) return
        val visto = actual.copy(finishedSeen = false)
        _progress.value = visto
        viewModelScope.launch { repo.save(visto) }
    }

    /** Cierra la pantalla de fin de plan sin repetirlo: el plan se queda terminado. */
    fun dismissPlanFinished() {
        val actual = _progress.value
        if (!actual.isFinished) return
        val cerrado = actual.copy(finishedSeen = true)
        _progress.value = cerrado
        viewModelScope.launch { repo.save(cerrado) }
    }

    /**
     * Qué plan pega después del que se acaba de terminar. Se apoya en el mismo recomendador
     * de la bienvenida, dándole por hecho el objetivo del plan terminado y que quien acaba un
     * plan entero ya entrena con constancia.
     */
    fun nextPlanSuggestion(): PlanAdvisor.Suggestion? {
        val terminado = PlanData.active
        val objetivo = when (terminado.goal) {
            // Quien acaba el plan de empezar ya no está empezando.
            PlanGoal.START -> PlanGoal.LOSE_FAT
            // Tras un plan duro, lo natural es sostener lo ganado.
            PlanGoal.MUSCLE, PlanGoal.STRENGTH -> PlanGoal.MAINTAIN
            null -> return null
            else -> terminado.goal
        }
        val dias = terminado.daysPerWeek.takeIf { it > 0 } ?: 3
        val respuestas = PlanAdvisor.Answers(
            goal = objetivo,
            shape = PlanAdvisor.Shape.REGULAR,
            days = PlanAdvisor.Days.entries.firstOrNull { it.n == dias } ?: PlanAdvisor.Days.THREE,
            place = if (terminado.equipment == PlanEquipment.NONE) PlanAdvisor.Place.HOME
            else PlanAdvisor.Place.GYM
        )
        return PlanAdvisor.recommend(_plans.value.filter { it.id != terminado.id }, respuestas).best
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

    /**
     * Peso de UNA serie suelta, apuntado a mano en la ficha del día.
     *
     * La lista se rellena hasta [totalSets] con series en blanco: una serie sin apuntar no es
     * un cero, es un hueco, y la tercera tiene que poder llevar peso aunque la segunda no lo
     * lleve. El peso del ejercicio pasa a ser el más alto de las series, igual que al terminar
     * un entreno guiado.
     */
    fun setDaySetWeight(
        day: Int,
        exerciseIndex: Int,
        setIndex: Int,
        totalSets: Int,
        weight: String
    ) = update { state ->
        val key = "$day-$exerciseIndex"
        val current = state.logs[key] ?: ExerciseLog()
        val size = maxOf(totalSets, setIndex + 1, current.sets.size)
        val sets = List(size) { i ->
            val previo = current.sets.getOrNull(i) ?: SetLog()
            if (i == setIndex) previo.copy(weight = weight.trim()) else previo
        }
        val tope = heaviestWeight(sets.map { it.weight })
        val updated = current.copy(
            sets = sets,
            weight = tope.ifBlank { "" }
        )
        val name = PlanData.dayByNumber(day)?.template?.exercises?.getOrNull(exerciseIndex)?.name
        val weights = if (tope.isNotBlank() && !name.isNullOrBlank()) {
            state.exerciseWeights + (name to tope)
        } else state.exerciseWeights
        state.copy(logs = state.logs + (key to updated), exerciseWeights = weights)
    }

    /**
     * Repeticiones de UNA serie suelta, apuntadas a mano en la ficha del día.
     *
     * Mismo criterio que [setDaySetWeight]: la lista se rellena con series en blanco para que
     * la tercera pueda llevar algo aunque la segunda esté vacía. El resumen del ejercicio se
     * queda con las de la primera serie, que es por donde se arranca la próxima vez.
     */
    fun setDaySetReps(
        day: Int,
        exerciseIndex: Int,
        setIndex: Int,
        totalSets: Int,
        reps: String
    ) = update { state ->
        val key = "$day-$exerciseIndex"
        val current = state.logs[key] ?: ExerciseLog()
        val size = maxOf(totalSets, setIndex + 1, current.sets.size)
        val sets = List(size) { i ->
            val previo = current.sets.getOrNull(i) ?: SetLog()
            if (i == setIndex) previo.copy(reps = reps.trim()) else previo
        }
        val arranque = sets.firstOrNull { it.reps.isNotBlank() }?.reps.orEmpty()
        val updated = current.copy(sets = sets, reps = arranque)
        val name = PlanData.dayByNumber(day)?.template?.exercises?.getOrNull(exerciseIndex)?.name
        val repsPorEjercicio = if (arranque.isNotBlank() && !name.isNullOrBlank()) {
            state.exerciseReps + (name to arranque)
        } else state.exerciseReps
        state.copy(logs = state.logs + (key to updated), exerciseReps = repsPorEjercicio)
    }

    /**
     * Dice a que ejercicio del catalogo se parece uno que la app no conoce, o lo desvincula
     * con null. Con eso hereda ilustracion, ficha, musculos y sitio en el mapa.
     */
    fun setExerciseAlias(name: String, catalogName: String?) = update { state ->
        val limpio = catalogName?.trim().orEmpty()
        val nuevos = if (limpio.isBlank()) state.exerciseAliases - name
        else state.exerciseAliases + (name to limpio)
        state.copy(exerciseAliases = nuevos)
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

    // --- Entrenamientos especiales (Rutina Militar / Quema Grasa) ----------

    /** Definición de las rutinas especiales, cargada del asset la primera vez que se usa. */
    val specialWorkouts: SpecialWorkoutsData by lazy {
        runCatching { SpecialWorkoutsLoader.load(getApplication()) }.getOrElse { SpecialWorkoutsData() }
    }

    /** Estado de frecuencia de la Rutina Militar esta semana (para el aviso no bloqueante). */
    fun militaryFrequency(): WeeklyFrequency.Status {
        val rutina = specialWorkouts.militar ?: return WeeklyFrequency.Status(0, 0, false)
        return WeeklyFrequency.militaryStatus(_history.value, rutina.frecuencia_semanal)
    }

    /** Estado de frecuencia de la Rutina Altura y Postura (aviso DIARIO: ya hecha hoy). */
    fun postureFrequency(): WeeklyFrequency.Status {
        val rutina = specialWorkouts.altura ?: return WeeklyFrequency.Status(0, 0, false)
        return WeeklyFrequency.routineStatus(
            _history.value,
            rutina.id,
            rutina.frecuencia_semanal,
            daily = rutina.esDiaria,
            dailyMax = rutina.frecuencia_diaria.max
        )
    }

    /** Estado de frecuencia de un ejercicio de quema grasa esta semana. */
    fun exerciseFrequency(exercise: EjercicioCatalogo): WeeklyFrequency.Status =
        WeeklyFrequency.exerciseStatus(_history.value, exercise)

    /** Programa (o cancela) el aviso de la cuenta atrás según la fase de una sesión de rutina. */
    private fun scheduleRoutineReminder(s: ActiveSession) {
        val app = getApplication<Application>()
        when (s.phase) {
            SessionPhase.TIMED_SET -> {
                if (s.timedTargetSeconds > 0 && !s.timedPaused) {
                    RestReminder.schedule(
                        app,
                        s.timedStartMillis + s.timedTargetSeconds * 1000L,
                        RestReminder.KIND_TIMED_SET,
                        s.dayNumber
                    )
                } else RestReminder.cancel(app)
            }
            SessionPhase.RESTING -> RestReminder.schedule(
                app,
                s.restStartMillis + s.restTargetSeconds * 1000L,
                RestReminder.KIND_BETWEEN_SETS,
                s.dayNumber
            )
            else -> RestReminder.cancel(app) // WORKING (reps) / FINISHED: sin cuenta atrás
        }
    }

    /** Inicia la Rutina Militar (secuencia guiada de pasos). Descarta cualquier sesión en curso. */
    fun startMilitarySession() = startFixedRoutine(specialWorkouts.militar)

    /** Inicia la Rutina Altura y Postura (secuencia guiada de 5 pasos con series). */
    fun startPostureSession() = startFixedRoutine(specialWorkouts.altura)

    private fun startFixedRoutine(rutina: com.marc.gymplan100.data.Rutina?) {
        if (rutina == null) return
        RestReminder.cancel(getApplication())
        val now = System.currentTimeMillis()
        val session = SpecialSessionEngine.startFixedSequence(rutina, nextDay(), now)
        scheduleRoutineReminder(session)
        saveActive(session)
    }

    /**
     * Marca la serie actual de una secuencia fija como hecha ([reps] para las de repeticiones) y
     * avanza. Con [endExercise] se da el paso por terminado aunque queden series (elegir 2 de 3).
     */
    fun completeFixedSerie(reps: String = "", endExercise: Boolean = false) {
        val s = _active.value ?: return
        val rutina = specialWorkouts.rutina(s.routineId ?: return) ?: return
        if (!rutina.esSecuenciaFija) return
        val next = SpecialSessionEngine.advanceFixedSerie(
            rutina, s, reps, System.currentTimeMillis(), endExercise
        )
        scheduleRoutineReminder(next)
        saveActive(next)
    }

    /** En el paso con alternativa (burpees), cambia a la variante por tiempo (jumping jacks). */
    fun chooseAlternative() {
        val s = _active.value ?: return
        val rutina = specialWorkouts.rutina(s.routineId ?: return) ?: return
        val next = SpecialSessionEngine.chooseAlternative(rutina, s, System.currentTimeMillis())
        if (next !== s) {
            scheduleRoutineReminder(next)
            saveActive(next)
        }
    }

    /** Inicia un ejercicio de quema grasa con el protocolo elegido. Descarta la sesión en curso. */
    fun startFatburnSession(exercise: EjercicioCatalogo, protocol: Protocolo) {
        RestReminder.cancel(getApplication())
        val now = System.currentTimeMillis()
        val session = SpecialSessionEngine.startFatburn(exercise, protocol, nextDay(), now)
        scheduleRoutineReminder(session)
        saveActive(session)
    }

    /** Completa la unidad (ronda/serie) actual de quema grasa y avanza (descanso o fin). */
    fun completeFatburnUnit(reps: String = "") {
        val s = _active.value ?: return
        val protocol = currentFatburnProtocol(s) ?: return
        val next = SpecialSessionEngine.completeFatburnUnit(protocol, s, reps, System.currentTimeMillis())
        scheduleRoutineReminder(next)
        saveActive(next)
    }

    /** Termina el descanso de la rutina (entre series/ejercicios de secuencia fija o de quema grasa). */
    fun endRoutineRest() {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.RESTING) return
        RestReminder.cancel(getApplication())
        val rutina = specialWorkouts.rutina(s.routineId ?: return)
        val next = if (rutina != null && rutina.esSecuenciaFija) {
            SpecialSessionEngine.endFixedRest(rutina, s, System.currentTimeMillis())
        } else {
            val protocol = currentFatburnProtocol(s) ?: return
            SpecialSessionEngine.endFatburnRest(protocol, s, System.currentTimeMillis())
        }
        scheduleRoutineReminder(next)
        saveActive(next)
    }

    private fun currentFatburnProtocol(s: ActiveSession): Protocolo? {
        val exercise = specialWorkouts.ejercicio(s.exerciseId ?: return null) ?: return null
        return exercise.protocolos.firstOrNull { it.nombre == s.protocolName }
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

    /**
     * Peso sugerido para la serie actual: el preparado en el descanso, si no el de la serie
     * anterior de la misma máquina, y si no el último peso conocido del ejercicio.
     *
     * La cuenta la hace [SessionEngine.weightForSet], que es la misma que usa el reloj para
     * saber con qué peso apuntar una serie marcada desde la muñeca.
     */
    /**
     * Qué toca hoy en el ejercicio en curso, y por qué, según lo que se hizo las veces anteriores.
     *
     * Solo en la PRIMERA serie del ejercicio: a partir de la segunda manda lo que se está
     * levantando hoy, no lo que dice el historial. Null cuando no hay nada que proponer.
     */
    fun progressionFor(session: ActiveSession): Progression.Suggestion? {
        val exercise = PlanData.dayByNumber(session.dayNumber)
            ?.template?.exercises?.getOrNull(session.exerciseIndex) ?: return null
        if (session.completedSets.any { it.exerciseIndex == session.exerciseIndex }) return null
        return Progression.suggest(
            exercise.scheme,
            Progression.historyOf(_progress.value, exercise.name, excludeDay = session.dayNumber),
            hasWeight = !exercise.withoutWeight
        )
    }

    fun suggestedWeight(session: ActiveSession): String {
        // Lo que se dejó preparado en el descanso manda sobre cualquier propuesta.
        if (session.plannedWeight.isBlank()) {
            progressionFor(session)?.weight?.takeIf { it.isNotBlank() }?.let { return it }
        }
        val fromSession = SessionEngine.weightForSet(session, _progress.value.exerciseWeights)
        if (fromSession.isNotBlank()) return fromSession
        return logFor(session.dayNumber, session.exerciseIndex).weight
    }

    /**
     * Repeticiones precargadas para la serie en curso. Es la misma cuenta que hace el reloj,
     * así que la pantalla y la muñeca apuntan siempre lo mismo.
     */
    fun suggestedReps(session: ActiveSession): String {
        if (session.currentReps.isBlank()) {
            progressionFor(session)?.reps?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return SessionEngine.repsForSet(session, _progress.value.exerciseReps)
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

    /**
     * Guarda el peso que se está poniendo a la serie EN CURSO, antes de darla por hecha.
     *
     * Hasta ahora ese peso vivía solo en la pantalla: si Android se llevaba la app por delante
     * mientras el móvil estaba en el bolsillo entre serie y serie, al volver el peso escrito
     * había desaparecido y la app pedía otra vez "pon el peso que uses hoy". Todo lo demás de
     * la sesión (la fase, las series hechas, el peso preparado en el descanso) sí se guardaba,
     * y esto no; ahora usa el mismo campo, que [SessionEngine] ya limpia al terminar la serie.
     */
    fun setSetWeight(weight: String) {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.WORKING && s.phase != SessionPhase.TIMED_SET) return
        val clean = weight.trim()
        if (s.plannedWeight == clean) return
        saveActive(s.copy(plannedWeight = clean))
    }

    /**
     * Guarda las repeticiones de la serie en curso, por el mismo motivo que el peso: si Android
     * mata la app entre serie y serie, lo apuntado sigue ahí al volver.
     */
    fun setSetReps(reps: String) {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.WORKING && s.phase != SessionPhase.TIMED_SET) return
        val clean = reps.trim()
        if (s.currentReps == clean) return
        saveActive(s.copy(currentReps = clean))
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

    /**
     * Marca la serie actual como hecha y arranca el descanso (o finaliza el día).
     *
     * [reps] llega en blanco desde donde no se preguntan (una serie por tiempo, una vuelta de
     * circuito); ahí se resuelve con la misma cascada que usa el reloj para el peso.
     */
    fun completeSet(
        weight: String,
        reps: String = "",
        warmup: Boolean = false,
        rir: String = ""
    ) {
        val s = _active.value ?: return
        if (s.phase != SessionPhase.WORKING && s.phase != SessionPhase.TIMED_SET) return
        val day = PlanData.dayByNumber(s.dayNumber) ?: return
        val exercise = day.template.exercises.getOrNull(s.exerciseIndex) ?: return

        // El peso de referencia es un efecto propio del ViewModel; la transición de fase la
        // calcula SessionEngine (compartida con la notificación) para no divergir.
        //
        // Se guarda el MÁS ALTO del ejercicio en esta sesión, no el de esta serie: bajando de
        // 12 a 11 en la última, "Mis pesos" no debe olvidar que hoy has movido 12.
        val cleanWeight = weight.trim()
        // Las series de calentamiento no tocan el peso de referencia: aproximar con la mitad
        // no puede hacer que "Mis pesos" olvide lo que de verdad mueves.
        val tope = heaviestWeight(
            s.completedSets
                .filter { it.exerciseIndex == s.exerciseIndex && !it.warmup }
                .map { it.weight } + (if (warmup) "" else cleanWeight)
        )
        if (tope.isNotBlank()) {
            update { it.copy(exerciseWeights = it.exerciseWeights + (exercise.name to tope)) }
        }
        val repsFinales = reps.trim().ifBlank { SessionEngine.repsForSet(s, _progress.value.exerciseReps) }
        val next = SessionEngine.completeSet(
            s, weight, System.currentTimeMillis(), repsFinales, warmup, rir
        )
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

    /**
     * Apunta el esfuerzo (RIR) de la última serie cerrada.
     *
     * Se pregunta en el descanso y no en la serie: justo cuando acabas es cuando sabes si te
     * has quedado corto, y la pantalla de la serie ya tiene bastante trabajo con el peso y las
     * repeticiones.
     */
    fun setLastSetRir(rir: String) {
        val s = _active.value ?: return
        val sets = s.completedSets
        if (sets.isEmpty()) return
        val actualizada = sets.toMutableList()
        actualizada[actualizada.lastIndex] = actualizada.last().copy(rir = rir.trim())
        saveActive(s.copy(completedSets = actualizada))
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
    /**
     * Termina un entreno libre apuntando lo que se ha hecho.
     *
     * Lo apuntado va a tres sitios, y por eso merece la pena: al historial (para verlo en
     * Resultados), a los pesos por ejercicio (para que la próxima vez te lo sugiera, tanto
     * aquí como en el entreno guiado) y, si el ejercicio es uno de los del día, también al
     * registro de ese día del plan.
     */
    fun finishFreeSession(entries: List<LoggedExercise>) {
        val limpias = entries
            .map { entrada ->
                val series = entrada.sets
                    .map { SetLog(it.weight.trim(), it.reps.trim()) }
                    .dropLastWhile { it.isEmpty }
                // El resumen del ejercicio sale de sus series: el peso más alto y las
                // repeticiones que se hicieron con él.
                val tope = heaviestWeight(series.map { it.weight })
                val repsDelTope = series.firstOrNull { it.weight == tope && it.reps.isNotBlank() }?.reps
                entrada.copy(
                    name = entrada.name.trim(),
                    weight = tope.ifBlank { entrada.weight.trim() },
                    reps = repsDelTope
                        ?: series.firstOrNull { it.reps.isNotBlank() }?.reps
                        ?: entrada.reps.trim(),
                    sets = series
                )
            }
            .filter { it.name.isNotEmpty() }

        if (limpias.isNotEmpty()) {
            val dia = PlanData.dayByNumber(_active.value?.dayNumber ?: 0)
            var estado = _progress.value
            limpias.forEach { entrada ->
                if (entrada.weight.isNotEmpty()) {
                    estado = estado.copy(
                        exerciseWeights = estado.exerciseWeights + (entrada.name to entrada.weight)
                    )
                }
                // Si coincide con un ejercicio del día, queda también en su ficha del plan.
                val indice = dia?.template?.exercises?.indexOfFirst { it.name == entrada.name } ?: -1
                if (indice >= 0) {
                    val clave = "${dia!!.number}-$indice"
                    val previo = estado.logs[clave] ?: ExerciseLog()
                    estado = estado.copy(
                        logs = estado.logs + (clave to previo.copy(
                            weight = entrada.weight.ifEmpty { previo.weight },
                            reps = entrada.reps.ifEmpty { previo.reps },
                            done = true,
                            sets = entrada.sets.ifEmpty { previo.sets }
                        ))
                    )
                }
            }
            _progress.value = estado
            viewModelScope.launch { repo.save(estado) }
        }

        finishSession(logged = limpias)
    }

    fun finishSession(logged: List<LoggedExercise> = emptyList()) {
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
            extra = s.extra,
            routineId = s.routineId,
            exerciseId = s.exerciseId,
            // Las secuencias fijas (militar, altura y postura) cuentan para la frecuencia solo si se
            // completaron (finalizar aparece al llegar al último paso). La quema grasa cuenta por
            // ejercicio (usa exerciseId, no este flag).
            routineCompleted = s.routineId != null &&
                specialWorkouts.rutina(s.routineId)?.esSecuenciaFija == true,
            logged = logged
        )
        viewModelScope.launch { repo.appendHistory(record) }
        _history.value = _history.value + record

        // Vuelca el entreno a Health Connect (Google Health) si el usuario lo conectó.
        syncToHealthConnect(s, record)

        // Las rutinas especiales (militar / quema grasa) son un bonus: se guardan en el historial
        // (para la frecuencia semanal y Resultados) pero NO marcan día ni disparan celebración.
        if (s.isRoutine) {
            RestReminder.cancel(getApplication())
            saveActive(null)
            return
        }

        // Un entrenamiento EXTRA es un bonus: se guarda en el historial pero NO marca
        // ningún día del plan ni toca los pesos ni dispara celebración de hito.
        if (s.extra) {
            RestReminder.cancel(getApplication())
            saveActive(null)
            return
        }

        val before = _progress.value
        val wasCompleted = s.dayNumber in before.completedDays
        val after = before.withFinishedDay(s.dayNumber, s.completedSets)
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
            session.routineId == SpecialWorkoutsLoader.MILITAR_ID -> "Rutina Militar"
            session.routineId != null -> {
                val exName = specialWorkouts.ejercicio(session.exerciseId ?: "")?.nombre
                "Quema grasa · ${exName ?: "ejercicio"}"
            }
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
        if (session.isRoutine) {
            val name = if (session.routineId == SpecialWorkoutsLoader.MILITAR_ID) "Rutina Militar"
            else specialWorkouts.ejercicio(session.exerciseId ?: "")?.nombre ?: "Quema grasa"
            sb.append("$name.\n")
        } else if (session.special || day == null) {
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
        sb.append("Duración: $mins min · " + contar(record.totalSets, "serie", "series"))
        return sb.toString().trim()
    }

    /** Descarta la sesión en curso sin guardar nada. */
    fun cancelSession() {
        RestReminder.cancel(getApplication())
        saveActive(null)
    }

    /** Oculta para siempre el aviso de optimización de batería. */
    fun dismissBatteryHint() = update { it.copy(batteryHintDismissed = true) }

    /** Invierte el orden de la lista de Resultados (se recuerda entre sesiones). */
    fun toggleResultsOrder() = update { it.copy(resultsNewestFirst = !it.resultsNewestFirst) }
}
