# Material para Google Play

Lo que hay que subir a Play Console, ya preparado.

- **`ficha.md`** — nombre, descripciones, categoría, respuestas del formulario de seguridad
  de datos y de la declaración de Health Connect. Copiar y pegar.
- **`graficos/`** — icono 512×512 y portada 1024×500, en PNG (lo que pide Play) y en SVG por
  si hay que retocarlos. Los dos salen del mismo vector del icono de la app, así que la marca
  es exactamente la misma, no una versión parecida.
- **`capturas/`** — las seis capturas **crudas** del móvil, 1080×2400, hechas sobre datos
  realistas (26 días entrenados) y no sobre una app recién instalada con todo a cero. Estas
  no se suben: son la materia prima.
- **`capturas-ficha/`** — las que **sí** se suben, 1080×1920, con el titular de cada pantalla
  escrito encima. Salen de las anteriores con `scripts/generar_capturas_play.py`.
- **`acceso-produccion.md`** — las diez respuestas del formulario de acceso a producción, que
  se rellena al terminar el test cerrado.

## Cómo se generaron los gráficos

El icono de la app solo existe como `VectorDrawable`, y Play pide PNG. En vez de redibujarlo,
`scripts/` del scratchpad tradujo los mismos paths y degradados a SVG y se rasterizó con
Chrome en modo headless. Si algún día cambia el icono de la app, hay que rehacerlos igual.

## Las capturas: por qué no se suben las del móvil

Dos motivos, y el primero es descalificatorio:

1. **El ratio.** Play no admite capturas con una relación mayor de **2:1**, y el móvil las hace
   1080×2400, que es 2,22:1. Las que había aquí eran inválidas y ni siquiera se habrían podido
   subir. Las de `capturas-ficha/` son 1080×1920.
2. **El tamaño al que se ven.** En el listado, la captura de un móvil entero mide unos 200 px de
   alto: no se lee ni un titular de la app. Lo único que se lee es el texto que se pone encima,
   y es lo que decide si alguien sigue mirando.

Cada titular describe **lo que se ve en esa pantalla**. Si algo no sale en la imagen, no se
escribe: una ficha que promete lo que la captura no enseña se paga en desinstalaciones.

## Recordatorios que cuestan un rechazo

- Subir **siempre el sabor `play`** (`bundlePlayRelease`), nunca el `directo`: ese lleva el
  auto-actualizador, y Play no admite que una app se actualice por fuera de la tienda.
- La app del **reloj** va aparte y tiene que compilarse también en `play`, o no se emparejará
  con el móvil (la Data Layer de Wear OS empareja por applicationId).
