package com.marcm.actualizador

import java.io.File

/**
 * Higiene de la carpeta privada donde aterrizan los APKs (`cacheDir/actualizaciones`).
 *
 * Solo pueden sobrevivir dos archivos, los de la versión que se está instalando:
 * `<vc>.apk.part` (descarga en curso) y `<vc>.apk` (descargado y verificado).
 * Cualquier otra cosa es basura de un intento anterior — típicamente un `.part`
 * de una descarga que el sistema mató a media, o el APK de una versión ya
 * instalada — y se borra.
 */
internal object CacheDescargas {

    /** Nombres que se conservan para la versión [conservarVc] (null = ninguno). */
    fun vigentes(conservarVc: Int?): Set<String> =
        conservarVc?.let { setOf("$it.apk", "$it.apk.part") }.orEmpty()

    /** Borra de [dir] todo lo que no sea de [conservarVc]. Devuelve cuántos borró. */
    fun limpiar(dir: File, conservarVc: Int?): Int {
        val conservados = vigentes(conservarVc)
        val archivos = dir.listFiles() ?: return 0
        return archivos.count { it.name !in conservados && it.delete() }
    }
}
