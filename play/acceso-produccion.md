# Formulario de acceso a producción

Las diez preguntas que hace Play Console al terminar el test cerrado, con la respuesta ya
escrita. Es **el formulario que más pesa** de todo el proceso: se lee a mano.

> **Lo importante de este archivo.** El proveedor de testing mandó estas respuestas ya
> redactadas, y la número 8 daba por hechos cuatro cambios que **todavía no existían**. Google
> comprueba la ficha. Lo que hay aquí abajo es lo que de verdad se ha hecho —los cuatro cambios
> están aplicados en la v2.12— así que se puede firmar sin mentir en nada.

Hay dos respuestas marcadas con ⚠️: son decisiones tuyas, no datos.

---

### 1. ¿Cómo reclutaste a los testers? ¿Amigos y familia, o un proveedor de pago?

```
Con un proveedor de testing de pago (Testers Community), para tener el mínimo de 12 testers
durante 14 días seguidos y, sobre todo, para que la app se probara en móviles y versiones de
Android que yo no tengo. El informe que devolvieron es un documento estructurado, no un "me
funciona bien".
```

⚠️ Si además la ha probado alguien de tu entorno —tu padre lleva usándola meses— dilo aquí:
suma, porque es exactamente el usuario final al que va dirigida.

### 2. ¿Cómo de fácil fue reclutarlos?

```
Fácil
```

### 3. Describe la participación de los testers durante el test cerrado

```
Probaron la app en varios dispositivos y versiones de Android, recorriendo el entrenamiento
guiado de principio a fin: sesión, series, descansos y los avisos con la pantalla apagada. No
apareció ningún fallo ni cierre inesperado. Lo que sí devolvieron fue un informe con cuatro
puntos de mejora, todos sobre cómo se presenta y se explica la app, no sobre su
funcionamiento.
```

### 4. Resume el feedback recibido e indica cómo lo recogiste

```
Se recogió en un informe escrito del proveedor, que probó la app en distintos dispositivos y
versiones de Android.

En lo técnico no encontraron nada: ni cierres inesperados ni errores, y todas las funciones
se comportaron como debían.

Las cuatro mejoras que propusieron fueron: (1) la descripción de la ficha era corta en
palabras clave y difícil de encontrar por búsqueda; (2) no había ningún recorrido inicial que
explicara la app a quien la abre por primera vez; (3) las capturas de la ficha eran capturas
de móvil sin nada que explicara qué se está viendo; y (4) no había forma de valorar la app
desde dentro.
```

### 5. ¿A quién va dirigida la app?

```
A quien va al gimnasio y quiere dejar de apuntar las series en las notas del móvil. Sirve
igual a quien empieza —trae un plan de 100 días que lleva de la mano— que a quien ya entrena
y solo quiere un registro de pesas que le diga cuándo subir la carga. También a quien entrena
en casa, porque el plan puede ser el suyo.
```

### 6. ¿Qué valor aporta la app a sus usuarios?

```
Guía la sesión en vivo —calentamiento, series, descansos con aviso incluso con la pantalla
apagada— y lleva el registro de todo lo que se levanta. Con eso hace lo que casi nadie hace
solo: decir cuándo toca subir el peso, con el salto que de verdad existe en la máquina, y
cuándo conviene bajarlo tras varias sesiones atascado.

Todo funciona dentro del teléfono. No hay cuentas, ni servidor, ni anuncios, ni analítica: la
variante que se publica en Play ni siquiera pide permiso de internet, y el usuario se lleva
sus datos con la copia de seguridad que trae la propia app.
```

### 7. ¿Cuántas instalaciones esperas el primer año?

⚠️ **La respuesta del proveedor decía «10k - 100k». Es mentira y no ayuda.** Una app sin
publicidad ni empresa detrás no hace eso, y una cifra inflada aquí no da puntos: lo que se
valora es que el resto de respuestas sean creíbles.

```
1k - 10k
```

### 8. ¿Qué cambios hiciste a raíz del test cerrado?

```
Los cuatro que salieron en el informe, y están en la versión 2.12:

1. Reescribí la ficha para que se pueda encontrar: el título ahora dice a qué juega la app, y
   la descripción empieza por lo que hace, con las palabras que la gente escribe de verdad al
   buscar.
2. Añadí un recorrido inicial de cinco pasos que sale la primera vez, después de elegir plan.
   Cada paso dice qué hace la app y, sobre todo, dónde está esa pantalla. Se puede saltar en
   cualquier momento y volver a ver desde Ajustes siempre que se quiera.
3. Rehice las capturas de la ficha: cada una lleva escrito qué se está viendo. De paso
   descubrí que las anteriores tenían una relación de 2,22:1, por encima del máximo de 2:1
   que admite Play.
4. Añadí "Valorar la app" en Ajustes, que lleva a la ficha de la tienda, y la valoración
   dentro de la app —la API oficial de Play— después de terminar un entrenamiento, que es el
   único momento en que la pregunta tiene sentido. Nunca antes del tercer entreno, y si se
   pregunta una vez no se vuelve a preguntar en cuatro meses.
```

### 9. ¿Cómo decidiste que la app está lista para producción?

```
Porque el test cerrado no encontró ningún fallo de funcionamiento en ningún dispositivo, y lo
único que quedaba pendiente era cómo se explica la app: eso es lo que se ha corregido. La app
lleva meses en uso real y diario —incluyendo el entrenamiento completo de un plan de 100
días—, tiene tests automáticos sobre las partes que hacen cuentas (progresión de cargas,
fuerza estimada, calendario de avisos) y sus datos son recuperables mediante copia de
seguridad, así que nadie pierde nada al cambiar de móvil.
```

### 10. ¿Qué has hecho diferente esta vez?

```
Tomarme el feedback como trabajo pendiente y no como una lista de sugerencias: los cuatro
puntos del informe están hechos y se pueden comprobar en la ficha y en la propia app. Y
resolverlos sin romper lo que hace distinta a la app: sigue sin cuentas, sin anuncios y sin
conexión, y la valoración se pide después de entrenar en lugar de interrumpir a alguien que
todavía no ha usado nada.
```

---

## Antes de enviarlo

- [ ] La ficha de Play tiene ya el título, la descripción y las capturas nuevas (`ficha.md`
      y `capturas-ficha/`). Si el formulario dice que se cambiaron y la ficha sigue con lo
      viejo, es peor que no haber contestado.
- [ ] La versión subida al track es la **2.12** o posterior: es la que lleva los cambios que
      describe la pregunta 8.
- [ ] Las dos respuestas con ⚠️ decididas por ti.
