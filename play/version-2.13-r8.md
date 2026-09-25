# R8 encendido — sale en la 2.13 (35 móvil / 10035 reloj)

> **25-sep-2026:** con la 2.12 ya en producción (24-sep), R8 deja de esperar y sale junto con
> el arreglo del reloj (2.12.1) y las mejoras de la 2.13 (terminar el entreno a medias
> guardando lo hecho y el plan «En casa»). Probado de nuevo sobre el código definitivo:
> 2.11 publicada con un entreno a medias → 2.13 release encima → sesión recuperada con su
> peso, ilustraciones, mapa muscular, terminar y guardar, Resultados, Estadísticas, fichas
> del catálogo y el plan nuevo; reloj (Wear OS 5, `wear_redondo`) arrancando y
> respondiendo. 0 FATAL. Lo de abajo es la historia de la preparación.

## Historia


> **21-sep-2026: R8 pasa a ser la 2.12.2 (34 móvil / 10034 reloj).** Play rechazó la 2.12 por
> el diseño del reloj, y el versionCode 10033 lo ocupa ahora ese arreglo (rama
> `arreglo-reloj-2.12.1`). Los AAB de R8 ya generados se han renombrado a `R8-NO-SUBIR-*`:
> hay que rehacerlos sobre esa rama con `appVersionCode=34`.

Preparada, probada y **sin subir**: rama `r8-2.12.1`. No se publica hasta que Google apruebe la
2.12 (32 móvil / 10032 reloj), enviada a revisión de producción el 18-sep-2026. Decisión del
18-sep: no se sube ningún cambio hasta que la primera versión esté publicada, porque las
actualizaciones son mucho más fáciles de publicar que el lanzamiento.

## Por qué

Play Console, en *Producción → Panel de control de la versión*, marca un problema en la 32:

> La optimización de código DEX está por debajo de nuestro umbral · Ofuscación (1 %).
> Corrígelo antes de **feb 2027**.

Por debajo del 25 % en cualquier categoría puede restar visibilidad en la tienda a partir de esa
fecha. La causa era `isMinifyEnabled = false` en el `release` del móvil y del reloj: R8 no
llegaba a ejecutarse y el código se subía entero, sin encoger ni ofuscar.

## Qué cambia

| Archivo | Cambio |
|---|---|
| `app/build.gradle.kts`, `wear/build.gradle.kts` | `isMinifyEnabled = true` e `isShrinkResources = true` en `release` |
| `app/src/main/res/raw/keep.xml` | nuevo: protege `@drawable/ex_*` del encogedor de recursos |
| `gradle.properties` | `appVersionCode=33`, `appVersionName=2.12.1` (reloj: 10033) |

Es un parche (2.12.1) y no un minor: la app no hace nada nuevo.

El `keep.xml` no es opcional. Las ilustraciones de los ejercicios se buscan por nombre con
`getIdentifier` (`ExerciseImages.kt`) y R8 no ve esas referencias: sin él las borraría del APK
y los ejercicios se quedarían sin dibujo **sin ningún error**.

No hizo falta ninguna regla de ProGuard propia: no hay reflexión en el código,
kotlinx.serialization 1.7.3 trae sus reglas, los `enum` conservan su nombre (lo que se guarda
en el DataStore sigue siendo compatible), el actualizador lee su manifiesto con `org.json` y
claves de texto, y R8 respeta solo las clases que declara el manifiesto.

## Tamaño

| | 2.12 (32) | 2.12.1 (33) |
|---|---|---|
| AAB móvil | 16,9 MB | **12,6 MB** |
| AAB reloj | 6,5 MB | **2,1 MB** |
| APK móvil (play) | 18,9 MB | **11,0 MB** |

## Cómo se probó (18-sep-2026, emulador `bmf_limpio`, Android 14)

Lo que R8 rompe no se ve compilando, solo en ejecución. Por eso la prueba empieza en la versión
**publicada** y actualiza encima, igual que le pasará a quien ya tenga la app:

