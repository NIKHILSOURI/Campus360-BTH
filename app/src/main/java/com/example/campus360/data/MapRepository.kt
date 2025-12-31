package com.example.campus360.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.IOException

class MapRepository(private val context: Context) {
    private val gson = Gson()
    
    // Multi-building support
    private val _buildings: MutableMap<String, BuildingData> = mutableMapOf()
    
    // Legacy single-building support (for backward compatibility)
    private var _mapInfo: MapInfo? = null
    private var _rooms: List<Room>? = null
    private var _roomsById: Map<String, Room> = emptyMap() 
    private var _graph: Graph? = null
    private var _nodesById: Map<String, Node> = emptyMap() 
    private var _floorplanBitmap: Bitmap? = null
    
    // Cross-building navigation: exit/entrance mappings
    // Building J exit -> Building H entrance
    private val jExitNodeId = "node_j1650_door_right" // Using existing exit node
    private val hEntranceNodeId = "h_entrance"
    // Building H exit -> Building J entrance  
    private val hExitNodeId = "h_entrance" // Same as entrance for now
    private val jEntranceNodeId = "node_staircase" // Using staircase as entrance
    
    val mapInfo: MapInfo? get() = _mapInfo
    val rooms: List<Room>? get() = _rooms
    val graph: Graph? get() = _graph
    val floorplanBitmap: Bitmap? get() = _floorplanBitmap
    
    // Multi-building accessors
    fun getBuilding(buildingId: String): BuildingData? = _buildings[buildingId]
    fun getAllBuildings(): Map<String, BuildingData> = _buildings.toMap()
    fun getBuildingRooms(buildingId: String): List<Room> = _buildings[buildingId]?.rooms ?: emptyList()
    fun getBuildingGraph(buildingId: String): Graph? = _buildings[buildingId]?.graph
    fun getBuildingMapInfo(buildingId: String): MapInfo? = _buildings[buildingId]?.mapInfo
    suspend fun getBuildingBitmap(buildingId: String): Bitmap? = withContext(Dispatchers.IO) {
        val building = _buildings[buildingId] ?: return@withContext null
        try {
            val bitmap = loadBuildingBitmap(building.mapImageAsset)
            if (bitmap == null) {
                android.util.Log.e("MapRepository", "Failed to load bitmap for building $buildingId: ${building.mapImageAsset}")
            } else {
                android.util.Log.d("MapRepository", "Loaded bitmap for building $buildingId: ${bitmap.width}x${bitmap.height}")
            }
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("MapRepository", "Error loading bitmap for building $buildingId", e)
            null
        }
    }
    
    suspend fun loadBuilding(buildingId: String): BuildingData? = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("MapRepository", "=== Loading building $buildingId ===")
            val prefix = when (buildingId) {
                "J" -> ""
                "H" -> "h_"
                else -> {
                    android.util.Log.e("MapRepository", "Unknown building ID: $buildingId")
                    return@withContext null
                }
            }
            
            android.util.Log.d("MapRepository", "Loading ${prefix}map_info.json")
            val mapInfoJson = try {
                loadJsonFromAssets("${prefix}map_info.json")
            } catch (e: Exception) {
                android.util.Log.e("MapRepository", "Failed to load ${prefix}map_info.json", e)
                return@withContext null
            }
            val mapInfoWrapper = gson.fromJson(mapInfoJson, MapInfoJson::class.java)
            val mapInfo = mapInfoWrapper?.map_info
            if (mapInfo == null) {
                android.util.Log.e("MapRepository", "Failed to parse map_info for building $buildingId")
                return@withContext null
            }
            android.util.Log.d("MapRepository", "Map info loaded: ${mapInfo.width}x${mapInfo.height}")
            
            android.util.Log.d("MapRepository", "Loading ${prefix}rooms.json")
            val roomsJson = try {
                loadJsonFromAssets("${prefix}rooms.json")
            } catch (e: Exception) {
                android.util.Log.e("MapRepository", "Failed to load ${prefix}rooms.json", e)
                return@withContext null
            }
            val roomsArray = gson.fromJson(roomsJson, Array<Room>::class.java)
            val rooms = roomsArray?.toList() ?: emptyList()
            android.util.Log.d("MapRepository", "Loaded ${rooms.size} rooms for building $buildingId")
            
