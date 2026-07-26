package com.marcm.actualizador

import android.content.Context

/**
 * Preferencias persistentes del módulo: el ajuste "buscar actualizaciones"
 * (activado por defecto), si ya se pidió el permiso de instalación una vez, y la
 * última versión disponible detectada (para pintar el banner al abrir aunque la
 * detección la hiciera el worker en segundo plano).
 */
class PrefsActualizador(context: Context) {

    private val sp = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    var buscarActivado: Boolean
        get() = sp.getBoolean(BUSCAR, true)
        set(v) = sp.edit().putBoolean(BUSCAR, v).apply()

    var permisoPedidoUnaVez: Boolean
        get() = sp.getBoolean(PERMISO_PEDIDO, false)
        set(v) = sp.edit().putBoolean(PERMISO_PEDIDO, v).apply()

    /** Última cadencia conocida del manifiesto (horas). 0 = aún sin conocer. */
    var checkHoras: Int
        get() = sp.getInt(CHECK_HORAS, 0)
        set(v) = sp.edit().putInt(CHECK_HORAS, v).apply()

    fun guardarDisponible(info: InfoActualizacion?) {
        val e = sp.edit()
        if (info == null) {
            e.remove(VC).remove(VN).remove(URL).remove(SHA).remove(NOTAS)
        } else {
            e.putInt(VC, info.versionCode)
                .putString(VN, info.versionName)
                .putString(URL, info.url)
                .putString(SHA, info.sha256)
                .putString(NOTAS, info.notas)
        }
        e.apply()
    }

    fun leerDisponible(): InfoActualizacion? {
        if (!sp.contains(VC)) return null
        return InfoActualizacion(
            versionCode = sp.getInt(VC, 0),
            versionName = sp.getString(VN, "").orEmpty(),
            url = sp.getString(URL, "").orEmpty(),
            sha256 = sp.getString(SHA, "").orEmpty(),
            notas = sp.getString(NOTAS, "").orEmpty(),
        )
    }

    companion object {
        private const val ARCHIVO = "actualizador"
        private const val BUSCAR = "buscar_activado"
        private const val PERMISO_PEDIDO = "permiso_pedido_una_vez"
        private const val CHECK_HORAS = "check_horas"
        private const val VC = "disp_version_code"
        private const val VN = "disp_version_name"
        private const val URL = "disp_url"
        private const val SHA = "disp_sha256"
        private const val NOTAS = "disp_notas"
    }
}
