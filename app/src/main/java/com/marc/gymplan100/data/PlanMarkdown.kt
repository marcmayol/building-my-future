package com.marc.gymplan100.data

/**
 * Lee un plan escrito en Markdown.
 *
 * El JSON es exacto pero incómodo de escribir a mano; en Markdown un plan se escribe (o se
 * pide a una IA) como se escribiría en una libreta:
 *
 * ```
 * # Mi plan de fuerza
 * Tres días por semana durante ocho semanas.
 *
 * ## Base (4 semanas)
 * Aprender el movimiento con peso cómodo.
 * Progresión: sube peso cuando las 10 salgan fáciles.
 *
 * ### Lunes — Empuje
 * Calentamiento: 6 min bici + movilidad de hombros
 * - Press de banca — 4 x 8
 * - Press de hombros — 3 x 10 (sin bloquear el codo)
 * - Plancha — 3 x 30 s
 * ```
 *
 * El parser es deliberadamente tolerante con la forma (los separadores, los acentos de los
 * títulos, el orden de las líneas sueltas) porque nadie escribe dos veces igual; una vez
 * convertido a [PlanDto] pasa por la MISMA validación que el JSON, que sí es estricta con lo
 * que rompería un entrenamiento.
 */
object PlanMarkdownParser {

    /** "## Base (4 semanas)", "## Fase 2: Carga - 6 semanas". */
    private val WEEKS = Regex("""(\d+)\s*semanas?""", RegexOption.IGNORE_CASE)

    /** Etiquetas de línea suelta dentro de un día o una fase. */
    private val WARMUP = Regex("""^(calentamiento|warm\s*-?up)\s*[:\-–—]\s*""", RegexOption.IGNORE_CASE)
    private val COOLDOWN = Regex(
        """^(estiramientos?|vuelta a la calma|cooldown|cool\s*-?down)\s*[:\-–—]\s*""",
        RegexOption.IGNORE_CASE
    )
    private val PROGRESSION = Regex("""^(progresi[oó]n|progression)\s*[:\-–—]\s*""", RegexOption.IGNORE_CASE)

    /** Viñetas de lista: "- ", "* ", "+ ", "1. ", "1) ". */
    private val BULLET = Regex("""^\s*(?:[-*+]|\d+[.)])\s+""")

    /** Un esquema al final de la línea cuando no hay separador: "Press de banca 4 x 8". */
    private val TRAILING_SCHEME = Regex(
        """\s+(\d+\s*[x×]\s*\d+.*|\d+\s*(?:vueltas?|series?|rondas?).*)$""",
        RegexOption.IGNORE_CASE
    )

    /** Quita el prefijo de encabezado y los adornos de negrita. */
    private fun cleanHeading(line: String, hashes: String): String =
        line.removePrefix(hashes).trim().trim('#').trim().replace("**", "").trim()

    /** "Fase 2: Carga" -> "Carga"; "Fase 3" -> "Fase 3". */
    private fun stripPhasePrefix(title: String): String {
        val m = Regex("""^fase\s*\d*\s*[:\-–—]\s*(.+)$""", RegexOption.IGNORE_CASE).find(title)
        return m?.groupValues?.get(1)?.trim() ?: title
    }

    /**
     * Parte "Lunes — Empuje" en día y título. Solo separa por guion largo o dos puntos: un
     * guion corto normal puede formar parte del propio nombre.
     */
    private fun splitDayTitle(heading: String): Pair<String, String> {
        val m = Regex("""^(.+?)\s*[:–—]\s*(.+)$""").find(heading)
            ?: Regex("""^(.+?)\s+-\s+(.+)$""").find(heading)
        return if (m != null) m.groupValues[1].trim() to m.groupValues[2].trim()
        else heading to ""
    }

    /**
     * Saca nombre, series y nota de una línea de ejercicio. Acepta cualquiera de los
     * separadores habituales y, si no hay ninguno, intenta reconocer el esquema al final.
     */
    private fun parseExercise(raw: String): EjercicioDto? {
        var line = BULLET.replace(raw, "").replace("**", "").trim().trimEnd('.')
        if (line.isEmpty()) return null

        // Nota entre paréntesis al final: "Press — 4 x 8 (sin bloquear el codo)".
        var note = ""
        val noteMatch = Regex("""\(([^()]*)\)\s*$""").find(line)
        if (noteMatch != null) {
            note = noteMatch.groupValues[1].trim()
            line = line.removeRange(noteMatch.range).trim()
        }

        // Se parte por el ÚLTIMO separador cuya parte derecha lleve algún número: así
        // "Curl 21 - 3 x 12" no se corta por el guion del nombre.
        val separators = Regex("""\s*[|:–—]\s*|\s+-\s+""")
        var name = line
        var scheme = ""
        for (m in separators.findAll(line).toList().asReversed()) {
            val right = line.substring(m.range.last + 1).trim()
            if (right.any { it.isDigit() }) {
                name = line.substring(0, m.range.first).trim()
                scheme = right
                break
            }
        }
        if (scheme.isEmpty()) {
            val trailing = TRAILING_SCHEME.find(line)
            if (trailing != null) {
                scheme = trailing.groupValues[1].trim()
                name = line.removeRange(trailing.range).trim()
            }
        }
        if (name.isEmpty()) return null
        // Sin esquema se devuelve igualmente: la validación común dirá qué ejercicio falta.
        return EjercicioDto(nombre = name, esquema = scheme, nota = note)
    }

