package com.marc.gymplan100.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gym_progress")

/**
 * Progreso e historial, separados por plan.
 *
 * Cada plan guarda lo suyo bajo su propio id ("progress_json__<id>"), así se puede cambiar de
 * plan y volver sin mezclar días ni pesos. El plan de la app usaba claves sin id hasta la v1.8;
 * esas se siguen leyendo como respaldo cuando la clave nueva aún no existe, de modo que el
 * progreso de siempre aparece tal cual tras actualizar y nunca se sobrescribe lo viejo.
 *
 * La sesión en curso y el perfil siguen siendo únicos: no dependen del plan.
 */
class ProgressRepository(private val context: Context) {

    private val legacyKey = stringPreferencesKey("progress_json")
    private val legacyHistoryKey = stringPreferencesKey("session_history_json")
    private val activeKey = stringPreferencesKey("active_session_json")
    private val profileKey = stringPreferencesKey("user_profile_json")
    private val json = Json { ignoreUnknownKeys = true }

    private fun progressKey(planId: String) = stringPreferencesKey("progress_json__$planId")
    private fun historyKey(planId: String) = stringPreferencesKey("session_history_json__$planId")

    /** El plan al que pertenece lo que se lee y se escribe: el activo en ese momento. */
    private val planId: String get() = PlanData.active.id

    private fun readProgress(prefs: Preferences, planId: String): ProgressState {
        val raw = prefs[progressKey(planId)]
            ?: if (planId == BuiltinPlan.ID) prefs[legacyKey] else null
        if (raw.isNullOrEmpty()) return ProgressState()
        return runCatching { json.decodeFromString<ProgressState>(raw) }.getOrElse { ProgressState() }
    }

    private fun readHistory(prefs: Preferences, planId: String): List<SessionRecord> {
        val raw = prefs[historyKey(planId)]
            ?: if (planId == BuiltinPlan.ID) prefs[legacyHistoryKey] else null
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching { json.decodeFromString<List<SessionRecord>>(raw) }.getOrElse { emptyList() }
    }

    val progress: Flow<ProgressState> = context.dataStore.data.map { readProgress(it, planId) }

    suspend fun save(state: ProgressState) {
        val id = planId
        context.dataStore.edit { prefs ->
            prefs[progressKey(id)] = json.encodeToString(state)
        }
    }

    /** Sesión de entrenamiento en curso (null si no hay ninguna). */
    val activeSession: Flow<ActiveSession?> = context.dataStore.data.map { prefs ->
        val raw = prefs[activeKey]
        if (raw.isNullOrEmpty()) null
        else runCatching { json.decodeFromString<ActiveSession>(raw) }.getOrNull()
    }

    suspend fun saveActiveSession(session: ActiveSession?) {
        context.dataStore.edit { prefs ->
            if (session == null) prefs.remove(activeKey)
            else prefs[activeKey] = json.encodeToString(session)
        }
    }

    /** Historial de sesiones finalizadas del plan activo, de más antigua a más reciente. */
    val history: Flow<List<SessionRecord>> = context.dataStore.data.map { readHistory(it, planId) }

    suspend fun appendHistory(record: SessionRecord) {
        val id = planId
        context.dataStore.edit { prefs ->
            prefs[historyKey(id)] = json.encodeToString(readHistory(prefs, id) + record)
        }
    }

    /** Días completados de un plan cualquiera, para enseñarlos en la lista de planes. */
    suspend fun completedDaysOf(planId: String): Int =
        readProgress(context.dataStore.data.first(), planId).completedDays.size

    /** Borra el progreso y el historial de un plan (al borrar el plan). */
    suspend fun clearPlanData(planId: String) {
        context.dataStore.edit { prefs ->
            prefs.remove(progressKey(planId))
            prefs.remove(historyKey(planId))
        }
    }

    /** Perfil del usuario (peso para las calorías, género para las ilustraciones, reloj). */
    val profile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        val raw = prefs[profileKey]
        if (raw.isNullOrEmpty()) UserProfile()
        else runCatching { json.decodeFromString<UserProfile>(raw) }.getOrElse { UserProfile() }
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[profileKey] = json.encodeToString(profile)
        }
    }
}
