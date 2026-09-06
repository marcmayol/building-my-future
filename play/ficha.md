# Ficha de Google Play

Todo lo que hay que pegar en Play Console, ya escrito. Los gráficos están en `graficos/`.

---

## Nombre de la app *(máx. 30)*

```
Building My Future: Gimnasio
```

> **Por qué el sufijo.** «Building My Future» a secas no dice a qué juega y no lo busca nadie.
> Play indexa el título con mucho peso, así que la palabra que la gente escribe de verdad
> —*gimnasio*— tiene que estar ahí. 28 de 30 caracteres.

## Descripción corta *(máx. 80)*

```
Gimnasio guiado: rutinas, pesas y cuándo subir el peso. Sin anuncios.
```

> 69 caracteres, con margen: Play cuenta los acentos y no conviene ir al límite. Mete
> *gimnasio*, *rutinas*, *pesas* y *peso* sin sonar a lista de palabras.

## Descripción completa *(máx. 4000)*

```
Un entrenador de gimnasio en el bolsillo: Building My Future te guía la rutina en vivo, te lleva el registro de pesas y series, y te dice cuándo subir el peso. Sin cuentas, sin nube, sin anuncios y sin conexión: funciona entero dentro del móvil.

No es una lista de ejercicios que vas tachando. Te lleva la sesión y aprende de lo que levantas.

TE GUÍA MIENTRAS ENTRENAS
• El descanso avisa con la pantalla apagada. Suena y vibra aunque el móvil esté en el bolsillo, y la cuenta atrás se ve en la pantalla de bloqueo con un botón para pasar a la siguiente serie sin desbloquear.
• Cada serie con su peso y sus repeticiones, que vienen puestas desde el plan: solo las tocas el día que no salen.
• Superseries y circuitos de verdad: los ejercicios encadenados van uno detrás de otro y el descanso cae al acabar la vuelta, cada uno con su cuenta atrás.
• «Máquina ocupada»: manda ese ejercicio al final y sigues con el siguiente sin perder el sitio.
• Deja la máquina lista: durante el descanso preparas el peso de la próxima serie.

TE DICE CUÁNDO SUBIR EL PESO, Y POR QUÉ
• Cuando cierras todas las series arriba del rango, te propone más carga, con el salto que de verdad existe en la máquina, y te escribe el motivo debajo.
• Y te dice cuándo bajar: si llevas tres sesiones atascado en un ejercicio, propone un 10 % menos para volver a subir desde ahí. Es la mitad que casi nadie hace solo.
• Las series de calentamiento se apuntan pero no cuentan, para que no ensucien tus números.
• Esa progresión de cargas es la diferencia entre ir al gimnasio y entrenar: sin ella se levanta el mismo peso durante meses.

TU DIARIO DE ENTRENAMIENTO, SIN ESCRIBIRLO
• Cada sesión queda guardada con sus ejercicios, sus pesos y sus repeticiones, serie a serie.
• Mapa muscular: qué trabaja cada ejercicio y, sobre todo, qué se ha llevado cada músculo esta semana. Es lo único que dice si vas compensado o llevas tres semanas sin tocar pierna.
• Fuerza estimada (1RM), porque los kilos a secas engañan: 40 × 12 y 50 × 5 son casi el mismo esfuerzo.
• Rachas, récords, kilos movidos y un mapa de calor de tu constancia.

EL PLAN ES TUYO
• Viene con un reto de 100 días y once rutinas más —fuerza, hipertrofia, cuerpo entero, torso/pierna, en casa—, pero puedes traerte la tuya escrita en Markdown o JSON, o crearla dentro de la app.
• Cada plan guarda su progreso por separado: alternas entre ellos sin perder nada.
• Recordatorios: dile qué días sueles entrenar y a qué hora, y te avisa con el día que toca. Si ya has entrenado ese día, no te molesta.

EN LA MUÑECA
• Con Wear OS marcas la serie desde el reloj, con el móvil en la taquilla.
• Se integra con Health Connect: cada entreno se guarda con su duración y el detalle de ejercicios y pesos.

TUS DATOS SON TUYOS
• No hay cuentas, ni registro, ni servidor, ni anuncios, ni analítica. Ni siquiera pide permiso de internet.
• Todo se queda en tu teléfono, y la app trae su propia copia de seguridad: guardas un archivo donde quieras y lo restauras en otro móvil tal y como estaba.

PARA QUIÉN ES
Para quien va al gimnasio y quiere dejar de apuntar las series en las notas del móvil. Sirve igual si empiezas —el plan de 100 días te lleva de la mano— que si ya entrenas y solo quieres un registro de pesas que te diga cuándo progresar. También en casa, con mancuernas o con tu propio peso.

Las ilustraciones de los ejercicios son del set libre everkinetic (CC BY-SA 4.0).
```

