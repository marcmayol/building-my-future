package com.marc.gymplan100.data

/**
 * Ventana al plan ACTIVO.
 *
 * Toda la app (pantallas, motor de sesión, estadísticas, logros y el puente con el reloj)
 * pregunta por el plan a través de aquí. Antes esto ERA el plan de 100 días; ahora es un
 * puntero al que el usuario tenga activo, así que el resto del código no ha tenido que
 * cambiar para que existan varios planes.
 *
 * Se escribe una sola vez al arrancar ([GymApp]) y luego solo al cambiar de plan desde la
 * pantalla de planes. Es un valor global mutable a propósito: el proceso puede arrancar sin
 * interfaz (el servicio del reloj o el aviso de descanso), y esos caminos también necesitan
 * el plan cargado. Por eso [PlanStore] guarda en SharedPreferences, que se lee al instante.
 */
object PlanData {

    @Volatile
    private var current: TrainingPlan = BuiltinPlan.plan

    /** El plan activo ahora mismo. */
    val active: TrainingPlan get() = current

    /** Cambia el plan activo. Lo llaman [PlanStore] al cargar y el ViewModel al elegir otro. */
    fun setActive(plan: TrainingPlan) {
        current = plan
    }

    val phases: List<Phase> get() = current.phases

    val days: List<TrainingDay> get() = current.days

    /** Días que dura el plan activo (100 en el integrado, libre en los importados). */
    val TOTAL_DAYS: Int get() = current.totalDays

    val exerciseNames: List<String> get() = current.exerciseNames

    fun dayByNumber(n: Int): TrainingDay? = current.dayByNumber(n)

    fun daysOfPhase(phaseNumber: Int): List<TrainingDay> = current.daysOfPhase(phaseNumber)

    fun weekWithinPhase(day: TrainingDay): Int = current.weekWithinPhase(day)
}