    /** Convierte el texto en plan, o explica por qué no se puede. */
    fun parse(text: String, now: Long = System.currentTimeMillis()): PlanImport {
        if (text.isBlank()) return PlanImport.Error("El archivo está vacío.")

        var planName = ""
        val planDescription = StringBuilder()

        val phases = mutableListOf<FaseDto>()
        var phaseName = ""
        var phaseWeeks = 1
        val phaseDescription = StringBuilder()
        var phaseProgression = ""
        var phaseDays = mutableListOf<DiaDto>()

        var dayName = ""
        var dayTitle = ""
        var dayWarmup = ""
        var dayCooldown = ""
        var dayExercises = mutableListOf<EjercicioDto>()
        var inDay = false

        fun flushDay() {
            if (!inDay) return
            phaseDays.add(
                DiaDto(
                    dia = dayName,
                    titulo = dayTitle,
                    calentamiento = dayWarmup,
                    estiramientos = dayCooldown,
                    ejercicios = dayExercises.toList()
                )
            )
            dayName = ""; dayTitle = ""; dayWarmup = ""; dayCooldown = ""
            dayExercises = mutableListOf()
            inDay = false
        }

        fun flushPhase() {
            flushDay()
            if (phaseDays.isEmpty()) return
            phases.add(
                FaseDto(
                    nombre = phaseName,
                    semanas = phaseWeeks,
                    descripcion = phaseDescription.toString().trim(),
                    progresion = phaseProgression,
                    dias = phaseDays.toList()
                )
            )
            phaseName = ""; phaseWeeks = 1; phaseProgression = ""
            phaseDescription.clear()
            phaseDays = mutableListOf()
        }

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("```") || line.all { it == '-' || it == '=' }) continue

            when {
                line.startsWith("#### ") -> Unit // niveles más profundos: se ignoran

                line.startsWith("### ") -> {
                    flushDay()
                    val heading = cleanHeading(line, "###")
                    val (name, title) = splitDayTitle(heading)
                    dayName = name
                    dayTitle = title
                    inDay = true
                }

                line.startsWith("## ") -> {
                    flushPhase()
                    val heading = cleanHeading(line, "##")
                    phaseWeeks = WEEKS.find(heading)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    phaseName = stripPhasePrefix(
                        heading
                            .replace(Regex("""\(\s*\d+\s*semanas?\s*\)""", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("""[,\-–—]?\s*\d+\s*semanas?""", RegexOption.IGNORE_CASE), "")
                            .trim()
                            .trimEnd(':', '-', '–', '—')
                            .trim()
                    )
                }

                line.startsWith("# ") -> {
                    flushPhase()
                    planName = cleanHeading(line, "#")
                }

                WARMUP.containsMatchIn(line) && inDay -> dayWarmup = WARMUP.replace(line, "").trim()
                COOLDOWN.containsMatchIn(line) && inDay -> dayCooldown = COOLDOWN.replace(line, "").trim()
                PROGRESSION.containsMatchIn(line) -> phaseProgression = PROGRESSION.replace(line, "").trim()

                BULLET.containsMatchIn(line) -> {
                    // Las viñetas anteriores al primer día son texto suelto: no son ejercicios.
                    if (inDay) parseExercise(line)?.let { dayExercises.add(it) }
                }

                else -> {
                    // Texto libre: descripción del plan o de la fase, según dónde estemos.
                    when {
                        inDay -> Unit
                        phaseName.isNotEmpty() -> phaseDescription.appendLine(line)
                        planName.isNotEmpty() -> planDescription.appendLine(line)
                    }
                }
            }
        }
        flushPhase()

        if (planName.isBlank()) {
            return PlanImport.Error(
                "Falta el nombre del plan. Empieza el archivo con una línea \"# Nombre del plan\"."
            )
        }
        if (phases.isEmpty()) {
            return PlanImport.Error(
                "No se ha encontrado ningún día. Escribe cada día con \"### Lunes — Empuje\" y " +
                    "sus ejercicios en una lista con guiones."
            )
        }

        return PlanCodec.fromDto(
            PlanDto(
                nombre = planName,
                descripcion = planDescription.toString().trim(),
                fases = phases,
                origen = "markdown"
            ),
            now
        )
    }
}
