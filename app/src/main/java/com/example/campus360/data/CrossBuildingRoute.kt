package com.example.campus360.data

/**
 * Represents a route that spans multiple buildings
 */
data class CrossBuildingRoute(
    val segments: List<RouteSegment>,
    val totalDistance: Double
)

data class RouteSegment(
    val buildingId: String,
    val route: Route,
    val instruction: String? = null // e.g., "Proceed to exit of Building J"
)

