package com.example.campus360.ui.search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.LocalContext
import com.example.campus360.R
import com.example.campus360.data.Room
import com.example.campus360.navigation.Screen
import com.example.campus360.ui.theme.PrimaryBlue

@Composable
fun SearchScreen(
    navController: NavController,
    query: String = "",
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.query.collectAsState()
    val filteredRooms by viewModel.filteredRooms.collectAsState()
    
    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            viewModel.setInitialQuery(query)
        }
    }
    
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SearchUiState.NavigateToDestination -> {
                navController.navigate(Screen.DestinationDetails.createRoute(state.roomId))
                viewModel.clearNavigation()
            }
            else -> {}
        }
    }
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Search.route
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
                        Screen.Map.route -> navController.navigate("${Screen.Map.route}?roomId=&startNodeId=&pickMode=false") {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        Screen.Search.route -> {}
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FC))
                .padding(innerPadding)
        ) {
            
            Header()
            
            
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setQuery(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val context = LocalContext.current
                Text(
                    text = context.getString(R.string.recent_searches),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4C669A),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRooms) { room ->
                        RoomResultItem(
                            room = room,
                            onClick = { viewModel.onRoomClick(room) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FC))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.size(48.dp))
        
        val context = LocalContext.current
        Text(
            text = context.getString(R.string.find_a_room),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D121B),
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp), 
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFFFFF), 
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = context.getString(R.string.search_icon),
                tint = Color(0xFF4C669A),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = context.getString(R.string.search_by_name),
                        color = Color(0xFFA0A4A8), 
                        fontSize = 15.5.sp 
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp), // Match search bar height
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedPlaceholderColor = Color(0xFFA0A4A8),
                    unfocusedPlaceholderColor = Color(0xFFA0A4A8)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp
                ),
                singleLine = true
            )
        }
    }
}

@Composable
private fun RoomResultItem(
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
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
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
                            tint = Color(0xFF0D121B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                
                Column {
                    Text(
                        text = room.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0D121B)
                    )
                    Text(
                        text = room.id,
                        fontSize = 14.sp,
                        color = Color(0xFF4C669A)
                    )
                }
            }
            
            
            val context = LocalContext.current
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = context.getString(R.string.navigate),
                tint = Color(0xFF4C669A),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

