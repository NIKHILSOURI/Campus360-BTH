package com.example.campus360.ui.map
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.example.campus360.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapState(
    val scale: Float = 1f,
    val translateX: Float = 0f,
    val translateY: Float = 0f,
    val route: Route? = null,
    val crossBuildingRoute: CrossBuildingRoute? = null,
    val currentSegmentIndex: Int = 0,
    val destinationRoom: Room? = null,
    val startNode: Node? = null,
    val destinationNode: Node? = null,
    val navigationSteps: List<NavigationStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val isRouteUnavailable: Boolean = false,
    val selectedBuilding: String = "J",
    val focusTarget: Pair<Double, Double>? = null
)

sealed class MapUiState {
    object Loading : MapUiState()
    data class Ready(val mapState: MapState) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MapRepository(application)
    
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    
    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState.asStateFlow()
    
    private val _floorplanBitmap = MutableStateFlow<Bitmap?>(null)
    val floorplanBitmap: StateFlow<Bitmap?> = _floorplanBitmap.asStateFlow()
    
    private val _mapInfo = MutableStateFlow<MapInfo?>(null)
    val mapInfo: StateFlow<MapInfo?> = _mapInfo.asStateFlow()
    
    private var initialBounds: BoundingBox? = null
    
    data class BoundingBox(
        val minX: Double,
        val minY: Double,
        val maxX: Double,
        val maxY: Double
    )
    
    init {
        loadMapData()
    }
    
    private fun loadMapData() {
        viewModelScope.launch {
            val success = repository.loadAllBuildings()
            
            if (success) {
                if (_mapState.value.crossBuildingRoute == null && _mapState.value.route == null) {
                    android.util.Log.d("MapViewModel", "No active navigation, loading default building J")
                    switchBuilding("J")
                } else {
                    android.util.Log.d("MapViewModel", "Navigation active, preserving selectedBuilding: ${_mapState.value.selectedBuilding}")
                }
            } else {
                val legacySuccess = repository.loadAllAssets()
                if (legacySuccess && repository.isDataLoaded()) {
                    val bitmap = repository.floorplanBitmap
                    val info = repository.mapInfo
                    if (bitmap != null && info != null) {
                        _floorplanBitmap.value = bitmap
                        _mapInfo.value = info
                        _uiState.value = MapUiState.Ready(_mapState.value)
                    } else {
                        _uiState.value = MapUiState.Error("Failed to load map data")
                    }
                } else {
                    _uiState.value = MapUiState.Error("Failed to load map data")
                }
            }
        }
    }
    
    fun switchBuilding(buildingId: String) {
        viewModelScope.launch {
            android.util.Log.d("MapViewModel", "Switching to building: $buildingId")
            
            var building = repository.getBuilding(buildingId)
            if (building == null) {
                android.util.Log.w("MapViewModel", "Building $buildingId not found, attempting to reload all buildings...")
                val reloadSuccess = repository.loadAllBuildings()
                if (reloadSuccess) {
                    building = repository.getBuilding(buildingId)
                }
            }
            
            if (building == null) {
                android.util.Log.e("MapViewModel", "Building $buildingId not found in repository after reload attempt")
                _uiState.value = MapUiState.Error("Building $buildingId not loaded. Please check if h_graph.json, h_rooms.json, and h_map_info.json exist in assets folder.")
                return@launch
            }
            
            android.util.Log.d("MapViewModel", "Building $buildingId found, loading bitmap: ${building.mapImageAsset}")
            val bitmap = repository.getBuildingBitmap(buildingId)
            
            if (bitmap == null) {
                android.util.Log.e("MapViewModel", "Failed to load bitmap for building $buildingId")
                _uiState.value = MapUiState.Error("Failed to load map image for Building $buildingId. Please ensure ${building.mapImageAsset} exists in assets folder.")
                return@launch
            }
            
            val finalMapInfo = building.mapInfo
            
            val routeForBuilding = getRouteForBuilding(buildingId)
            val segmentIndex = getSegmentIndexForBuilding(buildingId)
            
            android.util.Log.d("MapViewModel", "Successfully loaded building $buildingId: bitmap=${bitmap.width}x${bitmap.height}, mapInfo=${finalMapInfo.width}x${finalMapInfo.height}")
            android.util.Log.d("MapViewModel", "Route for building $buildingId: ${if (routeForBuilding != null) "${routeForBuilding.nodes.size} nodes" else "none"}, segmentIndex: $segmentIndex")
            
            _floorplanBitmap.value = bitmap
            _mapInfo.value = finalMapInfo
            
            _mapState.value = _mapState.value.copy(
                selectedBuilding = buildingId,
                route = routeForBuilding,
                startNode = routeForBuilding?.nodes?.firstOrNull(),
                destinationNode = routeForBuilding?.nodes?.lastOrNull(),
                navigationSteps = routeForBuilding?.steps ?: emptyList(),
                scale = 1f,
                translateX = 0f,
                translateY = 0f
            )
            _uiState.value = MapUiState.Ready(_mapState.value)
            android.util.Log.d("MapViewModel", "Switched to building: $buildingId with route: ${routeForBuilding != null}")
        }
    }
    