            android.util.Log.d("MapRepository", "Loading ${prefix}graph.json")
            val graphJson = try {
                loadJsonFromAssets("${prefix}graph.json")
            } catch (e: Exception) {
                android.util.Log.e("MapRepository", "Failed to load ${prefix}graph.json", e)
                return@withContext null
            }
            val loadedGraph = gson.fromJson(graphJson, Graph::class.java)
            if (loadedGraph == null) {
                android.util.Log.e("MapRepository", "Failed to parse graph for building $buildingId")
                return@withContext null
            }
            android.util.Log.d("MapRepository", "Graph loaded: ${loadedGraph.nodes.size} nodes, ${loadedGraph.edges.size} edges")
            
            // Make edges bidirectional
            val edgeSet = loadedGraph.edges.map { "${it.from}:${it.to}" }.toSet()
            val bidirectionalEdges = mutableListOf<Edge>()
            bidirectionalEdges.addAll(loadedGraph.edges)
            for (edge in loadedGraph.edges) {
                val reverseKey = "${edge.to}:${edge.from}"
                if (!edgeSet.contains(reverseKey)) {
                    bidirectionalEdges.add(Edge(from = edge.to, to = edge.from, weight = edge.weight))
                }
            }
            
            val graph = Graph(nodes = loadedGraph.nodes, edges = bidirectionalEdges)
            
            val mapImageAsset = when (buildingId) {
                "J" -> "Floor new.png"
                "H" -> "Hblock.png"
                else -> {
                    android.util.Log.e("MapRepository", "Unknown building ID for map asset: $buildingId")
                    return@withContext null
                }
            }
            android.util.Log.d("MapRepository", "Map image asset: $mapImageAsset")
            
            val exitNodeId = when (buildingId) {
                "J" -> jExitNodeId
                "H" -> hExitNodeId
                else -> null
            }
            
            val entranceNodeId = when (buildingId) {
                "J" -> jEntranceNodeId
                "H" -> hEntranceNodeId
                else -> null
            }
            
            android.util.Log.d("MapRepository", "Building $buildingId: exit=$exitNodeId, entrance=$entranceNodeId")
            
