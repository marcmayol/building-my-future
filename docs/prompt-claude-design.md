# Prompt para Claude Design — Bienvenida y cierre de plan

> Copia todo lo que hay debajo de la línea y pégalo en Claude Design, adjuntando además
> `docs/handoff-diseno.md` (el sistema visual ya aplicado) y capturas de las dos pantallas
> nuevas si las tienes a mano.

---

Hola. Trabajo en **Building My Future**, una app Android de entrenamiento de gimnasio que ya
tiene tu rediseño aplicado (el paquete que enviaste con la tipografía Archivo, la paleta
ámbar→naranja→magenta, los tokens de estado y las 23 pantallas). Adjunto `handoff-diseno.md`
con el sistema tal y como quedó implementado.

La app ha crecido y necesito que diseñes **dos pantallas nuevas** que ahora mismo existen pero
están sin vestir: las he montado con los tokens del sistema para que funcionen, pero les falta
tu criterio.

## Qué ha cambiado en la app

Antes había **un solo plan**: el reto de 100 días. Ahora hay **doce planes** que vienen con la
app (Arranque, el reto de 100 días, Hipertrofia, Fuerza Máxima, Mantenimiento, Calistenia,
Cardio y cinco bloques extra: piernas, brazos, core, postura y movilidad), más los que el
usuario se importe o se cree. Cada plan guarda su progreso por separado.

Eso abre dos momentos que antes no existían: **el principio** (¿qué plan hago?) y **el final**
(he terminado el plan, ¿y ahora qué?).

---

## Pantalla 1 · Bienvenida (primera apertura)

**Cuándo sale:** solo la primerísima vez, cuando no se ha elegido plan nunca y no hay nada
entrenado. Quien ya venía usando la app no la ve jamás.

**Qué hace:** ayuda a elegir plan con cuatro preguntas, o deja ir directo al catálogo o al
editor. El asistente es un atajo, nunca un peaje: las tres salidas están siempre a la vista.

**Contenido real, en dos estados:**

*Estado A — la puerta:*
- Versalita: `BUILDING MY FUTURE`
- Titular: **Vamos a elegir tu plan**
- Cuerpo: "Hay doce planes dentro. Puedo recomendarte uno en cuatro preguntas, o los miras tú
  mismo."
- Botón principal: **Ayúdame a elegir** · Secundario: **Ver los planes** · Terciario: **Crear el mío**

*Estado B — las preguntas (las cuatro juntas, se contestan tocando):*
- Titular: **Cuéntame cuatro cosas**
- Cuerpo: "No se guarda nada de esto: solo sirve para proponerte un plan."
- `¿Qué buscas?` — Perder grasa · Ganar músculo · Levantar más · Mantenerme · Moverme mejor
- `¿Cómo estás ahora?` — Parado hace tiempo · De vez en cuando · Entreno regular
- `¿Cuántos días por semana?` — 2 · 3 · 4 · 5
- `¿Dónde entrenas?` — En el gimnasio · En casa, sin material

