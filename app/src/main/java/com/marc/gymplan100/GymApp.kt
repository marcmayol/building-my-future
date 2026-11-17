package com.marc.gymplan100

import android.app.Application
import com.marc.gymplan100.data.Datos
import com.marc.gymplan100.plataforma.plataforma
import com.marc.gymplan100.data.ExerciseCatalog
import com.marc.gymplan100.data.leerAsset
import com.marc.gymplan100.data.load
import com.marc.gymplan100.data.PlanStore
import com.marc.gymplan100.ui.theme.AppLanguage
import com.marc.gymplan100.ui.theme.wrap
import com.marc.gymplan100.update.Updates

class GymApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // La lógica común (:shared) lee sus ficheros de datos por aquí: en Android, los assets.
        Datos.lector = { ruta -> leerAsset(ruta) }
        // Lo que la app compartida pide al sistema (avisos, Health Connect, el reloj…).
        plataforma = PlataformaAndroid(this)
        // Deja cargado el plan activo antes que nada: el proceso también arranca desde el
        // servicio del reloj o el aviso de descanso, y allí ya se pregunta por el plan.
        PlanStore.load(this)
        // El catálogo también: lo que se hace en lugar de un ejercicio saca de ahí su dibujo y
        // sus músculos, y eso se pregunta desde sitios sin contexto (el mapa muscular).
        ExerciseCatalog.load(this)
        // Y en qué idioma se pinta el contenido de serie, por si el proceso arranca sin
        // actividad (un aviso, el reloj).
        AppLanguage.wrap(this)
        // Comprobación periódica en segundo plano. En la variante de Play no hace nada:
        // allí actualiza la tienda y la app no puede instalarse a sí misma.
        Updates.onAppCreate(this)
    }
}