1. Se instaló la **32 sin R8** (compilada desde `main` en un worktree, misma firma), se eligió
   plan y se empezó el día 1: dos series de press de pecho con 2 kg y se dejó **a medias**
   («Reanudar luego»).
2. Se instaló la **33 con R8 encima** (`adb install -r`). Todo lo que sigue es en la 33:

| Prueba | Resultado |
|---|---|
| Arranca tras actualizar y muestra el entreno «En curso» | ✅ |
| Reanudar: vuelve a la serie 3 de 3, 2 kg y el descanso corriendo | ✅ la sesión serializada por la 32 se lee en la 33 |
| Aviso de fin de descanso con la **pantalla apagada** | ✅ alarma exacta contra `RestAlarmReceiver`; notificación «¡Descanso terminado!» |
| «Cómo se hace»: ficha (`fichas.json`) y mapa de músculos | ✅ |
| Género «Mujer»: ilustraciones `ex_*_f` por `getIdentifier` | ✅ las 166 `ex_*` siguen en el APK |
| «Valorar en Google Play» (librería de Play) | ✅ sin Play Store, abre la ficha en el navegador, como está previsto |
| Guardar una copia de seguridad | ✅ el JSON conserva los nombres de campo reales, no `a`/`b`/`c` |
| Restaurar esa copia | ✅ «Copia restaurada» |
| Terminar el día entero (15 series) y «Guardar el día» | ✅ resumen, logro «Primer paso», 1 de 100 días |
| Forzar cierre y reabrir | ✅ sigue 1/100, logros 1/11 |
| Estadísticas y Mis pesos | ✅ racha, 72 kg movidos, récord de 2 kg |
| `FATAL EXCEPTION` en todo el log de la sesión | **0** |

**Sin probar en ejecución: el reloj.** No hay emulador de Wear OS emparejado. Lo que sí se
comprobó en el `mapping.txt`: `StateListenerService`, `AlertListenerService` y `MainActivity`
del reloj, y `WearCommandListenerService` del móvil, conservan su nombre, y la comunicación va
por rutas y claves de texto. **Antes de subir, probarlo en el reloj de verdad**: iniciar un
entreno en el móvil y ver que el reloj muestra la serie y vibra al acabar el descanso.

La variante `directo` también compila con R8 (`assembleDirectoRelease`); el `ComprobacionWorker`
del actualizador mantiene nombre y constructor (WorkManager lo crea por nombre de clase). No se
ha probado la auto-actualización de punta a punta.

## Cómo publicarla (cuando la 2.12 ya esté en la tienda)

1. Probar el reloj de verdad (arriba).
2. Fusionar `r8-2.12.1` en `main`.
3. Los artefactos ya están en `para_subir/` (o se regeneran con
   `gradlew :app:bundlePlayRelease :wear:bundlePlayRelease`):
   - `MOVIL-33-v2.12.1.aab` + `MOVIL-33-mapping.txt`
   - `RELOJ-10033-v2.12.1.aab` + `RELOJ-10033-mapping.txt`
4. Producción (Teléfonos…) → Crear nueva versión → subir el AAB del móvil. Después, en
   *Explorador de app bundles → 33 → Descargas → Archivo de desofuscación*, subir su
   `mapping.txt`. Sin él, los informes de fallos de Play llegan con nombres ilegibles.
5. Lo mismo en el canal **Solo en Wear OS** con el del reloj.
6. Siguiente → Revisar y confirmar → Guardar en los dos canales, y enviar desde el Resumen de
   publicación.

Lección de la 2.12: en Play Console un borrador de versión **se pierde si navegas sin «Guardar
como borrador»**, pero el AAB subido se queda en la biblioteca (luego se reutiliza con «Añadir
de la biblioteca»; subirlo otra vez da «El código de versión ya se ha usado»).
