# Prompt para Claude Design — Las cuatro pantallas nuevas

> Copia todo lo que hay debajo de la línea y pégalo en Claude Design, adjuntando además
> `docs/handoff-diseno.md` (el sistema visual ya aplicado) y capturas de las pantallas
> nuevas si las tienes a mano.

---

Hola. Trabajo en **Building My Future**, una app Android de entrenamiento de gimnasio que ya
tiene tu rediseño aplicado (el paquete que enviaste con la tipografía Archivo, la paleta
ámbar→naranja→magenta, los tokens de estado y las 23 pantallas). Adjunto `handoff-diseno.md`
con el sistema tal y como quedó implementado.

La app ha crecido y necesito que diseñes **cuatro pantallas nuevas** que ahora mismo existen pero
están sin vestir: las he montado con los tokens del sistema para que funcionen, pero les falta
tu criterio.

## Qué ha cambiado en la app

Antes había **un solo plan**: el reto de 100 días. Ahora hay **doce planes** que vienen con la
app (Arranque, el reto de 100 días, Hipertrofia, Fuerza Máxima, Mantenimiento, Calistenia,
Cardio y cinco bloques extra: piernas, brazos, core, postura y movilidad), más los que el
usuario se importe o se cree. Cada plan guarda su progreso por separado.

Eso abre dos momentos que antes no existían: **el principio** (¿qué plan hago?) y **el final**
(he terminado el plan, ¿y ahora qué?). Y hay un tercero que ya existía y estaba cojo: al acabar
un **entrenamiento libre** solo se guardaba el tiempo, así que ahora se puede apuntar qué se ha
hecho.

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
- `¿Y AHORA QUÉ?` con **cinco** salidas, que hoy son cinco botones apilados y ahí es donde
  más falta haces:
  - Tarjeta destacada: **el plan que viene después**, con su motivo ("ya entrenas con
    constancia, 3 días por semana, sin material: el volumen está repartido para crecer") y
    botón **Seguir con este**
  - **Empezarlo de cero** — repetir el plan entero
  - **Repetir solo «Consolidación»** — solo la última fase, con la explicación debajo: "La
    última fase son 20 días: repetirla mantiene el nivel al que has llegado sin volver al
    principio". Solo aparece si el plan tiene más de una fase.
  - **Empezar otro · ayúdame a elegir** — relanza el asistente de la pantalla 1
  - **Ver todos los planes** · **Ahora no, déjalo así**

  Las tres primeras son variantes de "sigo entrenando" y las dos últimas son salidas. Hoy
  pesan todas igual y la pantalla parece un menú de opciones. Ese es el problema a resolver.

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

## Pantalla 3 · El plan terminado, fuera de su pantalla

Terminar un plan no se acaba en la pantalla de cierre: deja rastro en dos sitios más, y los
dos están sin resolver.

**En la portada.** Mientras hay plan en marcha, el héroe dice "HOY TE TOCA · Día 9" y lleva a
empezarlo. Con el plan acabado eso empujaba a repetir el último día sin querer, así que ahora
sale un héroe distinto: versalita `PLAN TERMINADO`, el nombre del plan, "Los 12 días, hechos.
Elige qué sigue cuando quieras" y un botón **Elegir el siguiente**. Lo he montado con el
degradado de marca, igual que el héroe de "entreno en curso", y **ahora mismo compiten**: son
dos estados muy distintos con el mismo tratamiento visual. Además, debajo sigue la barra de
progreso al 100 %, que repite lo que ya dice el héroe.

**En la lista de planes.** Cada plan es una tarjeta con nombre, descripción, "12 días · 1 fase
· 12 completados" y "De serie en la app". Un plan **terminado** no se distingue de uno a
medias: debería decir algo como "Terminado el 14 de agosto" y, si se ha repetido, "2ª vuelta
en curso". Los datos existen (fecha de fin y número de vueltas), solo falta el diseño.

Los tres estados que conviven en esa lista: **activo**, **terminado** y **sin empezar**.

---

## Pantalla 4 · Apuntar lo hecho al terminar un entreno libre

**Contexto:** además del entrenamiento guiado (serie a serie, con su cronómetro), la app tiene
un **entrenamiento libre**: un cronómetro que corre mientras entrenas a tu aire y que cuenta
como día del plan. Hasta ahora solo guardaba el tiempo y lo demás se perdía. Al pulsar
**Finalizar** aparece ahora esta pantalla para apuntarlo.

**Cuándo sale:** justo al terminar, antes de guardar. Es el último paso de un entreno, con la
persona cansada y con el móvil en la mano.

**Contenido real:**
- Versalita: `ENTRENO TERMINADO`
- Titular: **¿Qué has hecho?**
- Cuerpo: "Apunta lo que quieras y déjate el resto: el tiempo se guarda igual. Los pesos que
  pongas se quedan como los últimos de cada ejercicio."
- **Una fila por ejercicio**, precargadas con las del día del plan. Cada fila tiene:
  - un campo **Ejercicio** que es a la vez desplegable y escribible: se elige de la lista
    (primero los del día, luego el resto del plan) **o se escribe uno que no esté**, porque
    entrenando libre es normal salirse del guion
  - una **X** para quitar la fila
  - **Peso (kg)** y **Reps**
- Botón **Añadir otro ejercicio**
- Fijos abajo: **Guardar y terminar** y **Terminar sin apuntar nada**

**Estados a resolver:**
- Con los ejercicios del día precargados (el normal, hoy salen cinco).
- Sin plan detrás o día sin ejercicios: una sola fila en blanco.
- Desplegable abierto con las sugerencias.
- **Con el teclado abierto**: es el caso peliagudo. Hoy los botones van fijos abajo con
  `imePadding` porque, con ocho filas y el teclado, buscarlos al final de la lista era un
  scroll a ciegas.

**Lo que me importa:**
1. Que **no se sienta como un formulario de aduana** después de entrenar. Si a alguien le pesa,
   dejará de usar el entreno libre, y ese es el que usa quien va a su bola.
2. Que **"Terminar sin apuntar nada" no parezca un castigo ni una trampa**. Es una salida
   legítima y tiene que verse tranquila.
3. La fila es densa: nombre + peso + reps + borrar, en un ancho de móvil. Hoy la he resuelto en
   dos alturas (nombre arriba, peso y reps debajo) y ocupa bastante; con cinco ejercicios la
   pantalla es larga.
4. Si crees que esto debería ser una hoja inferior en vez de una pantalla, o que las
   repeticiones sobran y basta el peso, dímelo.

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

1. El diseño de las cuatro cosas con sus estados: la **bienvenida** (puerta / preguntas /
   resultado), el **cierre de plan** (normal y sin datos), **el plan terminado fuera de su
   pantalla** (héroe de la portada y tarjeta en la lista de planes) y **apuntar lo hecho al
   terminar un entreno libre** (precargado / en blanco / desplegable / con teclado).
2. Resolver el amontonamiento de las cinco salidas del cierre: qué jerarquía tienen "seguir
   con otro plan", "empezar de cero" y "repetir la última fase".
3. Si hace falta, **tokens o componentes nuevos** — por ejemplo un "chip de respuesta" para el
   asistente, que ahora mismo me he inventado con un `Text` redondeado, o un distintivo de
   "plan terminado" reutilizable en la portada y en la lista.
4. Que me digas qué **NO** debería hacer: si ves que meto demasiada información en el cierre,
   que las cinco salidas son tres, o que las cuatro preguntas sobran, prefiero saberlo.
