package com.marc.gymplan100.data

/**
 * Ficha de ayuda de un ejercicio: cómo se hace, errores típicos y alternativas
 * cuando la máquina está ocupada o no existe en tu gimnasio.
 * Pensada para consultarse en mitad del entreno sin salir de la app.
 */
data class ExerciseGuide(
    /** Músculos principales que trabaja. */
    val muscles: String,
    /** Explicación breve de la técnica, paso a paso. */
    val howTo: String,
    /** Fallos comunes a evitar. */
    val mistakes: List<String>,
    /** Qué hacer si la máquina está ocupada o no la tienes. */
    val alternatives: List<String>,
    /** Términos de búsqueda en YouTube para ver el movimiento en vídeo/short. */
    val videoQuery: String
)

object ExerciseGuides {

    // --- Guías base (se reutilizan para las variantes del mismo movimiento) ---

    private val pressPecho = ExerciseGuide(
        muscles = "Pectoral, hombro frontal y tríceps.",
        howTo = "Siéntate con la espalda apoyada en el respaldo y los agarres a la altura del " +
            "pecho/axila. Empuja hacia delante hasta casi estirar los brazos (sin bloquear de " +
            "golpe los codos) y vuelve despacio controlando. Mantén los omóplatos juntos y los " +
            "pies firmes en el suelo.",
        mistakes = listOf(
            "Bajar/soltar el peso de golpe en vez de controlar la vuelta.",
            "Despegar la espalda del respaldo o arquear mucho la zona lumbar.",
            "Bloquear los codos de golpe al final del empuje."
        ),
        alternatives = listOf(
            "Pec deck o aperturas en máquina.",
            "Press con mancuernas en banco plano.",
            "Flexiones (si no hay máquina libre)."
        ),
        videoQuery = "press de pecho en máquina técnica"
    )

    private val pressInclinado = ExerciseGuide(
        muscles = "Parte alta del pectoral, hombro frontal y tríceps.",
        howTo = "Es un press de pecho con el banco/respaldo inclinado (unos 30-45°), para cargar " +
            "más la parte alta del pecho. Empuja hacia arriba y adelante hasta casi estirar los " +
            "brazos y baja controlando. Si en su lugar haces pec deck: junta los brazos al frente " +
            "describiendo un arco amplio, apretando el pecho, y abre despacio.",
        mistakes = listOf(
            "Inclinar demasiado el banco (se convierte en press de hombro).",
            "Sacar los codos del todo hacia atrás al bajar.",
            "Usar impulso en vez de controlar el peso."
        ),
        alternatives = listOf(
            "Pec deck (aperturas en máquina).",
            "Press inclinado con mancuernas.",
            "Press de pecho plano si no hay banco inclinable."
        ),
        videoQuery = "press inclinado con mancuernas técnica"
    )

    private val pecDeck = ExerciseGuide(
        muscles = "Pectoral (sobre todo la parte interna) y hombro frontal.",
        howTo = "Sentado, con los antebrazos o las manos en las almohadillas, junta los brazos al " +
            "frente describiendo un arco amplio, como si abrazaras a alguien. Aprieta el pecho un " +
            "instante y abre despacio resistiendo, sin pasar los codos muy por detrás de la línea " +
            "del cuerpo.",
        mistakes = listOf(
            "Abrir demasiado atrás (estira en exceso el hombro).",
            "Usar impulso o soltar el peso al abrir.",
            "Encoger los hombros hacia las orejas."
        ),
        alternatives = listOf(
            "Aperturas con mancuernas en banco.",
            "Press de pecho en máquina.",
            "Cruce de poleas (cable crossover)."
        ),
        videoQuery = "pec deck aperturas en máquina técnica"
    )

