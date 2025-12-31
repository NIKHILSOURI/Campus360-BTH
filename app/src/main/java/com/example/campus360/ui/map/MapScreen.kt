package com.example.campus360.ui.map
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import com.example.campus360.R
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
    
    val selectedBuilding = mapState.selectedBuilding
    
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
        val windowInsets = WindowInsets.systemBars
        val insetsPadding = windowInsets.asPaddingValues()
        val topPadding = insetsPadding.calculateTopPadding()
        val bottomPadding = insetsPadding.calculateBottomPadding()
        
        Column(
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
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = topPadding),
                        color = Color(0xFFF8F9FC),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BuildingSelector(
                                selectedBuilding = selectedBuilding,
                                onBuildingSelected = { building ->
                                    android.util.Log.d("MapScreen", "Building selector clicked: $building")
                                    viewModel.switchBuilding(building)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val routeForCurrentBuilding = viewModel.getRouteForBuilding(selectedBuilding)
                        key(selectedBuilding) {
                            MapView(
                                bitmap = floorplanBitmap,
                            mapInfo = mapInfo,
                            route = routeForCurrentBuilding,
                            destinationNode = mapState.destinationNode,
                            startNode = mapState.startNode,
                            recenterTrigger = recenterTrigger,
                            scale = mapState.scale,
                            translateX = mapState.translateX,
                            translateY = mapState.translateY,
                            selectedBuilding = selectedBuilding,
                            focusTarget = mapState.focusTarget,
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
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1f)
                            )
                        }
                    
                        TopControls(
                            onBackClick = { 
                                Log.d("MapScreen", "Back button clicked in pickMode=$pickMode")
                                navController.popBackStack() 
                            },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .zIndex(10f)
                        )
                        
                        val crossRoute = mapState.crossBuildingRoute
                        val currentSegmentIndex = mapState.currentSegmentIndex
                        val segmentIndexForBuilding = viewModel.getSegmentIndexForBuilding(selectedBuilding)
                        
                        val shouldShowSwitchButton = if (crossRoute != null && segmentIndexForBuilding != null) {
                            val isFirstBuilding = segmentIndexForBuilding == 0
                            val isOnFirstSegment = currentSegmentIndex == 0
                            isFirstBuilding && isOnFirstSegment
                        } else {
                            false
                        }
                        
                        if (shouldShowSwitchButton && crossRoute != null) {
                            val currentSegment = crossRoute.segments.getOrNull(currentSegmentIndex)
                            val nextSegment = crossRoute.segments.getOrNull(currentSegmentIndex + 1)
                            val instruction = currentSegment?.instruction ?: nextSegment?.instruction ?: "Continue to next building"
                            val nextBuildingId = nextSegment?.buildingId ?: ""
                            
                            CrossBuildingNavigationBanner(
                                instruction = instruction,
                                onContinue = {
                                    android.util.Log.d("MapScreen", "Switch button clicked. Current building: $selectedBuilding, Next building: $nextBuildingId")
                                    viewModel.continueToNextBuildingSegment()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 60.dp, start = 16.dp, end = 16.dp)
                                    .zIndex(10f)
                                    .fillMaxWidth(0.9f)
                            )
                        }
                    
                    
                        if (pickMode || (sosMode && startNodeId.isEmpty())) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = bottomPadding + innerPadding.calculateBottomPadding() + 16.dp, start = 16.dp, end = 16.dp)
                                    .fillMaxWidth(0.9f)
                                    .zIndex(10f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (sosMode) Color(0xFFDC3545).copy(alpha = 0.9f) else PrimaryBlue.copy(alpha = 0.9f)
                            ) {
                                val context = LocalContext.current
                                Text(
                                    text = if (sosMode) context.getString(R.string.tap_to_select_sos) else context.getString(R.string.tap_to_select_start),
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
                                    .padding(top = 60.dp)
                                    .zIndex(10f)
                            )
                        }
                        
                        if (!pickMode) {
                            if (mapState.isRouteUnavailable) {
                                RouteUnavailableMessage(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 16.dp, bottom = bottomPadding + innerPadding.calculateBottomPadding() + 16.dp)
                                        .zIndex(10f)
                                        .fillMaxWidth(0.75f)
                                )
                            } else if (navigationSteps.isNotEmpty()) {
                                NavigationStepsPanel(
                                    steps = navigationSteps,
                                    currentStepIndex = currentStepIndex,
                                    onStepClick = { index -> viewModel.setCurrentStepIndex(index) },
                                    onToggleSteps = { showStepsPanel = !showStepsPanel },
                                    isExpanded = showStepsPanel,
                                    destinationName = if (sosMode) "Emergency Exit" else (mapState.destinationRoom?.name ?: "Destination"),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 16.dp, end = 80.dp, bottom = bottomPadding + innerPadding.calculateBottomPadding() + 16.dp)
                                        .zIndex(10f)
                                        .fillMaxWidth(0.75f)
                                )
                            }
                        }
                        
                        if (!pickMode) {
                            BottomControls(
                                onRecenter = {
                                    android.util.Log.d("MapScreen", "Recenter button clicked")
                                    viewModel.recenterMap()
                                },
                                onZoomIn = {
                                    android.util.Log.d("MapScreen", "Zoom in button clicked")
                                    viewModel.zoomIn()
                                },
                                onZoomOut = {
                                    android.util.Log.d("MapScreen", "Zoom out button clicked")
                                    viewModel.zoomOut()
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = bottomPadding + innerPadding.calculateBottomPadding() + 16.dp)
                                    .zIndex(10f)
                            )
                        }
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
                            val context = LocalContext.current
                            Button(onClick = { navController.popBackStack() }) {
                                Text(context.getString(R.string.go_back))
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
            val context = LocalContext.current
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = context.getString(R.string.back),
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
    onStepClick: (Int) -> Unit,
    onToggleSteps: () -> Unit,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    destinationName: String = ""
) {
    Column(
        modifier = modifier
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
        val context = LocalContext.current
        Text(
            text = if (isRouteUnavailable) context.getString(R.string.no_emergency_route) else context.getString(R.string.emergency_route),
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
                val context = LocalContext.current
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = context.getString(R.string.warning),
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = context.getString(R.string.no_directions),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D121B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = context.getString(R.string.no_route_message),
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
                    val context = LocalContext.current
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f %s", step.distance, context.getString(R.string.units)),
                        fontSize = 12.sp,
                        color = Color(0xFF4C669A).copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildingSelector(
    selectedBuilding: String,
    onBuildingSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState(), enabled = false),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BuildingButton(
            building = "J",
            isSelected = selectedBuilding == "J",
            onClick = { 
                android.util.Log.d("MapScreen", "Building J button clicked")
                onBuildingSelected("J") 
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        BuildingButton(
            building = "H",
            isSelected = selectedBuilding == "H",
            onClick = { 
                android.util.Log.d("MapScreen", "Building H button clicked")
                onBuildingSelected("H") 
            }
        )
    }
}

@Composable
private fun BuildingButton(
    building: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .clickable(
                onClick = {
                    android.util.Log.d("MapScreen", "BuildingButton clicked: Building $building")
                    onClick()
                },
                interactionSource = interactionSource
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryBlue else Color.White,
        tonalElevation = if (isSelected) 4.dp else 2.dp
    ) {
        Text(
            text = "Building $building",
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF0D121B),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun CrossBuildingNavigationBanner(
    instruction: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFF9800).copy(alpha = 0.9f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Route includes building switch",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = instruction,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Continue to Next Building",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
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
            onClick = {
                android.util.Log.d("BottomControls", "Recenter FAB clicked")
                onRecenter()
            },
            modifier = Modifier.size(48.dp),
            containerColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            val context = LocalContext.current
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = context.getString(R.string.recenter),
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        
        
        FloatingActionButton(
            onClick = {
                android.util.Log.d("BottomControls", "Zoom in FAB clicked")
                onZoomIn()
            },
            modifier = Modifier.size(48.dp),
            containerColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            val context = LocalContext.current
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = context.getString(R.string.zoom_in),
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        
        FloatingActionButton(
            onClick = {
                android.util.Log.d("BottomControls", "Zoom out FAB clicked")
                onZoomOut()
            },
            modifier = Modifier.size(48.dp),
            containerColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalDivider(
                    color = PrimaryBlue,
                    thickness = 3.dp,
                    modifier = Modifier.width(16.dp)
                )
            }
        }
    }
}

