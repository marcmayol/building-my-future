package com.marc.gymplan100.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Guarda los planes del usuario y cuál de ellos está activo.
 *
 * Usa SharedPreferences en lugar del DataStore que usa el resto de la app a propósito: el
 * proceso puede arrancar sin interfaz (el servicio del reloj, el aviso de descanso) y esos
 * caminos necesitan el plan cargado ya, no cuando termine una corrutina. Son unos pocos
 * planes y un id: leerlos de golpe no cuesta nada.
 *
 * Los planes se guardan en el mismo formato con el que se importan ([PlanDto]), así que
 * siempre pasan por la misma validación vengan de donde vengan.
 */
object PlanStore {

    private const val PREFS = "gym_planes"
    private const val KEY_PLANS = "planes"
    private const val KEY_ACTIVE = "plan_activo"

    /** Ya se eligió plan alguna vez (a mano o con el asistente): no hay que dar la bienvenida. */
    private const val KEY_ELEGIDO = "plan_elegido_alguna_vez"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun storedDtos(context: Context): List<PlanDto> {
        val raw = prefs(context).getString(KEY_PLANS, null)
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<PlanDto>>(raw) }.getOrElse { emptyList() }
    }

    private fun writeDtos(context: Context, dtos: List<PlanDto>) {
        prefs(context).edit().putString(KEY_PLANS, json.encodeToString(dtos)).apply()
    }

    /** Planes del usuario, del importado más recientemente al más antiguo. */
    fun customPlans(context: Context): List<TrainingPlan> =
        storedDtos(context)
            .mapNotNull { (PlanCodec.fromDto(it) as? PlanImport.Ok)?.plan }
            .sortedByDescending { it.importedAt }

    /**
     * Todos los planes disponibles: el reto de 100 días primero, después el resto de los que
     * vienen con la app (Arranque, Hipertrofia, los bloques extra…) y al final los del usuario.
     */
    fun allPlans(context: Context): List<TrainingPlan> =
        listOf(BuiltinPlan.plan) + BuiltinPlans.all(context) + customPlans(context)

    /** El plan tal y como se guardó, para volver a editarlo sin perder nada por el camino. */
    fun storedDto(context: Context, id: String): PlanDto? =
        storedDtos(context).firstOrNull { it.id == id }

    fun activeId(context: Context): String =
        prefs(context).getString(KEY_ACTIVE, null) ?: BuiltinPlan.ID

    /**
     * ¿Hay que darle la bienvenida y ayudarle a elegir plan?
     *
     * Solo la primera vez de verdad. Quien ya venía usando la app **nunca** ve el asistente,
     * aunque jamás haya tocado la pantalla de planes: se detecta por [tieneProgreso] (días
     * completados o entrenos en el historial) y se marca como elegido sin molestar.
     */
    fun necesitaBienvenida(context: Context, tieneProgreso: Boolean): Boolean {
        val p = prefs(context)
        if (p.getBoolean(KEY_ELEGIDO, false)) return false
        if (tieneProgreso || p.contains(KEY_ACTIVE)) {
            marcarPlanElegido(context)
            return false
        }
        return true
    }

    /** Deja de dar la bienvenida: ya eligió (con el asistente, de la lista o creando uno). */
    fun marcarPlanElegido(context: Context) {
        prefs(context).edit().putBoolean(KEY_ELEGIDO, true).apply()
    }

    /** El plan activo; si el guardado ya no existe, se vuelve al de la app. */
    fun activePlan(context: Context): TrainingPlan {
        val id = activeId(context)
        if (id == BuiltinPlan.ID) return BuiltinPlan.plan
        return BuiltinPlans.byId(context, id)
            ?: customPlans(context).firstOrNull { it.id == id }
            ?: BuiltinPlan.plan
    }

    /** Deja el plan activo cargado en [PlanData]. Se llama al arrancar el proceso. */
    fun load(context: Context) {
        PlanData.setActive(activePlan(context))
    }

    /**
     * Guarda un plan importado o editado. Si ya existía uno con su id, lo sustituye y el
     * progreso de ese plan se mantiene (es la vía para corregir un plan sin empezar de cero).
     */
    fun save(context: Context, dto: PlanDto) {
        val id = dto.id ?: return
        val rest = storedDtos(context).filterNot { it.id == id }
        writeDtos(context, rest + dto)
    }

    /** Borra un plan del usuario. Si era el activo, se vuelve al plan de la app. */
    fun delete(context: Context, id: String) {
        if (id == BuiltinPlan.ID) return
        writeDtos(context, storedDtos(context).filterNot { it.id == id })
        if (activeId(context) == id) setActive(context, BuiltinPlan.ID)
    }

    /** Cambia el plan activo y lo deja cargado. Devuelve el plan que queda activo. */
    fun setActive(context: Context, id: String): TrainingPlan {
        prefs(context).edit()
            .putString(KEY_ACTIVE, id)
            .putBoolean(KEY_ELEGIDO, true)
            .apply()
        val plan = activePlan(context)
        PlanData.setActive(plan)
        return plan
    }
}
