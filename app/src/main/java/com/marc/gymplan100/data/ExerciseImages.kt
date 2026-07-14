package com.marc.gymplan100.data

import androidx.annotation.DrawableRes
import com.marc.gymplan100.R

/**
 * Imagen de referencia por ejercicio (para identificar la máquina y el movimiento).
 * Clave: nombre exacto del ejercicio en el plan.
 * La del tríceps la aportó el usuario; el resto son de wger (licencia libre) y pueden
 * ser aproximadas. La plancha lateral es de Wikimedia Commons (everkinetic, CC BY-SA 3.0)
 * y la del dead bug es un pictograma vectorial propio (no había imagen libre).
 * Los ejercicios sin imagen disponible simplemente no muestran nada.
 */
object ExerciseImages {

    private val map: Map<String, Int> = mapOf(
        "Press de pecho en máquina" to R.drawable.ex_press_pecho_maquina,
        "Press de pecho" to R.drawable.ex_press_pecho,
        "Press inclinado o pec deck" to R.drawable.ex_press_inclinado_o,
        "Press inclinado" to R.drawable.ex_press_inclinado,
        "Pec deck (aperturas en máquina)" to R.drawable.ex_pec_deck,
        "Remo sentado en máquina" to R.drawable.ex_remo_sentado,
        "Remo sentado" to R.drawable.ex_remo_sentado2,
        "Remo en máquina" to R.drawable.ex_remo_maquina,
        "Remo en máquina o con mancuerna" to R.drawable.ex_remo_mancuerna,
        "Curl de bíceps con mancuernas" to R.drawable.ex_curl_biceps_mancuernas,
        "Curl de bíceps" to R.drawable.ex_curl_biceps,
        "Curl de bíceps + curl martillo" to R.drawable.ex_curl_martillo,
        "Curl martillo" to R.drawable.ex_curl_martillo,
        "Extensión de tríceps en polea" to R.drawable.ex_extension_triceps_polea,
        "Fondos en máquina o press francés" to R.drawable.ex_fondos_triceps,
        "Elevaciones laterales" to R.drawable.ex_elevaciones_laterales,
        "Jalón al pecho (polea)" to R.drawable.ex_jalon_pecho,
        "Jalón al pecho" to R.drawable.ex_jalon_pecho,
        "Jalón al pecho agarre neutro" to R.drawable.ex_jalon_neutro,
        "Press de hombros en máquina" to R.drawable.ex_press_hombros_maquina,
        "Press de hombros" to R.drawable.ex_press_hombros,
        "Face pull en polea" to R.drawable.ex_face_pull_polea,
        "Face pull" to R.drawable.ex_face_pull,
        "Plancha" to R.drawable.ex_plancha,
        "Plancha lateral + dead bug" to R.drawable.ex_plancha_lateral,
        "Plancha lateral" to R.drawable.ex_plancha_lateral,
        "Dead bug" to R.drawable.ex_dead_bug,
        "Hip thrust o puente de glúteos" to R.drawable.ex_hip_thrust,
        "Hip thrust" to R.drawable.ex_hip_thrust,
        "Prensa de piernas" to R.drawable.ex_prensa_piernas,
        "Curl de piernas (máquina)" to R.drawable.ex_curl_piernas,
        "Curl de piernas" to R.drawable.ex_curl_piernas,
        "Extensión de piernas" to R.drawable.ex_extension_piernas,
        "Elevación de gemelos" to R.drawable.ex_gemelos,
        "Sentadilla a banco/cajón" to R.drawable.ex_sentadilla_cajon,
        "Sentadilla goblet con mancuerna" to R.drawable.ex_goblet_mancuerna,
        "Sentadilla goblet o en máquina" to R.drawable.ex_goblet_maquina,
        "Sentadilla goblet" to R.drawable.ex_goblet,
    )

    @DrawableRes
    fun forName(name: String): Int? = map[name]
}
