package com.example.campus360.ui.choosestart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.LocalContext
import com.example.campus360.R
import com.example.campus360.data.Landmark
import com.example.campus360.navigation.Screen
import com.example.campus360.ui.theme.PrimaryBlue
import android.util.Log

@Composable
fun ChooseStartLocationScreen(
    navController: NavController,
    roomId: String,
    viewModel: ChooseStartLocationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val destinationRoom by viewModel.destinationRoom.collectAsState()
    val allPlaces by viewModel.allPlaces.collectAsState()
    val landmarks = viewModel.landmarks
    
    
    val currentRoomId = remember(roomId) { roomId }
    
    LaunchedEffect(roomId) {
        Log.d("ChooseStartLocationScreen", "Screen loaded with roomId: $roomId")
        if (roomId.isNotEmpty()) {
            viewModel.loadDestinationRoom(roomId)
        } else {
            Log.e("ChooseStartLocationScreen", "roomId is empty!")
        }
    }
    
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ChooseStartLocationUiState.NavigateToMap -> {
                Log.d("ChooseStartLocationScreen", "Navigating to map with route: roomId=${state.roomId}, startNodeId=${state.startNodeId}")
                navController.navigate("${Screen.Map.route}?roomId=${state.roomId}&startNodeId=${state.startNodeId}&pickMode=false") {
                    
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
                viewModel.clearNavigation()
            }
            is ChooseStartLocationUiState.ShowMapPicker -> {
                Log.d("ChooseStartLocationScreen", "Navigating to map picker: roomId=${state.roomId}")
                
                navController.navigate("${Screen.Map.route}?roomId=${state.roomId}&startNodeId=&pickMode=true") {
                    
                    launchSingleTop = true
                }
                viewModel.clearMapPicker()
            }
            else -> {}
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FC))
    ) {
        
        Header(onBackClick = { navController.popBackStack() })
        
      
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            
            val context = LocalContext.current
            Text(
                text = context.getString(R.string.choose_from_list),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D121B),
                modifier = Modifier.padding(vertical = 20.dp)
            )
            
            
            if (destinationRoom == null) {
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        if (allPlaces.isNotEmpty()) {
                            allPlaces.forEachIndexed { index, place ->
                                PlaceItem(
                                    room = place,
                                    onClick = { 
                                        Log.d("ChooseStartLocationScreen", "Place clicked in list: ${place.name}")
                                        viewModel.selectPlace(place) 
                                    }
                                )
                                if (index < allPlaces.size - 1) {
                                    HorizontalDivider(
                                        color = Color(0xFFCFD7E7),
                                        thickness = 1.dp
                                    )
                                }
                            }
                        } else {
                            
                            landmarks.forEachIndexed { index, landmark ->
                                LandmarkItem(
                                    landmark = landmark,
                                    onClick = { 
                                        Log.d("ChooseStartLocationScreen", "Landmark clicked in list: ${landmark.label}")
                                        viewModel.selectLandmark(landmark) 
                                    }
                                )
                                if (index < landmarks.size - 1) {
                                    HorizontalDivider(
                                        color = Color(0xFFCFD7E7),
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
        BottomNavigation(
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
                    Screen.Map.route -> navController.navigate("${Screen.Map.route}?roomId=&startNodeId=&pickMode=false") {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
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

@Composable
private fun Header(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FC))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            val context = LocalContext.current
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = context.getString(R.string.back),
                tint = Color(0xFF0D121B),
                modifier = Modifier.size(24.dp)
            )
        }
        
        val context = LocalContext.current
        Text(
            text = context.getString(R.string.start_location),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D121B),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun PlaceItem(
    room: com.example.campus360.data.Room,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                Log.d("PlaceItem", "Place clicked: ${room.name}")
                onClick() 
            },
        color = Color.White,
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color(0xFFE7EBF3)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = room.name,
                        tint = Color(0xFF0D121B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            
            Text(
                text = room.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF0D121B),
                modifier = Modifier.weight(1f)
            )
            
            
            val context = LocalContext.current
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = context.getString(R.string.select),
                tint = Color(0xFF4C669A),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LandmarkItem(
    landmark: Landmark,
    onClick: () -> Unit
) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        Log.d("LandmarkItem", "Landmark clicked: ${landmark.label}")
                        onClick() 
                    },
                color = Color.White,
                shape = RoundedCornerShape(0.dp)
            ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color(0xFFE7EBF3)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = landmark.label,
                        tint = Color(0xFF0D121B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            
            Text(
                text = landmark.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF0D121B),
                modifier = Modifier.weight(1f)
            )
            
          
            val context = LocalContext.current
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = context.getString(R.string.select),
                tint = Color(0xFF4C669A),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MapPickerSection(
    onShowMap: () -> Unit,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onMyLocation: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE7EBF3)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            
            Button(
                onClick = {
                    Log.d("MapPickerSection", "Map picker button clicked")
                    onShowMap()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                )
            ) {
                val context = LocalContext.current
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = context.getString(R.string.pick_from_map),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = context.getString(R.string.pick_from_map),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val context = LocalContext.current
            Text(
                text = context.getString(R.string.pick_from_map_instruction),
                fontSize = 14.sp,
                color = Color(0xFF4C669A),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF8F9FC),
        tonalElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = Color(0xFFE7EBF3), thickness = 1.dp)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = currentRoute == Screen.Home.route,
                    onClick = { onNavigate(Screen.Home.route) }
                )
                
                BottomNavItem(
                    icon = Icons.Default.Place,
                    label = "Map",
                    isSelected = currentRoute == Screen.Map.route,
                    onClick = { onNavigate(Screen.Map.route) }
                )
                
                BottomNavItem(
                    icon = Icons.Default.Search,
                    label = "Search",
                    isSelected = currentRoute == Screen.Search.route,
                    onClick = { onNavigate(Screen.Search.route) }
                )
                
                BottomNavItem(
                    icon = Icons.Outlined.Settings,
                    label = "Settings",
                    isSelected = currentRoute == Screen.Settings.route,
                    onClick = { onNavigate(Screen.Settings.route) }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF0D121B) else Color(0xFF4C669A),
            modifier = Modifier.size(24.dp)
        )
    }
}

