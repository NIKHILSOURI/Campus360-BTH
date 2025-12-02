package com.example.campus360.ui.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campus360.data.MapRepository
import com.example.campus360.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SplashUiState {
    object Loading : SplashUiState()
    data class NavigateToLanguage(val hasLanguage: Boolean) : SplashUiState()
    object NavigateToHome : SplashUiState()
    data class Error(val message: String) : SplashUiState()
}

class SplashViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MapRepository(application)
    private val preferencesManager = PreferencesManager(application)
    
    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
    
    private val _loadingProgress = MutableStateFlow(0f)
    val loadingProgress: StateFlow<Float> = _loadingProgress.asStateFlow()
    
    init {
        loadAssets()
    }
    
    private fun loadAssets() {
        viewModelScope.launch {
            try {
                _loadingProgress.value = 0.2f
                
        
                val success = repository.loadAllAssets()
                
                _loadingProgress.value = 0.8f
                
                if (success && repository.isDataLoaded()) {
                    _loadingProgress.value = 1.0f
                    

                    _uiState.value = SplashUiState.NavigateToHome
                } else {
                    _uiState.value = SplashUiState.Error("Failed to load assets")
                }
            } catch (e: Exception) {
                _uiState.value = SplashUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