    fun loadRoute(roomId: String, startNodeId: String) {
        viewModelScope.launch {
            val endRoom = repository.getRoomById(roomId)
            if (endRoom == null) {
                _mapState.value = _mapState.value.copy(
                    route = null,
                    crossBuildingRoute = null,
                    destinationRoom = null,
                    startNode = null,
                    destinationNode = null,
                    navigationSteps = emptyList(),
                    currentStepIndex = 0,
                    currentSegmentIndex = 0,
                    isRouteUnavailable = true
                )
                return@launch
            }
            
            val endBuildingId = repository.getRoomBuildingId(roomId) ?: _mapState.value.selectedBuilding
            
            var startBuildingId = _mapState.value.selectedBuilding
            val startRoom = repository.getRoomById(startNodeId)
            if (startRoom != null) {
                startBuildingId = repository.getRoomBuildingId(startRoom.id) ?: _mapState.value.selectedBuilding
            } else {
                for ((buildingId, building) in repository.getAllBuildings()) {
                    if (building.graph.nodes.any { it.id == startNodeId }) {
                        startBuildingId = buildingId
                        break
                    }
                }
            }
            
            if (startBuildingId != endBuildingId) {
                android.util.Log.d("MapViewModel", "=== Starting cross-building navigation ===")
                android.util.Log.d("MapViewModel", "Start building: $startBuildingId, End building: $endBuildingId")
                
                val startRoomId = if (startRoom != null) {
                    startRoom.id
                } else {
                    val building = repository.getBuilding(startBuildingId)
                    val room = building?.rooms?.find { it.nodeId == startNodeId }
                    room?.id ?: startNodeId // Fallback to nodeId if no room found
                }
                
                android.util.Log.d("MapViewModel", "Computing cross-building route: $startRoomId -> $roomId")
                val crossRoute = repository.getCrossBuildingRoute(startRoomId, roomId)
                
                if (crossRoute != null && crossRoute.segments.isNotEmpty()) {
                    val firstSegment = crossRoute.segments[0]
                    val initialBuildingId = firstSegment.buildingId
                    
                    android.util.Log.d("MapViewModel", "Cross-building route computed successfully")
                    android.util.Log.d("MapViewModel", "Segments: [0]=${crossRoute.segments[0].buildingId}, [1]=${if (crossRoute.segments.size > 1) crossRoute.segments[1].buildingId else "N/A"}")
                    android.util.Log.d("MapViewModel", "Initial building (from segment 0): $initialBuildingId")
                    android.util.Log.d("MapViewModel", "Setting selectedBuilding to: $initialBuildingId (start building)")
                    
                    _mapState.value = _mapState.value.copy(
                        crossBuildingRoute = crossRoute,
                        currentSegmentIndex = 0,
                        route = firstSegment.route,
                        destinationRoom = endRoom,
                        startNode = firstSegment.route.nodes.firstOrNull(),
                        destinationNode = firstSegment.route.nodes.lastOrNull(),
                        navigationSteps = firstSegment.route.steps,
                        currentStepIndex = 0,
                        isRouteUnavailable = false,
                        selectedBuilding = initialBuildingId
                    )
                    
                    android.util.Log.d("MapViewModel", "Calling switchBuilding($initialBuildingId) to load map")
                    switchBuilding(initialBuildingId)
                    
                    android.util.Log.d("MapViewModel", "Cross-building navigation initialized. Map should show: $initialBuildingId")
                } else {
                    android.util.Log.e("MapViewModel", "Failed to compute cross-building route")
                    _mapState.value = _mapState.value.copy(
                        route = null,
                        crossBuildingRoute = null,
                        isRouteUnavailable = true
                    )
                }
                return@launch
            }
            
            val buildingId = endBuildingId
            val route = repository.getRoute(startNodeId, endRoom.nodeId, buildingId)
            
            val building = repository.getBuilding(buildingId)
            val startNode = building?.graph?.nodes?.find { it.id == startNodeId }
            val destinationNode = building?.graph?.nodes?.find { it.id == endRoom.nodeId }
            
            val isRouteUnavailable = route == null || route.nodes.isEmpty()
            
            _mapState.value = _mapState.value.copy(
                route = route,
                crossBuildingRoute = null,
                currentSegmentIndex = 0,
                destinationRoom = endRoom,
                startNode = startNode,
                destinationNode = destinationNode,
                navigationSteps = if (isRouteUnavailable) emptyList() else route.steps,
                currentStepIndex = 0,
                isRouteUnavailable = isRouteUnavailable,
                selectedBuilding = buildingId
            )
            
            switchBuilding(buildingId)
            
            if (!isRouteUnavailable) {
                val nodes = route!!.nodes
                val minX = nodes.minOfOrNull { it.x } ?: 0.0
                val minY = nodes.minOfOrNull { it.y } ?: 0.0
                val maxX = nodes.maxOfOrNull { it.x } ?: 0.0
                val maxY = nodes.maxOfOrNull { it.y } ?: 0.0
                
                initialBounds = BoundingBox(minX, minY, maxX, maxY)
            } else if (destinationNode != null) {
                val node = destinationNode
                initialBounds = BoundingBox(
                    node.x - 20.0,
                    node.y - 20.0,
                    node.x + 20.0,
                    node.y + 20.0
                )
            }
        }
    }
    
