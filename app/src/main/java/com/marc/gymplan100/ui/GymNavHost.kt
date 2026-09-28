package com.marc.gymplan100.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.marc.gymplan100.PlanViewModel
import com.marc.gymplan100.R

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
    const val PLANS = "plans"
    const val PLAN_EDITOR = "plan-editor"
    const val SPECIAL = "special"
    const val FATBURN = "fatburn"
    const val FATBURN_EX = "fatburn/{id}"
    const val ROUTINE = "routine"
    fun phase(n: Int) = "phase/$n"
    fun day(n: Int) = "day/$n"
    fun session(n: Int) = "session/$n"
    fun fatburnEx(id: String) = "fatburn/$id"
}

/** Las pestañas de la barra de abajo. Fuera de estas pantallas la barra no se ve. */
private enum class Pestana(val route: String, val label: String, val icon: Int) {
    INICIO(Routes.HOME, "Inicio", R.drawable.ic_inicio),
    LOGROS(Routes.ACHIEVEMENTS, "Logros", R.drawable.ic_logros),
    PESOS(Routes.WEIGHTS, "Mis pesos", R.drawable.ic_pesos),
    RESULTADOS(Routes.RESULTS, "Resultados", R.drawable.ic_resultados),
    ESTADISTICAS(Routes.STATS, "Estadísticas", R.drawable.ic_estadisticas)
}

/** Género del perfil, para elegir la ilustración del ejercicio (true = mujer). */
val LocalIsFemale = staticCompositionLocalOf { false }

@Composable
fun GymNavHost(
    openSessionDay: Int? = null,
    onSessionConsumed: () -> Unit = {}
) {
    val viewModel: PlanViewModel = viewModel()
    val celebration by viewModel.celebration.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()

    // Nada se dibuja hasta tener el progreso leído. Es cosa de un parpadeo, pero en ese
    // parpadeo la portada decía "plan nuevo · empieza cuando quieras · día 1" a quien lleva
    // cuarenta días hechos, y también sacaba la bienvenida a quien ya eligió plan hace meses.
    // Mejor un instante de fondo vacío que un instante de mentira.
    val loaded by viewModel.loaded.collectAsState()
    if (!loaded) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    CompositionLocalProvider(LocalIsFemale provides (profile.gender == "Mujer")) {
    Box {
    // Al cambiar de plan se rehace la navegación desde el principio: las pantallas abiertas
    // hablaban de días y fases que el plan nuevo puede no tener.
    key(activePlan.id) {
    val navController = rememberNavController()

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

    val rutaActual = navController.currentBackStackEntryAsState().value?.destination?.route
    val conBarra = Pestana.entries.any { it.route == rutaActual }

    // El Scaffold de toda la app. Sin márgenes propios (contentWindowInsets a cero): cada
    // pantalla sigue pidiendo los suyos, y así las que no llevan barra quedan como estaban.
    // Con la barra, el hueco que ocupa se consume para que el Scaffold de cada pantalla no
    // vuelva a sumar debajo el margen de la barra del sistema.
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (conBarra) BarraInferior(navController, rutaActual)
        }
    ) { inner ->
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier.padding(inner).consumeWindowInsets(inner)
    ) {
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
                onOpenSpecial = { navController.navigate(Routes.SPECIAL) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenPlans = { navController.navigate(Routes.PLANS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenPlans = { navController.navigate(Routes.PLANS) }
            )
        }
        composable(Routes.PLANS) {
            PlansScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEditPlan = { navController.navigate(Routes.PLAN_EDITOR) }
            )
        }
        composable(Routes.PLAN_EDITOR) {
            PlanEditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.RESULTS) {
            ResultsScreen(viewModel = viewModel)
        }
        composable(Routes.STATS) {
            StatisticsScreen(viewModel = viewModel)
        }
        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(viewModel = viewModel)
        }
        composable(Routes.WEIGHTS) {
            ExerciseWeightsScreen(viewModel = viewModel)
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
                onStartSession = { navController.navigate(Routes.session(it)) },
                onStartFreeSession = { day ->
                    viewModel.startSpecialSession(day)
                    navController.navigate(Routes.session(day))
                }
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
                onOpenFatburn = { navController.navigate(Routes.FATBURN) },
                onOpenPosture = {
                    viewModel.startPostureSession()
                    navController.navigate(Routes.ROUTINE)
                }
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
    }

    // Cierre del plan: encima de la navegación, al marcar el último día y una vez cerrada la
    // celebración de ese día. Vive aquí dentro porque necesita el navController para llevar
    // al catálogo si se quiere cambiar de plan.
    val progress by viewModel.progress.collectAsState()
    if (progress.isFinished && celebration == null) {
        PlanFinishedScreen(
            viewModel = viewModel,
            onSeePlans = { navController.navigate(Routes.PLANS) },
            onDismiss = {}
        )
    }
    }

        // Encima de todo lo demás y la última en dibujarse: la primera vez no hay plan elegido,
        // así que la portada que hay detrás no significa nada todavía. Después no vuelve a salir.
        val needsWelcome by viewModel.needsWelcome.collectAsState()
        if (needsWelcome) {
            WelcomeScreen(
                viewModel = viewModel,
                onSeePlans = { viewModel.welcomeDone() },
                onCreatePlan = { viewModel.welcomeDone() },
                onDone = { viewModel.welcomeDone() }
            )
        }

        // Y detrás de la bienvenida, el recorrido: qué hace la app y dónde está cada cosa. Los
        // dos van encima de todo, pero nunca a la vez.
        val needsTour by viewModel.needsTour.collectAsState()
        if (!needsWelcome && needsTour) {
            TourScreen(onDone = { viewModel.tourDone() })
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

/**
 * Logros, pesos, resultados y estadísticas, a un toque desde cualquiera de ellas. Cada pestaña
 * guarda dónde estaba (scroll incluido) y volver atrás desde una lleva siempre a Inicio.
 */
@Composable
private fun BarraInferior(navController: NavHostController, rutaActual: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        Pestana.entries.forEach { pestana ->
            NavigationBarItem(
                selected = rutaActual == pestana.route,
                onClick = {
                    if (rutaActual == pestana.route) return@NavigationBarItem
                    navController.navigate(pestana.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(painterResource(pestana.icon), contentDescription = null) },
                // Sin el espaciado de las etiquetas del tema: con él «Estadísticas» no cabía ni
                // a tamaño normal. Y en unidades fijas: con la letra del sistema al 150 % las
                // cinco etiquetas se cortaban, y el icono ya dice qué es cada pestaña.
                label = {
                    Text(
                        pestana.label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = with(LocalDensity.current) { 12.dp.toSp() },
                            letterSpacing = 0.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                )
            )
        }
    }
}
