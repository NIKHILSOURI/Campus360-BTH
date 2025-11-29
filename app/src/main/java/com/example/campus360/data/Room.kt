package com.example.campus360.data

data class Position(
    val x: Double,
    val y: Double
)

data class Room(
    val id: String,
    val name: String,
    val type: String,
    val position: Position,
    val nodeId: String,
    val building: String = "Building J",
    val floor: String = "Floor 1"
) {
    val location: String
        get() = "$building - $floor"
}