    private val jalon = ExerciseGuide(
        muscles = "Dorsal ancho (espalda), bíceps y parte alta de la espalda.",
        howTo = "Sentado y con los muslos sujetos bajo el rodillo, agarra la barra algo más ancho " +
            "que los hombros. Tira de la barra hacia la parte alta del pecho llevando los codos " +
            "hacia abajo y juntando los omóplatos. Sube controlando hasta estirar casi del todo. " +
            "Si toca 'agarre neutro', usa el maneral en V o triángulo (palmas enfrentadas): " +
            "trabaja igual la espalda con menos tensión en muñecas y hombros.",
        mistakes = listOf(
            "Echar mucho el cuerpo hacia atrás y tirar con impulso.",
            "Llevar la barra detrás de la nuca.",
            "Tirar solo con los brazos sin juntar los omóplatos."
        ),
        alternatives = listOf(
            "Dominadas (asistidas con goma o negativas si no llegas).",
            "Pullover con mancuerna tumbado en banco.",
            "Jalón en cualquier polea alta con maneral neutro o triángulo."
        ),
        videoQuery = "jalón al pecho en polea técnica"
    )

    private val remoSentado = ExerciseGuide(
        muscles = "Espalda media (romboides, trapecio), dorsal y bíceps.",
        howTo = "Sentado con el pecho apoyado (o pies firmes al frente y espalda recta), agarra los " +
            "manerales y tira llevando los codos hacia atrás, pegados al cuerpo, juntando los " +
            "omóplatos. Vuelve despacio estirando los brazos sin redondear la espalda. Empieza el " +
            "tirón juntando las escápulas, no solo con los brazos.",
        mistakes = listOf(
            "Balancear el tronco hacia atrás para mover el peso.",
            "Redondear la espalda al estirar los brazos.",
            "Encoger los hombros hacia las orejas."
        ),
        alternatives = listOf(
            "Remo con barra inclinado (bent-over row).",
            "Remo a un brazo con mancuerna apoyando rodilla en el banco.",
            "Remo en polea baja con triángulo."
        ),
        videoQuery = "remo sentado en polea técnica"
    )

    private val remoMaquina = ExerciseGuide(
        muscles = "Espalda media y dorsal, con bíceps de ayuda.",
        howTo = "Con el pecho contra la almohadilla, agarra los manerales y tira hacia atrás " +
            "llevando los codos cerca del cuerpo y apretando los omóplatos. Vuelve controlando. " +
            "Con mancuerna a un brazo: apoya rodilla y mano del mismo lado en el banco, espalda " +
            "recta, y sube la mancuerna hacia la cadera pegando el codo al costado.",
        mistakes = listOf(
            "Tirar con impulso del cuerpo en vez de la espalda.",
            "Abrir mucho los codos hacia los lados.",
            "No completar el recorrido (tirón corto)."
        ),
        alternatives = listOf(
            "Remo sentado en polea.",
            "Remo con barra inclinado.",
            "Remo a un brazo con mancuerna."
        ),
        videoQuery = "remo en máquina espalda técnica"
    )

    private val pressHombros = ExerciseGuide(
        muscles = "Hombro (deltoides), con tríceps y parte alta del pecho de ayuda.",
        howTo = "Sentado con la espalda apoyada, los agarres a la altura de los hombros/orejas. " +
            "Empuja hacia el techo hasta casi estirar los brazos (sin bloquear de golpe) y baja " +
            "controlando hasta la altura de las orejas. Mantén el core activado para no arquear la " +
            "zona lumbar.",
        mistakes = listOf(
            "Arquear mucho la espalda al empujar.",
            "Bajar poco recorrido (no llegar a la altura de las orejas).",
            "Empujar con impulso de piernas/tronco."
        ),
        alternatives = listOf(
            "Press militar sentado con mancuernas.",
            "Press de hombros en multiestación con poleas.",
            "Elevaciones laterales si no hay para press."
        ),
        videoQuery = "press de hombros en máquina técnica"
    )

