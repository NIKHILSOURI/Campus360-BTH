package com.example.campus360.data

import kotlin.math.*

class RoutingEngine(private val graph: Graph) {
    
    data class PathNode(
        val node: Node,
        val gCost: Double = 0.0,
        val hCost: Double = 0.0,
        val parent: PathNode? = null
    ) {
        val fCost: Double get() = gCost + hCost
    }
    
    fun getRoute(startNodeId: String, endNodeId: String): Route? {
        val startNode = graph.nodes.find { it.id == startNodeId }
        val endNode = graph.nodes.find { it.id == endNodeId }
        
        if (startNode == null || endNode == null) {
            return null
        }
        
        if (startNodeId == endNodeId) {
            return Route(
                nodes = listOf(startNode),
                totalDistance = 0.0,
                steps = listOf(
                    NavigationStep(
                        stepNumber = 1,
                        instruction = "You are already at your destination",
                        direction = NavigationDirection.ARRIVE,
                        distance = 0.0,
                        fromNode = startNode,
                        toNode = startNode
                    )
                )
            )
        }
        
        val openSet = mutableListOf<PathNode>()
        val closedSet = mutableSetOf<String>()
        
        openSet.add(PathNode(startNode, hCost = heuristic(startNode, endNode)))
        
        while (openSet.isNotEmpty()) {
            val current = openSet.minByOrNull { it.fCost } ?: break
            openSet.remove(current)
            closedSet.add(current.node.id)
            
            if (current.node.id == endNodeId) {
                val path = mutableListOf<Node>()
                var node: PathNode? = current
                while (node != null) {
                    path.add(0, node.node)
                    node = node.parent
                }
                
                var totalDistance = 0.0
                for (i in 0 until path.size - 1) {
                    val edge = graph.edges.find { 
                        it.from == path[i].id && it.to == path[i + 1].id 
                    }
                    totalDistance += edge?.weight ?: 0.0
                }
                
                val steps = generateNavigationSteps(path)
                return Route(path, totalDistance, steps)
            }
            
            val neighbors = graph.edges
                .filter { it.from == current.node.id }
                .mapNotNull { edge ->
                    graph.nodes.find { it.id == edge.to }
                }
            
            for (neighbor in neighbors) {
                if (closedSet.contains(neighbor.id)) {
                    continue
                }
                
                val tentativeGCost = current.gCost + getEdgeWeight(current.node.id, neighbor.id)
                val existingNode = openSet.find { it.node.id == neighbor.id }
                
                if (existingNode == null) {
                    openSet.add(
                        PathNode(
                            node = neighbor,
                            gCost = tentativeGCost,
                            hCost = heuristic(neighbor, endNode),
                            parent = current
                        )
                    )
                } else if (tentativeGCost < existingNode.gCost) {
                    openSet.remove(existingNode)
                    openSet.add(
                        PathNode(
                            node = neighbor,
                            gCost = tentativeGCost,
                            hCost = existingNode.hCost,
                            parent = current
                        )
                    )
                }
            }
        }
        
        return null
    }
    
    private fun generateNavigationSteps(path: List<Node>): List<NavigationStep> {
        if (path.size < 2) {
            return emptyList()
        }
        
        val steps = mutableListOf<NavigationStep>()
        
        steps.add(
            NavigationStep(
                stepNumber = 1,
                instruction = "Start from your location",
                direction = NavigationDirection.START,
                distance = 0.0,
                fromNode = path[0],
                toNode = path[0]
            )
        )
        
        for (i in 0 until path.size - 1) {
            val fromNode = path[i]
            val toNode = path[i + 1]
            val distance = getEdgeWeight(fromNode.id, toNode.id)
            
            val direction = if (i == 0) {
                calculateDirection(fromNode, toNode, if (i + 1 < path.size - 1) path[i + 2] else null)
            } else {
                val prevNode = path[i - 1]
                calculateTurnDirection(prevNode, fromNode, toNode)
            }
            
            val instruction = when (direction) {
                NavigationDirection.STRAIGHT -> "Continue straight"
                NavigationDirection.LEFT -> "Turn left"
                NavigationDirection.RIGHT -> "Turn right"
                NavigationDirection.SLIGHT_LEFT -> "Slight left"
                NavigationDirection.SLIGHT_RIGHT -> "Slight right"
                NavigationDirection.SHARP_LEFT -> "Sharp left"
                NavigationDirection.SHARP_RIGHT -> "Sharp right"
                NavigationDirection.ARRIVE -> "Arrive at destination"
                NavigationDirection.START -> "Start"
            }
            
            val isLastStep = i == path.size - 2
            
            steps.add(
                NavigationStep(
                    stepNumber = steps.size + 1,
                    instruction = if (isLastStep) "Arrive at destination" else instruction,
                    direction = if (isLastStep) NavigationDirection.ARRIVE else direction,
                    distance = distance,
                    fromNode = fromNode,
                    toNode = toNode
                )
            )
        }
        
        return steps
    }
    
