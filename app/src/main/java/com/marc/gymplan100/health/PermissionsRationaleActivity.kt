package com.marc.gymplan100.health

import android.content.Context
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.R
import com.marc.gymplan100.ui.theme.AppLanguage
import com.marc.gymplan100.ui.theme.GymPlanTheme

/**
 * Pantalla que Health Connect abre cuando el usuario pulsa "Política de privacidad" o
 * "¿Por qué se necesita este permiso?". Explica qué datos escribimos y por qué.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    // La abre Health Connect, no la app: sin esto saldría en el idioma del móvil y no en el
    // elegido en Ajustes.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

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
            stringResource(R.string.ini_health_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            stringResource(R.string.ini_health_intro),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            stringResource(R.string.ini_health_what),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            stringResource(R.string.ini_health_what_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            stringResource(R.string.ini_health_only_write),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}
