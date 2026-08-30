# Ficha de Google Play

Todo lo que hay que pegar en Play Console, ya escrito. Los gráficos están en `graficos/`.

---

## Nombre de la app *(máx. 30)*

```
Building My Future
```

## Descripción corta *(máx. 80)*

```
Te guía la sesión y te dice cuándo subir el peso. Sin cuentas ni anuncios.
```

## Descripción completa *(máx. 4000)*

```
Building My Future no es una lista de ejercicios que vas tachando: te lleva la sesión en vivo y aprende de lo que levantas.

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

LO QUE NO VE UN EJERCICIO SUELTO
• Mapa muscular: qué trabaja cada ejercicio y, sobre todo, qué se ha llevado cada músculo esta semana. Es lo único que dice si vas compensado o llevas tres semanas sin tocar pierna.
• Fuerza estimada (1RM), porque los kilos a secas engañan: 40 × 12 y 50 × 5 son casi el mismo esfuerzo.
• Rachas, récords, kilos movidos y un mapa de calor de tu constancia.

EL PLAN ES TUYO
• Viene con un reto de 100 días y once planes más, pero puedes traerte el tuyo escrito en Markdown o JSON, o crearlo dentro de la app.
• Cada plan guarda su progreso por separado: alternas entre ellos sin perder nada.
• Recordatorios: dile qué días sueles entrenar y a qué hora, y te avisa con el día que toca. Si ya has entrenado ese día, no te molesta.

EN LA MUÑECA
• Con Wear OS marcas la serie desde el reloj, con el móvil en la taquilla.
• Se integra con Health Connect: cada entreno se guarda con su duración y el detalle de ejercicios y pesos.

TUS DATOS SON TUYOS
• No hay cuentas, ni registro, ni servidor, ni anuncios, ni analítica.
• Todo se queda en tu teléfono, y la app trae su propia copia de seguridad: guardas un archivo donde quieras y lo restauras en otro móvil tal y como estaba.

Las ilustraciones de los ejercicios son del set libre everkinetic (CC BY-SA 4.0).
```

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

## Antes de subir

1. **Compilar el sabor `play`**, nunca el `directo`:
   `gradlew.bat :app:bundlePlayRelease` (Play prefiere AAB) y `:wear:assemblePlayRelease` si se sube el reloj.
2. **Play App Signing**: el keystore actual (`building-my-future-release.jks`) pasa a ser la *upload key*. Guardarlo como oro.
3. **La app del reloj va aparte** y con el mismo applicationId (`com.marcmayol.buildingmyfuture`); si no, la Data Layer no las empareja.
4. Si la cuenta es personal y nueva: **12 testers durante 14 días seguidos** en test cerrado antes de poder pasar a producción.
