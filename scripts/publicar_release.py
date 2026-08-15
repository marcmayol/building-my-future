"""Publica una release de Building My Future y actualiza el manifiesto de updates.

Ritual completo (hermano del de Grimorio de Salud y del de DracPDF): build de los
APK de release FIRMADOS (móvil y reloj), lectura del versionCode/versionName
(fuente única: gradle.properties), cálculo del sha256, verificación de coherencia
(el versionCode de los APK construidos, leído con aapt2, debe coincidir con el que
se escribirá en el manifiesto y ser mayor que el ya publicado; la firma debe seguir
siendo la misma de siempre — si algo no cuadra, aborta), creación de la Release en
GitHub con ambos assets (gh CLI, verificando antes gh auth status) y publicación del
manifiesto docs/updates.json en GitHub Pages (commit + push), verificando después
que la URL pública ya sirve el versionCode nuevo (reintentando por la caché del CDN).

El manifiesto solo describe el APK del móvil: el del reloj se instala a mano por adb
(Wear OS no permite que el móvil lo instale fuera de Play Store), pero viaja en la
misma Release para no tener que compilarlo cuando haga falta.

Secretos: la firma sale de keystore.properties (fuera del repo, gitignored) o de
variables de entorno BMF_STORE_FILE / BMF_STORE_PASSWORD / BMF_KEY_ALIAS /
BMF_KEY_PASSWORD. Si faltan, aborta con mensaje claro. Ningún secreto se escribe
en el repo.

Uso:
    python scripts/publicar_release.py              # construye y publica
    python scripts/publicar_release.py --dry-run    # prepara sin publicar
    python scripts/publicar_release.py --notas "…"  # notas de la versión
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import time
import urllib.request
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
GRADLE_PROPS = RAIZ / "gradle.properties"
MANIFIESTO = RAIZ / "docs" / "updates.json"
FIRMA_ESPERADA = RAIZ / "scripts" / "firma_esperada.txt"
APK_MOVIL = RAIZ / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk"
APK_RELOJ = RAIZ / "wear" / "build" / "outputs" / "apk" / "release" / "wear-release.apk"

_REPO = "marcmayol/building-my-future"
_PAGES_URL = "https://marcmayol.com/building-my-future/updates.json"
_CHECK_HORAS = 24
_ENV_FIRMA = (
    "BMF_STORE_FILE",
    "BMF_STORE_PASSWORD",
    "BMF_KEY_ALIAS",
    "BMF_KEY_PASSWORD",
)


# --- utilidades ---------------------------------------------------------------

def _ejecutar(cmd: list[str], **kw) -> None:
    print("»", " ".join(cmd))
    if subprocess.call(cmd, cwd=str(RAIZ), **kw) != 0:
        raise SystemExit(f"Falló: {' '.join(cmd)}")

def _salida(cmd: list[str]) -> str:
    return subprocess.run(
        cmd, cwd=str(RAIZ), capture_output=True, text=True
    ).stdout

def sha256(ruta: Path) -> str:
    h = hashlib.sha256()
    with ruta.open("rb") as f:
        for bloque in iter(lambda: f.read(65536), b""):
            h.update(bloque)
    return h.hexdigest()

def _gradlew() -> str:
    return "gradlew.bat" if os.name == "nt" else "./gradlew"


# --- versión (fuente única: gradle.properties) --------------------------------

def leer_version() -> tuple[int, str]:
    texto = GRADLE_PROPS.read_text(encoding="utf-8")
    vc = re.search(r"^appVersionCode\s*=\s*(\d+)", texto, re.MULTILINE)
    vn = re.search(r"^appVersionName\s*=\s*(.+)$", texto, re.MULTILINE)
    if not vc or not vn:
        raise SystemExit("No se pudo leer appVersionCode/appVersionName de gradle.properties.")
    return int(vc.group(1)), vn.group(1).strip()


# --- firma --------------------------------------------------------------------

def asegurar_firma() -> None:
    """Comprueba que hay credenciales de firma; si vienen por env, las materializa
    en un keystore.properties temporal (borrado al terminar). Nunca sobrescribe uno
    existente ni deja secretos en el repo."""
    props = RAIZ / "keystore.properties"
    if props.exists():
        print("Firma: usando keystore.properties existente.")
        return
    if all(os.environ.get(k) for k in _ENV_FIRMA):
        print("Firma: usando variables de entorno (keystore.properties temporal).")
        props.write_text(
            f"storeFile={os.environ['BMF_STORE_FILE']}\n"
            f"storePassword={os.environ['BMF_STORE_PASSWORD']}\n"
            f"keyAlias={os.environ['BMF_KEY_ALIAS']}\n"
            f"keyPassword={os.environ['BMF_KEY_PASSWORD']}\n",
            encoding="utf-8",
        )
        import atexit
        atexit.register(lambda: props.exists() and props.unlink())
        return
    raise SystemExit(
        "Faltan credenciales de firma. Crea keystore.properties en la raíz (fuera de "
        "git) con storeFile/storePassword/keyAlias/keyPassword, o define las variables "
        f"de entorno: {', '.join(_ENV_FIRMA)}."
    )


# --- herramientas del SDK (verificación de coherencia) ------------------------

def _sdk_dir() -> Path:
    local = RAIZ / "local.properties"
    if local.exists():
        m = re.search(r"sdk\.dir=(.+)", local.read_text(encoding="utf-8"))
        if m:
            return Path(m.group(1).strip().replace("\\\\", "\\").replace("\\:", ":"))
    for env in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(env):
            return Path(os.environ[env])
    raise SystemExit("No encuentro el Android SDK (local.properties o ANDROID_HOME).")

def _build_tool(nombre: str) -> Path:
    """Ruta a una herramienta de build-tools, la de versión más alta disponible."""
    exe = f"{nombre}.exe" if os.name == "nt" else nombre
    candidatos = sorted((_sdk_dir() / "build-tools").glob(f"*/{exe}"), reverse=True)
    if not candidatos:
        # apksigner es un .bat en Windows; aapt2 sí es .exe.
        alt = sorted((_sdk_dir() / "build-tools").glob(f"*/{nombre}.bat"), reverse=True)
        if alt:
            return alt[0]
        raise SystemExit(f"No encuentro {nombre} en build-tools del SDK.")
    return candidatos[0]

def version_code_del_apk(apk: Path) -> int:
    salida = _salida([str(_build_tool("aapt2")), "dump", "badging", str(apk)])
    m = re.search(r"versionCode='(\d+)'", salida)
    if not m:
        raise SystemExit(f"No pude leer el versionCode de {apk.name} con aapt2.")
    return int(m.group(1))

def huella_firma(apk: Path) -> str | None:
    """SHA-256 del certificado de firma del APK, o None si apksigner no está."""
    try:
        salida = _salida([str(_build_tool("apksigner")), "verify", "--print-certs", str(apk)])
    except SystemExit:
        return None
    m = re.search(r"certificate SHA-256 digest:\s*([0-9a-fA-F]+)", salida)
    return m.group(1).lower() if m else None


# --- manifiesto ---------------------------------------------------------------

def nombre_asset(vn: str, reloj: bool = False) -> str:
    sufijo = "-reloj" if reloj else ""
    return f"building-my-future{sufijo}-v{vn}.apk"

def url_release(version_name: str) -> str:
    return (
        f"https://github.com/{_REPO}/releases/download/"
        f"v{version_name}/{nombre_asset(version_name)}"
    )

def generar_manifiesto(vc: int, vn: str, sha: str, notas: str) -> dict:
    return {
        "versionCode": vc,
        "versionName": vn,
        "url": url_release(vn),
        "sha256": sha,
        "notas": notas or f"Building My Future {vn}.",
        "check_horas": _CHECK_HORAS,
    }

def version_code_publicado() -> int | None:
    """versionCode del último manifiesto COMMITEADO (None si es el primero).

    Se lee de git y no del working tree a propósito: un --dry-run previo ya ha
    reescrito docs/updates.json con la versión que estamos preparando, y compararse
    contra sí misma abortaría siempre."""
    ruta = MANIFIESTO.relative_to(RAIZ).as_posix()
    salida = _salida(["git", "show", f"HEAD:{ruta}"])
    if not salida.strip():
        return None
    try:
        return int(json.loads(salida)["versionCode"])
    except Exception:  # noqa: BLE001
        return None

def verificar_coherencia(vc_declarado: int, apk: Path, reloj: Path, manifiesto: dict) -> None:
    """Cinturón: el versionCode de los APK construidos, el declarado y el del
    manifiesto coinciden; el sha256 del manifiesto es el del APK real; la versión sube
    respecto a la publicada; y la firma sigue siendo la misma (si cambia, ninguna
    instalación existente podrá actualizarse)."""
    for etiqueta, ruta in (("móvil", apk), ("reloj", reloj)):
        vc_apk = version_code_del_apk(ruta)
        if vc_apk != vc_declarado:
            raise SystemExit(
                f"El APK del {etiqueta} tiene versionCode {vc_apk}, pero "
                f"gradle.properties declara {vc_declarado}. Aborto."
            )
    if manifiesto["versionCode"] != vc_declarado:
        raise SystemExit("El versionCode del manifiesto no coincide con el declarado.")
    if manifiesto["sha256"] != sha256(apk):
        raise SystemExit("El sha256 del manifiesto no coincide con el APK construido.")

    publicado = version_code_publicado()
    if publicado is not None and vc_declarado <= publicado:
        raise SystemExit(
            f"El versionCode {vc_declarado} no supera al ya publicado ({publicado}): "
            "nadie detectaría la actualización. Sube appVersionCode. Aborto."
        )

    esperada = (
        FIRMA_ESPERADA.read_text(encoding="utf-8").strip().lower()
        if FIRMA_ESPERADA.is_file() else ""
    )
    for etiqueta, ruta in (("móvil", apk), ("reloj", reloj)):
        huella = huella_firma(ruta)
        if huella is None:
            print(f"Aviso: no pude leer la firma del APK del {etiqueta} (falta apksigner).")
            continue
        if esperada:
            if esperada != huella:
                raise SystemExit(
                    f"La firma del APK del {etiqueta} ha cambiado respecto a la de las "
                    f"versiones ya distribuidas ({esperada[:16]}… → {huella[:16]}…). Con "
                    "otra firma, ninguna instalación existente puede actualizarse. Aborto."
                )
            print(f"Firma del {etiqueta} verificada: {huella[:16]}…")
        else:
            FIRMA_ESPERADA.write_text(huella + "\n", encoding="utf-8")
            esperada = huella
            print(f"Firma registrada por primera vez en {FIRMA_ESPERADA.name}: {huella[:16]}…")


# --- construcción -------------------------------------------------------------

def construir() -> tuple[Path, Path]:
    asegurar_firma()
    _ejecutar([_gradlew(), ":app:assembleRelease", ":wear:assembleRelease"])
    for ruta in (APK_MOVIL, APK_RELOJ):
        if not ruta.is_file():
            raise SystemExit(f"No se generó el APK de release: {ruta}")
    return APK_MOVIL, APK_RELOJ

def preparar(notas: str) -> tuple[dict, Path, Path]:
    """Construye, genera y escribe el manifiesto tras verificar coherencia."""
    vc, vn = leer_version()
    apk, reloj = construir()
    manifiesto = generar_manifiesto(vc, vn, sha256(apk), notas)
    verificar_coherencia(vc, apk, reloj, manifiesto)
    MANIFIESTO.parent.mkdir(parents=True, exist_ok=True)
    MANIFIESTO.write_text(
        json.dumps(manifiesto, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    return manifiesto, apk, reloj


# --- publicación --------------------------------------------------------------

def _asset_con_nombre(apk: Path, vn: str, reloj: bool = False) -> Path:
    destino = apk.with_name(nombre_asset(vn, reloj))
    if destino != apk:
        destino.write_bytes(apk.read_bytes())
    return destino

def verificar_gh() -> None:
    if subprocess.call(["gh", "auth", "status"]) != 0:
        raise SystemExit("gh no está autenticado. Ejecuta: gh auth login")

def publicar(apk: Path, reloj: Path, manifiesto: dict, notas: str) -> None:
    vn = manifiesto["versionName"]
    asset_movil = _asset_con_nombre(apk, vn)
    asset_reloj = _asset_con_nombre(reloj, vn, reloj=True)
    _ejecutar([
        "gh", "release", "create", f"v{vn}", str(asset_movil), str(asset_reloj),
        "--repo", _REPO,
        "--title", f"Building My Future {vn}",
        "--notes", notas or f"Building My Future {vn}.",
    ])
    _ejecutar(["git", "add", str(MANIFIESTO), str(FIRMA_ESPERADA)])
    _ejecutar(["git", "commit", "-m", f"Publica el manifiesto de la v{vn}"])
    _ejecutar(["git", "push", "origin", "main"])

def verificar_url_publica(vc_esperado: int, intentos: int = 30, espera_s: int = 10) -> None:
    """La URL de Pages puede tardar por la caché del CDN: reintenta unos minutos."""
    for i in range(1, intentos + 1):
        try:
            with urllib.request.urlopen(_PAGES_URL, timeout=15) as r:
                data = json.loads(r.read().decode("utf-8"))
            if data.get("versionCode") == vc_esperado:
                print(f"URL pública OK: sirve versionCode {vc_esperado}.")
                return
            print(f"[{i}/{intentos}] Pages sirve {data.get('versionCode')}, esperaba {vc_esperado}…")
        except Exception as e:  # noqa: BLE001
            print(f"[{i}/{intentos}] Aún no disponible ({e.__class__.__name__})…")
        time.sleep(espera_s)
    raise SystemExit(
        "La URL pública no sirvió el versionCode nuevo a tiempo. La Release SÍ se "
        "creó; revisa GitHub Pages (rama main, carpeta /docs) y la caché del CDN."
    )


def asegurar_arbol_limpio() -> None:
    """La Release se etiqueta con el commit actual: lo que no esté commiteado no viaja.

    Publicar con cambios sin commitear deja un tag que no contiene el código que se
    acaba de compilar, y descubrirlo semanas después es imposible.
    """
    salida = subprocess.run(
        ["git", "status", "--porcelain"], cwd=RAIZ, capture_output=True, text=True
    ).stdout.strip()
    if salida:
        sucios = "
".join("  " + l for l in salida.splitlines()[:12])
        raise SystemExit(
            "Hay cambios sin commitear: la Release quedaría etiquetada sin ellos.
"
            f"{sucios}
"
            "Commitea (o guarda en stash) y vuelve a lanzarlo."
        )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Publica una release de Building My Future.")
    parser.add_argument("--dry-run", action="store_true", help="prepara sin publicar")
    parser.add_argument("--notas", default="", help="notas de la versión")
    args = parser.parse_args(argv)

    if not args.dry_run:
        asegurar_arbol_limpio()
        verificar_gh()

    manifiesto, apk, reloj = preparar(args.notas)
    print(f"Manifiesto v{manifiesto['versionName']} "
          f"(versionCode {manifiesto['versionCode']}, sha256 {manifiesto['sha256'][:12]}…)")
    print(f"APK móvil: {apk}")
    print(f"APK reloj: {reloj}")

    if args.dry_run:
        print("--dry-run: preparado sin publicar (Release y manifiesto no subidos).")
        return 0

    publicar(apk, reloj, manifiesto, args.notas)
    verificar_url_publica(manifiesto["versionCode"])
    print(f"Release v{manifiesto['versionName']} publicada y manifiesto en Pages.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
