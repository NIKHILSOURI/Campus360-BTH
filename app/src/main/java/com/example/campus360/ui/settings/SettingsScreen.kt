package com.example.campus360.ui.settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.campus360.navigation.Screen
import com.example.campus360.ui.theme.PrimaryBlue
import androidx.compose.ui.platform.LocalContext
import com.example.campus360.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        context.getString(R.string.settings),
                        color = Color(0xFF0D121B),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F9FC)
                )
            )
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Settings.route
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
                        Screen.Search.route -> navController.navigate("${Screen.Search.route}?query=") {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        Screen.Settings.route -> {
                            
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
                .verticalScroll(rememberScrollState())
        ) {
            LanguageSelectionSection(
                selectedLanguage = state.selectedLanguage,
                onLanguageSelected = { languageCode ->
                    viewModel.updateLanguage(languageCode)
                    (context as? android.app.Activity)?.recreate()
                }
            )
            
            DownloadMapSection(
                onDownloadMap = {
                    viewModel.downloadMap()
                }
            )
            
            ContactSection()
            
            AboutSection(
                appVersion = state.appVersion
            )
        }
    }
}

@Composable
private fun LanguageSelectionSection(
    selectedLanguage: String?,
    onLanguageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            text = context.getString(R.string.language),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D121B),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
        
        LanguageOption(
            languageName = context.getString(R.string.english),
            isSelected = selectedLanguage == "en",
            onClick = { onLanguageSelected("en") },
            languageCode = "en"
        )
        
        LanguageOption(
            languageName = context.getString(R.string.swedish),
            isSelected = selectedLanguage == "sv",
            onClick = { onLanguageSelected("sv") },
            languageCode = "sv"
        )
    }
}

@Composable
private fun LanguageOption(
    languageName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    languageCode: String = ""
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = languageName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0D121B)
            )
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadMapSection(
    onDownloadMap: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        Text(
            text = context.getString(R.string.map),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D121B),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable(onClick = onDownloadMap),
            shape = RoundedCornerShape(12.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.download_map),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0D121B)
                )
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Download",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactSection() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = context.getString(R.string.contact),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D121B),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
        
        
        ContactItem(
            email = "Test@gmail.com",
            name = "Nikhi,Ajay,Vaatsav"
        )
    }
}

@Composable
private fun ContactItem(
    email: String,
    name: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Email",
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = email,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0D121B)
                )
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF4C669A),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AboutSection(
    appVersion: String
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        Text(
            text = context.getString(R.string.about_campus360),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0D121B),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
        
        
        AboutItem(
            title = context.getString(R.string.app_description),
            description = context.getString(R.string.app_description_text)
        )
        
        
        AboutItem(
            title = context.getString(R.string.course_university_info),
            description = context.getString(R.string.course_university_info_text)
        )
        
        
        AboutItem(
            title = context.getString(R.string.version_number),
            description = "${context.getString(R.string.version_number)} $appVersion"
        )
    }
}

@Composable
private fun AboutItem(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0D121B),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = description,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF4C669A),
            lineHeight = 20.sp
        )
    }
}


