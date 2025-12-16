package com.example.campus360.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.campus360.data.Room
import com.example.campus360.navigation.Screen
import com.example.campus360.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryResultsScreen(
    navController: NavController,
    category: String,
    viewModel: CategoryResultsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    
    LaunchedEffect(category) {
        viewModel.loadCategoryRooms(category)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.getCategoryName(category),
                        color = Color(0xFF0D121B),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F9FC)
                )
            )
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            BottomNavigation(currentRoute = currentRoute ?: Screen.Home.route) { route ->
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FC))
                .padding(innerPadding)
        ) {
            if (rooms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No rooms found in this category",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rooms) { room ->
                        RoomItem(
                            room = room,
                            onClick = {
                                navController.navigate(Screen.DestinationDetails.createRoute(room.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomItem(
    room: Room,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            val icon = when (room.type.lowercase()) {
                "clothing" -> Icons.Default.ShoppingCart
                "coffee" -> Icons.Default.Favorite
                "electronics" -> Icons.Default.Phone
                "convenience" -> Icons.Default.ShoppingCart
                "toilet", "restroom" -> Icons.Default.Info
                "sports" -> Icons.Default.Star
                "beauty" -> Icons.Default.Favorite
                "lifestyle" -> Icons.Default.Star
                "retail" -> Icons.Default.ShoppingCart
                "entrance" -> Icons.Default.Home
                "lecture", "lecture_hall" -> Icons.Default.PlayArrow
                "lab", "laboratory" -> Icons.Default.Build
                "conference", "conference_room" -> Icons.Default.PlayArrow
                "classroom", "class" -> Icons.Default.Menu
                else -> Icons.Default.LocationOn
            }
            
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
                        imageVector = icon,
                        contentDescription = room.type,
                        tint = Color(0xFF4C669A),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = room.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0D121B)
                )
                Text(
                    text = room.location,
                    fontSize = 14.sp,
                    color = Color(0xFF4C669A)
                )
            }
            
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate",
                tint = Color(0xFF4C669A),
                modifier = Modifier.size(20.dp)
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
                    icon = Icons.Outlined.Search,
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
