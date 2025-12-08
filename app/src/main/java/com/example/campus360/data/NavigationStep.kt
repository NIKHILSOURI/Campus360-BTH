package com.example.campus360.data

enum class NavigationDirection {
    STRAIGHT,
    LEFT,
    RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    ARRIVE,
    START
}

data class NavigationStep(
    val stepNumber: Int,
    val instruction: String,
    val direction: NavigationDirection,
    val distance: Double, // in map units
    val fromNode: Node,
    val toNode: Node,
    val landmark: String? = null // Optional landmark reference
)



