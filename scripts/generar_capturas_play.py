# -*- coding: utf-8 -*-
"""
Monta las capturas de la ficha de Play a partir de las capturas crudas del móvil.

Por qué existe esto y no se suben las capturas tal cual:

1. **Ratio.** Play no admite capturas con una relación mayor de 2:1, y el móvil las hace
   1080x2400, que es 2,22:1. Las de antes eran directamente inválidas. Aquí salen 1080x1920.
2. **Se ven a 200 px de alto.** En el listado, la captura de un móvil entero es una mancha:
   no se lee ni un titular. Lo que se lee es el texto que se pone encima, y eso es lo único
   que decide si alguien sigue mirando.

Cada captura lleva un titular que dice **lo que se ve en esa pantalla**, no una promesa
genérica. La cifra de la cuarta es la que sale en la propia imagen.

    python scripts/generar_capturas_play.py

Lee de `play/capturas/` y escribe en `play/capturas-ficha/`.
"""

from __future__ import annotations

import os
import sys

from PIL import Image, ImageDraw, ImageFont

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ORIGEN = os.path.join(RAIZ, "play", "capturas")
DESTINO = os.path.join(RAIZ, "play", "capturas-ficha")
FUENTE = os.path.join(RAIZ, "app", "src", "main", "res", "font", "archivo_variable.ttf")

ANCHO, ALTO = 1080, 1920

# La misma paleta de la app (ui/theme/Color.kt), no uno parecido.
FONDO = (10, 7, 8)
TITULO = (255, 255, 255)
SUBTITULO = (176, 164, 158)
AMBAR = (255, 178, 77)
NARANJA = (255, 106, 61)
MAGENTA = (255, 46, 110)

MARGEN = 76

# (archivo, titular, apoyo). El titular describe ESA pantalla: si algo no se ve en la
# imagen, no se escribe.
CAPTURAS = [
    (
        "01-portada.png",
        "Abres y ya sabes\nqué toca hoy",
        "El día que te toca, empezado de un toque.",
    ),
    (
        "02-serie.png",
        "Te dice cuándo\ncambiar el peso",
        "«Llevas 3 sesiones sin pasar de aquí»: y te explica por qué bajar.",
    ),
    (
        "03-descanso.png",
        "El descanso\ncorre solo",
        "Marcas qué tal ha ido y dejas puesto el peso de la siguiente.",
    ),
    (
        "04-stats.png",
        "152.322 kg\nen 26 días",
        "Rachas, récords y las horas que llevas de verdad.",
    ),
    (
        "05-musculos.png",
        "Qué músculo se ha\nllevado la semana",
        "El mapa que avisa de lo que llevas tiempo sin tocar.",
    ),
    (
        "06-dia.png",
        "El día, ejercicio\na ejercicio",
        "Con la máquina de cada uno y cómo se hace.",
    ),
]


def fuente(tam: int, peso: int) -> ImageFont.FreeTypeFont:
    """Archivo variable en el peso que se pida, con respaldo si PIL no sabe variarla."""
    f = ImageFont.truetype(FUENTE, tam)
    try:
        f.set_variation_by_axes([peso, 100.0])
    except Exception:
        pass
    return f


def barra_de_marca(img: Image.Image) -> None:
    """El degradado de la app (ámbar → naranja → magenta) como firma superior."""
    d = ImageDraw.Draw(img)
    alto = 10
    for x in range(ANCHO):
        t = x / (ANCHO - 1)
        if t < 0.5:
            u = t * 2
            c = tuple(int(AMBAR[i] + (NARANJA[i] - AMBAR[i]) * u) for i in range(3))
        else:
            u = (t - 0.5) * 2
            c = tuple(int(NARANJA[i] + (MAGENTA[i] - NARANJA[i]) * u) for i in range(3))
        d.line([(x, 0), (x, alto)], fill=c)


def redondea(img: Image.Image, radio: int) -> Image.Image:
    mascara = Image.new("L", img.size, 0)
    ImageDraw.Draw(mascara).rounded_rectangle([0, 0, img.width - 1, img.height - 1], radio, fill=255)
    fuera = Image.new("RGBA", img.size, (0, 0, 0, 0))
    fuera.paste(img, (0, 0), mascara)
    return fuera


def compone(nombre: str, titular: str, apoyo: str) -> Image.Image:
    lienzo = Image.new("RGB", (ANCHO, ALTO), FONDO)
    barra_de_marca(lienzo)
    d = ImageDraw.Draw(lienzo)

    f_tit = fuente(84, 700)
    f_sub = fuente(38, 400)

    y = 132
    for linea in titular.split("\n"):
        d.text((MARGEN, y), linea, font=f_tit, fill=TITULO)
        y += 96

    y += 18
    # El apoyo se parte a mano: son frases cortas y así no depende de textwrap.
    palabras = apoyo.split()
    linea, lineas = "", []
    for p in palabras:
        prueba = (linea + " " + p).strip()
        if d.textlength(prueba, font=f_sub) > ANCHO - 2 * MARGEN:
            lineas.append(linea)
            linea = p
        else:
            linea = prueba
    lineas.append(linea)
    for l in lineas:
        d.text((MARGEN, y), l, font=f_sub, fill=SUBTITULO)
        y += 52

    # La captura entra por abajo y se sale del lienzo a propósito: se ve que es un móvil
    # sin gastar media imagen en enseñar el marco entero.
    cruda = Image.open(os.path.join(ORIGEN, nombre)).convert("RGB")
    ancho_movil = 780
    alto_movil = int(cruda.height * ancho_movil / cruda.width)
    movil = cruda.resize((ancho_movil, alto_movil), Image.LANCZOS)
    movil = redondea(movil, 36)

    arriba = max(y + 60, 560)
    x = (ANCHO - ancho_movil) // 2
    lienzo.paste(movil, (x, arriba), movil)

    # Un filo claro para que el negro de la captura no se funda con el negro del fondo.
    visible = ALTO - arriba
    ImageDraw.Draw(lienzo).rounded_rectangle(
        [x, arriba, x + ancho_movil - 1, arriba + min(alto_movil, visible + 40) - 1],
        36,
        outline=(58, 46, 42),
        width=2,
    )
    return lienzo


def main() -> int:
    if not os.path.isdir(ORIGEN):
        print("No encuentro %s" % ORIGEN)
        return 1
    os.makedirs(DESTINO, exist_ok=True)
    for nombre, titular, apoyo in CAPTURAS:
        if not os.path.exists(os.path.join(ORIGEN, nombre)):
            print("  falta %s, la salto" % nombre)
            continue
        salida = os.path.join(DESTINO, nombre)
        compone(nombre, titular, apoyo).save(salida)
        print("  %s  %dx%d" % (nombre, ANCHO, ALTO))
    print("Listas en %s" % DESTINO)
    return 0


if __name__ == "__main__":
    sys.exit(main())