    private val curlBiceps = ExerciseGuide(
        muscles = "Bíceps (parte delantera del brazo).",
        howTo = "De pie con una mancuerna en cada mano, brazos estirados a los lados y palmas al " +
            "frente. Flexiona los codos subiendo el peso hacia los hombros sin mover el codo de su " +
            "sitio (queda pegado al costado). Aprieta arriba y baja controlando. El 'curl martillo' " +
            "es igual pero con las palmas mirándose (agarre neutro), trabaja también el antebrazo.",
        mistakes = listOf(
            "Balancear el cuerpo para subir el peso.",
            "Mover el codo hacia delante en vez de mantenerlo fijo.",
            "Bajar soltando el peso sin control."
        ),
        alternatives = listOf(
            "Curl en polea baja con barra.",
            "Curl en banco predicador o máquina de curl.",
            "Curl con barra Z."
        ),
        videoQuery = "curl de bíceps con mancuernas técnica"
    )

    private val extTriceps = ExerciseGuide(
        muscles = "Tríceps (parte de atrás del brazo).",
        howTo = "De pie frente a la polea alta con cuerda o barra, codos pegados al cuerpo. Empuja " +
            "hacia abajo estirando los codos del todo y vuelve despacio sin despegar los codos del " +
            "costado. El movimiento solo ocurre en el codo; el resto del brazo queda quieto.",
        mistakes = listOf(
            "Despegar los codos del cuerpo y ayudarte con el hombro.",
            "Inclinarte sobre la polea para empujar con el peso del cuerpo.",
            "No estirar del todo el codo abajo."
        ),
        alternatives = listOf(
            "Press francés con mancuerna o barra Z.",
            "Extensión de tríceps por encima de la cabeza con mancuerna.",
            "Fondos en banco o en máquina."
        ),
        videoQuery = "extensión de tríceps en polea técnica"
    )

    private val fondosTriceps = ExerciseGuide(
        muscles = "Tríceps, con pecho y hombro de ayuda.",
        howTo = "En la máquina de fondos: empuja hacia abajo estirando los codos y sube controlando. " +
            "Como press francés: tumbado o sentado, baja la mancuerna/barra hacia la frente " +
            "flexionando solo los codos (que apuntan al techo) y estira de nuevo.",
        mistakes = listOf(
            "Abrir mucho los codos hacia los lados.",
            "Bajar demasiado rápido sin control.",
            "Usar un peso que te obligue a hacer trampa con el cuerpo."
        ),
        alternatives = listOf(
            "Extensión de tríceps en polea con cuerda.",
            "Fondos entre dos bancos.",
            "Press cerrado con mancuernas."
        ),
        videoQuery = "press francés tríceps técnica"
    )

    private val elevacionesLaterales = ExerciseGuide(
        muscles = "Parte media del hombro (deltoides lateral), la que da anchura.",
        howTo = "De pie, una mancuerna en cada mano a los lados, codos ligeramente flexionados y " +
            "fijos. Sube los brazos hacia los lados hasta la altura de los hombros formando una 'T'. " +
            "Imagina que vacías una jarra: el meñique sube un pelín más alto que el pulgar. Baja " +
            "despacio resistiendo; la bajada es donde más trabaja el músculo.",
        mistakes = listOf(
            "Usar demasiado peso y subir con balanceo del cuerpo.",
            "Encoger los hombros hacia las orejas (entra el trapecio).",
            "Subir por encima de la línea de los hombros."
        ),
        alternatives = listOf(
            "Elevaciones laterales en polea (un brazo).",
            "Elevaciones laterales en máquina específica.",
            "Hazlas sentado para más estabilidad."
        ),
        videoQuery = "elevaciones laterales hombro técnica"
    )

