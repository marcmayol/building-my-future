package com.marc.gymplan100

import android.app.Application
import com.marc.gymplan100.data.ExerciseCatalog
import com.marc.gymplan100.data.PlanStore
import com.marc.gymplan100.update.Updates

class GymApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Deja cargado el plan activo antes que nada: el proceso también arranca desde el
        // servicio del reloj o el aviso de descanso, y allí ya se pregunta por el plan.
        PlanStore.load(this)
        // El catálogo también: lo que se hace en lugar de un ejercicio saca de ahí su dibujo y
        // sus músculos, y eso se pregunta desde sitios sin contexto (el mapa muscular).
        ExerciseCatalog.load(this)
        // Comprobación periódica en segundo plano. En la variante de Play no hace nada:
        // allí actualiza la tienda y la app no puede instalarse a sí misma.
        Updates.onAppCreate(this)
    }
}
