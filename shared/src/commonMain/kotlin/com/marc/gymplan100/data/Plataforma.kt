package com.marc.gymplan100.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Lo que la lógica común necesita de cada plataforma para guardar cosas.
 *
 * En Android [PlatformContext] es el propio `Context`, así que todo lo que ya recibía un
 * contexto (PlanStore, ProgressRepository) se llama igual que antes. En el iPhone es un
 * objeto sin nada dentro: allí no hace falta contexto para abrir un almacén.
 */
expect abstract class PlatformContext

/** Ajustes pequeños con nombre (SharedPreferences en Android, NSUserDefaults en iOS). */
interface Preferencias {
    fun getString(clave: String): String?
    fun getBoolean(clave: String, porDefecto: Boolean): Boolean
    fun contains(clave: String): Boolean

    /** Cambios en bloque. [sincrono]: escribir ya en disco, antes de seguir (como commit). */
    fun editar(sincrono: Boolean = false, cambios: Editor.() -> Unit)

    interface Editor {
        fun putString(clave: String, valor: String?)
        fun putBoolean(clave: String, valor: Boolean)
        fun remove(clave: String)
        fun clear()
    }
}

expect fun PlatformContext.preferencias(nombre: String): Preferencias

/** El almacén del progreso, las sesiones y el perfil («gym_progress»). Uno por proceso. */
expect fun PlatformContext.almacenProgreso(): DataStore<Preferences>
