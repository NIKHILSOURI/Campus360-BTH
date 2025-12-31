package com.example.campus360.ui.home
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.LocalContext
import com.example.campus360.R
import com.example.campus360.navigation.Screen
import com.example.campus360.ui.theme.PrimaryBlue

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentRooms by viewModel.recentRooms.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories = viewModel.categories
    
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is HomeUiState.NavigateToSearch -> {
                val encodedQuery = java.net.URLEncoder.encode(state.query, "UTF-8")
                navController.navigate("${Screen.Search.route}?query=$encodedQuery")
                viewModel.clearNavigation()
            }
            is HomeUiState.NavigateToCategory -> {
                navController.navigate("category_results/${state.category.id}")
                viewModel.clearNavigation()
            }
            is HomeUiState.NavigateToMap -> {
                navController.navigate("${Screen.Map.route}?roomId=&startNodeId=&pickMode=false")
                viewModel.clearNavigation()
            }
            is HomeUiState.NavigateToSOS -> {
                navController.navigate("${Screen.Map.route}?roomId=&startNodeId=&pickMode=false&sosMode=true")
                viewModel.clearNavigation()
            }
            else -> {}
        }
    }
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
            com.example.campus360.ui.components.BottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    when (route) {
                        Screen.Home.route -> {}
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FC))
                .padding(innerPadding)
        ) {
            
            Header(
                onGlobeClick = { navController.navigate(Screen.Settings.route) }
            )
            
          
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearchClick = { viewModel.search(searchQuery) }
            )
            
            
            QuickCategories(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategoryClick = { viewModel.selectCategory(it) }
            )
            
            
            if (recentRooms.isNotEmpty()) {
                RecentDestinations(
                    rooms = recentRooms,
                    onRoomClick = { room ->
                        navController.navigate(Screen.DestinationDetails.createRoute(room.id))
                    }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            
            Spacer(modifier = Modifier.height(24.dp))
            OpenMapButton(
                onClick = { viewModel.openMap() },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            )
            
            
            Spacer(modifier = Modifier.height(12.dp))
            SOSButton(
                onClick = { viewModel.openSOS() },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = innerPadding.calculateBottomPadding())
            )
        }
    }
}

@Composable
private fun Header(onGlobeClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FC))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = context.getString(R.string.map_icon),
            tint = Color(0xFF0D121B),
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = context.getString(R.string.app_name),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D121B),
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        IconButton(onClick = onGlobeClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = context.getString(R.string.language_icon),
                tint = Color(0xFF0D121B),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE7EBF3)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSearchClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = context.getString(R.string.search_icon),
                tint = Color(0xFF4C669A),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = if (query.isEmpty()) context.getString(R.string.search_placeholder) else query,
                color = if (query.isEmpty()) Color(0xFF4C669A).copy(alpha = 0.6f) else Color(0xFF0D121B),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickCategories(
    categories: List<Category>,
    selectedCategory: String?,
    onCategoryClick: (Category) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        val context = LocalContext.current
        Text(
            text = context.getString(R.string.quick_categories),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D121B),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            categories.forEach { category ->
                CategoryButton(
                    category = category,
                    isSelected = selectedCategory == category.id,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryButton(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val icon = when (category.id) {
            "class" -> Icons.Default.Menu
            "lab" -> Icons.Default.Build
            "hall" -> Icons.Default.Home
            "popular" -> Icons.Default.Star
            else -> Icons.Default.LocationOn
        }
        
        val iconColor = if (isSelected) {
            Color(0xFF0D121B)
        } else {
            Color(0xFF4C669A)
        }
        
        Icon(
            imageVector = icon,
            contentDescription = category.name,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = category.name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = iconColor
        )
        
        if (isSelected) {
            HorizontalDivider(
                modifier = Modifier
                    .width(32.dp)
                    .height(3.dp)
                    .padding(top = 4.dp),
                color = PrimaryBlue,
                thickness = 3.dp
            )
        }
    }
}

@Composable
private fun RecentDestinations(
    rooms: List<com.example.campus360.data.Room>,
    onRoomClick: (com.example.campus360.data.Room) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        val context = LocalContext.current
        Text(
            text = context.getString(R.string.recent_destinations),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D121B),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            rooms.forEach { room ->
                RecentDestinationItem(
                    room = room,
                    onClick = { onRoomClick(room) }
                )
            }
        }
    }
}

@Composable
private fun RecentDestinationItem(
    room: com.example.campus360.data.Room,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(Color(0xFFF8F9FC)),
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
                val context = LocalContext.current
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = context.getString(R.string.location),
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
    }
}

@Composable
private fun OpenMapButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp), // 56-60px range (58dp ≈ 58px)
        shape = RoundedCornerShape(14.dp), // 12-16px range (14dp ≈ 14px)
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3D6DFF) // Primary blue #3D6DFF
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 3.dp, // Soft elevation 2-4dp
            pressedElevation = 2.dp
        ),
        contentPadding = PaddingValues(0.dp) // Remove default padding for better centering
    ) {
        val context = LocalContext.current
        Text(
            text = context.getString(R.string.open_map),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SOSButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFDC3545) // Red color for emergency
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp, // Higher elevation for prominence
            pressedElevation = 3.dp
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = context.getString(R.string.sos_icon),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = context.getString(R.string.sos),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
