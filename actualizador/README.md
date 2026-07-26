# Módulo `actualizador`

Sistema de auto-actualización para apps Android distribuidas **fuera de Play Store**
(APK firmado en GitHub Releases). Reutilizable: solo conoce una URL de manifiesto.

## Cómo funciona

1. La app publica un manifiesto `updates.json` en una URL pública (GitHub Pages).
2. El módulo lo comprueba **al abrir**, **periódicamente** (WorkManager) y **a mano**
   (Ajustes), comparando **por `versionCode` entero** (`remoto > actual`).
3. Si hay novedad, descarga el APK a almacenamiento privado, **verifica el SHA-256**
   contra el manifiesto y, solo si coincide, instala por `PackageInstaller`.

Tolerancia a fallos: en las comprobaciones **automáticas** cualquier error (sin red,
JSON roto, HTTP ≠ 200…) muere en silencio. Solo la comprobación **manual** informa.

## Contrato del manifiesto (`updates.json`)

```json
{
  "versionCode": 5,
  "versionName": "1.4",
  "url": "https://github.com/<owner>/<repo>/releases/download/v1.4/app-v1.4.apk",
  "sha256": "<64 hex>",
  "notas": "Qué cambia…",
  "check_horas": 24
}
```

- `versionCode` **entero** obligatorio; la comparación es entera, nunca por strings.
- `sha256`: 64 hex; se verifica **antes** de instalar. Hash incorrecto ⇒ se borra el
  APK y jamás se instala.
- `url`: descarga directa del asset de la Release (https).
- `check_horas`: cadencia de la comprobación periódica.

## API pública

```kotlin
val actualizador = Actualizador(
    app = this,                              // Application
    config = ActualizadorConfig(
        manifiestoUrl = "https://…/updates.json",
        versionCodeActual = BuildConfig.VERSION_CODE,
        checkHorasPorDefecto = 24,
    ),
)

actualizador.programarPeriodica()            // WorkManager (llamar en Application.onCreate)
actualizador.comprobar(Modo.AUTOMATICO)      // al abrir (suspend); silenciosa
actualizador.comprobar(Modo.MANUAL)          // desde Ajustes; informa
actualizador.actualizarAhora()               // permiso → descarga → verifica → instala
actualizador.onPermisoQuizaConcedido()       // en onResume, tras conceder el permiso
val estado: StateFlow<EstadoActualizacion> = actualizador.estado   // para el banner/UI
var buscar = actualizador.buscarAutomaticamente                    // ajuste (default ON)
```

### Estados (`EstadoActualizacion`)

`Inactivo`, `Comprobando`, `AlDia`, `Disponible(info)`, `Descargando(porcentaje)`,
`Verificando`, `PidiendoPermiso`, `Instalando`, `Error(tipo, mensaje)`.

## Flujo de instalación y reanudación

`Permiso → Descarga (.part) → Verificación SHA → renombra a .apk → Sesión PackageInstaller.`

- **Permiso**: `canRequestPackageInstalls()`; si falta, se manda a
  `ACTION_MANAGE_UNKNOWN_APP_SOURCES` (una vez) y se reanuda en `onPermisoQuizaConcedido`.
- **Descarga**: a `cacheDir/actualizaciones/<vc>.apk.part`; si el proceso muere, el
  `.part` se re-descarga de cero (el `.apk` definitivo solo existe tras verificar).
  Un `<vc>.apk` ya verificado **se reutiliza** en lugar de volver a bajarlo, y lo que
  no pertenece a la versión en curso se borra al arrancar (`CacheDescargas`).
- **Instalación**: si la app es el *installer of record* de sí misma (API 31+), la
  sesión pide `USER_ACTION_NOT_REQUIRED` y la actualización se aplica en silencio; si
  no (primera vez), el sistema muestra su confirmación. Traducción: **la 1ª
  auto-actualización pide confirmar; a partir de ahí, silenciosas** (API 31+).
  Requiere el permiso `UPDATE_PACKAGES_WITHOUT_USER_ACTION`: sin él Android ignora la
  petición sin avisar y pide confirmación siempre.
- **Play Protect**: la primera instalación de una firma sin reputación se bloquea con
  «Aplicación bloqueada para proteger tu dispositivo»; hay que entrar en *Más
  detalles → Instalar de todas formas*. Ocurre una sola vez por dispositivo y no
  depende de este módulo.
- **Respaldo**: si la sesión de `PackageInstaller` no se puede crear o confirmar, se
  cae al intent clásico de instalación con `FileProvider` (`Instalador.Via.INTENT`).
  Esa vía no devuelve resultado, así que el estado `Instalando` se deshace en el
  siguiente `onPermisoQuizaConcedido()`; si la instalación sí ocurrió, el proceso se
  reinicia y el `init` limpia el estado al comparar `versionCode`.

## Requisitos de la app anfitriona

- `buildFeatures { buildConfig = true }` (para `BuildConfig.VERSION_CODE`).
- Registrar el receiver del resultado de instalación y el `FileProvider` del respaldo:
  ambos se declaran en el manifest del módulo y se mergean solos (el authority es
  `${applicationId}.actualizador.fileprovider`).
- Distribuir un **APK release firmado con una keystore estable** (misma firma en todas
  las versiones; si cambia, `PackageInstaller` no puede actualizar).

## Testing

Todo lo testeable sin dispositivo está cubierto en `src/test` (25 tests JVM, sin red
real: `Transport` y `AbridorConexion` son interfaces funcionales y los tests inyectan
dobles en memoria): comparación de versiones, parseo del manifiesto (versión
mayor/igual, JSON roto, HTTP ≠ 200, sin red), verificación SHA (hash
correcto/incorrecto), descarga con progreso y borrado ante error, limpieza de la
caché de descargas, y las decisiones "auto calla / manual informa".
`Instalador` y `ComprobacionWorker` dependen del framework y se validan en dispositivo.

```bash
gradlew.bat :actualizador:testDebugUnitTest
```
