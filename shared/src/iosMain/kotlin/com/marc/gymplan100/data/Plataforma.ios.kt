package com.marc.gymplan100.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

actual abstract class PlatformContext

/** En el iPhone no hay contexto que pasar: todos usan este. */
object IosContext : PlatformContext()

private class PreferenciasIos(private val d: NSUserDefaults) : Preferencias {
    override fun getString(clave: String): String? = d.stringForKey(clave)
    override fun getBoolean(clave: String, porDefecto: Boolean): Boolean =
        if (d.objectForKey(clave) == null) porDefecto else d.boolForKey(clave)
    override fun contains(clave: String): Boolean = d.objectForKey(clave) != null

    override fun editar(sincrono: Boolean, cambios: Preferencias.Editor.() -> Unit) {
        object : Preferencias.Editor {
            override fun putString(clave: String, valor: String?) {
                if (valor == null) d.removeObjectForKey(clave) else d.setObject(valor, clave)
            }
            override fun putBoolean(clave: String, valor: Boolean) { d.setBool(valor, clave) }
            override fun remove(clave: String) { d.removeObjectForKey(clave) }
            override fun clear() {
                d.dictionaryRepresentation().keys.forEach { d.removeObjectForKey(it as String) }
            }
        }.cambios()
        if (sincrono) d.synchronize()
    }
}

private val abiertas = mutableMapOf<String, Preferencias>()

actual fun PlatformContext.preferencias(nombre: String): Preferencias =
    abiertas.getOrPut(nombre) { PreferenciasIos(NSUserDefaults(suiteName = nombre)) }

@OptIn(ExperimentalForeignApi::class)
private val progreso: DataStore<Preferences> by lazy {
    val docs: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { (requireNotNull(docs).path + "/gym_progress.preferences_pb").toPath() }
    )
}

actual fun PlatformContext.almacenProgreso(): DataStore<Preferences> = progreso
