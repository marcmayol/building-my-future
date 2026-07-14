@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.data.ExerciseGuides
import com.marc.gymplan100.data.ExerciseImages
import java.net.URLEncoder

/**
 * Panel deslizable con la ficha del ejercicio: imagen, técnica, errores y alternativas.
 * Se abre desde la pantalla de la serie para consultar en mitad del entreno.
 */
@Composable
fun ExerciseGuideSheet(
    exerciseName: String,
    scheme: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val guide = ExerciseGuides.forName(exerciseName)
    val imageRes = ExerciseImages.forName(exerciseName)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                exerciseName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                scheme,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (imageRes != null) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = exerciseName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                )
            }

            // Vídeo: abre una búsqueda en YouTube del movimiento (devuelve shorts y tutoriales).
            // Más robusto que un enlace fijo (los vídeos se borran/privatizan).
            val context = LocalContext.current
            val videoQuery = guide?.videoQuery?.takeIf { it.isNotBlank() } ?: "$exerciseName técnica"
            FilledTonalButton(
                onClick = {
                    val url = "https://www.youtube.com/results?search_query=" +
                        URLEncoder.encode(videoQuery, "UTF-8")
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("  Ver vídeo en YouTube")
            }

            if (guide == null) {
                Text(
                    "Todavía no hay ficha para este ejercicio. Fíjate en la imagen y haz el " +
                        "movimiento controlando el peso, con la espalda neutra y sin impulso.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                return@Column
            }

            GuideSection("Músculos que trabaja") {
                Text(guide.muscles, style = MaterialTheme.typography.bodyMedium)
            }

            GuideSection("Cómo se hace") {
                Text(guide.howTo, style = MaterialTheme.typography.bodyMedium)
            }

            GuideSection("Errores típicos a evitar") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    guide.mistakes.forEach { Bullet(it) }
                }
            }

            GuideSection("Si está ocupada o no la tienes") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    guide.alternatives.forEach { Bullet(it) }
                }
            }
        }
    }
}

@Composable
private fun GuideSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
