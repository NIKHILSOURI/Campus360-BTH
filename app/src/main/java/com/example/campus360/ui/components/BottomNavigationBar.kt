package com.example.campus360.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.campus360.R
import com.example.campus360.navigation.Screen
import com.example.campus360.ui.theme.PrimaryBlue

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFF8F9FC),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(
                color = Color(0xFFE7EBF3),
                thickness = 1.dp
            )
            
            val context = LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = context.getString(R.string.home),
                    isSelected = currentRoute == Screen.Home.route,
                    onClick = { onNavigate(Screen.Home.route) }
                )
                
                BottomNavItem(
                    icon = Icons.Default.Place,
                    label = context.getString(R.string.map),
                    isSelected = currentRoute == Screen.Map.route || currentRoute.startsWith(Screen.Map.route),
                    onClick = { onNavigate(Screen.Map.route) }
                )
                
                BottomNavItem(
                    icon = Icons.Outlined.Search,
                    label = context.getString(R.string.search),
                    isSelected = currentRoute == Screen.Search.route || currentRoute.startsWith(Screen.Search.route),
                    onClick = { onNavigate(Screen.Search.route) }
                )
                
                BottomNavItem(
                    icon = Icons.Outlined.Settings,
                    label = context.getString(R.string.settings),
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

