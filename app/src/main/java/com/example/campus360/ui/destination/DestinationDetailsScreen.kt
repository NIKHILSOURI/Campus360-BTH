package com.example.campus360.ui.destination

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.campus360.navigation.Screen
import com.example.campus360.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationDetailsScreen(
    navController: NavController,
    roomId: String,
    viewModel: DestinationDetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    
    LaunchedEffect(roomId) {
        android.util.Log.d("DestinationDetails", "Loading room with ID: $roomId")
        if (roomId.isNotEmpty()) {
            viewModel.loadRoom(roomId)
        } else {
            android.util.Log.e("DestinationDetails", "Room ID is empty!")
        }
    }
    
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is DestinationDetailsUiState.NavigateToMap -> {
                navController.navigate("${Screen.Map.route}?roomId=${state.roomId}&startNodeId=&pickMode=false")
                viewModel.clearNavigation()
            }
            is DestinationDetailsUiState.NavigateToChooseStart -> {
                navController.navigate(Screen.ChooseStartLocation.createRoute(state.roomId))
                viewModel.clearNavigation()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Room Details",
                        color = Color(0xFF0D121B),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0D121B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F9FC)
                )
            )
        },
        bottomBar = {
            BottomNavigation(
                currentRoute = currentRoute ?: Screen.Home.route,
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
    ) { innerPadding ->
        when (val state = uiState) {
            is DestinationDetailsUiState.Idle -> {
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is DestinationDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is DestinationDetailsUiState.Success -> {
                
                val room = remember(state.room) { state.room }
                RoomInformation(
                    room = room,
                    onNavigateFromLocation = { 
                        try {
                            viewModel.navigateFromMyLocation(room.id) 
                        } catch (e: Exception) {
                            android.util.Log.e("DestinationDetails", "Error navigating from location", e)
                        }
                    },
                    onShowOnMap = { 
                        try {
                            viewModel.showOnMap(room.id) 
                        } catch (e: Exception) {
                            android.util.Log.e("DestinationDetails", "Error showing on map", e)
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is DestinationDetailsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                        Button(
                            onClick = { navController.popBackStack() }
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@composable
private fun RoomInformation(
    room: com.example.campus360.data.Room,
    onNavigateFromLocation: () -> Unit,
    onShowOnMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FC))
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        
        Text(
            text = "Room Information",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D121B),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            color = Color.White
        ) {
            Column {
                InfoRow(
                    label = "Room ID",
                    value = room.id
                )
                
                HorizontalDivider(color = Color(0xFFCFD7E7), thickness = 1.dp)
                
                InfoRow(
                    label = "Room Name/Type",
                    value = room.name
                )
                
                HorizontalDivider(color = Color(0xFFCFD7E7), thickness = 1.dp)
                
                InfoRow(
                    label = "Location",
                    value = room.location
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateFromLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                )
            ) {
                Text(
                    text = "Navigate from my location",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Button(
                onClick = onShowOnMap,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7EBF3),
                    contentColor = Color(0xFF0D121B)
                )
            ) {
                Text(
                    text = "Show on map",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D121B)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF4C669A),
            modifier = Modifier.weight(0.3f)
        )
        
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF0D121B),
            modifier = Modifier.weight(0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
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
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
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
            tint = if (isSelected) PrimaryBlue else Color(0xFF4C669A),
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PrimaryBlue else Color(0xFF4C669A)
        )
    }
}