    private val facePull = ExerciseGuide(
        muscles = "Hombro posterior, trapecio y rotadores (salud del hombro).",
        howTo = "En polea alta con cuerda, agarra con las palmas hacia dentro y tira hacia tu cara " +
            "abriendo los codos a la altura de los hombros, llevando las manos hacia las orejas. " +
            "Aprieta los omóplatos atrás y vuelve despacio. Peso ligero y movimiento limpio.",
        mistakes = listOf(
            "Usar demasiado peso y tirar con todo el cuerpo.",
            "Bajar los codos (se convierte en remo).",
            "No abrir las manos al final."
        ),
        alternatives = listOf(
            "Pájaros (aperturas posteriores) con mancuernas inclinado.",
            "Face pull con goma elástica.",
            "Remo al cuello en polea."
        ),
        videoQuery = "face pull en polea técnica"
    )

    private val superserie = ExerciseGuide(
        muscles = "Bíceps y tríceps (los dos músculos opuestos del brazo).",
        howTo = "Una SUPERSERIE significa hacer dos ejercicios seguidos SIN descanso entre ellos, " +
            "y descansar solo al terminar los dos. Aquí: haces las repeticiones de curl de bíceps " +
            "e inmediatamente, sin parar, las de extensión de tríceps. Eso es una serie completa; " +
            "entonces descansas y repites. Mientras un músculo trabaja, el otro descansa, así que " +
            "ahorras tiempo y el brazo queda bien congestionado.",
        mistakes = listOf(
            "Descansar entre el curl y la extensión (rompe la superserie).",
            "Elegir un peso tan alto que no completes el segundo ejercicio.",
            "Mover los codos de su sitio en cualquiera de los dos."
        ),
        alternatives = listOf(
            "Haz los dos ejercicios por separado con su descanso si prefieres.",
            "Curl en polea + extensión en polea.",
            "Curl con mancuernas + fondos en banco."
        ),
        videoQuery = "qué es una superserie bíceps tríceps"
    )

    private val prensa = ExerciseGuide(
        muscles = "Cuádriceps, glúteo y femoral.",
        howTo = "Sentado en la prensa, pies a la anchura de los hombros en la plataforma. Baja el " +
            "peso flexionando las rodillas hasta unos 90° (sin que la zona lumbar se despegue del " +
            "respaldo) y empuja de vuelta sin bloquear de golpe las rodillas. Controla la bajada.",
        mistakes = listOf(
            "Bajar tanto que la cadera/lumbar se despega del asiento.",
            "Bloquear las rodillas de golpe arriba.",
            "Poner los pies demasiado bajos (carga excesiva en rodillas)."
        ),
        alternatives = listOf(
            "Sentadilla goblet con mancuerna.",
            "Sentadilla en máquina Smith o hack.",
            "Zancadas con mancuernas."
        ),
        videoQuery = "prensa de piernas técnica"
    )

    private val curlPiernas = ExerciseGuide(
        muscles = "Femoral (parte de atrás del muslo).",
        howTo = "En la máquina (tumbado o sentado), coloca el rodillo sobre los tobillos. Flexiona " +
            "las rodillas llevando el talón hacia el glúteo y vuelve despacio controlando. Mantén " +
            "la cadera pegada al banco.",
        mistakes = listOf(
            "Levantar la cadera para ayudarte.",
            "Hacer el recorrido corto.",
            "Bajar el peso de golpe."
        ),
        alternatives = listOf(
            "Curl femoral con mancuerna entre los pies (tumbado).",
            "Peso muerto rumano con mancuernas (técnica cuidada).",
            "Curl nórdico asistido."
        ),
        videoQuery = "curl femoral en máquina técnica"
    )

    private val extPiernas = ExerciseGuide(
        muscles = "Cuádriceps (parte delantera del muslo).",
        howTo = "Sentado, con el rodillo sobre los empeines, estira las rodillas hasta arriba " +
            "apretando el cuádriceps un instante. Baja despacio resistiendo, sin dejar caer el peso. " +
            "Espalda apoyada en el respaldo.",
        mistakes = listOf(
            "Soltar el peso de golpe en la bajada.",
            "Usar impulso levantando el cuerpo.",
            "Cargar tanto que solo hagas medio recorrido."
        ),
        alternatives = listOf(
            "Sentadilla goblet.",
            "Zancadas o sentadilla búlgara.",
            "Prensa de piernas con pies bajos."
        ),
        videoQuery = "extensión de cuádriceps en máquina técnica"
    )

