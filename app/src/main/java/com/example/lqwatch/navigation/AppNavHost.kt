package com.example.lqwatch.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lqwatch.reader.ReaderHomeScreen
import com.example.lqwatch.reader.ReaderScreen
import com.example.lqwatch.ui.home.HomeScreen
import com.example.lqwatch.ui.music.MusicPlaceholderScreen

object Routes {
    const val HOME = "home"
    const val MUSIC = "music"
    const val READER = "reader"
    const val READER_READ = "reader/read?uri={uri}&name={name}"

    fun readerRead(uri: String, name: String): String =
        "reader/read?uri=${Uri.encode(uri)}&name=${Uri.encode(name)}"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenReader = { navController.navigate(Routes.READER) },
                onOpenMusic = { navController.navigate(Routes.MUSIC) }
            )
        }
        composable(Routes.MUSIC) {
            MusicPlaceholderScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.READER) {
            ReaderHomeScreen(
                onBack = { navController.popBackStack() },
                onOpenDocument = { uri, name ->
                    navController.navigate(Routes.readerRead(uri.toString(), name))
                }
            )
        }
        composable(
            route = Routes.READER_READ,
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")?.let { Uri.parse(it) }
            val name = backStackEntry.arguments?.getString("name") ?: ""
            if (uri != null) {
                ReaderScreen(uri = uri, displayName = name, onBack = { navController.popBackStack() })
            }
        }
    }
}
