# Building My Future · handoff de diseño

Documento para trabajar el diseño de la app sin leer el código: qué hace, para quién, cómo está
hecha y qué restricciones son innegociables. Estado: **v1.8 + planes propios + rediseño visual
aplicado** (agosto 2026).

> **El rediseño de agosto de 2026 ya está en el código.** Las secciones de identidad visual y de
> pantallas describen lo que hay hoy. Lo que sigue pendiente está en §11.

---

## 1. Qué es

Una app Android para **seguir un plan de gimnasio día a día y entrenar con ella en la mano**.
No es un registro que se rellena después: acompaña la sesión en directo (calentamiento, series,
descansos con aviso, peso de cada serie) y guarda el resultado. Tiene app de reloj (Wear OS)
para controlar el entreno sin sacar el móvil.

Nació como un reto cerrado de 100 días. Desde ahora **el plan es intercambiable**: el de 100
días viene de serie, pero el usuario puede importar el suyo o crearlo dentro de la app.

**Usuario:** una persona, su autor, y algún allegado. No hay cuentas, ni servidor, ni
sincronización: todo vive en el móvil. Se instala fuera de Play Store y se actualiza sola.

**Idioma:** español de España, tuteo, tono directo y cálido, sin jerga de producto. Los textos
explican el *porqué* cuando importa ("para que suenen los avisos", "cada plan guarda su propio
progreso"). Ese tono es parte de la identidad: consérvalo.

## 2. Dónde se usa (esto manda sobre todo lo demás)

- **De pie en un gimnasio**, a menudo con una sola mano libre, a veces con guantes o sudor.
- Entre series hay **60–120 segundos** en los que se mira la pantalla de reojo.
- La información crítica (qué toca ahora, cuánto peso, cuánto queda) debe leerse **a un brazo
  de distancia y de un vistazo**. Los números grandes de hoy no son decorativos.
- La app suena y vibra cuando toca volver a la serie, también con la pantalla apagada.
- Hay **máquinas ocupadas**: el orden de los ejercicios se reordena sobre la marcha.

## 3. Plataforma y técnica

| | |
|---|---|
| Móvil | Android, Kotlin + **Jetpack Compose**, Material 3 |
| Reloj | Wear OS, Compose for Wear (módulo aparte, se comunica por Data Layer) |
| Datos | Locales (DataStore + SharedPreferences). Sin backend, sin login |
| Salud | Escribe cada entreno en **Health Connect** (Google Health) |
| Distribución | APK propio, se autoactualiza desde marcmayol.com/building-my-future |
| Temas | Claro y oscuro, ambos obligatorios; siguen el ajuste del sistema |
| Accesibilidad | Debe aguantar `font_scale` 1.3 y 1.5 sin romperse (se prueba en cada versión) |

## 4. Identidad visual actual

**Marca:** degradado del logo, ámbar → naranja → magenta, sobre negro. El icono son dos
mancuernas cruzadas formando una X. **La idea rectora del rediseño: el degradado es un reloj.**
Ámbar = preparas, naranja = trabajas, magenta = esperas. El degradado completo se reserva a la
marca y al progreso; nunca va detrás de texto.

```
Marca      ámbar #FFB24D · naranja #FF6A3D · magenta #FF2E6E
Tinta      (versión para tema claro) #C87A16 · #D4451A · #B3134C

Claro      primary #C8391B   onPrimary #FFFFFF
           primaryContainer #FFDACE  (héroe de Inicio, cinta de recuento)
           secondary #B3134C  secondaryContainer #FFD9E2
           background #FBF6F4  surface #FFFFFF  surfaceVariant #F1E7E2
           texto #221A17 / secundario #58504B  ·  outlineVariant #E4D6D0

Oscuro     primary #FF6A4D   onPrimary #44100A
           primaryContainer #8A2616   secondary #FF5E8A
           background #0A0708 (negro del logo)  surface #161013
           surfaceVariant #2A2024  texto #F2EDEA / secundario #CBBFB9
```

**Tokens de estado** (fuera del `ColorScheme` de M3, en un `CompositionLocal`): `warmup`, `work`,
`rest`, `streak`, `special`, `positive`, más las tres superficies teñidas de la sesión y el
degradado de marca. Son los que hacen que el color signifique lo mismo en toda la app.

- **Tipografía: Archivo** (SIL OFL, variable, empaquetada en `res/font`). Se eligió por sus
  **cifras tabulares**: con la anterior los cronómetros bailaban al cambiar de dígito. La cuenta
  atrás va a 62 sp (la mitad de lo que proponía el diseño original, por petición expresa) y
  respeta los 20 dp de margen.
- **Formas: tres radios, no cinco.** 8 dp campos y chips, 16 dp bloques de sección, 24 dp héroes
  y hojas inferiores. Botones en píldora.
- **Espaciado base 4**, margen lateral de pantalla 20 dp, 24 entre bloques, 12 dentro.
- **Iconografía lineal propia** (`ic_logros`, `ic_pesos`, `ic_resultados`, `ic_estadisticas`,
  trazo 1,9 dp) para la navegación; el set core de Material no traía trofeo, mancuerna ni barras.
  **Los emoji se quedan solo en Logros**, donde son contenido celebratorio y no navegación.
- **Ilustraciones propias de ejercicios:** 30 ejercicios × 2 versiones (masculina y femenina),
  60 JPG. Estilo fijo: **dos fotogramas del movimiento, camiseta naranja, contorno negro, fondo
  blanco**. Se eligen según el género del perfil. Son un activo propio, generado a medida.
- **Mapa muscular:** silueta SVG de frente y espalda que resalta músculo principal y secundario.
- **Sonido:** `rest_done.wav` al terminar una cuenta atrás, con vibración.

## 5. Mapa de navegación

```
Inicio
├── Fase N ─────────► Día N ──► Ficha de ejercicio (hoja inferior)
│                       └────► Sesión guiada  ◄── también desde Inicio y desde el reloj
├── Entrenamientos especiales
│     ├── Entrenamiento libre (extra)
│     ├── Rutina Militar ─────► Sesión de rutina
│     ├── Quema Grasa ──► Ejercicio ──► protocolo ──► Sesión de rutina
│     └── Altura y Postura ───► Sesión de rutina
├── Logros
├── Mis pesos
├── Resultados
├── Estadísticas
└── Configuración
      └── Mis planes ──► Editor de plan          ← NUEVO
```

## 6. Las pantallas, una a una

### Inicio
El panel de control, con la **jerarquía invertida**: manda lo que toca hoy, no el marcador.
1. **Cabecera**: logo 38 dp, "Building My Future" y un botón circular de ajustes a la derecha.
2. **Versalitas**: el plan y la fase actual ("PLAN DE 100 DÍAS · FASE 2 FUERZA", o el nombre del
   plan propio).
3. **Avisos** (solo si aplican): permiso de batería, versión nueva.
4. **Héroe**: con entreno a medias, degradado naranja→magenta, "EN CURSO · TOCA PARA REANUDAR",
   "Día 42" y botón **Reanudar**; sin entreno, `primaryContainer`, "HOY TE TOCA", "Día 3", el
   entreno y sus ejercicios, botón **Empezar día 3** y enlace "Ver el día antes de empezar".
5. **Progreso en una sola lectura**: "42 de 100 días", "42 %" y una cinta de 8 dp con el
   degradado. Antes eran cuatro formas de decir lo mismo.
6. **Entrenamientos especiales** en su propia fila con borde y la aclaración "no cuentan día":
   ya no compite con el botón del día.
7. **Accesos como filas** con icono lineal y separador de 1 dp: Logros (con "3/11"), Mis pesos,
   Resultados, Estadísticas. Configuración vive en la cabecera.
8. **Fases del plan**: una tarjeta por fase con "2 · Fuerza", "12/25", rango y barra de 6 dp.

### Fase N
Cabecera con rango de días, semanas, descripción y **Progresión** (cómo subir de peso). Debajo,
la lista de días de la fase: número grande, "Día N", título del entreno, "Semana X de la fase" y
marca de completado.

### Día N
Lo que toca hacer hoy, para consultarlo o para lanzarlo:
- Título del entreno + fase.
- **Empezar entrenamiento guiado** (primario) y **Entrenar libre · cuenta como día**.
- Tarjeta **Calentamiento**.
- Una tarjeta por ejercicio: ilustración, nombre, esquema ("4 x 8"), nota opcional, casilla de
  hecho, campos **Peso (kg)** y **Reps** (precargadas del esquema) y botón **¿Cómo se hace? ·
  músculos y técnica**.
- **Vuelta a la calma**.
- Botón para marcar el día completado (o deshacerlo).

### Ficha del ejercicio (hoja inferior)
Nombre, esquema, botón **Ver vídeo en YouTube** (busca el nombre), **Músculos que trabaja** con
el mapa muscular (frente/espalda, principal/secundario), **Cómo se hace**, **Errores típicos a
evitar** y **Si está ocupada o no la tienes** (alternativas). Si el ejercicio no está en el
catálogo, se dice con todas las letras y queda el vídeo como salida.

### Sesión guiada · el corazón de la app
**El estado tiñe la pantalla entera**: ámbar preparas, naranja trabajas, magenta esperas, con un
halo radial en oscuro. A un brazo y de reojo, el color ya dice si toca darle o estás
descansando, sin leer una palabra. Estructura fija en los cinco estados: cabecera (estado a la
izquierda, contexto y salida a la derecha), cifra o contenido apoyado en la parte baja, tarjeta
de contexto, fila de secundarios y **un único botón principal que nunca se mueve de sitio**.

| Estado | Qué muestra | Secundarios | Acción principal |
|---|---|---|---|
| Calentamiento | Cuenta atrás, "restante · <calentamiento>", tarjeta HOY TOCA | −1 min · Pausa · +1 min | **Empezar ejercicios** |
| Serie | Nombre grande, esquema, ilustración, bloque **PESO DE ESTA SERIE** | Máquina ocupada · Cómo se hace · Ejercicios | **Serie hecha** |
| Serie por tiempo | Cuenta atrás, "aguanta" / "¡tiempo!", ejercicio | −10 s · Pausa · +10 s | **Serie hecha** |
| Descanso | Cuenta atrás, barra de progreso, tarjeta DESPUÉS con el peso a preparar | −30 s · Ejercicios · +30 s | **Empezar ya** (con halo al acabar) |
| Libre / extra | Cronómetro corriendo | — | **Finalizar y guardar** |
| Fin | Rejilla de resultados y pesos del día | — | **Guardar el día** (degradado) |

**El peso ya no se teclea**: dos círculos de ±2,5 kg (mantener pulsado repite), rueda para saltos
grandes y el teclado como escape tocando la cifra. Sin peso previo se muestra "—" y se explica.

Extras que se conservan:
- **Máquina ocupada**: manda el ejercicio al final de la cola; al repetir, el botón pasa a "Otra
  ocupada".
- **Ejercicios**: el plan del día en una hoja, con marcas *ahora* / *hecho*.
- Salir pide confirmación: **Reanudar luego** o **Descartar el entreno**.
- El peso de cada serie se sugiere solo (el de la serie anterior, o el último de esa máquina).

### Entrenamientos especiales
Menú de sesiones que **no cuentan como día del plan**: entrenamiento libre extra, **Rutina
Militar** (secuencia guiada de 13 pasos), **Quema Grasa** (catálogo de ejercicios con protocolos
tipo HIIT: rondas, series, AMRAP) y **Altura y Postura** (rutina diaria). Cada una avisa, sin
bloquear, si ya se ha cumplido la frecuencia recomendada de la semana ("Ya has cumplido esta
semana · Entrenar igualmente / Mejor no").

### Sesión de rutina
Como la sesión guiada pero por pasos: "Paso 3 de 13", series o rondas, repeticiones hechas,
alternativa cuando un ejercicio tiene variante, descansos con ±15 s y saltar.

### Resultados
Historial de sesiones: fecha, hora, duración, series, etiqueta *Especial* o *Extra*. Orden
invertible (recientes/antiguos, se recuerda). Tarjeta de **Google Health**: conectar o "conectado",
explicando que los entrenos se vuelcan con duración y detalle.

### Estadísticas
**Un dato manda y el resto vive en pestañas.** Héroe con la **racha actual** (degradado en
oscuro, `primaryContainer` en claro) y una línea que la compara con la mejor. Debajo, tres
pestañas M3:
- **Resumen**: rejilla de días/entrenos/tiempo/series, gráfica de **entrenos por semana** con
  navegación, y récords personales.
- **Pesos**: progresión por ejercicio con el peso actual y los registros.
- **Constancia**: esta semana / este mes y la rejilla de últimas semanas (lunes arriba).

Sin datos: el héroe va con borde en vez de relleno, el 0 apagado y "Esto se llena con el primer
entreno. No hay nada que configurar."

### Logros
Héroe con "X de N logros" y cuánto falta para el siguiente. Lista de más fácil a más difícil con
**emoji** (aquí sí), título y, en los bloqueados, un candado y "Te faltan N días". **Se calculan
sobre el plan activo**: hitos de 1, 5 y 10 días, cuartos del plan (en el de 100 días son
25/50/75/100) y uno por fase con su nombre real ("Adaptación superada").

### Mis pesos
Lista de todos los ejercicios del plan con el último peso usado, editable. Es la chuleta que se
mira al llegar a una máquina.

### Configuración
1. **Plan de entrenamiento** (nuevo): qué plan sigues y botón **Mis planes**.
2. Datos personales con ruedas: peso, altura, género (sirven para estimar calorías y para elegir
   la ilustración masculina o femenina).
3. **Uso reloj o pulsómetro**: si está activo, no se estiman calorías (las reales vienen del pulso).
4. **Actualizaciones**: buscar automáticamente, buscar ahora, instalar, versión.
5. **Rellenar desde Google Health**.

### Mis planes (nuevo)
- Explica en una línea que **cada plan guarda su propio progreso** y que cambiar no borra nada.
- Tarjeta **Añadir un plan**: *Crear un plan* (primario), *Importar un archivo*, *Ver el formato
  del archivo* (diálogo con ejemplo en Markdown o JSON y botón de copiar).
- Lista de planes: el de la app primero, luego los del usuario. Cada tarjeta lleva nombre,
  descripción, "12 días · 2 fases · 3 completados", origen y fecha ("Importado el 11 ago 2026
  (Markdown)" / "Creado el…"), chip **Activo**, y botones **Seguir este plan**, **Editar**, **Borrar**.
- Con un entrenamiento a medias, cambiar de plan se bloquea y se explica por qué.
- Borrar avisa de que se lleva por delante el progreso de ese plan.

### Editor de plan (nuevo)
**Cuatro niveles navegables**, cada uno con su scroll corto y su acción, en vez del formulario
anidado de una sola pantalla:
1. **Plan**: cinta con "Con lo escrito salen N días" (se recalcula siempre), nombre,
   descripción, lista de fases con su recuento y su error si lo tienen, y **Guardar el plan**.
2. **Fase**: nombre, **semanas** con stepper grande, progresión, lista de días, y
   Borrar fase / Hecho.
3. **Día**: día de la semana, título, calentamiento y los ejercicios con asa de reordenar, chip
   del esquema y el error en su propia fila.
4. **Ejercicio** (hoja inferior): nombre, aviso de si está en el catálogo, **Series** y
   **Reps** con steppers, conmutador Reps/Segundos, nota, y un escape a texto libre para
   esquemas que no encajan ("3 vueltas").

**Validación en línea y por nivel**: el error se pinta en la fila que falla y sube resumido al
nivel de arriba, así que "Guardar" nunca es la primera vez que te enteras.

## 7. El reloj (Wear OS)

Refleja la sesión del móvil y la controla: estado actual, cronómetro o cuenta atrás, y botones
**Serie hecha**, **Máquina ocupada** y, sin sesión, **Empezar entreno** con el siguiente día
pendiente. Vibra al terminar los descansos y muestra un **chip de entrenamiento en curso en la
esfera** (Ongoing Activity). Sin sesión dice "Sin entrenamiento en curso".

## 8. Notificaciones

- **Descansos**: aviso sonoro al terminar calentamiento, serie por tiempo o descanso
  ("¡Descanso terminado! · A por la siguiente serie"), con acciones **Empezar ya**, **Serie hecha**
  o **Saltar descanso** desde la propia notificación.
- **Temporizador en curso**: notificación persistente con la cuenta atrás, "toca para abrir".

## 9. Vocabulario de datos

Números que aparecen y hay que respetar: días completados / total del plan, porcentaje, serie
actual / total, ejercicio actual / total, peso en kg, repeticiones, segundos de descanso, racha
en días, duración en minutos, series totales, logros conseguidos / totales, "X/N" por fase.

## 10. Reglas para el rediseño

**Innegociable**
1. Los dos temas, claro y oscuro, con la misma calidad. Nada de colores sueltos fuera del tema.
2. Legible a un brazo, en movimiento y con prisa: la sesión guiada manda.
3. Aguantar texto grande (1.3 y 1.5) sin cortes ni solapes. Verificar en captura, no en teoría.
4. Un gesto principal por estado: en la sesión nunca debe haber dos botones que compitan.
5. Degradar bien: sin ilustración, sin ficha, sin descripción o sin progreso, la pantalla tiene
   que seguir teniendo sentido. Con planes propios esto pasa a menudo.
6. El plan es de duración libre: nada puede asumir 100 días, 4 fases ni 5 días por semana.

**Se puede tocar**
- Densidad, motion, y las pantallas que aún no se han rediseñado (ver §11).

**No tocar sin motivo**
- Las ilustraciones de ejercicios y su estilo, el mapa muscular, el icono de la app y el tono de
  los textos.

## 11. Lo que queda por hacer

Del rediseño de agosto de 2026 quedó fuera de alcance, y hereda tokens y patrones pero conserva
la disposición antigua:

- **Wear OS**, notificaciones, Resultados, Mis pesos, Configuración, Fase N, Día N y
  Entrenamientos especiales.
- En Resultados, las sesiones especiales y las del plan se distinguen solo por una etiqueta:
  deberían usar el token `special`.
- El descanso pide un botón de **Pausa** que hoy no existe en el motor de sesión (solo ±30 s);
  añadirlo toca `SessionEngine` y las alarmas, no solo la pantalla.
- El resumen del final de sesión enseña duración, series y descansos; el diseño proponía además
  racha y diferencia de peso, que esa pantalla todavía no calcula.