    private val hipThrust = ExerciseGuide(
        muscles = "Glúteo (principal) y femoral.",
        howTo = "Apoya la parte alta de la espalda en un banco, con una barra o disco sobre la " +
            "cadera (usa almohadilla). Pies firmes en el suelo. Sube la cadera apretando los " +
            "glúteos hasta que el tronco quede paralelo al suelo y baja controlando. El puente de " +
            "glúteos es la versión en el suelo, sin banco.",
        mistakes = listOf(
            "Arquear la zona lumbar en vez de empujar con el glúteo.",
            "No subir del todo (recorrido corto).",
            "Apoyar el cuello en vez de la parte alta de la espalda."
        ),
        alternatives = listOf(
            "Puente de glúteos en el suelo con disco.",
            "Hip thrust en máquina específica.",
            "Patada de glúteo en polea."
        ),
        videoQuery = "hip thrust técnica glúteo"
    )

    private val gemelos = ExerciseGuide(
        muscles = "Gemelos (pantorrilla).",
        howTo = "De pie (o sentado en la máquina), con la punta de los pies en el escalón y los " +
            "talones libres. Sube de puntillas lo máximo posible apretando el gemelo y baja despacio " +
            "dejando que el talón pase por debajo del escalón para estirar bien. Recorrido completo.",
        mistakes = listOf(
            "Hacer rebotes rápidos sin control.",
            "Recorrido corto (no estirar abajo ni subir del todo).",
            "Doblar mucho las rodillas (en la versión de pie)."
        ),
        alternatives = listOf(
            "Elevación de gemelos de pie con mancuerna en la mano.",
            "Gemelos en prensa (empujando con la punta).",
            "A una pierna en un escalón."
        ),
        videoQuery = "elevación de gemelos técnica"
    )

    private val sentadillaCajon = ExerciseGuide(
        muscles = "Cuádriceps, glúteo y femoral.",
        howTo = "De pie de espaldas a un banco/cajón, pies a la anchura de los hombros. Baja la " +
            "cadera hacia atrás como para sentarte, roza el cajón con el glúteo (sin dejarte caer) " +
            "y vuelve a subir empujando con los talones. El cajón te marca la profundidad y te da " +
            "seguridad.",
        mistakes = listOf(
            "Dejarte caer de golpe sobre el cajón.",
            "Que las rodillas se vayan hacia dentro.",
            "Redondear la espalda al bajar."
        ),
        alternatives = listOf(
            "Sentadilla goblet con mancuerna.",
            "Prensa de piernas.",
            "Sentadilla libre sin cajón (si controlas la técnica)."
        ),
        videoQuery = "sentadilla a cajón técnica"
    )

    private val goblet = ExerciseGuide(
        muscles = "Cuádriceps, glúteo y core.",
        howTo = "Sujeta una mancuerna o pesa rusa con las dos manos pegada al pecho (como una copa, " +
            "de ahí 'goblet'). Pies algo más anchos que los hombros. Baja en sentadilla manteniendo " +
            "el pecho alto y la espalda recta, hasta que los muslos queden paralelos al suelo, y " +
            "sube empujando con los talones.",
        mistakes = listOf(
            "Redondear la espalda o caer hacia delante.",
            "Que las rodillas colapsen hacia dentro.",
            "Despegar los talones del suelo."
        ),
        alternatives = listOf(
            "Sentadilla en máquina (hack o Smith).",
            "Prensa de piernas.",
            "Sentadilla a cajón."
        ),
        videoQuery = "sentadilla goblet técnica"
    )