    fun loadDestinationOnly(roomId: String) {
        viewModelScope.launch {
            val room = repository.getRoomById(roomId)
            if (room != null) {
                val buildingId = repository.getRoomBuildingId(roomId) ?: "J"
                val building = repository.getBuilding(buildingId)
                val destinationNode = building?.graph?.nodes?.find { it.id == room.nodeId }
                
                android.util.Log.d("MapViewModel", "Loading destination only: room=$roomId, building=$buildingId, node=${destinationNode?.id}")
                
                val focusCoord = if (destinationNode != null) {
                    Pair(destinationNode.x, destinationNode.y)
                } else if (room.position != null) {
                    Pair(room.position.x, room.position.y)
                } else {
                    null
                }
                
                _mapState.value = _mapState.value.copy(
                    destinationRoom = room,
                    destinationNode = destinationNode,
                    route = null,
                    startNode = null,
                    selectedBuilding = buildingId,
                    crossBuildingRoute = null,
                    currentSegmentIndex = 0,
                    focusTarget = focusCoord
                )
                
                switchBuilding(buildingId)
                
                if (focusCoord != null) {
                    centerOnCoordinate(focusCoord.first, focusCoord.second)
                }
            }
        }
    }
    
    fun centerOnCoordinate(worldX: Double, worldY: Double) {
        viewModelScope.launch {
            val mapInfo = _mapInfo.value
            if (mapInfo != null) {
                val targetScale = 1.5f.coerceIn(0.5f, 5f)
                _mapState.value = _mapState.value.copy(
                    scale = targetScale,
                    translateX = 0f,
                    translateY = 0f,
                    focusTarget = Pair(worldX, worldY)
                )
                _recenterTrigger.value = _recenterTrigger.value + 1
                android.util.Log.d("MapViewModel", "Centering on coordinate: ($worldX, $worldY) with scale $targetScale")
                
                kotlinx.coroutines.delay(100)
                _mapState.value = _mapState.value.copy(focusTarget = null)
            }
        }
    }
    
