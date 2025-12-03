
package com.example.campus360.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object LanguageSelection : Screen("language_selection")
    object Home : Screen("home")
    object Search : Screen("search")
    object Map : Screen("map")
    object CategoryResults : Screen("category_results/{category}")
    object DestinationDetails : Screen("destination_details/{roomId}")
    object ChooseStartLocation : Screen("choose_start_location/{roomId}")
    object Settings : Screen("settings")
    
    fun createRoute(vararg params: String): String {
        var route = this.route
        var paramIndex = 0
        while (route.contains("{") && paramIndex < params.size) {
            val startIndex = route.indexOf("{")
            val endIndex = route.indexOf("}", startIndex)
            if (endIndex != -1) {
                val paramName = route.substring(startIndex + 1, endIndex)
                route = route.replace("{$paramName}", params[paramIndex])
                paramIndex++
            } else {
                break
            }
        }
        return route
    }
}

