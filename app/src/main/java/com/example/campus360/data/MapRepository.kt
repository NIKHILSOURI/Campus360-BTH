
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
    
    private var _mapInfo: MapInfo? = null
    private var _rooms: List<Room>? = null
    private var _roomsById: Map<String, Room> = emptyMap() // Fast O(1) lookup
    private var _graph: Graph? = null
    private var _nodesById: Map<String, Node> = emptyMap() 
    private var _floorplanBitmap: Bitmap? = null
    
    val mapInfo: MapInfo? get() = _mapInfo
    val rooms: List<Room>? get() = _rooms
    val graph: Graph? get() = _graph
    val floorplanBitmap: Bitmap? get() = _floorplanBitmap
    
    suspend fun loadAllAssets(): Boolean = withContext(Dispatchers.IO) {
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
    
    private fun loadBitmapFromAssets(fileName: String): Bitmap? {
        return try {
            val inputStream = context.assets.open(fileName)
            

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            

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
                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
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
        return _roomsById[roomId]
    }
    
    fun searchRooms(query: String): List<Room> {
        val allRooms = _rooms ?: return emptyList()
        val lowerQuery = query.lowercase().trim()
        if (lowerQuery.isEmpty()) return emptyList()
        

        return allRooms.filter { room ->
            room.name.lowercase().contains(lowerQuery) ||
            room.id.lowercase().contains(lowerQuery) ||
            room.type.lowercase().contains(lowerQuery)
        }
    }
    
    fun getRoutingEngine(): RoutingEngine? {
        val graph = _graph ?: return null
        return RoutingEngine(graph)
    }
    
    fun getRoute(startNodeId: String, endNodeId: String): Route? {
        if (!isDataLoaded()) {
            return null
        }
        val engine = getRoutingEngine() ?: return null
        return try {
            engine.getRoute(startNodeId, endNodeId)
        } catch (e: Exception) {
            android.util.Log.e("MapRepository", "Error getting route from $startNodeId to $endNodeId", e)
            null
        }
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