    fun updateMapState(scale: Float, translateX: Float, translateY: Float) {
        _mapState.value = _mapState.value.copy(
            scale = scale,
            translateX = translateX,
            translateY = translateY
        )
    }
    
    private var _recenterTrigger = MutableStateFlow(0)
    val recenterTrigger: StateFlow<Int> = _recenterTrigger.asStateFlow()
    
    fun recenterMap() {
        _mapState.value = _mapState.value.copy(
            scale = 1f,
            translateX = 0f,
            translateY = 0f
        )
        _recenterTrigger.value = _recenterTrigger.value + 1
    }
    
    fun zoomIn() {
        val currentScale = _mapState.value.scale
        val newScale = (currentScale * 1.2f).coerceAtMost(5f)
        android.util.Log.d("MapViewModel", "Zoom in: $currentScale -> $newScale")
        _mapState.value = _mapState.value.copy(
            scale = newScale
        )
        _mapState.value = _mapState.value.copy(
            translateX = _mapState.value.translateX,
            translateY = _mapState.value.translateY
        )
    }
    
    fun zoomOut() {
        val currentScale = _mapState.value.scale
        val newScale = (currentScale / 1.2f).coerceAtLeast(0.5f)
        android.util.Log.d("MapViewModel", "Zoom out: $currentScale -> $newScale")
        _mapState.value = _mapState.value.copy(
            scale = newScale
        )
        _mapState.value = _mapState.value.copy(
            translateX = _mapState.value.translateX,
            translateY = _mapState.value.translateY
        )
    }
    
    fun findNearestNode(x: Double, y: Double): Node? {
        return try {
            val building = repository.getBuilding(_mapState.value.selectedBuilding)
            if (building == null) {
                android.util.Log.w("MapViewModel", "Building not loaded: ${_mapState.value.selectedBuilding}")
                return null
            }
            val engine = RoutingEngine(building.graph)
            engine.findNearestNode(x, y)
        } catch (e: Exception) {
            android.util.Log.e("MapViewModel", "Error finding nearest node", e)
            null
        }
    }
    
    fun continueToNextBuildingSegment() {
        viewModelScope.launch {
            val crossRoute = _mapState.value.crossBuildingRoute ?: return@launch
            val currentSegment = _mapState.value.currentSegmentIndex
            
            if (currentSegment < crossRoute.segments.size - 1) {
                val nextSegment = crossRoute.segments[currentSegment + 1]
                val nextBuildingId = nextSegment.buildingId
                
                android.util.Log.d("MapViewModel", "=== Switching to next building segment ===")
                android.util.Log.d("MapViewModel", "Current segment: $currentSegment/${crossRoute.segments.size - 1}, Next building: $nextBuildingId")
                android.util.Log.d("MapViewModel", "Next segment route nodes: ${nextSegment.route.nodes.size}")
                
                _mapState.value = _mapState.value.copy(
                    route = null,
                    navigationSteps = emptyList()
                )
                
                switchBuilding(nextBuildingId)
                
                _mapState.value = _mapState.value.copy(
                    currentSegmentIndex = currentSegment + 1,
                    route = nextSegment.route,
                    startNode = nextSegment.route.nodes.firstOrNull(),
                    destinationNode = nextSegment.route.nodes.lastOrNull(),
                    navigationSteps = nextSegment.route.steps,
                    currentStepIndex = 0,
                    selectedBuilding = nextBuildingId,
                    scale = 1f,
                    translateX = 0f,
                    translateY = 0f
                )
                
                android.util.Log.d("MapViewModel", "Building switch complete. New building: $nextBuildingId, Route nodes: ${nextSegment.route.nodes.size}, Selected building in state: ${_mapState.value.selectedBuilding}")
            }
        }
    }
    
