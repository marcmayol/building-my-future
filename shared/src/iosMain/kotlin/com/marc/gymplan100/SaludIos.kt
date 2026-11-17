package com.marc.gymplan100

import com.marc.gymplan100.plataforma.Salud
import com.marc.gymplan100.plataforma.localeApp
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDate
import platform.Foundation.NSSortDescriptor
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.languageCode
import platform.HealthKit.HKAuthorizationStatusSharingAuthorized
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKMetricPrefixKilo
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKQuantity
import platform.HealthKit.HKQuantitySample
import platform.HealthKit.HKQuantityType
import platform.HealthKit.HKQuantityTypeIdentifierBodyMass
import platform.HealthKit.HKSampleQuery
import platform.HealthKit.HKSampleSortIdentifierEndDate
import platform.HealthKit.HKUnit
import platform.HealthKit.HKWorkout
import platform.HealthKit.HKWorkoutActivityTypeTraditionalStrengthTraining
import platform.HealthKit.gramUnitWithMetricPrefix
import platform.HealthKit.kilocalorieUnit
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

/**
 * HealthKit, el Health Connect del iPhone: guarda cada entreno terminado (fuerza, con su
 * duración y calorías) y lee el último peso para Ajustes. En los textos sale como «Salud».
 */
object SaludIos : Salud {

    private val store by lazy { HKHealthStore() }
    private val tipoEntreno get() = HKObjectType.workoutType()
    private val tipoPeso get() = HKQuantityType.quantityTypeForIdentifier(HKQuantityTypeIdentifierBodyMass)!!

    override val nombre: String
        get() = if (localeApp.languageCode == "es") "Salud" else "Apple Health"

    override val disponible: Boolean get() = HKHealthStore.isHealthDataAvailable()
    override val necesitaActualizar: Boolean = false

    // HealthKit solo dice si se puede escribir; si se puede leer no lo cuenta nunca (privacidad).
    override suspend fun tienePermisos(): Boolean =
        disponible && store.authorizationStatusForType(tipoEntreno) == HKAuthorizationStatusSharingAuthorized

    /** Abre la hoja del sistema para dar permiso; [alVolver] al cerrarla, en el hilo principal. */
    fun pedirPermisos(alVolver: () -> Unit) {
        if (!disponible) return alVolver()
        store.requestAuthorizationToShareTypes(setOf(tipoEntreno), readTypes = setOf(tipoPeso)) { _, _ ->
            dispatch_async(dispatch_get_main_queue()) { alVolver() }
        }
    }

    override suspend fun escribirEntreno(titulo: String, notas: String, inicioMs: Long, finMs: Long, kcal: Double?) {
        if (!tienePermisos()) return
        val inicio = NSDate.dateWithTimeIntervalSince1970(inicioMs / 1000.0)
        val fin = NSDate.dateWithTimeIntervalSince1970(finMs / 1000.0)
        val energia = kcal?.takeIf { it > 0 }?.let { HKQuantity.quantityWithUnit(HKUnit.kilocalorieUnit(), it) }
        @Suppress("DEPRECATION") // HKWorkoutBuilder pide una sesión en marcha; aquí el entreno ya terminó.
        val entreno = HKWorkout.workoutWithActivityType(
            HKWorkoutActivityTypeTraditionalStrengthTraining,
            startDate = inicio,
            endDate = fin,
            duration = (finMs - inicioMs) / 1000.0,
            totalEnergyBurned = energia,
            totalDistance = null,
            metadata = null
        )
        suspendCancellableCoroutine { c -> store.saveObject(entreno) { _, _ -> c.resume(Unit) } }
    }

    override suspend fun ultimoPesoKg(): Double? {
        if (!disponible) return null
        return suspendCancellableCoroutine { c ->
            val consulta = HKSampleQuery(
                sampleType = tipoPeso,
                predicate = null,
                limit = 1u,
                sortDescriptors = listOf(NSSortDescriptor(key = HKSampleSortIdentifierEndDate, ascending = false))
            ) { _, resultados, _ ->
                val muestra = resultados?.firstOrNull() as? HKQuantitySample
                c.resume(muestra?.quantity?.doubleValueForUnit(HKUnit.gramUnitWithMetricPrefix(HKMetricPrefixKilo)))
            }
            store.executeQuery(consulta)
        }
    }
}
