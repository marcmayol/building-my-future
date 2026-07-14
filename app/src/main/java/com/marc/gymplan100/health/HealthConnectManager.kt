package com.marc.gymplan100.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import java.time.Instant
import java.time.ZoneId

/**
 * Puente con Health Connect (la plataforma sobre la que se apoya Google Health).
 *
 * Escribimos: cada vez que se finaliza un entrenamiento insertamos una sesión de
 * ejercicio de tipo fuerza (duración + detalle en notas) y, junto a ella, las calorías
 * activas estimadas. Ese registro de calorías es imprescindible para que el coach y los
 * objetivos de Google Health cuenten el entreno; la sesión sola no aporta métrica.
 *
 * Leemos (solo si el usuario concede permiso): su peso y altura más recientes, para poder
 * rellenar el perfil de la app desde Google Health.
 */
class HealthConnectManager(private val context: Context) {

    /** Permiso mínimo para que un entreno aparezca como sesión en Google Health. */
    private val writeExercise = HealthPermission.getWritePermission(ExerciseSessionRecord::class)

    /** Permiso para escribir las calorías activas que el coach necesita contar. */
    private val writeCalories =
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class)

    /** Permisos de lectura para nutrir el perfil desde Google Health. */
    private val readWeight = HealthPermission.getReadPermission(WeightRecord::class)
    private val readHeight = HealthPermission.getReadPermission(HeightRecord::class)

    /** Conjunto completo que se solicita al conectar con Google Health. */
    val permissions: Set<String> =
        setOf(writeExercise, writeCalories, readWeight, readHeight)

    /** Health Connect está instalado y disponible en este dispositivo. */
    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    /** Health Connect existe pero hace falta actualizar el proveedor (Android 13 e inferiores). */
    val needsProviderUpdate: Boolean
        get() = HealthConnectClient.getSdkStatus(context) ==
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    private suspend fun granted(): Set<String> {
        if (!isAvailable) return emptySet()
        return client.permissionController.getGrantedPermissions()
    }

    /**
     * True si el usuario nos concedió al menos el permiso de escritura de ejercicio, que es
     * el que consideramos "conectado con Google Health".
     */
    suspend fun hasAllPermissions(): Boolean = granted().contains(writeExercise)

    /** True si podemos leer peso/altura del usuario. */
    suspend fun canReadProfile(): Boolean = granted().containsAll(setOf(readWeight, readHeight))

    /**
     * Inserta la sesión de ejercicio (tipo fuerza) y, si tenemos permiso y una estimación,
     * también las calorías activas del mismo intervalo. Cada registro se escribe solo si su
     * permiso está concedido, así una conexión antigua (solo ejercicio) sigue funcionando.
     */
    suspend fun writeWorkout(
        title: String,
        notes: String,
        startMillis: Long,
        endMillis: Long,
        activeKcal: Double?
    ) {
        val perms = granted()
        if (writeExercise !in perms) return
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(startMillis)
        // Health Connect exige fin estrictamente posterior al inicio.
        val end = Instant.ofEpochMilli(endMillis.coerceAtLeast(startMillis + 1000L))
        val startOffset = zone.rules.getOffset(start)
        val endOffset = zone.rules.getOffset(end)

        val records = mutableListOf<androidx.health.connect.client.records.Record>(
            ExerciseSessionRecord(
                metadata = Metadata.manualEntry(),
                startTime = start,
                startZoneOffset = startOffset,
                endTime = end,
                endZoneOffset = endOffset,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                title = title,
                notes = notes.ifBlank { null }
            )
        )
        if (writeCalories in perms && activeKcal != null && activeKcal > 0) {
            records += ActiveCaloriesBurnedRecord(
                metadata = Metadata.manualEntry(),
                startTime = start,
                startZoneOffset = startOffset,
                endTime = end,
                endZoneOffset = endOffset,
                energy = Energy.kilocalories(activeKcal)
            )
        }
        client.insertRecords(records)
    }

    /** Peso más reciente registrado en Google Health, en kg (null si no hay o falta permiso). */
    suspend fun latestWeightKg(): Double? {
        if (readWeight !in granted()) return null
        return runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now())
                )
            ).records.maxByOrNull { it.time }?.weight?.inKilograms
        }.getOrNull()
    }

    /** Altura más reciente registrada en Google Health, en cm (null si no hay o falta permiso). */
    suspend fun latestHeightCm(): Double? {
        if (readHeight !in granted()) return null
        return runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = HeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.before(Instant.now())
                )
            ).records.maxByOrNull { it.time }?.height?.inMeters?.let { it * 100.0 }
        }.getOrNull()
    }
}
