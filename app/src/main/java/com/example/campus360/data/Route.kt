package com.example.campus360.data

data class Route(
    val nodes: List<Node>,
    val totalDistance: Double = 0.0,
    val steps: List<NavigationStep> = emptyList()
)


