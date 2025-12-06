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
    val destinationRoom: Room? = null,
    val startNode: Node? = null,
    val destinationNode: Node? = null,
    val navigationSteps: List<NavigationStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val isRouteUnavailable: Boolean = false
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
            if (repository.isDataLoaded()) {
                val bitmap = repository.floorplanBitmap
                val info = repository.mapInfo
                
                if (bitmap != null && info != null) {
                    _floorplanBitmap.value = bitmap
                    _mapInfo.value = info
                    _uiState.value = MapUiState.Ready(_mapState.value)
                    return@launch
                }
            }
            
            val success = repository.loadAllAssets()
            
            if (success && repository.isDataLoaded()) {
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
    
    fun loadRoute(roomId: String, startNodeId: String) {
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
            
            val room = repository.getRoomById(roomId)
            if (room == null) {
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
            
            val route = repository.getRoute(startNodeId, room.nodeId)
            
            val startNode = repository.getNodeById(startNodeId)
            val destinationNode = repository.getNodeById(room.nodeId)
            
            val isRouteUnavailable = route == null || route.nodes.isEmpty()
            
            _mapState.value = _mapState.value.copy(
                route = route,
                destinationRoom = room,
                startNode = startNode,
                destinationNode = destinationNode,
                navigationSteps = if (isRouteUnavailable) emptyList() else (route?.steps ?: emptyList()),
                currentStepIndex = 0,
                isRouteUnavailable = isRouteUnavailable
            )
            
            if (!isRouteUnavailable) {
                route?.let { validRoute ->
                    val nodes = validRoute.nodes
                    val minX = nodes.minOfOrNull { it.x } ?: 0.0
                    val minY = nodes.minOfOrNull { it.y } ?: 0.0
                    val maxX = nodes.maxOfOrNull { it.x } ?: 0.0
                    val maxY = nodes.maxOfOrNull { it.y } ?: 0.0
                    
                    initialBounds = BoundingBox(minX, minY, maxX, maxY)
                }
            } else if (destinationNode != null) {
                destinationNode.let { node ->
                    initialBounds = BoundingBox(
                        node.x - 20.0,
                        node.y - 20.0,
                        node.x + 20.0,
                        node.y + 20.0
                    )
                }
            }
        }
    }
    
    fun loadDestinationOnly(roomId: String) {
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
            
            val room = repository.getRoomById(roomId)
            if (room != null) {
                val destinationNode = repository.getNodeById(room.nodeId)
                
                _mapState.value = _mapState.value.copy(
                    destinationRoom = room,
                    destinationNode = destinationNode,
                    route = null,
                    startNode = null
                )
                
                
                destinationNode?.let { node ->
                    initialBounds = BoundingBox(
                        node.x - 20.0,
                        node.y - 20.0,
                        node.x + 20.0,
                        node.y + 20.0
                    )
                }
            }
        }
    }
    
    fun getInitialBounds(): BoundingBox? {
        return initialBounds ?: _mapInfo.value?.let { info ->
            BoundingBox(0.0, 0.0, info.width, info.height)
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
    
    fun triggerRecenter() {
        _recenterTrigger.value = _recenterTrigger.value + 1
    }
    
    fun zoomIn() {
        _mapState.value = _mapState.value.copy(
            scale = (_mapState.value.scale * 1.2f).coerceAtMost(5f)
        )
    }
    
    fun zoomOut() {
        _mapState.value = _mapState.value.copy(
            scale = (_mapState.value.scale / 1.2f).coerceAtLeast(0.5f)
        )
    }
    
    fun findNearestNode(x: Double, y: Double): Node? {
        return try {
            if (!repository.isDataLoaded()) {
                android.util.Log.w("MapViewModel", "Data not loaded when finding nearest node")
                return null
            }
            repository.findNearestNode(x, y)
        } catch (e: Exception) {
            android.util.Log.e("MapViewModel", "Error finding nearest node", e)
            null
        }
    }
    
    fun setCurrentStepIndex(index: Int) {
        val steps = _mapState.value.navigationSteps
        if (index >= 0 && index < steps.size) {
            _mapState.value = _mapState.value.copy(currentStepIndex = index)
        }
    }
    
    fun nextStep() {
        val currentIndex = _mapState.value.currentStepIndex
        val steps = _mapState.value.navigationSteps
        if (currentIndex < steps.size - 1) {
            _mapState.value = _mapState.value.copy(currentStepIndex = currentIndex + 1)
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
                destinationRoom = null, // No room for exit
                startNode = startNode,
                destinationNode = exitNode,
                navigationSteps = if (isRouteUnavailable) emptyList() else (route?.steps ?: emptyList()),
                currentStepIndex = 0,
                isRouteUnavailable = isRouteUnavailable
            )
            
            if (!isRouteUnavailable) {
                route?.let { validRoute ->
                    val nodes = validRoute.nodes
                    val minX = nodes.minOfOrNull { it.x } ?: 0.0
                    val minY = nodes.minOfOrNull { it.y } ?: 0.0
                    val maxX = nodes.maxOfOrNull { it.x } ?: 0.0
                    val maxY = nodes.maxOfOrNull { it.y } ?: 0.0
                    
                    initialBounds = BoundingBox(minX, minY, maxX, maxY)
                }
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

