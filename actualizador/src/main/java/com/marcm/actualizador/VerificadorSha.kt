package com.marcm.actualizador

import java.io.File
import java.security.MessageDigest

/**
 * Verificación de integridad del APK descargado. Regla de oro: un APK cuyo
 * SHA-256 no coincide con el del manifiesto se BORRA y jamás se instala.
 */
object VerificadorSha {

    /** SHA-256 de un fichero, en hex minúsculas. */
    fun calcular(archivo: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        archivo.inputStream().use { entrada ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = entrada.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().aHexMinuscula()
    }

    /**
     * Comprueba que [archivo] tiene exactamente el hash [esperado]. Si NO coincide
     * (o no se puede leer), borra el archivo y devuelve false. Solo un true permite
     * seguir a la instalación.
     */
    fun verificarOBorrar(archivo: File, esperado: String): Boolean {
        val ok = try {
            calcular(archivo).equals(esperado.trim(), ignoreCase = true)
        } catch (e: Exception) {
            false
        }
        if (!ok) archivo.delete()
        return ok
    }
}
