"""
Pasa los dibujos de `res/drawable-nodpi` de JPG a WebP sin que se note.

Los dibujos son vector plano sobre blanco (pocos colores, bordes limpios), que es justo donde
el JPG desperdicia más: a calidad 82 el WebP pesa menos de la mitad. Para no fiarse del ojo,
cada imagen se compara píxel a píxel con la original y, si la diferencia media de algún canal
pasa de [UMBRAL] (sobre 255), se queda en JPG y se avisa.

Android carga el drawable por nombre, no por extensión: el código no cambia.

Uso: python scripts/optimizar_dibujos.py
"""
import io
from pathlib import Path

from PIL import Image, ImageChops, ImageStat

DIBUJOS = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "drawable-nodpi"
CALIDAD = 82
UMBRAL = 2.0


def main() -> None:
    antes = despues = 0
    rechazados = []
    for jpg in sorted(DIBUJOS.glob("*.jpg")):
        original = Image.open(jpg).convert("RGB")
        buf = io.BytesIO()
        original.save(buf, "WEBP", quality=CALIDAD, method=6)
        buf.seek(0)
        diferencia = max(ImageStat.Stat(ImageChops.difference(original, Image.open(buf).convert("RGB"))).mean)
        if diferencia > UMBRAL:
            rechazados.append((jpg.name, round(diferencia, 2)))
            continue
        antes += jpg.stat().st_size
        despues += buf.getbuffer().nbytes
        jpg.with_suffix(".webp").write_bytes(buf.getvalue())
        jpg.unlink()
    print(f"{antes // 1024} KB -> {despues // 1024} KB")
    for nombre, d in rechazados:
        print(f"Se queda en JPG (diferencia {d}): {nombre}")


if __name__ == "__main__":
    main()