> **Qué cambió para el ASO.** El primer párrafo es nuevo y es el que más pesa: es lo que se ve
> sin desplegar y lo que Play indexa con más fuerza. Lleva *gimnasio*, *rutina*, *registro de
> pesas*, *series*, *peso* y *entrenador* en dos frases que se leen como una frase, no como una
> lista de palabras. Se añadieron dos encabezados con intención de búsqueda («diario de
> entrenamiento», «para quién es»), los nombres de los tipos de rutina —*fuerza*, *hipertrofia*,
> *cuerpo entero*, *torso/pierna*, *en casa*— y *progresión de cargas*, que es la búsqueda de
> quien ya entrena. Nada de repetir palabras porque sí: Play penaliza el amontonamiento.

---

## Datos de la ficha

| Campo | Valor |
|---|---|
| Categoría | Salud y bienestar *(o Deportes)* |
| Etiquetas | entrenamiento, gimnasio, fuerza, rutinas |
| Correo de contacto | marcmayolorell@gmail.com |
| Web | https://marcmayol.com/building-my-future/ |
| Política de privacidad | https://marcmayol.com/building-my-future/privacidad.html |
| Clasificación | Apta para todos (no hay contenido sensible) |
| Anuncios | No |
| Compras en la app | No |

## Seguridad de los datos *(el formulario)*

- **¿Recoge o comparte datos?** → **No.** La app no envía nada a ningún servidor: la variante de Play ni siquiera pide permiso de internet.
- Los datos de salud que escribe en Health Connect **no salen del dispositivo**; los gestiona Health Connect, no la app.
- **¿Cifra los datos en tránsito?** No aplica: no hay tránsito.
- **¿Se pueden borrar?** Sí, desinstalando o borrando los datos de la app.

## Health Connect

Hay que rellenar la declaración aparte. Lo que hace la app:

- **Escribe**: sesión de ejercicio (tipo fuerza, con duración y una nota con ejercicios y pesos) y calorías activas estimadas.
- **Lee**: el peso corporal más reciente, solo al pulsar «Importar de Google Health», para rellenar el perfil.
- **No** comparte esos datos con nadie ni los envía fuera del dispositivo.

---

## Una sola ficha, dos artefactos

Wear OS **no lleva ficha aparte**: el móvil y el reloj van en la MISMA app de Play y en la
misma versión. Quien busque la app desde el reloj la verá porque el artefacto del reloj
declara `uses-feature android.hardware.type.watch`.

Para que Play los acepte juntos hacen falta tres cosas, y las tres están ya:

| | |
|---|---|
| Mismo `applicationId` | `com.marcmayol.buildingmyfuture` en los dos |
| `versionCode` **distinto** por artefacto | móvil `N`, reloj `N + 10000` (31 y 10031) |
| El reloj declarado como dependiente | `com.google.android.wearable.standalone = false` |

Ese `standalone=false` es importante y es verdad: el móvil es el cerebro y el reloj es el
mando. Si dijera `true`, Play ofrecería la app a relojes sin el teléfono y no funcionaría.

```
gradlew.bat :app:bundlePlayRelease     -> app/build/outputs/bundle/playRelease/app-play-release.aab
gradlew.bat :wear:bundlePlayRelease    -> wear/build/outputs/bundle/playRelease/wear-play-release.aab
```

Los dos se suben a la **misma release** del mismo track.

> **Mira siempre qué hay subido antes de numerar.** El `versionCode` solo puede subir, y el
> historial no es correlativo: a 5-sep-2026 el explorador de app bundles tiene **30** y **28**
> del móvil y **10029** y **10028** del reloj. Un 29 se habría rechazado dos veces —el móvil
> por debajo del 30, y el reloj chocando con un 10029 que ya existe—, así que la 2.12 va con
> **31 / 10031**. Saltar números no cuesta nada; repetirlos, una subida perdida.

## Antes de subir

1. **Compilar el sabor `play`**, nunca el `directo`: el directo lleva el auto-actualizador y
   eso es rechazo seguro.
2. **Subir los DOS bundles a la misma release**: el del móvil y el del reloj. No son dos apps
   ni dos fichas (ver arriba).
3. **Play App Signing**: el keystore actual (`building-my-future-release.jks`) pasa a ser la
   *upload key*. Guardarlo como oro: sin él no se puede volver a subir nada.
4. Si la cuenta es personal y nueva: **12 testers durante 14 días seguidos** en test cerrado
   antes de poder pasar a producción.
5. Ese periodo de prueba es buen momento para **migrar tu móvil y el de tu padre**: guardar
   copia desde Ajustes, instalar la de Play, restaurar. Hasta entonces, la app directa sigue
   funcionando y actualizándose como siempre.