            val buildingData = BuildingData(
                id = buildingId,
                name = if (buildingId == "J") "Building J" else "Building H",
                mapImageAsset = mapImageAsset,
                mapInfo = mapInfo,
                graph = graph,
                rooms = rooms,
                exitNodeId = exitNodeId,
                entranceNodeId = entranceNodeId
            )
            android.util.Log.d("MapRepository", "=== Successfully loaded building $buildingId ===")
            buildingData
        } catch (e: Exception) {
            android.util.Log.e("MapRepository", "Exception loading building $buildingId", e)
            e.printStackTrace()
            null
        }
    }
    
    suspend fun loadAllBuildings(): Boolean = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("MapRepository", "Loading all buildings...")
            
            // Load Building J
            val buildingJ = loadBuilding("J")
            if (buildingJ != null) {
                _buildings["J"] = buildingJ
                android.util.Log.d("MapRepository", "Building J loaded: ${buildingJ.rooms.size} rooms, ${buildingJ.graph.nodes.size} nodes")
            } else {
                android.util.Log.w("MapRepository", "Building J failed to load")
            }
            
            // Load Building H
            val buildingH = loadBuilding("H")
            if (buildingH != null) {
                _buildings["H"] = buildingH
                android.util.Log.d("MapRepository", "Building H loaded: ${buildingH.rooms.size} rooms, ${buildingH.graph.nodes.size} nodes")
            } else {
                android.util.Log.w("MapRepository", "Building H failed to load - check if h_graph.json, h_rooms.json, h_map_info.json exist")
            }
            
            // For backward compatibility, set Building J as default
            if (buildingJ != null) {
                _mapInfo = buildingJ.mapInfo
                _rooms = buildingJ.rooms
                _roomsById = buildingJ.rooms.associateBy { it.id }
                _graph = buildingJ.graph
                _nodesById = buildingJ.graph.nodes.associateBy { it.id }
                _floorplanBitmap = loadBuildingBitmap(buildingJ.mapImageAsset)
                android.util.Log.d("MapRepository", "Default building J bitmap loaded: ${_floorplanBitmap != null}")
            }
            
            val success = _buildings.isNotEmpty()
            android.util.Log.d("MapRepository", "Loaded ${_buildings.size} buildings. Success: $success")
            success
        } catch (e: Exception) {
            android.util.Log.e("MapRepository", "Error loading all buildings", e)
            e.printStackTrace()
            false
        }
    }
    
    suspend fun loadAllAssets(): Boolean = withContext(Dispatchers.IO) {
        // Try loading all buildings first
        val buildingsLoaded = loadAllBuildings()
        if (buildingsLoaded) {
            return@withContext true
        }
        
        // Fallback to legacy single-building loading
        try {
            android.util.Log.d("MapRepository", "Loading all assets in parallel...")
            val startTime = System.currentTimeMillis()
            

            val mapInfoDeferred = async {
                try {
                    val mapInfoJson = loadJsonFromAssets("map_info.json")
                    val mapInfoWrapper = gson.fromJson(mapInfoJson, MapInfoJson::class.java)
                    mapInfoWrapper?.map_info
                } catch (e: Exception) {
                    android.util.Log.e("MapRepository", "Error loading map_info.json", e)
                    null
                }
            }
            
            val roomsDeferred = async {
                try {
                    val roomsJson = loadJsonFromAssets("rooms.json")
                    val roomsArray = gson.fromJson(roomsJson, Array<Room>::class.java)
                    roomsArray?.toList() ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("MapRepository", "Error loading rooms.json", e)
                    emptyList<Room>()
                }
            }
            
            val graphDeferred = async {
                try {
                    val graphJson = loadJsonFromAssets("graph.json")
                    gson.fromJson(graphJson, Graph::class.java)
                } catch (e: Exception) {
                    android.util.Log.e("MapRepository", "Error loading graph.json", e)
                    null
                }
            }
            
            val bitmapDeferred = async {
                loadBitmapFromAssets("Floor new.png")
            }
            

            val mapInfo = mapInfoDeferred.await()
            val rooms = roomsDeferred.await()
            val loadedGraph = graphDeferred.await()
            

            if (mapInfo == null) {
                android.util.Log.e("MapRepository", "Failed to parse map_info.json")
                return@withContext false
            }
            _mapInfo = mapInfo
            android.util.Log.d("MapRepository", "Map info loaded: ${_mapInfo?.width}x${_mapInfo?.height}")
            

            _rooms = rooms
            _roomsById = rooms.associateBy { it.id }
            android.util.Log.d("MapRepository", "Rooms loaded: ${_rooms?.size}")
            

            if (loadedGraph == null) {
                android.util.Log.e("MapRepository", "Invalid graph data: graph is null")
                return@withContext false
            }
            
            if (loadedGraph.nodes.isEmpty() || loadedGraph.edges.isEmpty()) {
                android.util.Log.e("MapRepository", "Invalid graph data: empty nodes or edges")
                return@withContext false
            }
            

            val edgeSet = loadedGraph.edges.map { "${it.from}:${it.to}" }.toSet()
            val bidirectionalEdges = mutableListOf<Edge>()
            bidirectionalEdges.addAll(loadedGraph.edges)
            
            for (edge in loadedGraph.edges) {
                val reverseKey = "${edge.to}:${edge.from}"
                if (!edgeSet.contains(reverseKey)) {
                    bidirectionalEdges.add(Edge(
                        from = edge.to,
                        to = edge.from,
                        weight = edge.weight
                    ))
                }
            }
            
            _graph = Graph(
                nodes = loadedGraph.nodes,
                edges = bidirectionalEdges
            )

            _nodesById = loadedGraph.nodes.associateBy { it.id }
            android.util.Log.d("MapRepository", "Graph loaded: ${_graph?.nodes?.size} nodes, ${_graph?.edges?.size} edges")
            

            _floorplanBitmap = bitmapDeferred.await()
            if (_floorplanBitmap == null) {
                android.util.Log.e("MapRepository", "Failed to load floorplan bitmap")
                return@withContext false
            }
            android.util.Log.d("MapRepository", "Floorplan bitmap loaded: ${_floorplanBitmap?.width}x${_floorplanBitmap?.height}")
            
            val loadTime = System.currentTimeMillis() - startTime
            android.util.Log.d("MapRepository", "All assets loaded in ${loadTime}ms")
            
            val isLoaded = isDataLoaded()
            android.util.Log.d("MapRepository", "Data loaded: $isLoaded")
            isLoaded
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("MapRepository", "OutOfMemoryError loading assets", e)
            System.gc()
            false
        } catch (e: Exception) {
            android.util.Log.e("MapRepository", "Error loading assets", e)
            e.printStackTrace()
            false
        }
    }
    
    private fun loadJsonFromAssets(fileName: String): String {
        return try {

            val inputStream = context.assets.open(fileName)
            inputStream.buffered(32 * 1024).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            android.util.Log.e("MapRepository", "Error loading JSON: $fileName", e)
            throw e
        } catch (e: Exception) {
            android.util.Log.e("MapRepository", "Unexpected error loading JSON: $fileName", e)
            throw e
        }
    }
    
    private fun loadBuildingBitmap(fileName: String): Bitmap? {
        return loadBitmapFromAssets(fileName)
    }
    
    private fun loadBitmapFromAssets(fileName: String): Bitmap? {
        return try {
            android.util.Log.d("MapRepository", "Attempting to load bitmap asset: $fileName")
            // List available assets for debugging
            try {
                val assetList = context.assets.list("")?.filter { it.endsWith(".png", ignoreCase = true) }
                android.util.Log.d("MapRepository", "Available PNG assets: ${assetList?.joinToString()}")
            } catch (e: Exception) {
                android.util.Log.w("MapRepository", "Could not list assets", e)
            }
            
            val inputStream = context.assets.open(fileName)
            android.util.Log.d("MapRepository", "Successfully opened asset stream for: $fileName")
            

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            android.util.Log.d("MapRepository", "Bitmap bounds read: ${options.outWidth}x${options.outHeight}")
            

            val maxDimension = 2048
            val width = options.outWidth
            val height = options.outHeight
            var sampleSize = 1
            
            if (width > maxDimension || height > maxDimension) {
                val halfWidth = width / 2
                val halfHeight = height / 2
                while ((halfWidth / sampleSize) >= maxDimension && 
                       (halfHeight / sampleSize) >= maxDimension) {
                    sampleSize *= 2
                }
            }
            
            android.util.Log.d("MapRepository", "Bitmap dimensions: ${width}x${height}, sample size: $sampleSize")
            

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            
            val newInputStream = context.assets.open(fileName)
            val bitmap = BitmapFactory.decodeStream(newInputStream, null, decodeOptions)
            newInputStream.close()
            
            if (bitmap == null) {
                android.util.Log.e("MapRepository", "Failed to decode bitmap: $fileName")
            } else {
                android.util.Log.d("MapRepository", "Bitmap decoded successfully: ${bitmap.width}x${bitmap.height}")
            }
            bitmap
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("MapRepository", "OutOfMemoryError loading bitmap: $fileName", e)
            System.gc() 
            null
        } catch (e: IOException) {
            android.util.Log.e("MapRepository", "Error loading bitmap: $fileName", e)
            e.printStackTrace()
            null
        } catch (e: Exception) {
            android.util.Log.e("MapRepository", "Unexpected error loading bitmap: $fileName", e)
            e.printStackTrace()
            null
        }
    }
    
    fun isDataLoaded(): Boolean {
        return _mapInfo != null && _rooms != null && _graph != null && _floorplanBitmap != null
    }
    
    fun getRoomsByCategory(category: String): List<Room> {
        val allRooms = _rooms ?: return emptyList()
        return allRooms.filter { room ->
            when (category.lowercase()) {
                "clothing" -> room.type.lowercase() == "clothing"
                "coffee" -> room.type.lowercase() == "coffee"
                "electronics" -> room.type.lowercase() == "electronics"
                "convenience" -> room.type.lowercase() == "convenience"
                "toilets", "toilet", "restroom" -> room.type.lowercase() == "toilet" || room.type.lowercase() == "restroom"
                "sports" -> room.type.lowercase() == "sports"
                "beauty" -> room.type.lowercase() == "beauty"
                "lifestyle" -> room.type.lowercase() == "lifestyle"
                "retail" -> room.type.lowercase() == "retail"
                "entrance" -> room.type.lowercase() == "entrance"
                "lecture halls", "lecture" -> room.type.lowercase() == "lecture" || room.type.lowercase() == "lecture_hall"
                "classrooms", "classroom" -> room.type.lowercase() == "classroom" || room.type.lowercase() == "class"
                "labs", "lab" -> room.type.lowercase() == "lab" || room.type.lowercase() == "laboratory"
                "popular" -> room.type.lowercase() == "cafeteria" || 
                            room.type.lowercase() == "study_area" || 
                            room.type.lowercase() == "computer_lab" || 
                            room.type.lowercase() == "student_service"
                else -> room.type.lowercase() == category.lowercase()
            }
        }
    }
    
    fun getRoomsByIds(roomIds: List<String>): List<Room> {
        if (_roomsById.isEmpty()) return emptyList()
        return roomIds.mapNotNull { id -> _roomsById[id] }
    }
    
    fun getRoomById(roomId: String): Room? {
        // First check legacy single-building storage
        val legacyRoom = _roomsById[roomId]
        if (legacyRoom != null) return legacyRoom
        
        // Search across all buildings
        for (building in _buildings.values) {
            val room = building.rooms.find { it.id == roomId }
            if (room != null) return room
        }
        return null
    }
    
    fun searchRooms(query: String, buildingId: String? = null): List<Room> {
        val allRooms = if (buildingId != null) {
            _buildings[buildingId]?.rooms ?: emptyList()
        } else {
            // Search across all buildings
            _buildings.values.flatMap { it.rooms }
        }
        
        val lowerQuery = query.lowercase().trim()
        if (lowerQuery.isEmpty()) return emptyList()
        
        return allRooms.filter { room ->
            room.name.lowercase().contains(lowerQuery) ||
            room.id.lowercase().contains(lowerQuery) ||
            room.type.lowercase().contains(lowerQuery) ||
            room.building.lowercase().contains(lowerQuery)
        }
    }
    
    fun getRoutingEngine(): RoutingEngine? {
        val graph = _graph ?: return null
        return RoutingEngine(graph)
    }
    
    fun getRoute(startNodeId: String, endNodeId: String, buildingId: String? = null): Route? {
        val targetBuilding = buildingId ?: "J" // Default to J for backward compatibility
        val building = _buildings[targetBuilding] ?: return null
        val engine = RoutingEngine(building.graph)
        return try {
            engine.getRoute(startNodeId, endNodeId)
        } catch (e: Exception) {
            android.util.Log.e("MapRepository", "Error getting route from $startNodeId to $endNodeId", e)
            null
        }
    }
    
    /**
     * Get cross-building route if start and end are in different buildings
     */
    fun getCrossBuildingRoute(startRoomId: String, endRoomId: String): CrossBuildingRoute? {
        val startRoom = getRoomById(startRoomId) ?: return null
        val endRoom = getRoomById(endRoomId) ?: return null
        
        val startBuildingId = if (startRoom.building.contains("J")) "J" else "H"
        val endBuildingId = if (endRoom.building.contains("J")) "J" else "H"
        
        android.util.Log.d("MapRepository", "=== Building cross-building route ===")
        android.util.Log.d("MapRepository", "Start: $startRoomId in $startBuildingId, End: $endRoomId in $endBuildingId")
        
        // Same building - use regular route
        if (startBuildingId == endBuildingId) {
            val building = _buildings[startBuildingId] ?: return null
            val route = getRoute(startRoom.nodeId, endRoom.nodeId, startBuildingId)
            return if (route != null) {
                CrossBuildingRoute(
                    segments = listOf(RouteSegment(startBuildingId, route)),
                    totalDistance = route.totalDistance
                )
            } else null
        }
        
        // Cross-building route
        val startBuilding = _buildings[startBuildingId] ?: return null
        val endBuilding = _buildings[endBuildingId] ?: return null
        
        val startExitNodeId = startBuilding.exitNodeId ?: return null
        val endEntranceNodeId = endBuilding.entranceNodeId ?: return null
        
        android.util.Log.d("MapRepository", "Cross-building route: $startBuildingId -> $endBuildingId")
        android.util.Log.d("MapRepository", "Segment 0 will be: $startBuildingId (start -> exit)")
        android.util.Log.d("MapRepository", "Segment 1 will be: $endBuildingId (entrance -> end)")
        
        // Segment 0: Start room -> Exit of start building (ALWAYS FIRST)
        val segment0Route = getRoute(startRoom.nodeId, startExitNodeId, startBuildingId)
        if (segment0Route == null) {
            android.util.Log.e("MapRepository", "Failed to compute segment 0 route in $startBuildingId")
            return null
        }
        
        // Segment 1: Entrance of end building -> End room (ALWAYS SECOND)
        val segment1Route = getRoute(endEntranceNodeId, endRoom.nodeId, endBuildingId)
        if (segment1Route == null) {
            android.util.Log.e("MapRepository", "Failed to compute segment 1 route in $endBuildingId")
            return null
        }
        
        val crossRoute = CrossBuildingRoute(
            segments = listOf(
                RouteSegment(
                    buildingId = startBuildingId, // Segment 0: start building
                    route = segment0Route,
                    instruction = "Proceed to exit of ${startBuilding.name}. Switch to ${endBuilding.name}."
                ),
                RouteSegment(
                    buildingId = endBuildingId, // Segment 1: end building
                    route = segment1Route,
                    instruction = "Entered ${endBuilding.name}. Continue to ${endRoom.name}."
                )
            ),
            totalDistance = segment0Route.totalDistance + segment1Route.totalDistance
        )
        
        android.util.Log.d("MapRepository", "Cross-building route created: segments[0]=${crossRoute.segments[0].buildingId}, segments[1]=${crossRoute.segments[1].buildingId}")
        return crossRoute
    }
    
    fun getRoomBuildingId(roomId: String): String? {
        val room = getRoomById(roomId) ?: return null
        return if (room.building.contains("J")) "J" else "H"
    }
    
    fun findNearestNode(x: Double, y: Double): Node? {
        if (!isDataLoaded()) {
            android.util.Log.w("MapRepository", "Data not loaded when finding nearest node")
            return null
        }
        val engine = getRoutingEngine() ?: return null
        return try {
            engine.findNearestNode(x, y)
        } catch (e: Exception) {
            android.util.Log.e("MapRepository", "Error finding nearest node", e)
            null
        }
    }
    
    fun getNodeById(nodeId: String): Node? {
        return _nodesById[nodeId]
    }
    
    fun getNodesByIds(nodeIds: List<String>): List<Node> {
        if (_nodesById.isEmpty()) return emptyList()
        return nodeIds.mapNotNull { id -> _nodesById[id] }
    }
    
    fun getExitNodes(): List<Node> {
        val engine = getRoutingEngine() ?: return emptyList()
        val mapInfo = _mapInfo ?: return emptyList()
        return engine.getExitNodes(mapInfo.width, mapInfo.height)
    }
    
    fun findNearestExitNode(startNodeId: String): Node? {
        if (!isDataLoaded()) {
            return null
        }
        val engine = getRoutingEngine() ?: return null
        val mapInfo = _mapInfo ?: return null
        return try {
            engine.findNearestExitNode(startNodeId, mapInfo.width, mapInfo.height)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

