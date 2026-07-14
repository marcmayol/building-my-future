package com.marc.gymplan100.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gym_progress")

class ProgressRepository(private val context: Context) {

    private val key = stringPreferencesKey("progress_json")
    private val activeKey = stringPreferencesKey("active_session_json")
    private val historyKey = stringPreferencesKey("session_history_json")
    private val profileKey = stringPreferencesKey("user_profile_json")
    private val json = Json { ignoreUnknownKeys = true }

    val progress: Flow<ProgressState> = context.dataStore.data.map { prefs ->
        val raw = prefs[key]
        if (raw.isNullOrEmpty()) {
            ProgressState()
        } else {
            runCatching { json.decodeFromString<ProgressState>(raw) }.getOrElse { ProgressState() }
        }
    }

    suspend fun save(state: ProgressState) {
        context.dataStore.edit { prefs ->
            prefs[key] = json.encodeToString(state)
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

    /** Historial de sesiones finalizadas, de más antigua a más reciente. */
    val history: Flow<List<SessionRecord>> = context.dataStore.data.map { prefs ->
        val raw = prefs[historyKey]
        if (raw.isNullOrEmpty()) emptyList()
        else runCatching { json.decodeFromString<List<SessionRecord>>(raw) }.getOrElse { emptyList() }
    }

    suspend fun appendHistory(record: SessionRecord) {
        context.dataStore.edit { prefs ->
            val current = prefs[historyKey]?.let {
                runCatching { json.decodeFromString<List<SessionRecord>>(it) }.getOrElse { emptyList() }
            } ?: emptyList()
            prefs[historyKey] = json.encodeToString(current + record)
        }
    }

    /** Perfil del usuario (peso, altura, género) para el cálculo de calorías. */
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
