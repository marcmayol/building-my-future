package com.marc.gymplan100

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.marc.gymplan100.ui.GymNavHost
import com.marc.gymplan100.ui.theme.GymPlanTheme

class MainActivity : ComponentActivity() {

    companion object {
        /** Día de la sesión a abrir directamente (deep-link desde la notificación de la cuenta atrás). */
        const val EXTRA_OPEN_SESSION_DAY = "open_session_day"
    }

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** Día a abrir solicitado por la notificación; lo consume GymNavHost al navegar. */
    private var openSessionDay by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        readSessionExtra(intent)
        setContent {
            GymPlanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GymNavHost(
                        openSessionDay = openSessionDay,
                        onSessionConsumed = { openSessionDay = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readSessionExtra(intent)
    }

    private fun readSessionExtra(intent: Intent?) {
        val day = intent?.getIntExtra(EXTRA_OPEN_SESSION_DAY, -1) ?: -1
        if (day > 0) openSessionDay = day
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
