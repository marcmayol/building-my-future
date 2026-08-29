package com.marc.gymplan100

import android.app.Application
import com.marc.gymplan100.data.PlanStore
import com.marc.gymplan100.update.Updates

class GymApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Deja cargado el plan activo antes que nada: el proceso también arranca desde el
        // servicio del reloj o el aviso de descanso, y allí ya se pregunta por el plan.
        PlanStore.load(this)
        // Comprobación periódica en segundo plano. En la variante de Play no hace nada:
        // allí actualiza la tienda y la app no puede instalarse a sí misma.
        Updates.onAppCreate(this)
    }
}
