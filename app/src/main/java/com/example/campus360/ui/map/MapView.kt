package com.example.campus360.ui.map
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.campus360.data.MapInfo
import com.example.campus360.data.Node
import com.example.campus360.data.Route
import com.example.campus360.ui.theme.PrimaryBlue
import kotlin.math.*

@Composable
fun MapView(
    bitmap: Bitmap?,
    mapInfo: MapInfo?,
    route: Route?,
    destinationNode: Node?,
    startNode: Node?,
    recenterTrigger: Int = 0,
    scale: Float = 1f,
    translateX: Float = 0f,
    translateY: Float = 0f,
    selectedBuilding: String = "J",
    onScaleChange: (Float) -> Unit = {},
    onTranslateChange: (Float, Float) -> Unit = { _, _ -> },
    onMapClick: ((Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var internalScale by remember { mutableStateOf(scale) }
    var internalOffsetX by remember { mutableStateOf(translateX) }
    var internalOffsetY by remember { mutableStateOf(translateY) }
    var isUserInteracting by remember { mutableStateOf(false) }
    var lastRoute by remember { mutableStateOf<Route?>(null) }
    var lastDestinationNode by remember { mutableStateOf<Node?>(null) }
    var lastRecenterTrigger by remember { mutableStateOf(0) }
    var interactionEndTime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(scale, translateX, translateY) {
        // Always sync when scale changes (zoom buttons), but respect user interaction for pan
        if (!isUserInteracting || scale != internalScale) {
            internalScale = scale
            internalOffsetX = translateX
            internalOffsetY = translateY
            android.util.Log.d("MapView", "Syncing map state: scale=$scale, translateX=$translateX, translateY=$translateY")
        }
    }
    
    LaunchedEffect(bitmap, mapInfo, canvasSize) {
        if (bitmap != null && mapInfo != null && canvasSize.width > 0 && canvasSize.height > 0 && !isUserInteracting) {
            val mapWidth = mapInfo.width.toFloat()
            val mapHeight = mapInfo.height.toFloat()
            val canvasWidth = canvasSize.width.toFloat()
            val canvasHeight = canvasSize.height.toFloat()
            
            if (mapWidth > 0 && mapHeight > 0) {
                val scaleX = canvasWidth / mapWidth
                val scaleY = canvasHeight / mapHeight
                val fitScale = min(scaleX, scaleY) * 0.9f
                
                internalScale = fitScale
                internalOffsetX = (canvasWidth - mapWidth * fitScale) / 2
                internalOffsetY = (canvasHeight - mapHeight * fitScale) / 2
                onScaleChange(fitScale)
                onTranslateChange(internalOffsetX, internalOffsetY)
            }
        }
    }
    
    LaunchedEffect(route, destinationNode) {
        if (bitmap != null && mapInfo != null && canvasSize.width > 0 && canvasSize.height > 0) {
            val timeSinceInteraction = System.currentTimeMillis() - interactionEndTime
            if (timeSinceInteraction > 300 || interactionEndTime == 0L) {
                val routeChanged = route != lastRoute
                val destinationChanged = destinationNode != lastDestinationNode
                
                if (routeChanged || destinationChanged) {
                    lastRoute = route
                    lastDestinationNode = destinationNode
                    
                    val mapWidth = mapInfo.width.toFloat()
                    val mapHeight = mapInfo.height.toFloat()
                    val canvasWidth = canvasSize.width.toFloat()
                    val canvasHeight = canvasSize.height.toFloat()
                    
                    if (mapWidth <= 0 || mapHeight <= 0) {
                        return@LaunchedEffect
                    }
                    
                    val scaleX = canvasWidth / mapWidth
                    val scaleY = canvasHeight / mapHeight
                    val fitScale = min(scaleX, scaleY) * 0.9f
                    
                    if (route != null && route.nodes.isNotEmpty()) {
                        val nodes = route.nodes
                        val minX = nodes.minOfOrNull { it.x } ?: 0.0
                        val minY = nodes.minOfOrNull { it.y } ?: 0.0
                        val maxX = nodes.maxOfOrNull { it.x } ?: 0.0
                        val maxY = nodes.maxOfOrNull { it.y } ?: 0.0
                        
                        val routeWidth = (maxX - minX).toFloat()
                        val routeHeight = (maxY - minY).toFloat()
                        
                        if (routeWidth > 0 && routeHeight > 0) {
                            val routeScaleX = canvasWidth / routeWidth
                            val routeScaleY = canvasHeight / routeHeight
                            val routeFitScale = min(routeScaleX, routeScaleY) * 0.8f
                            
                            val centerX = ((minX + maxX) / 2).toFloat()
                            val centerY = ((minY + maxY) / 2).toFloat()
                            
                            internalScale = routeFitScale
                            internalOffsetX = canvasWidth / 2 - centerX * routeFitScale
                            internalOffsetY = canvasHeight / 2 - centerY * routeFitScale
                            onScaleChange(routeFitScale)
                            onTranslateChange(internalOffsetX, internalOffsetY)
                        } else {
                            internalScale = fitScale
                            internalOffsetX = (canvasWidth - mapWidth * fitScale) / 2
                            internalOffsetY = (canvasHeight - mapHeight * fitScale) / 2
                            onScaleChange(fitScale)
                            onTranslateChange(internalOffsetX, internalOffsetY)
                        }
                    } else if (destinationNode != null) {
                        val destScale = fitScale * 2f
                        internalScale = destScale
                        internalOffsetX = canvasWidth / 2 - destinationNode.x.toFloat() * destScale
                        internalOffsetY = canvasHeight / 2 - destinationNode.y.toFloat() * destScale
                        onScaleChange(destScale)
                        onTranslateChange(internalOffsetX, internalOffsetY)
                    }
                }
            }
        }
    }
    
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > lastRecenterTrigger && bitmap != null && mapInfo != null && canvasSize.width > 0 && canvasSize.height > 0) {
            lastRecenterTrigger = recenterTrigger
            isUserInteracting = false
            interactionEndTime = 0L
            
            val mapWidth = mapInfo.width.toFloat()
            val mapHeight = mapInfo.height.toFloat()
            val canvasWidth = canvasSize.width.toFloat()
            val canvasHeight = canvasSize.height.toFloat()
            
            if (mapWidth <= 0 || mapHeight <= 0) {
                return@LaunchedEffect
            }
            
            val scaleX = canvasWidth / mapWidth
            val scaleY = canvasHeight / mapHeight
            val fitScale = min(scaleX, scaleY) * 0.9f
            
            if (route != null && route.nodes.isNotEmpty()) {
                val nodes = route.nodes
                val minX = nodes.minOfOrNull { it.x } ?: 0.0
                val minY = nodes.minOfOrNull { it.y } ?: 0.0
                val maxX = nodes.maxOfOrNull { it.x } ?: 0.0
                val maxY = nodes.maxOfOrNull { it.y } ?: 0.0
                
                val routeWidth = (maxX - minX).toFloat()
                val routeHeight = (maxY - minY).toFloat()
                
                if (routeWidth > 0 && routeHeight > 0) {
                    val routeScaleX = canvasWidth / routeWidth
                    val routeScaleY = canvasHeight / routeHeight
                    val routeFitScale = min(routeScaleX, routeScaleY) * 0.8f
                    
                    val centerX = ((minX + maxX) / 2).toFloat()
                    val centerY = ((minY + maxY) / 2).toFloat()
                    
                    internalScale = routeFitScale
                    internalOffsetX = canvasWidth / 2 - centerX * routeFitScale
                    internalOffsetY = canvasHeight / 2 - centerY * routeFitScale
                    onScaleChange(routeFitScale)
                    onTranslateChange(internalOffsetX, internalOffsetY)
                } else {
                    internalScale = fitScale
                    internalOffsetX = (canvasWidth - mapWidth * fitScale) / 2
                    internalOffsetY = (canvasHeight - mapHeight * fitScale) / 2
                    onScaleChange(fitScale)
                    onTranslateChange(internalOffsetX, internalOffsetY)
                }
            } else if (destinationNode != null) {
                val destScale = fitScale * 2f
                internalScale = destScale
                internalOffsetX = canvasWidth / 2 - destinationNode.x.toFloat() * destScale
                internalOffsetY = canvasHeight / 2 - destinationNode.y.toFloat() * destScale
                onScaleChange(destScale)
                onTranslateChange(internalOffsetX, internalOffsetY)
            } else {
                internalScale = fitScale
                internalOffsetX = (canvasWidth - mapWidth * fitScale) / 2
                internalOffsetY = (canvasHeight - mapHeight * fitScale) / 2
                onScaleChange(fitScale)
                onTranslateChange(internalOffsetX, internalOffsetY)
            }
        }
    }
    
    val clickGesture = Modifier.pointerInput(Unit) {
        detectTapGestures { offset ->
            onMapClick?.invoke(
                ((offset.x - internalOffsetX) / internalScale).toDouble(),
                ((offset.y - internalOffsetY) / internalScale).toDouble()
            )
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { centroid, pan, zoom, rotation ->
                        isUserInteracting = true
                        val newScale = (internalScale * zoom).coerceIn(0.5f, 5f)
                        internalScale = newScale
                        internalOffsetX += pan.x
                        internalOffsetY += pan.y
                        onScaleChange(newScale)
                        onTranslateChange(internalOffsetX, internalOffsetY)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isUserInteracting = true
                    },
                    onDrag = { change, dragAmount ->
                        isUserInteracting = true
                        internalOffsetX += dragAmount.x
                        internalOffsetY += dragAmount.y
                        onTranslateChange(internalOffsetX, internalOffsetY)
                    },
                    onDragEnd = {
                        interactionEndTime = System.currentTimeMillis()
                        isUserInteracting = false
                    }
                )
            }
            .then(if (onMapClick != null) clickGesture else Modifier)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (bitmap == null || mapInfo == null) {
                drawContext.canvas.drawRect(
                    androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                    androidx.compose.ui.graphics.Paint().apply {
                        color = androidx.compose.ui.graphics.Color.LightGray
                    }
                )
                return@Canvas
            }
            
            drawImage(
                image = bitmap.asImageBitmap(),
                srcSize = IntSize(bitmap.width, bitmap.height),
                dstSize = IntSize(
                    (mapInfo.width * internalScale).toInt(),
                    (mapInfo.height * internalScale).toInt()
                ),
                dstOffset = IntOffset(internalOffsetX.toInt(), internalOffsetY.toInt())
            )
            
            route?.let { route ->
                if (route.nodes.size > 1) {
                    val path = Path()
                    val nodePositions = route.nodes.map { node ->
                        Offset(
                            node.x.toFloat() * internalScale + internalOffsetX,
                            node.y.toFloat() * internalScale + internalOffsetY
                        )
                    }
                    
                    nodePositions.forEachIndexed { index, offset ->
                        if (index == 0) {
                            path.moveTo(offset.x, offset.y)
                        } else {
                            path.lineTo(offset.x, offset.y)
                        }
                    }
                    
                    drawPath(
                        path = path,
                        color = Color(0x66000000),
                        style = Stroke(width = 14f * internalScale.coerceIn(0.5f, 2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    
                    drawPath(
                        path = path,
                        color = PrimaryBlue,
                        style = Stroke(width = 12f * internalScale.coerceIn(0.5f, 2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    
                    if (nodePositions.size > 1) {
                        val arrowInterval = maxOf(1, nodePositions.size / 8)
                        for (i in 0 until nodePositions.size - 1 step arrowInterval) {
                            val from = nodePositions[i]
                            val to = nodePositions[i + 1]
                            
                            val midX = (from.x + to.x) / 2f
                            val midY = (from.y + to.y) / 2f
                            
                            val dx = to.x - from.x
                            val dy = to.y - from.y
                            val distance = sqrt(dx * dx + dy * dy)
                            
                            if (distance > 0.1f) {
                                val angle = atan2(dy, dx)
                                
                                val arrowSize = 12f * internalScale.coerceIn(0.5f, 2f)
                                val arrowPath = Path().apply {
                                    val headX = midX + cos(angle) * arrowSize * 0.5f
                                    val headY = midY + sin(angle) * arrowSize * 0.5f
                                    val leftX = midX - cos(angle) * arrowSize * 0.3f + sin(angle) * arrowSize * 0.4f
                                    val leftY = midY - sin(angle) * arrowSize * 0.3f - cos(angle) * arrowSize * 0.4f
                                    val rightX = midX - cos(angle) * arrowSize * 0.3f - sin(angle) * arrowSize * 0.4f
                                    val rightY = midY - sin(angle) * arrowSize * 0.3f + cos(angle) * arrowSize * 0.4f
                                    
                                    moveTo(headX, headY)
                                    lineTo(leftX, leftY)
                                    lineTo(rightX, rightY)
                                    close()
                                }
                                
                                drawPath(
                                    path = arrowPath,
                                    color = Color.White,
                                    style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                                drawPath(
                                    path = arrowPath,
                                    color = PrimaryBlue,
                                    style = Fill
                                )
                            }
                        }
                    }
                }
            }
            
            startNode?.let { node ->
                val x = node.x.toFloat() * internalScale + internalOffsetX
                val y = node.y.toFloat() * internalScale + internalOffsetY
                val markerRadius = 20f * internalScale.coerceIn(0.5f, 2f)
                
                drawCircle(
                    color = Color(0x80000000),
                    radius = markerRadius + 4f,
                    center = Offset(x, y),
                    style = Fill
                )
                
                drawCircle(
                    color = PrimaryBlue,
                    radius = markerRadius,
                    center = Offset(x, y),
                    style = Fill
                )
                
                drawCircle(
                    color = Color.White,
                    radius = markerRadius,
                    center = Offset(x, y),
                    style = Stroke(width = 3f * internalScale.coerceIn(0.5f, 1.5f))
                )
                
                drawCircle(
                    color = Color.White,
                    radius = markerRadius * 0.4f,
                    center = Offset(x, y),
                    style = Fill
                )
            }
            
            destinationNode?.let { node ->
                val x = node.x.toFloat() * internalScale + internalOffsetX
                val y = node.y.toFloat() * internalScale + internalOffsetY
                val markerRadius = 22f * internalScale.coerceIn(0.5f, 2f)
                
                drawCircle(
                    color = Color(0x80000000),
                    radius = markerRadius + 4f,
                    center = Offset(x, y),
                    style = Fill
                )
                
                drawCircle(
                    color = Color(0xFFFF5252),
                    radius = markerRadius,
                    center = Offset(x, y),
                    style = Fill
                )
                
                drawCircle(
                    color = Color.White,
                    radius = markerRadius,
                    center = Offset(x, y),
                    style = Stroke(width = 3f * internalScale.coerceIn(0.5f, 1.5f))
                )
                
                drawCircle(
                    color = Color.White,
                    radius = markerRadius * 0.4f,
                    center = Offset(x, y),
                    style = Fill
                )
            }
        }
    }
}

