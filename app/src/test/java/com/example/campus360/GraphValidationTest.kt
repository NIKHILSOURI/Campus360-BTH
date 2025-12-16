package com.example.campus360

import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class GraphValidationTest {
    
    @Test
    fun testGraphJsonStructure() {
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
        val mapWidth = 1400.0
        val mapHeight = 1100.0
        val threshold = 5.0
        
        val topNode = com.example.campus360.data.Node("top", 100.0, 3.0)
        assertTrue("Top node should be exit", topNode.y <= threshold)
        
        val rightNode = com.example.campus360.data.Node("right", 1396.0, 100.0)
        assertTrue("Right node should be exit", rightNode.x >= mapWidth - threshold)
        
        val bottomNode = com.example.campus360.data.Node("bottom", 100.0, 1096.0)
        assertTrue("Bottom node should be exit", bottomNode.y >= mapHeight - threshold)
        
        val leftNode = com.example.campus360.data.Node("left", 3.0, 100.0)
        assertTrue("Left node should be exit", leftNode.x <= threshold)
    }
}

