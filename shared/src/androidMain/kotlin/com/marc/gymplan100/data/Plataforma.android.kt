package com.marc.gymplan100.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

actual typealias PlatformContext = Context

private class PreferenciasAndroid(private val p: SharedPreferences) : Preferencias {
    override fun getString(clave: String): String? = p.getString(clave, null)
    override fun getBoolean(clave: String, porDefecto: Boolean): Boolean = p.getBoolean(clave, porDefecto)
    override fun contains(clave: String): Boolean = p.contains(clave)

    override fun editar(sincrono: Boolean, cambios: Preferencias.Editor.() -> Unit) {
        val e = p.edit()
        object : Preferencias.Editor {
            override fun putString(clave: String, valor: String?) { e.putString(clave, valor) }
            override fun putBoolean(clave: String, valor: Boolean) { e.putBoolean(clave, valor) }
            override fun remove(clave: String) { e.remove(clave) }
            override fun clear() { e.clear() }
        }.cambios()
        if (sincrono) e.commit() else e.apply()
    }
}

actual fun PlatformContext.preferencias(nombre: String): Preferencias =
    PreferenciasAndroid(applicationContext.getSharedPreferences(nombre, Context.MODE_PRIVATE))

// El mismo nombre de siempre: es el fichero donde está el progreso de quien ya entrena.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gym_progress")

actual fun PlatformContext.almacenProgreso(): DataStore<Preferences> = applicationContext.dataStore
