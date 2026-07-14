# Plan 100 Días

App Android nativa (Kotlin + Jetpack Compose) con un plan de entrenamiento de fuerza de 100 días, 5 sesiones por semana. Permite navegar por las 4 fases, ver cada día con sus ejercicios, registrar peso y repeticiones por ejercicio y marcar días como completados. Todo el progreso se guarda en el dispositivo (DataStore).

## Stack

- Kotlin 2.0.21
- Jetpack Compose + Material 3
- Navigation Compose
- DataStore Preferences + kotlinx.serialization (persistencia local)
- AGP 8.7.3, Gradle 8.9
- minSdk 26, targetSdk/compileSdk 35

## Cómo abrir y ejecutar

1. Abre Android Studio (Ladybug o superior).
2. File > Open y selecciona la carpeta `GymPlan100`.
3. Espera a que Gradle sincronice (descargará dependencias la primera vez).
4. Conecta un dispositivo o arranca un emulador y pulsa Run.

Desde terminal, con un JDK 17+ y un dispositivo conectado:

```
./gradlew installDebug
```

Para generar el APK de depuración:

```
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Estructura

```
app/src/main/java/com/marc/gymplan100/
├── MainActivity.kt
├── PlanViewModel.kt
├── data/
│   ├── Models.kt              modelo del plan
│   ├── PlanData.kt            contenido de las 4 fases y los 100 días
│   ├── ProgressModels.kt      estado serializable del progreso
│   └── ProgressRepository.kt  persistencia con DataStore
└── ui/
    ├── GymNavHost.kt          navegación
    ├── HomeScreen.kt          progreso global y fases
    ├── PhaseScreen.kt         días de una fase
    ├── DayScreen.kt           ejercicios, registro y completar día
    └── theme/                 colores, tipografía y tema
```

## Ideas para ampliar

- Gráfica de evolución de peso levantado por ejercicio.
- Temporizador de descanso entre series.
- Exportar el progreso a CSV.
- Recordatorios diarios con notificaciones.
