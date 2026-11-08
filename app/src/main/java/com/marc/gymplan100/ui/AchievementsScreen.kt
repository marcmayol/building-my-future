@file:OptIn(ExperimentalMaterial3Api::class)

package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.R
import com.marc.gymplan100.data.Achievement
import com.marc.gymplan100.data.Achievements
import com.marc.gymplan100.data.PlanData
import com.marc.gymplan100.ui.theme.LocalAppColors
import com.marc.gymplan100.ui.theme.LocalAppTextStyles
import com.marc.gymplan100.ui.theme.Space

/**
 * Logros.
 *
 * Aquí los emoji se quedan: en la navegación eran ruido, pero en un logro son el contenido
 * celebratorio. Los bloqueados no se esconden, se apagan y dicen cuánto falta.
 */
@Composable
fun AchievementsScreen(
    viewModel: PlanViewModel
) {
    val progress by viewModel.progress.collectAsState()
    val unlocked = Achievements.unlockedIds(progress)
    val all = Achievements.all
    val done = progress.completedDays.count { it in 1..PlanData.TOTAL_DAYS }
    val next = all.firstOrNull { it.id !in unlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ayu_logros_titulo), style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Space.screen,
                end = Space.screen,
                top = inner.calculateTopPadding() + Space.x2,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Space.x2)
        ) {
            item {
                AchievementsHero(
                    unlocked = unlocked.size,
                    total = all.size,
                    next = next,
                    doneDays = done
                )
                Spacer(Modifier.height(Space.x3))
            }

            items(all, key = { it.id }) { achievement ->
                AchievementRow(
                    achievement = achievement,
                    unlocked = achievement.id in unlocked,
                    doneDays = done
                )
            }

            item {
                Spacer(Modifier.height(Space.x3))
                Text(
                    stringResource(R.string.ayu_logros_nota),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AchievementsHero(unlocked: Int, total: Int, next: Achievement?, doneDays: Int) {
    val app = LocalAppColors.current
    val styles = LocalAppTextStyles.current
    val dark = isSystemInDarkTheme()
    val onHero = if (dark) Color(0xFF3D0B02) else MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                if (dark) app.brandGradient
                else androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.primaryContainer
                )
            )
            .padding(Space.screen)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$unlocked", style = styles.bigNumber, color = onHero)
            Spacer(Modifier.width(Space.x2))
            Text(
                pluralStringResource(R.plurals.ayu_logros_de_total, total, total),
                style = MaterialTheme.typography.titleLarge,
                color = onHero,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (next != null) {
            Spacer(Modifier.height(Space.x2))
            val missing = (next.atDay - doneDays).coerceAtLeast(0)
            val nextTitle = achievementTitle(next).lowercase()
            Text(
                if (missing > 0)
                    pluralStringResource(R.plurals.ayu_logros_siguiente_en, missing, missing, nextTitle)
                else stringResource(R.string.ayu_logros_siguiente_un_entreno, nextTitle),
                style = MaterialTheme.typography.bodyMedium,
                color = onHero
            )
        } else {
            Spacer(Modifier.height(Space.x2))
            Text(
                stringResource(R.string.ayu_logros_todos),
                style = MaterialTheme.typography.bodyMedium,
                color = onHero
            )
        }
    }
}

@Composable
private fun AchievementRow(achievement: Achievement, unlocked: Boolean, doneDays: Int) {
    val missing = (achievement.atDay - doneDays).coerceAtLeast(0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (unlocked) 1f else 0.45f)
            .padding(vertical = Space.x3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (unlocked) achievement.emoji else "🔒",
            fontSize = 28.sp
        )
        Spacer(Modifier.width(Space.x4))
        Column(modifier = Modifier.weight(1f)) {
            Text(achievementTitle(achievement), style = MaterialTheme.typography.titleMedium)
            Text(
                if (!unlocked && missing > 0)
                    pluralStringResource(R.plurals.ayu_logros_te_faltan, missing, missing)
                else achievementDescription(achievement),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Título de un logro en el idioma en uso, a partir de su id. */
@Composable
internal fun achievementTitle(a: Achievement): String = when (a.id) {
    "first" -> stringResource(R.string.ayu_logro_first_title)
    "week" -> stringResource(R.string.ayu_logro_week_title)
    "ten" -> stringResource(R.string.ayu_logro_ten_title)
    "quarter" -> stringResource(R.string.ayu_logro_quarter_title)
    "half" -> stringResource(R.string.ayu_logro_half_title)
    "threequarter" -> stringResource(R.string.ayu_logro_threequarter_title)
    "complete" -> stringResource(R.string.ayu_logro_complete_title)
    else -> stringResource(R.string.ayu_logro_phase_title, a.phaseName)
}

/** Descripción de un logro en el idioma en uso, a partir de su id. */
@Composable
internal fun achievementDescription(a: Achievement): String = when {
    a.id == "first" -> stringResource(R.string.ayu_logro_first_desc)
    a.id == "complete" -> pluralStringResource(R.plurals.ayu_logro_complete_desc, a.atDay, a.atDay)
    a.id.startsWith("phase") -> stringResource(R.string.ayu_logro_phase_desc, a.phaseNumber)
    else -> pluralStringResource(R.plurals.ayu_logro_desc_days, a.atDay, a.atDay)
}