    private fun calculateDirection(from: Node, to: Node, next: Node?): NavigationDirection {
        if (next == null) {
            return NavigationDirection.STRAIGHT
        }
        
        val angle1 = atan2(to.y - from.y, to.x - from.x)
        val angle2 = atan2(next.y - to.y, next.x - to.x)
        val angleDiff = normalizeAngle(angle2 - angle1)
        
        return when {
            abs(angleDiff) < PI / 12 -> NavigationDirection.STRAIGHT
            angleDiff in (PI / 12)..(PI / 3) -> NavigationDirection.SLIGHT_RIGHT
            angleDiff in (-PI / 3)..(-PI / 12) -> NavigationDirection.SLIGHT_LEFT
            angleDiff in (PI / 3)..(2 * PI / 3) -> NavigationDirection.RIGHT
            angleDiff in (-2 * PI / 3)..(-PI / 3) -> NavigationDirection.LEFT
            angleDiff > 2 * PI / 3 -> NavigationDirection.SHARP_RIGHT
            angleDiff < -2 * PI / 3 -> NavigationDirection.SHARP_LEFT
            else -> NavigationDirection.STRAIGHT
        }
    }
    
    private fun calculateTurnDirection(prev: Node, current: Node, next: Node): NavigationDirection {
        val v1x = current.x - prev.x
        val v1y = current.y - prev.y
        val v2x = next.x - current.x
        val v2y = next.y - current.y
        
        val crossProduct = v1x * v2y - v1y * v2x
        val dotProduct = v1x * v2x + v1y * v2y
        
        val v1Length = sqrt(v1x * v1x + v1y * v1y)
        val v2Length = sqrt(v2x * v2x + v2y * v2y)
        
        if (v1Length == 0.0 || v2Length == 0.0) {
            return NavigationDirection.STRAIGHT
        }
        
        val cosAngle = dotProduct / (v1Length * v2Length)
        val angle = acos(cosAngle.coerceIn(-1.0, 1.0))
        
        return when {
            angle < PI / 12 -> NavigationDirection.STRAIGHT
            crossProduct > 0 -> {
                when {
                    angle < PI / 6 -> NavigationDirection.SLIGHT_RIGHT
                    angle < PI / 2 -> NavigationDirection.RIGHT
                    else -> NavigationDirection.SHARP_RIGHT
                }
            }
            else -> {
                when {
                    angle < PI / 6 -> NavigationDirection.SLIGHT_LEFT
                    angle < PI / 2 -> NavigationDirection.LEFT
                    else -> NavigationDirection.SHARP_LEFT
                }
            }
        }
    }
    
    private fun normalizeAngle(angle: Double): Double {
        var normalized = angle
        while (normalized > PI) normalized -= 2 * PI
        while (normalized < -PI) normalized += 2 * PI
        return normalized
    }
    
    private fun heuristic(node1: Node, node2: Node): Double {
        val dx = node1.x - node2.x
        val dy = node1.y - node2.y
        return sqrt(dx * dx + dy * dy)
    }
    
    private fun getEdgeWeight(fromId: String, toId: String): Double {
        val edge = graph.edges.find { it.from == fromId && it.to == toId }
        if (edge != null) {
            return edge.weight
        }
        val fromNode = graph.nodes.find { it.id == fromId }
        val toNode = graph.nodes.find { it.id == toId }
        return if (fromNode != null && toNode != null) {
            heuristic(fromNode, toNode)
        } else {
            Double.MAX_VALUE
        }
    }
    
    fun findNearestNode(x: Double, y: Double): Node? {
        return graph.nodes.minByOrNull { node ->
            val dx = node.x - x
            val dy = node.y - y
            sqrt(dx * dx + dy * dy)
        }
    }
    
    fun getNodeById(nodeId: String): Node? {
        return graph.nodes.find { it.id == nodeId }
    }
    
    fun getExitNodes(mapWidth: Double, mapHeight: Double, threshold: Double = 5.0): List<Node> {
        return graph.nodes.filter { node ->
            val nearLeft = node.x <= threshold
            val nearRight = node.x >= mapWidth - threshold
            val nearTop = node.y <= threshold
            val nearBottom = node.y >= mapHeight - threshold
            nearLeft || nearRight || nearTop || nearBottom
        }
    }
    

    fun findNearestExitNode(startNodeId: String, mapWidth: Double, mapHeight: Double): Node? {
        val startNode = graph.nodes.find { it.id == startNodeId } ?: return null
        val exitNodes = getExitNodes(mapWidth, mapHeight)
        
        if (exitNodes.isEmpty()) {
            return null
        }
        
        var nearestExit: Node? = null
        var shortestDistance = Double.MAX_VALUE
        
        for (exitNode in exitNodes) {
            val route = getRoute(startNodeId, exitNode.id)
            if (route != null && route.totalDistance < shortestDistance) {
                shortestDistance = route.totalDistance
                nearestExit = exitNode
            }
        }
        
        return nearestExit
    }
}


