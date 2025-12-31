
package com.example.campus360.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.campus360.ui.category.CategoryResultsScreen
import com.example.campus360.ui.choosestart.ChooseStartLocationScreen
import com.example.campus360.ui.destination.DestinationDetailsScreen
import com.example.campus360.ui.home.HomeScreen
import com.example.campus360.ui.map.MapScreen
import com.example.campus360.ui.search.SearchScreen
import com.example.campus360.ui.settings.SettingsScreen
import com.example.campus360.ui.splash.SplashScreen
import android.util.Log
import java.net.URLDecoder

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        
        composable(
            route = "${Screen.Search.route}?query={query}",
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val query = try {
                URLDecoder.decode(backStackEntry.arguments?.getString("query") ?: "", "UTF-8")
            } catch (_: Exception) {
                backStackEntry.arguments?.getString("query") ?: ""
            }
            SearchScreen(navController = navController, query = query)
        }
        
        composable(
            route = "${Screen.Map.route}?roomId={roomId}&startNodeId={startNodeId}&pickMode={pickMode}&sosMode={sosMode}",
            arguments = listOf(
                navArgument("roomId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("startNodeId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("pickMode") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("sosMode") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val startNodeId = backStackEntry.arguments?.getString("startNodeId") ?: ""
            val pickMode = backStackEntry.arguments?.getBoolean("pickMode") ?: false
            val sosMode = backStackEntry.arguments?.getBoolean("sosMode") ?: false
            MapScreen(
                navController = navController,
                roomId = roomId,
                startNodeId = startNodeId,
                pickMode = pickMode,
                sosMode = sosMode
            )
        }
        
        composable(
            route = Screen.CategoryResults.route,
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            CategoryResultsScreen(navController = navController, category = category)
        }
        
        composable(
            route = Screen.DestinationDetails.route,
            arguments = listOf(
                navArgument("roomId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val roomId = try {
                backStackEntry.arguments?.getString("roomId") ?: ""
            } catch (e: Exception) {
                Log.e("NavGraph", "Error getting roomId argument", e)
                ""
            }
            Log.d("NavGraph", "DestinationDetails route with roomId: $roomId")
            DestinationDetailsScreen(navController = navController, roomId = roomId)
        }
        
        composable(
            route = Screen.ChooseStartLocation.route,
            arguments = listOf(
                navArgument("roomId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            ChooseStartLocationScreen(navController = navController, roomId = roomId)
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}

