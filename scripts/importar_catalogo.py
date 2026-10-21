"""
Trae a la app el catálogo de ejercicios de exercise-api (marcmayol.com/exercise-api).

Deja tres cosas, y solo añade: lo que ya está en la app no se toca.

- `res/drawable-nodpi/ex_<slug>_{m,f}.webp` para los ejercicios que aún no tienen dibujo.
  WebP y no JPG para que doblar el catálogo no doble el peso de la app.
- `assets/fichas.json` con la ficha (músculos, técnica, errores y alternativas) de cada slug.
- `assets/catalogo.json`: nombre en español, grupo, material y músculos de cada slug. Es lo
  que lista el selector de «he hecho otro ejercicio» y lo que da músculos al mapa cuando el
  ejercicio no está en `MuscleTargets`.

Uso: python scripts/importar_catalogo.py [ruta de exercise-api]
"""
import json
import sys
from pathlib import Path

from PIL import Image

RAIZ = Path(__file__).resolve().parent.parent
MAIN = RAIZ / "app" / "src" / "main"
DIBUJOS = MAIN / "res" / "drawable-nodpi"
API = Path(sys.argv[1] if len(sys.argv) > 1 else r"C:\Users\marcm\Desktop\exercise-api")


def ficha(e: dict) -> dict:
    musculos = [m["es"] for m in e.get("primaryMuscles", []) + e.get("secondaryMuscles", [])]
    texto = ", ".join(musculos[:-1]) + (" y " if len(musculos) > 1 else "") + musculos[-1] if musculos else ""
    return {
        "muscles": (texto[:1].upper() + texto[1:] + ".") if texto else "",
        "howTo": " ".join(e["instructions"]["es"]),
        "mistakes": e.get("mistakes", {}).get("es", []),
        "alternatives": e.get("alternatives", {}).get("es", []),
        "videoQuery": e["name"]["es"] + " técnica",
    }


def main() -> None:
    lista = json.loads((API / "v1" / "exercises.json").read_text(encoding="utf8"))["exercises"]
    fichas_path = MAIN / "assets" / "fichas.json"
    fichas = json.loads(fichas_path.read_text(encoding="utf8"))
    catalogo = []
    nuevos_dibujos = nuevas_fichas = 0
    for resumen in lista:
        slug = resumen["slug"]
        e = json.loads((API / "v1" / "exercises" / f"{slug}.json").read_text(encoding="utf8"))
        base = "ex_" + slug.replace("-", "_")
        for sufijo, letra in (("m", "m"), ("f", "f")):
            ya = any((DIBUJOS / f"{base}_{letra}.{ext}").exists() for ext in ("jpg", "png", "webp"))
            if ya:
                continue
            origen = API / "images" / f"{slug}-{sufijo}.jpg"
            if not origen.exists():
                continue
            Image.open(origen).convert("RGB").save(DIBUJOS / f"{base}_{letra}.webp", "WEBP", quality=80, method=6)
            nuevos_dibujos += 1
        if slug not in fichas:
            fichas[slug] = ficha(e)
            nuevas_fichas += 1
        catalogo.append({
            "slug": slug,
            "nombre": e["name"]["es"],
            "grupo": e["group"]["id"],
            "material": e["equipment"]["id"],
            "principales": [m["id"] for m in e.get("primaryMuscles", [])],
            "secundarios": [m["id"] for m in e.get("secondaryMuscles", [])],
        })
    fichas_path.write_text(json.dumps(fichas, ensure_ascii=False, indent=1) + "\n", encoding="utf8")
    (MAIN / "assets" / "catalogo.json").write_text(
        json.dumps(catalogo, ensure_ascii=False, indent=1) + "\n", encoding="utf8"
    )
    print(f"{len(catalogo)} ejercicios en el catálogo, {nuevos_dibujos} dibujos y {nuevas_fichas} fichas nuevas")


if __name__ == "__main__":
    main()