    private val plancha = ExerciseGuide(
        muscles = "Core (abdomen y zona lumbar), hombros.",
        howTo = "Apoya los antebrazos y las puntas de los pies en el suelo, codos bajo los hombros. " +
            "Mantén el cuerpo en línea recta de la cabeza a los talones, apretando abdomen y " +
            "glúteos. Aguanta el tiempo de la serie respirando con normalidad. No es por " +
            "repeticiones: es aguantar la postura.",
        mistakes = listOf(
            "Subir el culo o hundir la cadera.",
            "Aguantar la respiración.",
            "Dejar caer la cabeza o mirar al frente forzando el cuello."
        ),
        alternatives = listOf(
            "Plancha con rodillas apoyadas (más fácil).",
            "Plancha alta (sobre las manos).",
            "Dead bug o abdominales hipopresivos."
        ),
        videoQuery = "plancha abdominal técnica correcta"
    )

    private val planchaLateral = ExerciseGuide(
        muscles = "Core lateral (oblicuos) y estabilidad del hombro.",
        howTo = "Túmbate de lado con las piernas estiradas, una sobre otra, y apoya el antebrazo " +
            "en el suelo con el codo justo debajo del hombro. Eleva la cadera hasta que el cuerpo " +
            "forme una línea recta de los pies a la cabeza y aguanta el tiempo de la serie " +
            "respirando con normalidad. Al terminar, cambia de lado: el tiempo indicado es por lado.",
        mistakes = listOf(
            "Dejar caer la cadera hacia el suelo (rompe la línea del cuerpo).",
            "Apoyar el codo adelantado o atrasado en vez de justo bajo el hombro.",
            "Rotar el tronco hacia delante o hacia atrás en vez de quedarte de perfil."
        ),
        alternatives = listOf(
            "Plancha lateral con la rodilla de abajo apoyada (más fácil).",
            "Plancha frontal normal.",
            "Elevaciones de cadera de lado, apoyado en el antebrazo."
        ),
        videoQuery = "plancha lateral técnica correcta"
    )

    private val deadBug = ExerciseGuide(
        muscles = "Core profundo (abdomen y transverso) y control de la zona lumbar.",
        howTo = "Túmbate boca arriba con los brazos estirados hacia el techo y las rodillas " +
            "flexionadas a 90° (como un bicho patas arriba, de ahí el nombre). Pega la zona lumbar " +
            "al suelo y, sin perder ese contacto, baja despacio a la vez un brazo por detrás de la " +
            "cabeza y la pierna contraria estirándola hacia el suelo. Vuelve al centro y repite con " +
            "el lado opuesto. Es por repeticiones alternando lados; el control manda, no la prisa.",
        mistakes = listOf(
            "Arquear la zona lumbar: debe quedar pegada al suelo todo el rato.",
            "Ir rápido o con impulso en vez de movimiento lento y controlado.",
            "Aguantar la respiración (espira al estirar brazo y pierna)."
        ),
        alternatives = listOf(
            "Dead bug solo con las piernas (brazos quietos) si te cuesta coordinar.",
            "Bird dog (a cuatro patas, estirar brazo y pierna contraria).",
            "Plancha frontal si prefieres un isométrico."
        ),
        videoQuery = "dead bug ejercicio técnica abdomen"
    )

    private val circuitoCore = ExerciseGuide(
        muscles = "Todo el core: abdomen, oblicuos y lumbar.",
        howTo = "Es un circuito: haces los ejercicios indicados uno detrás de otro, con poco o " +
            "ningún descanso entre ellos, y al terminar la ronda descansas. Cada 'vuelta' es una " +
            "ronda completa del circuito. Cuida la técnica de cada ejercicio aunque vayas con algo " +
            "de fatiga; mejor parar que hacerlo mal.",
        mistakes = listOf(
            "Ir tan rápido que pierdes la técnica.",
            "Saltarte el descanso entre vueltas si lo necesitas.",
            "Arquear la lumbar en los ejercicios de suelo."
        ),
        alternatives = listOf(
            "Sustituye cualquier ejercicio del circuito por una plancha.",
            "Reduce el número de vueltas si vienes cansado.",
            "Cámbialo por 3 series de plancha + dead bug."
        ),
        videoQuery = "circuito core abdominales rutina"
    )