    fun getRouteForBuilding(buildingId: String): Route? {
        val state = _mapState.value
        val crossRoute = state.crossBuildingRoute
        
        if (crossRoute != null) {
            val segment = crossRoute.segments.find { it.buildingId == buildingId }
            return segment?.route
        } else {
            return state.route
        }
    }
    
    fun getSegmentIndexForBuilding(buildingId: String): Int? {
        val crossRoute = _mapState.value.crossBuildingRoute ?: return null
        return crossRoute.segments.indexOfFirst { it.buildingId == buildingId }.takeIf { it >= 0 }
    }
    
    fun setCurrentStepIndex(index: Int) {
        val steps = _mapState.value.navigationSteps
        if (index >= 0 && index < steps.size) {
            _mapState.value = _mapState.value.copy(currentStepIndex = index)
        }
    }
    
    fun previousStep() {
        val currentIndex = _mapState.value.currentStepIndex
        if (currentIndex > 0) {
            _mapState.value = _mapState.value.copy(currentStepIndex = currentIndex - 1)
        }
    }
    
    fun loadSOSRoute(startNodeId: String) {
        viewModelScope.launch {
            if (!repository.isDataLoaded()) {
                val success = repository.loadAllAssets()
                if (!success || !repository.isDataLoaded()) {
                    _mapState.value = _mapState.value.copy(
                        route = null,
                        destinationRoom = null,
                        startNode = null,
                        destinationNode = null,
                        navigationSteps = emptyList(),
                        currentStepIndex = 0,
                        isRouteUnavailable = true
                    )
                    return@launch
                }
            }
            
            val exitNode = repository.findNearestExitNode(startNodeId)
            val startNode = repository.getNodeById(startNodeId)
            
            if (exitNode == null || startNode == null) {
                _mapState.value = _mapState.value.copy(
                    route = null,
                    destinationRoom = null,
                    startNode = startNode,
                    destinationNode = null,
                    navigationSteps = emptyList(),
                    currentStepIndex = 0,
                    isRouteUnavailable = true
                )
                return@launch
            }
            
            val route = repository.getRoute(startNodeId, exitNode.id)
            val isRouteUnavailable = route == null || route.nodes.isEmpty()
            
            _mapState.value = _mapState.value.copy(
                route = route,
                destinationRoom = null,
                startNode = startNode,
                destinationNode = exitNode,
                navigationSteps = if (isRouteUnavailable) emptyList() else route.steps,
                currentStepIndex = 0,
                isRouteUnavailable = isRouteUnavailable
            )
            
            if (!isRouteUnavailable) {
                val nodes = route!!.nodes
                val minX = nodes.minOfOrNull { it.x } ?: 0.0
                val minY = nodes.minOfOrNull { it.y } ?: 0.0
                val maxX = nodes.maxOfOrNull { it.x } ?: 0.0
                val maxY = nodes.maxOfOrNull { it.y } ?: 0.0
                
                initialBounds = BoundingBox(minX, minY, maxX, maxY)
            } else {
                initialBounds = BoundingBox(
                    exitNode.x - 20.0,
                    exitNode.y - 20.0,
                    exitNode.x + 20.0,
                    exitNode.y + 20.0
                )
            }
        }
    }
}

