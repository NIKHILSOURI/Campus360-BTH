package com.example.campus360.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.campus360.data.NavigationDirection
import com.example.campus360.data.NavigationStep
import com.example.campus360.navigation.Screen
import com.example.campus360.ui.theme.PrimaryBlue
import android.util.Log

@Composable
fun MapScreen(
    navController: NavController,
    roomId: String = "",
    startNodeId: String = "",
    pickMode: Boolean = false,
    sosMode: Boolean = false,
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val mapState by viewModel.mapState.collectAsState()
    val floorplanBitmap by viewModel.floorplanBitmap.collectAsState()
    val mapInfo by viewModel.mapInfo.collectAsState()
    val recenterTrigger by viewModel.recenterTrigger.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    
    
    val currentRoomId = remember(roomId) { roomId }
    
    
    val navigationSteps = mapState.navigationSteps
    val currentStepIndex = mapState.currentStepIndex
    var showStepsPanel by remember { mutableStateOf(false) }
    
    LaunchedEffect(roomId, startNodeId, pickMode, sosMode) {
        Log.d("MapScreen", "MapScreen loaded: roomId=$roomId, startNodeId=$startNodeId, pickMode=$pickMode, sosMode=$sosMode")
        if (sosMode) {
            
            if (startNodeId.isNotEmpty()) {
                Log.d("MapScreen", "Loading SOS route: startNodeId=$startNodeId")
                viewModel.loadSOSRoute(startNodeId)
            } else {
                
                Log.d("MapScreen", "startNodeId is empty in SOS mode, enabling location pick")
            }
        } else if (pickMode) {
            
            if (roomId.isNotEmpty()) {
                Log.d("MapScreen", "Loading destination only for pick mode: roomId=$roomId")
                viewModel.loadDestinationOnly(roomId)
            } else {
                Log.e("MapScreen", "roomId is empty in pick mode!")
            }
        } else {
            if (roomId.isNotEmpty() && startNodeId.isNotEmpty()) {
                Log.d("MapScreen", "Loading route: roomId=$roomId, startNodeId=$startNodeId")
                viewModel.loadRoute(roomId, startNodeId)
            } else if (roomId.isNotEmpty()) {
                Log.d("MapScreen", "Loading destination only: roomId=$roomId")
                viewModel.loadDestinationOnly(roomId)
            }
        }
    }
    
    Scaffold(
        bottomBar = {
            if (!pickMode) {
                val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Map.route
                com.example.campus360.ui.components.BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        when (route) {
                            Screen.Home.route -> navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            Screen.Map.route -> {
                                // Already on Map screen, do nothing
                            }
                            Screen.Search.route -> navController.navigate("${Screen.Search.route}?query=") {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            Screen.Settings.route -> navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            else -> {}
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FC))
        ) {
            when (val state = uiState) {
                is MapUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is MapUiState.Ready -> {
                
                    MapView(
                        bitmap = floorplanBitmap,
                        mapInfo = mapInfo,
                        route = mapState.route,
                        destinationNode = mapState.destinationNode,
                        startNode = mapState.startNode,
                        recenterTrigger = recenterTrigger,
                        scale = mapState.scale,
                        translateX = mapState.translateX,
                        translateY = mapState.translateY,
                        onScaleChange = { scale ->
                            viewModel.updateMapState(scale, mapState.translateX, mapState.translateY)
                        },
                        onTranslateChange = { tx, ty ->
                            viewModel.updateMapState(mapState.scale, tx, ty)
                        },
                        onMapClick = if (pickMode || (sosMode && startNodeId.isEmpty())) { x, y ->
                            
                            try {
                                Log.d("MapScreen", "Map clicked in ${if (sosMode) "SOS" else "pick"} mode at ($x, $y)")
                                val node = viewModel.findNearestNode(x, y)
                                node?.let {
                                    Log.d("MapScreen", "Node selected: ${it.id} at ($x, $y)")
                                    if (sosMode) {
                                        
                                        Log.d("MapScreen", "Navigating to SOS route: startNodeId=${it.id}")
                                        navController.navigate("${Screen.Map.route}?roomId=&startNodeId=${it.id}&pickMode=false&sosMode=true") {
                                            launchSingleTop = true
                                        }
                                    } else if (currentRoomId.isNotEmpty()) {
                                        
                                        Log.d("MapScreen", "Navigating to map with route: roomId=$currentRoomId, startNodeId=${it.id}")
                                        navController.navigate("${Screen.Map.route}?roomId=$currentRoomId&startNodeId=${it.id}&pickMode=false&sosMode=false") {
                                            
                                            popUpTo(Screen.ChooseStartLocation.route) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        Log.e("MapScreen", "roomId is empty, cannot navigate")
                                        navController.popBackStack()
                                    }
                                } ?: run {
                                    Log.e("MapScreen", "No node found at ($x, $y)")
                                }
                            } catch (e: Exception) {
                                Log.e("MapScreen", "Error handling map click", e)
                            }
                        } else null,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                   
                    TopControls(
                        onBackClick = { 
                            Log.d("MapScreen", "Back button clicked in pickMode=$pickMode")
                            navController.popBackStack() 
                        },
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    
                    
                    if (pickMode || (sosMode && startNodeId.isEmpty())) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp, start = 16.dp, end = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (sosMode) Color(0xFFDC3545).copy(alpha = 0.9f) else PrimaryBlue.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = if (sosMode) "Tap on the map to select your current location for emergency exit route" else "Tap on the map to select your starting location",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    
                    if (sosMode && !pickMode) {
                        EmergencyRouteLabel(
                            isRouteUnavailable = mapState.isRouteUnavailable,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        )
                    }
                    
                    
                    if (!pickMode) {
                        if (mapState.isRouteUnavailable) {
                            
                            RouteUnavailableMessage(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 16.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp)
                            )
                        } else if (navigationSteps.isNotEmpty()) {
                            
                            NavigationStepsPanel(
                                steps = navigationSteps,
                                currentStepIndex = currentStepIndex,
                                destinationName = if (sosMode) "Emergency Exit" else (mapState.destinationRoom?.name ?: "Destination"),
                                onStepClick = { index -> viewModel.setCurrentStepIndex(index) },
                                onToggleSteps = { showStepsPanel = !showStepsPanel },
                                isExpanded = showStepsPanel,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 16.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp)
                            )
                        }
                    }
                    
                    // Bottom Controls (zoom, recenter)
                    if (!pickMode) {
                        BottomControls(
                            onRecenter = {
                                // Trigger recenter - this will reset the map to show the route/destination
                                viewModel.recenterMap()
                            },
                            onZoomIn = {
                                viewModel.zoomIn()
                            },
                            onZoomOut = {
                                viewModel.zoomOut()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp)
                        )
                    }
                }
                is MapUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Go Back")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopControls(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        
        FloatingActionButton(
            onClick = onBackClick,
            modifier = Modifier.size(48.dp),
            containerColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF0D121B),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}



@Composable
private fun NavigationStepsPanel(
    steps: List<NavigationStep>,
    currentStepIndex: Int,
    destinationName: String,
    onStepClick: (Int) -> Unit,
    onToggleSteps: () -> Unit,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.9f)
    ) {
        
        Button(
            onClick = onToggleSteps,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue,
                contentColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (currentStepIndex < steps.size) {
                            "Step ${currentStepIndex + 1} of ${steps.size}"
                        } else {
                            "Navigation Steps"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (currentStepIndex < steps.size) {
                        Text(
                            text = steps[currentStepIndex].instruction,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Hide Steps" else "Show Steps",
                    tint = Color.White
                )
            }
        }
        
        
        if (isExpanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                tonalElevation = 4.dp
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(steps.size) { index ->
                        val step = steps[index]
                        NavigationStepItem(
                            step = step,
                            isCurrent = index == currentStepIndex,
                            onClick = { onStepClick(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyRouteLabel(
    isRouteUnavailable: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isRouteUnavailable) Color(0xFFFF5252) else Color(0xFFFF9800),
        tonalElevation = 4.dp
    ) {
        Text(
            text = if (isRouteUnavailable) "No emergency exit route found" else "Emergency route to nearest exit",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun RouteUnavailableMessage(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(0.9f),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No directions found",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D121B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = "We couldn't find a route to this destination.",
                    fontSize = 14.sp,
                    color = Color(0xFF4C669A),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun NavigationStepItem(
    step: NavigationStep,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isCurrent) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = if (isCurrent) PrimaryBlue else Color(0xFFE7EBF3)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "${step.stepNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) Color.White else Color(0xFF0D121B)
                    )
                }
            }
            
            
            val directionIcon = when (step.direction) {
                NavigationDirection.START -> Icons.Default.PlayArrow
                NavigationDirection.STRAIGHT -> Icons.AutoMirrored.Filled.ArrowForward
                NavigationDirection.LEFT -> Icons.AutoMirrored.Filled.ArrowBack
                NavigationDirection.RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
                NavigationDirection.SLIGHT_LEFT -> Icons.Default.Place
                NavigationDirection.SLIGHT_RIGHT -> Icons.Default.Place
                NavigationDirection.SHARP_LEFT -> Icons.AutoMirrored.Filled.ArrowBack
                NavigationDirection.SHARP_RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
                NavigationDirection.ARRIVE -> Icons.Default.Place
            }
            
            
            val rotation = when (step.direction) {
                NavigationDirection.LEFT -> -90f
                NavigationDirection.SLIGHT_LEFT -> -45f
                NavigationDirection.SHARP_LEFT -> -135f
                NavigationDirection.RIGHT -> 90f
                NavigationDirection.SLIGHT_RIGHT -> 45f
                NavigationDirection.SHARP_RIGHT -> 135f
                NavigationDirection.START,
                NavigationDirection.STRAIGHT,
                NavigationDirection.ARRIVE -> 0f
            }
            
            Icon(
                imageVector = directionIcon,
                contentDescription = step.instruction,
                tint = if (isCurrent) PrimaryBlue else Color(0xFF4C669A),
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
            )
            
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = step.instruction,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) Color(0xFF0D121B) else Color(0xFF4C669A)
                )
                if (step.distance > 0) {
                    Text(
                        text = String.format("%.1f units", step.distance),
                        fontSize = 12.sp,
                        color = Color(0xFF4C669A).copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomControls(
    onRecenter: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        
        FloatingActionButton(
            onClick = onRecenter,
            modifier = Modifier.size(48.dp),
            containerColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Recenter",
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        
        
        FloatingActionButton(
            onClick = onZoomIn,
            modifier = Modifier.size(48.dp),
            containerColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Zoom In",
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        
        
        FloatingActionButton(
            onClick = onZoomOut,
            modifier = Modifier.size(48.dp),
            containerColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Zoom Out",
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

