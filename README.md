# Building My Future 

App Android nativa (Kotlin + Jetpack Compose) para seguir un plan de entrenamiento. No es solo una lista de ejercicios: **te guía la sesión en vivo** —calentamiento, series, descansos con aviso—, **te dice cuándo toca subir el peso y por qué**, y guarda todo tu progreso en el dispositivo. Sin cuenta, sin nube y sin anuncios.

> 🏋️ Reto personal de 100 días para construir el hábito del gimnasio, con la sesión guiada paso a paso incluso con la pantalla apagada.

## 📲 Descargar e instalar

1. Ve a la sección [**Releases**](https://github.com/marcmayol/building-my-future/releases) y descarga el APK de la última versión (`building-my-future-vX.Y.apk`).
2. Ábrelo en el móvil Android. Si te avisa, permite **instalar apps de orígenes desconocidos** para tu navegador o gestor de archivos.
3. Instálalo y listo.

Requisitos: Android 8.0 (API 26) o superior. La app es gratuita y todos los datos se quedan en tu teléfono.

**A partir de ahí se actualiza sola.** La app consulta un manifiesto público
([`updates.json`](https://marcmayol.com/building-my-future/updates.json)) al abrirse y una vez al día,
avisa con un banner cuando hay versión nueva, descarga el APK, **comprueba su SHA-256** y lo instala.
Se puede desactivar y forzar a mano en *Configuración → Actualizaciones*. Detalles en
[`actualizador/README.md`](actualizador/README.md).

La Release incluye también el APK del reloj (`building-my-future-reloj-vX.Y.apk`), que se instala
por `adb` (Wear OS no permite instalarlo desde el móvil fuera de Play Store).

## ✨ Características

**Entrenamiento guiado en vivo**
- Calentamiento con cuenta atrás ajustable (pausa/reanuda).
- **Cada serie con su peso y sus repeticiones**: la primera con 10 kg x 12, la segunda con 12 x 12 y la tercera con 11 x 10 se apuntan tal cual, y el desglose se guarda con el día. Las repeticiones vienen puestas desde el plan: solo se tocan el día que no salen. También **series por tiempo** (planchas, isométricos) con su propia cuenta atrás.
- **Superseries de verdad**: los ejercicios encadenados se hacen uno detrás de otro y el descanso cae al acabar el par, cada uno con su peso y su progresión.
- **Series de calentamiento**: se apuntan, pero no cuentan para los kilos movidos, la fuerza estimada ni la progresión.
- **Qué tal ha ido**: al descansar puedes marcar si la serie fue fácil, justa o al fallo.
- **El peso sugerido sube solo**: cuando cierras todas las series en el tope del rango, la app propone más carga —con el salto que de verdad existe en la máquina— y escribe el motivo debajo. Si llevas tres sesiones atascado, propone bajar un 10 % para volver a subir desde ahí.
- **Descanso con temporizador** entre series y entre ejercicios, con avisos que **suenan aunque tengas la pantalla apagada** o la app en segundo plano.
- **Peso ajustable durante el descanso**: deja preparado el peso de la siguiente serie para tener la máquina lista.
- Botón **"máquina ocupada"**: reordena el ejercicio al final y pasa al siguiente pendiente.
- Guía de cada ejercicio ("¿cómo se hace?", máquina y alternativas) con imágenes de referencia.

**Tu propio plan**
- El reto de 100 días viene de serie, pero puedes **traerte el tuyo o crearlo en la app**: fases con las semanas que quieras, sus días y sus ejercicios. La duración es libre.
- **Importa un archivo** en **Markdown** (se escribe como en una libreta) o en **JSON**; el formato se ve y se copia desde *Mis planes → Ver el formato*.
- **Editor dentro de la app** para crear un plan desde cero o retocar uno importado, reordenando fases, días y ejercicios.
- **Cada plan guarda su progreso por separado**: puedes alternar entre planes y al volver tus días, pesos y entrenos siguen ahí. Los hitos y las estadísticas se recalculan sobre el plan activo.

**Seguimiento y motivación**
- **Estadísticas**: resumen (racha actual y máxima, días completados, tiempo total, series y **kilos movidos**), **gráfica de progresión de peso** por ejercicio con vista de **fuerza estimada (1RM)** —que es la única forma de comparar días con esquemas distintos—, **constancia** (entrenos por semana + mapa de calor tipo calendario) y **records personales**.
- **Mapa muscular**: qué trabaja cada ejercicio (en su ficha y, durante el descanso, el de lo que viene), qué toca hoy en la ficha del día y —en Estadísticas— **lo que se ha llevado cada músculo en los últimos siete días**, contado en series. Es lo único que dice si vas compensado o llevas tres semanas sin tocar pierna.
- **Logros / hitos** del reto con celebración al desbloquearlos (y sorpresa al completar los 100 días 🏆).
- **Mis pesos**: el peso de referencia de cada máquina —el **más alto** que has movido en ella—, siempre a mano.
- **Resultados**: histórico de cada día con su duración y los pesos de cada ejercicio, **serie a serie** ("10 · 12 · 11 kg"), incluido lo que apuntes al acabar un entreno libre.
- Entrenamiento **especial** (sesión libre guiada) y **extra** (bonus que no cuenta como día del plan); al terminar se apunta lo hecho **con peso y repeticiones de cada serie**.

**Que no se te pase**
- **Recordatorios**: dile qué días sueles entrenar y a qué hora, y la app te avisa con el día que toca. El plan sigue yendo por días numerados —el día 47 es el 47 lo hagas el martes o el sábado—, así que son **100 días de entrenamiento** repartidos como entrenes tú. Si ese día ya has entrenado, no te molesta.

**Tus datos son tuyos**
- **Copia de seguridad**: guarda un archivo con todo (días, pesos, historial, planes y ajustes) donde quieras —Drive, el PC, la tarjeta— y restaurándolo recuperas el móvil tal y como estaba. Sin cuenta y sin nube: el archivo es tuyo y lo puedes abrir.
- **Ejercicios que la app no conoce**: si tu plan trae uno con otro nombre, dile a cuál se parece y hereda ilustración, ficha, músculos y su sitio en el mapa.

**Integraciones**
- **Wear OS**: controla la sesión desde el reloj (p. ej. Pixel Watch). Si el reloj mide el pulso, las calorías reales las aporta Google Health y la app evita el doble conteo.
- **Health Connect (Google Health)**: cada entreno se guarda automáticamente con su duración y el detalle de ejercicios y pesos; estimación de calorías activas (fórmula MET) cuando no hay reloj.

Diseño con tema cálido naranja/magenta y **modo oscuro**, que puedes dejar en el del móvil o fijar en claro u oscuro desde *Ajustes → Tema*.

## 🧱 Stack

- Kotlin 2.0.21 · Jetpack Compose + Material 3
- Navigation Compose
- DataStore Preferences + kotlinx.serialization (persistencia local)
- Health Connect Client · Play Services Wearable (módulo `wear`)
- minSdk 26 · targetSdk 35 · compileSdk 36

## 📁 Estructura

```
app/src/main/java/com/marc/gymplan100/
├── MainActivity.kt
├── PlanViewModel.kt
├── data/
│   ├── PlanData.kt            ventana al plan activo (lo que usa toda la app)
│   ├── BuiltinPlan.kt         el reto de 100 días que viene de serie
│   ├── TrainingPlan.kt        un plan cualquiera: fases -> días numerados
│   ├── PlanJson.kt            formato de intercambio y validación
│   ├── PlanMarkdown.kt        lector de planes escritos en Markdown
│   ├── PlanStore.kt           planes guardados y cuál está activo
│   ├── Models.kt / ProgressModels.kt / SessionModels.kt
│   ├── SessionEngine.kt       transiciones de la sesión, superseries incluidas (puras)
│   ├── Progression.kt         cuándo toca subir peso, y por qué (doble progresión + deload)
│   ├── ProgressRepository.kt  persistencia con DataStore
│   ├── Backup.kt              copia de seguridad de todo, en un archivo tuyo
│   ├── Statistics.kt          estadísticas, fuerza estimada (1RM) y kilos movidos
│   ├── MuscleLoad.kt          qué se ha llevado cada músculo, en series
│   ├── MuscleTargets.kt / MuscleMapData.kt   qué trabaja cada ejercicio y sus trazados
│   ├── ExerciseGuides.kt / ExerciseImages.kt   guías e imágenes
│   ├── ExerciseAliases.kt     «este ejercicio mío es como este del catálogo»
│   └── Achievements.kt        logros e hitos
├── health/                    integración con Health Connect
├── notify/                    avisos de descanso y recordatorios de ir al gimnasio
├── wear/                      puente con el reloj (Wear OS)
└── ui/                        pantallas Compose (Home, sesión, estadísticas, resultados…)
    └── theme/AppTheme.kt      claro, oscuro o el del móvil
wear/                          app del reloj (Wear OS)
```

## 🛠️ Compilar desde el código

Con un JDK 17+ y un dispositivo/emulador conectado:

```bash
./gradlew installDebug      # instala la versión de depuración
./gradlew assembleRelease   # genera la APK de release firmada
```

La APK de release queda en `app/build/outputs/apk/release/app-release.apk`.

La versión (`appVersionCode` / `appVersionName`) vive en `gradle.properties`: es la fuente única que
comparten el móvil, el reloj y el manifiesto de actualizaciones.

### Publicar una versión

```bash
python scripts/publicar_release.py --dry-run       # construye y verifica sin publicar
python scripts/publicar_release.py --notas "…"     # publica Release + manifiesto
```

El script construye los dos APK firmados, comprueba con `aapt2` que el `versionCode` construido
coincide con el declarado y supera al publicado, verifica con `apksigner` que la firma sigue siendo
la de siempre (si cambiara, ninguna instalación existente podría actualizarse), crea la Release con
`gh` y publica `docs/updates.json` en GitHub Pages comprobando que la URL pública ya lo sirve.

### Firma de release

La firma se lee de `keystore.properties` (en la raíz, **fuera del control de versiones**), que apunta al keystore `.jks`. Ambos están en `.gitignore` y **nunca se suben al repo**. Si el archivo no existe, el proyecto compila igualmente (sin firma configurada), por lo que se puede clonar y compilar en depuración sin nada extra.

## 🎨 Diseño

[`docs/handoff-diseno.md`](docs/handoff-diseno.md) cuenta la app entera desde el punto de vista del
diseño: para quién es, dónde se usa, la paleta y la tipografía actuales, todas las pantallas con sus
estados, el reloj, las notificaciones y qué se puede tocar y qué no en un rediseño. Está escrito para
entenderse sin leer el código.

## 📄 Licencia

Proyecto personal. Las imágenes de ejercicios provienen del set libre [everkinetic](https://github.com/everkinetic/data).
