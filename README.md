# Building My Future · Plan 100 Días

App Android nativa (Kotlin + Jetpack Compose) para seguir un plan de entrenamiento de fuerza de **100 días, 5 sesiones por semana**, repartido en 4 fases. No es solo una lista de ejercicios: **te guía la sesión en vivo** —calentamiento, series, descansos con aviso— y guarda todo tu progreso en el dispositivo.

> 🏋️ Reto personal de 100 días para construir el hábito del gimnasio, con la sesión guiada paso a paso incluso con la pantalla apagada.

## 📲 Descargar e instalar

1. Ve a la sección [**Releases**](https://github.com/marcmayol/building-my-future/releases) y descarga el archivo `app-release.apk` de la última versión.
2. Ábrelo en el móvil Android. Si te avisa, permite **instalar apps de orígenes desconocidos** para tu navegador o gestor de archivos.
3. Instálalo y listo.

Requisitos: Android 8.0 (API 26) o superior. La app es gratuita y todos los datos se quedan en tu teléfono.

## ✨ Características

**Entrenamiento guiado en vivo**
- Calentamiento con cuenta atrás ajustable (pausa/reanuda).
- Series con registro de peso; **series por tiempo** (planchas, isométricos) con su propia cuenta atrás.
- **Descanso con temporizador** entre series y entre ejercicios, con avisos que **suenan aunque tengas la pantalla apagada** o la app en segundo plano.
- **Peso ajustable durante el descanso**: deja preparado el peso de la siguiente serie para tener la máquina lista.
- Botón **"máquina ocupada"**: reordena el ejercicio al final y pasa al siguiente pendiente.
- Guía de cada ejercicio ("¿cómo se hace?", máquina y alternativas) con imágenes de referencia.

**Seguimiento y motivación**
- **Estadísticas**: resumen (racha actual y máxima, días completados, tiempo total, series), **gráfica de progresión de peso** por ejercicio, **constancia** (entrenos por semana + mapa de calor tipo calendario) y **records personales**.
- **Logros / hitos** del reto con celebración al desbloquearlos (y sorpresa al completar los 100 días 🏆).
- **Mis pesos**: el peso de referencia de cada máquina, siempre a mano.
- **Resultados**: histórico de cada día con su duración y los pesos de cada ejercicio.
- Entrenamiento **especial** (sesión libre guiada) y **extra** (bonus que no cuenta como día del plan).

**Integraciones**
- **Wear OS**: controla la sesión desde el reloj (p. ej. Pixel Watch). Si el reloj mide el pulso, las calorías reales las aporta Google Health y la app evita el doble conteo.
- **Health Connect (Google Health)**: cada entreno se guarda automáticamente con su duración y el detalle de ejercicios y pesos; estimación de calorías activas (fórmula MET) cuando no hay reloj.

Diseño con tema cálido naranja/magenta y **modo oscuro**.

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
│   ├── PlanData.kt            las 4 fases y los 100 días
│   ├── Models.kt / ProgressModels.kt / SessionModels.kt
│   ├── SessionEngine.kt       transiciones de la sesión (funciones puras)
│   ├── ProgressRepository.kt  persistencia con DataStore
│   ├── Statistics.kt          cálculos de estadísticas
│   ├── ExerciseGuides.kt / ExerciseImages.kt   guías e imágenes
│   └── Achievements.kt        logros e hitos
├── health/                    integración con Health Connect
├── notify/                    avisos de descanso (suenan con pantalla apagada)
├── wear/                      puente con el reloj (Wear OS)
└── ui/                        pantallas Compose (Home, sesión, estadísticas, resultados…)
wear/                          app del reloj (Wear OS)
```

## 🛠️ Compilar desde el código

Con un JDK 17+ y un dispositivo/emulador conectado:

```bash
./gradlew installDebug      # instala la versión de depuración
./gradlew assembleRelease   # genera la APK de release firmada
```

La APK de release queda en `app/build/outputs/apk/release/app-release.apk`.

### Firma de release

La firma se lee de `keystore.properties` (en la raíz, **fuera del control de versiones**), que apunta al keystore `.jks`. Ambos están en `.gitignore` y **nunca se suben al repo**. Si el archivo no existe, el proyecto compila igualmente (sin firma configurada), por lo que se puede clonar y compilar en depuración sin nada extra.

## 📄 Licencia

Proyecto personal. Las imágenes de ejercicios provienen del set libre [everkinetic](https://github.com/everkinetic/data).
