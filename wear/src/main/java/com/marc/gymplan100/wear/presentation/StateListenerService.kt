package com.marc.gymplan100.wear.presentation

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Mantiene el chip de la esfera al día con lo que publica el móvil en la Data Layer.
 *
 * Va aparte de la pantalla a propósito (mismo motivo que [AlertListenerService]): el chip tiene
 * que aparecer y actualizarse aunque la app del reloj no esté abierta, y el sistema despierta un
 * WearableListenerService en cada cambio de estado.
 */
class StateListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        try {
            for (event in events) {
                val item = event.dataItem
                if (item.uri.path != PATH_STATE) continue
                OngoingSession.update(this, parseState(DataMapItem.fromDataItem(item).dataMap))
            }
        } finally {
            events.release()
        }
    }
}
