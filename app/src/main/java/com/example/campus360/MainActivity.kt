package com.example.campus360

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.campus360.navigation.NavGraph
import com.example.campus360.ui.theme.Campus360Theme
import com.example.campus360.util.LocaleHelper
import com.example.campus360.util.PreferencesManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        
        val preferencesManager = PreferencesManager(this)
        val savedLanguage = preferencesManager.getLanguage() ?: "sv" 
        LocaleHelper.setLocale(this, savedLanguage)
        
        enableEdgeToEdge()
        setContent {
            Campus360Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
    
    override fun attachBaseContext(newBase: Context?) {
        val preferencesManager = PreferencesManager(newBase!!)
        val savedLanguage = preferencesManager.getLanguage() ?: "sv" // Default to Swedish
        super.attachBaseContext(LocaleHelper.setLocale(newBase, savedLanguage))
    }
}
