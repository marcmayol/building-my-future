# Material para Google Play

Lo que hay que subir a Play Console, ya preparado.

- **`ficha.md`** — nombre, descripciones, categoría, respuestas del formulario de seguridad
  de datos y de la declaración de Health Connect. Copiar y pegar.
- **`graficos/`** — icono 512×512 y portada 1024×500, en PNG (lo que pide Play) y en SVG por
  si hay que retocarlos. Los dos salen del mismo vector del icono de la app, así que la marca
  es exactamente la misma, no una versión parecida.
- **`capturas/`** — seis capturas de 1080×2400 hechas sobre datos realistas (26 días
  entrenados), no sobre una app recién instalada con todo a cero.

## Cómo se generaron los gráficos

El icono de la app solo existe como `VectorDrawable`, y Play pide PNG. En vez de redibujarlo,
`scripts/` del scratchpad tradujo los mismos paths y degradados a SVG y se rasterizó con
Chrome en modo headless. Si algún día cambia el icono de la app, hay que rehacerlos igual.

## Recordatorios que cuestan un rechazo

- Subir **siempre el sabor `play`** (`bundlePlayRelease`), nunca el `directo`: ese lleva el
  auto-actualizador, y Play no admite que una app se actualice por fuera de la tienda.
- La app del **reloj** va aparte y tiene que compilarse también en `play`, o no se emparejará
  con el móvil (la Data Layer de Wear OS empareja por applicationId).
