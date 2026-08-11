# Volver al diseño anterior sin perder los planes propios

El rediseño visual de la v2.0 vive en **un único commit aislado**
(`88a86bd · Rediseño visual: el estado tiñe la pantalla y el peso ya no se teclea`), justo
encima del commit que trae los planes propios
(`b582ccc · Tus propios planes…`, publicado como v1.9).

Esa separación no es casual: **deshacer el rediseño no toca la función nueva**. Los planes
importados, el editor, el progreso por plan y los logros proporcionales se quedan como están.

## Cómo se deshace

```bash
git revert --no-edit 88a86bd          # deja el aspecto de la v1.8 con la función de la v1.9
# subir appVersionCode y appVersionName en gradle.properties (p. ej. 12 / 2.1)
python scripts/publicar_release.py --notas "Vuelve el diseño anterior; los planes propios se quedan"
```

Se publica como **versión nueva** (2.1), no reinstalando la 1.9: Android no deja instalar encima
un `versionCode` menor, así que "volver" bajando de versión obligaría a desinstalar la app, y con
ella se irían el progreso y los planes.

El revert deja `gradle.properties` con la versión de la v1.9, así que hay que volver a subirla a
mano antes de publicar. Es el único paso manual.

## Qué cambia exactamente al revertir

Vuelve el aspecto anterior en: tema (colores y tipografía del sistema), Inicio, sesión guiada,
Estadísticas, Logros y la ficha del ejercicio. **El peso vuelve a escribirse con el teclado.**

No cambia nada de: importar planes en JSON o Markdown, el editor de planes, el progreso separado
por plan, los logros calculados sobre el plan activo, ni los avisos, el reloj o Health Connect.

Las dos pantallas nuevas (Mis planes y el editor) se quedan como están: nacieron con la v1.9 y no
tienen una versión anterior a la que volver. Con el tema antiguo se ven con sus colores.

## Si solo molesta una parte

No hace falta revertir el commit entero. Los cambios están separados por archivo:

| Qué molesta | Qué revertir |
|---|---|
| La tipografía o los colores | `app/src/main/java/com/marc/gymplan100/ui/theme/` |
| La sesión teñida y el peso con botones | `ui/WorkoutSessionScreen.kt` y `ui/SessionKit.kt` |
| El Inicio nuevo | `ui/HomeScreen.kt` |
| Las pestañas de Estadísticas | `ui/StatisticsScreen.kt` |

```bash
git checkout b582ccc -- app/src/main/java/com/marc/gymplan100/ui/theme/
```

Ojo con las dependencias: la sesión rediseñada usa los tokens de estado del tema nuevo, así que
si se revierte el tema hay que revertir también la sesión (o al revés, quedarse con ambos).
`Steppers.kt` (los botones ± ) es de la v1.9 y lo usa el editor: **no se revierte nunca**.
