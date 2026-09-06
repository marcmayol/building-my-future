package com.marc.gymplan100.review

import android.content.Context

/**
 * Cuándo tiene sentido pedir una valoración, y cuándo no.
 *
 * Esta es la parte que decide, y está aquí —fuera de las dos variantes— por dos razones: se
 * puede probar sin Android, y así la regla es la misma escriba quien escriba el diálogo.
 *
 * La regla, entera: **después de entrenar, nunca antes**. Pedir la nota nada más abrir la app
 * es pedírsela a alguien que todavía no la ha usado, y esa nota siempre es peor. Se espera a
 * que haya [ENTRENOS_MINIMOS] sesiones terminadas —a la tercera ya sabes si la app te sirve—
 * y, si se pregunta, no se vuelve a preguntar en [DIAS_ENTRE_PETICIONES] días.
 *
 * Lo que se cuenta son entrenos **terminados**, no días abiertos: quien deja la sesión a medias
 * no ha tenido todavía la experiencia por la que se le pregunta.
 */
object MomentoDeValorar {

    /** Sesiones terminadas antes de abrir la boca. */
    const val ENTRENOS_MINIMOS = 3

    /** Si ya se preguntó una vez, esto es lo que tarda en volver a preguntarse. */
    const val DIAS_ENTRE_PETICIONES = 120

    private const val UN_DIA_MS = 24L * 60 * 60 * 1000

    private const val PREFS = "gym_valoracion"
    private const val KEY_ENTRENOS = "entrenos_terminados"
    private const val KEY_ULTIMA = "ultima_peticion"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * ¿Toca preguntar?
     *
     * @param entrenos sesiones terminadas desde que se instaló.
     * @param ultimaPeticion cuándo se preguntó por última vez, o 0 si nunca.
     */
    fun toca(entrenos: Int, ultimaPeticion: Long, ahora: Long): Boolean {
        if (entrenos < ENTRENOS_MINIMOS) return false
        if (ultimaPeticion <= 0L) return true
        // Un reloj que va hacia atrás (cambio de zona, fecha a mano) no debe convertirse en
        // "pregunta otra vez": si la última petición está en el futuro, se espera.
        val transcurrido = ahora - ultimaPeticion
        return transcurrido >= DIAS_ENTRE_PETICIONES * UN_DIA_MS
    }

    /** Suma un entreno terminado y devuelve cuántos van. */
    fun apuntaEntreno(context: Context): Int {
        val p = prefs(context)
        val n = p.getInt(KEY_ENTRENOS, 0) + 1
        p.edit().putInt(KEY_ENTRENOS, n).apply()
        return n
    }

    /** Los entrenos terminados que llevamos contados. */
    fun entrenos(context: Context): Int = prefs(context).getInt(KEY_ENTRENOS, 0)

    /** Cuándo se preguntó por última vez (0 = nunca). */
    fun ultimaPeticion(context: Context): Long = prefs(context).getLong(KEY_ULTIMA, 0L)

    /**
     * Deja constancia de que se ha preguntado.
     *
     * Se llama al **lanzar** el diálogo, no al recibir respuesta, porque Google no dice si el
     * usuario valoró ni si llegó a ver algo. Como no se puede saber, se cuenta el intento: es
     * la única forma de no insistir.
     */
    fun marcaPedida(context: Context, ahora: Long = System.currentTimeMillis()) {
        prefs(context).edit().putLong(KEY_ULTIMA, ahora).apply()
    }

    /** El atajo de siempre: apunta el entreno y dice si con este toca preguntar. */
    fun tocaTrasEntrenar(context: Context, ahora: Long = System.currentTimeMillis()): Boolean =
        toca(apuntaEntreno(context), ultimaPeticion(context), ahora)
}
