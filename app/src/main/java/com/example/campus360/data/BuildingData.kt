package com.example.campus360.data

data class BuildingData(
    val id: String, // "J" or "H"
    val name: String, // "Building J" or "Building H"
    val mapImageAsset: String, // "Floor new.png" or "Hblock.png"
    val mapInfo: MapInfo,
    val graph: Graph,
    val rooms: List<Room>,
    val exitNodeId: String? = null, // Node ID for building exit (for cross-building navigation)
    val entranceNodeId: String? = null // Node ID for building entrance (for cross-building navigation)
)

