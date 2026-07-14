package com.marc.gymplan100.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.ui.theme.GymPlanTheme

/**
 * Pantalla que Health Connect abre cuando el usuario pulsa "Política de privacidad" o
 * "¿Por qué se necesita este permiso?". Explica qué datos escribimos y por qué.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymPlanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RationaleContent()
                }
            }
        }
    }
}

@Composable
private fun RationaleContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            "Building My Future y Health Connect",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Cuando terminas un entrenamiento en la app, guardamos esa sesión en Health Connect " +
                "(la plataforma de Google Health) para que tus entrenos aparezcan junto al resto " +
                "de tu actividad física.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            "Qué escribimos",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            "• La sesión de ejercicio (tipo fuerza) con su fecha y duración.\n" +
                "• El detalle de cada ejercicio y los pesos usados, en las notas de la sesión.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "La app solo escribe estos datos: nunca lee tu información de salud. Puedes revocar " +
                "el permiso en cualquier momento desde los ajustes de Health Connect.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}
