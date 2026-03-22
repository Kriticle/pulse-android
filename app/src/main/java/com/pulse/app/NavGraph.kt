package com.pulse.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pulse.app.ui.screens.levelselect.LevelSelectScreen
import com.pulse.app.ui.screens.prelevel.PreLevelScreen
import com.pulse.app.ui.screens.play.PlayScreen
import com.pulse.app.ui.screens.results.ResultsScreen
import com.pulse.app.ui.screens.auth.LoginScreen
import com.pulse.app.ui.screens.profile.ProfileScreen
import com.pulse.app.SupabaseClient
import io.github.jan.supabase.auth.auth
import androidx.compose.runtime.remember

sealed class Screen(val route: String) {
    object LevelSelect : Screen("level_select")
    object Profile : Screen("profile")
    object PreLevel : Screen("pre_level/{levelId}") {
        fun createRoute(levelId: String) = "pre_level/$levelId"
    }
    object Play : Screen("play/{levelId}") {
        fun createRoute(levelId: String) = "play/$levelId"
    }
    object Results : Screen("results/{levelId}/{score}") {
        fun createRoute(levelId: String, score: Int) = "results/$levelId/$score"
    }
    object Login : Screen("login")
}


@Composable
fun PulseNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val authSession = remember {
        try {
            SupabaseClient.client.auth.currentSessionOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /*val startDestination = if (authSession != null)
        Screen.LevelSelect.route
    else
        Screen.Login.route*/

    val startDestination = Screen.LevelSelect.route

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
                onComplete = { score ->
                    navController.navigate(Screen.Results.createRoute(levelId, score)) {
                        popUpTo(Screen.Play.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Results.route) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getString("levelId") ?: return@composable
            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
            ResultsScreen(
                levelId = levelId,
                score = score,
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