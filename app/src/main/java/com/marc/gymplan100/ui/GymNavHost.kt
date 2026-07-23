package com.marc.gymplan100.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.marc.gymplan100.PlanViewModel

object Routes {
    const val HOME = "home"
    const val PHASE = "phase/{n}"
    const val DAY = "day/{n}"
    const val SESSION = "session/{n}"
    const val ACHIEVEMENTS = "achievements"
    const val WEIGHTS = "weights"
    const val RESULTS = "results"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val SPECIAL = "special"
    const val FATBURN = "fatburn"
    const val FATBURN_EX = "fatburn/{id}"
    const val ROUTINE = "routine"
    fun phase(n: Int) = "phase/$n"
    fun day(n: Int) = "day/$n"
    fun session(n: Int) = "session/$n"
    fun fatburnEx(id: String) = "fatburn/$id"
}

/** Género del perfil, para elegir la ilustración del ejercicio (true = mujer). */
val LocalIsFemale = staticCompositionLocalOf { false }

@Composable
fun GymNavHost(
    openSessionDay: Int? = null,
    onSessionConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val viewModel: PlanViewModel = viewModel()
    val celebration by viewModel.celebration.collectAsState()
    val profile by viewModel.profile.collectAsState()

    // Deep-link desde la notificación de la cuenta atrás: abre la sesión en curso. Si es una
    // rutina especial, abre su pantalla de rutina en vez de la sesión normal del plan.
    LaunchedEffect(openSessionDay) {
        if (openSessionDay != null && openSessionDay > 0) {
            if (viewModel.activeSession.value?.isRoutine == true) {
                navController.navigate(Routes.ROUTINE)
            } else {
                navController.navigate(Routes.session(openSessionDay))
            }
            onSessionConsumed()
        }
    }

    CompositionLocalProvider(LocalIsFemale provides (profile.gender == "Mujer")) {
    Box {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onOpenPhase = { navController.navigate(Routes.phase(it)) },
                onOpenDay = { navController.navigate(Routes.day(it)) },
                onResumeSession = { day ->
                    if (viewModel.activeSession.value?.isRoutine == true) {
                        navController.navigate(Routes.ROUTINE)
                    } else {
                        navController.navigate(Routes.session(day))
                    }
                },
                onStartExtra = {
                    val refDay = viewModel.startExtraSession()
                    navController.navigate(Routes.session(refDay))
                },
                onOpenSpecial = { navController.navigate(Routes.SPECIAL) },
                onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                onOpenWeights = { navController.navigate(Routes.WEIGHTS) },
                onOpenResults = { navController.navigate(Routes.RESULTS) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.RESULTS) {
            ResultsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.STATS) {
            StatisticsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.WEIGHTS) {
            ExerciseWeightsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.PHASE,
            arguments = listOf(navArgument("n") { type = NavType.IntType })
        ) { entry ->
            val n = entry.arguments?.getInt("n") ?: 1
            PhaseScreen(
                phaseNumber = n,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenDay = { navController.navigate(Routes.day(it)) }
            )
        }
        composable(
            route = Routes.DAY,
            arguments = listOf(navArgument("n") { type = NavType.IntType })
        ) { entry ->
            val n = entry.arguments?.getInt("n") ?: 1
            DayScreen(
                dayNumber = n,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onStartSession = { navController.navigate(Routes.session(it)) }
            )
        }
        composable(
            route = Routes.SESSION,
            arguments = listOf(navArgument("n") { type = NavType.IntType })
        ) { entry ->
            val n = entry.arguments?.getInt("n") ?: 1
            WorkoutSessionScreen(
                dayNumber = n,
                viewModel = viewModel,
                onExit = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }
        composable(Routes.SPECIAL) {
            SpecialMenuScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onFreeWorkout = {
                    val refDay = viewModel.startExtraSession()
                    navController.navigate(Routes.session(refDay))
                },
                onOpenMilitary = {
                    viewModel.startMilitarySession()
                    navController.navigate(Routes.ROUTINE)
                },
                onOpenFatburn = { navController.navigate(Routes.FATBURN) }
            )
        }
        composable(Routes.FATBURN) {
            FatburnListScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenExercise = { id -> navController.navigate(Routes.fatburnEx(id)) }
            )
        }
        composable(
            route = Routes.FATBURN_EX,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: ""
            FatburnExerciseScreen(
                exerciseId = id,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onStarted = { navController.navigate(Routes.ROUTINE) }
            )
        }
        composable(Routes.ROUTINE) {
            RoutineSessionScreen(
                viewModel = viewModel,
                onExit = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }
    }

        celebration?.let {
            CelebrationDialog(
                celebration = it,
                onDismiss = { viewModel.clearCelebration() },
                onPlayAnthem = { viewModel.playChampionsVideo() }
            )
        }
    }
    }
}
