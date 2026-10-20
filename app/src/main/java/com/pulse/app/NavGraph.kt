package com.pulse.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.pulse.app.ui.screens.levelselect.LevelSelectScreen
import com.pulse.app.ui.screens.prelevel.PreLevelScreen
import com.pulse.app.ui.screens.play.PlayScreen
import com.pulse.app.ui.screens.results.ResultsScreen
import com.pulse.app.ui.screens.auth.LoginScreen
import com.pulse.app.ui.screens.profile.ProfileScreen
import io.github.jan.supabase.auth.auth
import java.util.UUID

sealed class Screen(val route: String) {
    object LevelSelect : Screen("level_select")
    object Profile : Screen("profile")
    object PreLevel : Screen("pre_level/{levelId}") {
        fun createRoute(levelId: String) = "pre_level/$levelId"
    }
    object Play : Screen("play/{levelId}") {
        fun createRoute(levelId: String) = "play/$levelId"
    }
    object Results : Screen("results/{levelId}/{score}/{durationSeconds}/{accuracyPct}/{attemptId}") {
        fun createRoute(levelId: String, score: Int, durationSeconds: Int, accuracyPct: Float, attemptId: String) =
            "results/$levelId/$score/$durationSeconds/$accuracyPct/$attemptId"
    }
    object Login : Screen("login")
}


@Composable
fun PulseNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    var isCheckingSession by remember { mutableStateOf(true) }
    var hasSession by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasSession = try {
            SupabaseClient.client.auth.loadFromStorage()
                    || SupabaseClient.client.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            false
        }
        isCheckingSession = false
    }

    if (isCheckingSession) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (hasSession)
        Screen.LevelSelect.route
    else
        Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.LevelSelect.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LevelSelect.route) {
            LevelSelectScreen(
                onLevelClick = { level ->
                    navController.navigate(Screen.PreLevel.createRoute(level.id))
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PreLevel.route) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getString("levelId") ?: return@composable
            PreLevelScreen(
                levelId = levelId,
                onStart = {
                    navController.navigate(Screen.Play.createRoute(levelId)) {
                        popUpTo(Screen.PreLevel.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Play.route) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getString("levelId") ?: return@composable
            PlayScreen(
                levelId = levelId,
                onComplete = { score, durationSeconds, accuracyPct ->
                    navController.navigate(
                        Screen.Results.createRoute(
                            levelId = levelId,
                            score = score,
                            durationSeconds = durationSeconds,
                            accuracyPct = accuracyPct,
                            attemptId = UUID.randomUUID().toString()
                        )
                    ) {
                        popUpTo(Screen.Play.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Results.route) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getString("levelId") ?: return@composable
            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
            val durationSeconds = backStackEntry.arguments
                ?.getString("durationSeconds")
                ?.toIntOrNull() ?: 0
            val accuracyPct = backStackEntry.arguments?.getString("accuracyPct")?.toFloatOrNull() ?: 0f
            val attemptId = backStackEntry.arguments?.getString("attemptId") ?: return@composable
            ResultsScreen(
                levelId = levelId,
                score = score,
                durationSeconds = durationSeconds,
                accuracyPct = accuracyPct,
                attemptId = attemptId,
                onPlayAgain = {
                    navController.navigate(Screen.PreLevel.createRoute(levelId)) {
                        popUpTo(Screen.Results.route) { inclusive = true }
                    }
                },
                onLevelSelect = {
                    navController.navigate(Screen.LevelSelect.route) {
                        popUpTo(Screen.Results.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
