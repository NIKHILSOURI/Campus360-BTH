package com.example.campus360.data

data class CrossBuildingRoute(
    val segments: List<RouteSegment>,
    val totalDistance: Double
)

data class RouteSegment(
    val buildingId: String,
    val route: Route,
    val instruction: String? = null
)