    private val finisher = ExerciseGuide(
        muscles = "Pectoral, hombro y tríceps (remate final).",
        howTo = "Un 'finisher' es un remate al final del entreno para apurar el músculo. Aquí: una " +
            "sola serie de press de pecho con poco peso, haciendo todas las repeticiones que " +
            "puedas hasta el fallo técnico (cuando no puedes hacer otra con buena forma). Baja el " +
            "peso respecto a tus series normales.",
        mistakes = listOf(
            "Usar peso alto: el finisher es ligero y a repeticiones altas.",
            "Perder la técnica en las últimas repeticiones.",
            "Hacerlo si vienes muy cansado y compromete la forma."
        ),
        alternatives = listOf(
            "Flexiones al fallo.",
            "Pec deck ligero al fallo.",
            "Sáltalo si vienes sin energía."
        ),
        videoQuery = "press de pecho al fallo finisher"
    )

    // --- Mapa nombre del plan -> guía ---

    private val map: Map<String, ExerciseGuide> = mapOf(
        "Press de pecho en máquina" to pressPecho,
        "Press de pecho" to pressPecho,
        "Press inclinado o pec deck" to pressInclinado,
        "Press inclinado" to pressInclinado,
        "Pec deck (aperturas en máquina)" to pecDeck,
        "Jalón al pecho (polea)" to jalon,
        "Jalón al pecho" to jalon,
        "Jalón al pecho agarre neutro" to jalon,
        "Remo sentado en máquina" to remoSentado,
        "Remo sentado" to remoSentado,
        "Remo en máquina" to remoMaquina,
        "Remo en máquina o con mancuerna" to remoMaquina,
        "Press de hombros en máquina" to pressHombros,
        "Press de hombros" to pressHombros,
        "Curl de bíceps con mancuernas" to curlBiceps,
        "Curl de bíceps + curl martillo" to curlBiceps,
        "Curl martillo" to curlBiceps,
        "Curl de bíceps" to curlBiceps,
        "Extensión de tríceps en polea" to extTriceps,
        "Fondos en máquina o press francés" to fondosTriceps,
        "Elevaciones laterales" to elevacionesLaterales,
        "Face pull en polea" to facePull,
        "Face pull" to facePull,
        "Superserie curl + extensión de tríceps" to superserie,
        "Superserie curl + extensión" to superserie,
        "Prensa de piernas" to prensa,
        "Curl de piernas (máquina)" to curlPiernas,
        "Curl de piernas" to curlPiernas,
        "Extensión de piernas" to extPiernas,
        "Hip thrust o puente de glúteos" to hipThrust,
        "Hip thrust" to hipThrust,
        "Elevación de gemelos" to gemelos,
        "Sentadilla a banco/cajón" to sentadillaCajon,
        "Sentadilla goblet con mancuerna" to goblet,
        "Sentadilla goblet o en máquina" to goblet,
        "Sentadilla goblet" to goblet,
        "Plancha" to plancha,
        "Plancha lateral + dead bug" to planchaLateral,
        "Plancha lateral" to planchaLateral,
        "Dead bug" to deadBug,
        "Circuito core (plancha, rodillas, plancha lateral)" to circuitoCore,
        "Circuito core (plancha, rueda, elevación piernas)" to circuitoCore,
        "Circuito core (plancha, plancha lateral, dead bug)" to circuitoCore,
        "Finisher: press de pecho ligero al fallo" to finisher,
    )

    fun forName(name: String): ExerciseGuide? = map[name]
}
