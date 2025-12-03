package com.example.campus360.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campus360.data.MapRepository
import com.example.campus360.data.Room
import com.example.campus360.navigation.Screen
import com.example.campus360.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Category(
    val id: String,
    val name: String,
    val type: String
)

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class NavigateToSearch(val query: String) : HomeUiState()
    data class NavigateToCategory(val category: Category) : HomeUiState()
    object NavigateToMap : HomeUiState()
    object NavigateToSOS : HomeUiState()
    object Idle : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MapRepository(application)
    private val preferencesManager = PreferencesManager(application)
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _recentRooms = MutableStateFlow<List<Room>>(emptyList())
    val recentRooms: StateFlow<List<Room>> = _recentRooms.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()
    
    val categories = listOf(
        Category("class", "Class", "class"),
        Category("lab", "Lab", "lab"),
        Category("hall", "Hall", "hall"),
        Category("popular", "Popular", "popular")
    )
    
    init {
        loadRecentDestinations()
    }
    
    fun loadRecentDestinations() {
        viewModelScope.launch {
            try {
                if (!repository.isDataLoaded()) {
                    repository.loadAllAssets()
                }
                
                val recentIds = preferencesManager.getRecentDestinations()
                if (recentIds.isNotEmpty()) {
                    val rooms = repository.getRoomsByIds(recentIds)
                    _recentRooms.value = rooms
                } else {
                    _recentRooms.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _recentRooms.value = emptyList()
            }
        }
    }
    
    fun selectCategory(category: Category) {
        _selectedCategory.value = category.id
        _uiState.value = HomeUiState.NavigateToCategory(category)
    }
    
    fun search(query: String) {
        _uiState.value = HomeUiState.NavigateToSearch(query)
    }
    
    fun openMap() {
        _uiState.value = HomeUiState.NavigateToMap
    }
    
    fun openSOS() {
        _uiState.value = HomeUiState.NavigateToSOS
    }
    
    fun clearNavigation() {
        _uiState.value = HomeUiState.Idle
    }
    
    fun getRoomsByCategory(category: Category): List<Room> {
        return repository.getRoomsByCategory(category.type)
    }
}