*Estado C — el resultado (aparece al contestar las cuatro):*
- Versalita: `TE PROPONGO`
- Tarjeta destacada con el plan recomendado: nombre, "12 días · 3 por semana", **el motivo
  escrito** ("vienes de estar parado, 3 días por semana: esto es para coger el hábito sin
  machacarte") y botón **Empezar con este**.
- Debajo, dos alternativas con el mismo formato pero sin destacar.
- Una línea: "Y si te sobra un día, encima de cualquiera de estos puedes montar Bloque Postura,
  Movilidad."
- Cierre: **Ver los doce planes** · **Mejor me creo el mío**
- Caso raro pero real: si nada encaja, versalita `NO TENGO NADA QUE ENCAJE` y un texto que
  manda al catálogo o al editor.

**Lo que me importa de esta pantalla:**
1. Que **no parezca un formulario**. Son cuatro preguntas sobre el cuerpo de alguien que
   quizá lleva años parado; el tono tiene que ser de conversación, no de alta médica.
2. Que la frase de "no se guarda nada" **se vea y se crea**. Es verdad: las respuestas se usan
   para calcular y se tiran. Merece un tratamiento visual honesto, sin letra pequeña.
3. Que el motivo de la recomendación sea protagonista. La gracia no es que la app acierte, es
   que la persona pueda decir "eso no es lo mío" con criterio.
4. Decidir si las cuatro preguntas van **en una pantalla scrollable** (como ahora) o **en pasos**.
   Yo lo he hecho de una porque son cortas, pero si crees que por pasos se contesta mejor,
   dímelo y lo cambio.

---

## Pantalla 2 · Cierre de plan

**Cuándo sale:** al marcar el último día del plan, después de la celebración de ese día. Una
sola vez.

**Contenido real:**
- Versalita: `PLAN TERMINADO` (o `VUELTA 2 TERMINADA` si es una repetición)
- Titular: el nombre del plan (**Movilidad**)
- Bloque con el degradado de marca: **12 de 12 días**, y debajo "en 4 semanas · empezaste el
  14 de agosto"
- Bloque de datos: Tiempo entrenado · Series hechas · Mejor racha
- `LO QUE HA SUBIDO`: hasta cuatro ejercicios con "Prensa de piernas 60 → 85 kg"
- `¿Y AHORA QUÉ?` con tres salidas:
  - Tarjeta destacada: **el plan que viene después**, con su motivo ("ya entrenas con
    constancia, 3 días por semana, sin material: el volumen está repartido para crecer") y
    botón **Seguir con este**
  - **Otra vuelta a Movilidad** (para los planes que están pensados para repetirse)
  - **Ver todos los planes** · **Ahora no, déjalo así**

**Estado vacío importante:** si alguien completó el plan marcando los días a mano, sin usar el
entrenamiento guiado, no hay tiempo, ni series, ni pesos. En ese caso sale este texto:
"Marcaste los días a mano, sin usar el entrenamiento guiado, así que de este plan no queda
registro de tiempo, series ni pesos." **No quiero que ese caso parezca un error**: es una forma
legítima de usar la app.

**Lo que me importa de esta pantalla:**
1. Que se sienta un **cierre**, no un cuadro de mandos. Terminar 36 sesiones es un momento.
2. Que los números respiren: hoy están apilados y compiten entre ellos.
3. Que **"¿y ahora qué?" no eclipse al resumen**, ni al revés. Son dos mitades: mirar atrás y
   mirar adelante.
4. El caso de "sin datos" no puede quedar como un hueco triste.

---

## Restricciones técnicas (importantes)

- **Jetpack Compose + Material 3**, tema propio ya montado: `Space` (base 4), `Touch`
  (alturas mínimas de toque), `AppShapes` (8/16/24), `LocalAppColors` (tokens de estado:
  `work`, `rest`, `warmup`, `streak`, `brandGradient`…) y `LocalAppTextStyles.tabular` para
  cifras.
- **Tipografía Archivo variable**, ya integrada, con cifras tabulares.
- **Sin material-icons-extended**: solo iconos del set core o vectores propios.
- Tiene que aguantar **texto del sistema al 150 %** y funcionar en **claro y oscuro**. En este
  proyecto eso ha roto cosas de verdad (botones partidos, chips que se cortan), así que si un
  diseño depende de que un texto quepa en una línea, dilo.
- Nada de fuentes ni imágenes externas: todo local.

## Qué te pido

1. El diseño de las dos pantallas con sus estados (bienvenida A/B/C y cierre normal/sin datos).
2. Si hace falta, **tokens o componentes nuevos** — por ejemplo un "chip de respuesta" para el
   asistente, que ahora mismo me he inventado con un `Text` redondeado.
3. Que me digas qué **NO** debería hacer: si ves que meto demasiada información en el cierre,
   o que las cuatro preguntas sobran, prefiero saberlo.
