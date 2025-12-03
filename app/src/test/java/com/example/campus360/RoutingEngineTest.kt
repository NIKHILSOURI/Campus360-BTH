package com.example.campus360

import com.example.campus360.data.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for RoutingEngine functionality including exit node identification
 */
class RoutingEngineTest {
    
    @Test
    fun testGetExitNodes() {
        // Create a simple graph with nodes on boundaries
        val nodes = listOf(
            Node("node_top", 10.0, 3.0),      // Top boundary (y <= 5.0)
            Node("node_right", 1396.0, 100.0), // Right boundary (x >= 1395.0)
            Node("node_bottom", 100.0, 1096.0), // Bottom boundary (y >= 1095.0)
            Node("node_left", 3.0, 100.0),     // Left boundary (x <= 5.0)
            Node("node_center", 700.0, 550.0)  // Center (not an exit)
        )
        
        val edges = listOf<Edge>()
        val graph = Graph(nodes, edges)
        val engine = RoutingEngine(graph)
        
        val exitNodes = engine.getExitNodes(1400.0, 1100.0, 5.0)
        
        assertEquals("Should find 4 exit nodes", 4, exitNodes.size)
        assertTrue("Should include top exit", exitNodes.any { it.id == "node_top" })
        assertTrue("Should include right exit", exitNodes.any { it.id == "node_right" })
        assertTrue("Should include bottom exit", exitNodes.any { it.id == "node_bottom" })
        assertTrue("Should include left exit", exitNodes.any { it.id == "node_left" })
        assertFalse("Should not include center node", exitNodes.any { it.id == "node_center" })
    }
    
    @Test
    fun testFindNearestExitNode() {
        // Create a graph with a path from start to exit
        val nodes = listOf(
            Node("start", 100.0, 100.0),
            Node("middle", 200.0, 100.0),
            Node("exit1", 5.0, 100.0),      // Left exit
            Node("exit2", 1395.0, 100.0)    // Right exit (farther)
        )
        
        val edges = listOf(
            Edge("start", "middle", 100.0),
            Edge("middle", "exit1", 195.0),
            Edge("middle", "exit2", 1195.0)
        )
        
        val graph = Graph(nodes, edges)
        val engine = RoutingEngine(graph)
        
        val nearestExit = engine.findNearestExitNode("start", 1400.0, 1100.0)
        
        assertNotNull("Should find an exit", nearestExit)
        assertEquals("Should find the nearest exit", "exit1", nearestExit?.id)
    }
    
    @Test
    fun testGetRoute() {
        val nodes = listOf(
            Node("start", 0.0, 0.0),
            Node("middle", 10.0, 10.0),
            Node("end", 20.0, 20.0)
        )
        
        val edges = listOf(
            Edge("start", "middle", 14.14),
            Edge("middle", "end", 14.14)
        )
        
        val graph = Graph(nodes, edges)
        val engine = RoutingEngine(graph)
        
        val route = engine.getRoute("start", "end")
        
        assertNotNull("Route should exist", route)
        assertEquals("Route should have 3 nodes", 3, route?.nodes?.size)
        assertEquals("Route should start at start", "start", route?.nodes?.first()?.id)
        assertEquals("Route should end at end", "end", route?.nodes?.last()?.id)
    }
    
    @Test
    fun testGetRouteNoPath() {
        val nodes = listOf(
            Node("start", 0.0, 0.0),
            Node("end", 20.0, 20.0)
        )
        
        val edges = listOf<Edge>() // No edges
        
        val graph = Graph(nodes, edges)
        val engine = RoutingEngine(graph)
        
        val route = engine.getRoute("start", "end")
        
        assertNull("Route should not exist when no path", route)
    }
}

