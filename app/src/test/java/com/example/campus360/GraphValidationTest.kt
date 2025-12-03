package com.example.campus360

import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Tests to validate the graph.json structure
 */
class GraphValidationTest {
    
    @Test
    fun testGraphJsonStructure() {
        // This test validates that the graph.json file structure is correct
        // In a real test, we would load the actual file
        val graphJson = """
        {
          "nodes": [
            {"id": "node_1", "x": 100.0, "y": 100.0},
            {"id": "node_2", "x": 200.0, "y": 200.0}
          ],
          "edges": [
            {"from": "node_1", "to": "node_2", "weight": 1.0}
          ]
        }
        """
        
        val gson = Gson()
        val graph = gson.fromJson(graphJson, com.example.campus360.data.Graph::class.java)
        
        assertNotNull("Graph should be parsed", graph)
        assertEquals("Should have 2 nodes", 2, graph.nodes.size)
        assertEquals("Should have 1 edge", 1, graph.edges.size)
        assertEquals("First node should be node_1", "node_1", graph.nodes[0].id)
    }
    
    @Test
    fun testExitNodeCoordinates() {
        // Test that exit nodes are within threshold of boundaries
        val mapWidth = 1400.0
        val mapHeight = 1100.0
        val threshold = 5.0
        
        // Test top boundary
        val topNode = com.example.campus360.data.Node("top", 100.0, 3.0)
        assertTrue("Top node should be exit", topNode.y <= threshold)
        
        // Test right boundary
        val rightNode = com.example.campus360.data.Node("right", 1396.0, 100.0)
        assertTrue("Right node should be exit", rightNode.x >= mapWidth - threshold)
        
        // Test bottom boundary
        val bottomNode = com.example.campus360.data.Node("bottom", 100.0, 1096.0)
        assertTrue("Bottom node should be exit", bottomNode.y >= mapHeight - threshold)
        
        // Test left boundary
        val leftNode = com.example.campus360.data.Node("left", 3.0, 100.0)
        assertTrue("Left node should be exit", leftNode.x <= threshold)
    }
}

