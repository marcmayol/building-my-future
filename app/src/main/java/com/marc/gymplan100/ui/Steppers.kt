package com.marc.gymplan100.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.Touch
import kotlinx.coroutines.delay

/**
 * Botón redondo de un stepper (± peso, ± semanas, ± series).
 *
 * Mantener pulsado repite: subir de 20 a 60 kg de 2,5 en 2,5 son 16 toques, y en el gimnasio
 * eso no lo hace nadie.
 */
@Composable
fun StepperButton(
    symbol: String,
    onStep: () -> Unit,
    enabled: Boolean = true,
    size: Dp = Touch.stepper,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val step by rememberUpdatedState(onStep)
    val outline = LocalAppColors.current.onTintOutline

    LaunchedEffect(pressed, enabled) {
        if (pressed && enabled) {
            delay(400)
            while (true) {
                step()
                delay(120)
            }
        }
    }

    OutlinedButton(
        onClick = onStep,
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.5.dp, outline.copy(alpha = if (enabled) 1f else 0.38f)),
        contentPadding = PaddingValues(0.dp),
        interactionSource = interaction,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.size(size)
    ) {
        Text(symbol, style = MaterialTheme.typography.headlineSmall)
    }
}
