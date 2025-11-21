package com.example.campus360.ui.search

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    data class NavigateToDestination(val roomId: String) : SearchUiState()
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MapRepository(application)
    private val preferencesManager = PreferencesManager(application)
    
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    
    private val _allRooms = MutableStateFlow<List<Room>>(emptyList())
    val allRooms: StateFlow<List<Room>> = _allRooms.asStateFlow()
    
    private val _filteredRooms = MutableStateFlow<List<Room>>(emptyList())
    val filteredRooms: StateFlow<List<Room>> = _filteredRooms.asStateFlow()
    
    init {
        loadRooms()
    }
    
    private fun loadRooms() {
        viewModelScope.launch {
            
            if (!repository.isDataLoaded()) {
                repository.loadAllAssets()
            }
            val rooms = repository.rooms ?: emptyList()
            _allRooms.value = rooms
            _filteredRooms.value = rooms
        }
    }
    
    fun setQuery(query: String) {
        _query.value = query
        filterRooms(query)
    }
    
    fun setInitialQuery(query: String) {
        if (_query.value.isEmpty()) {
            setQuery(query)
        }
    }
    
    private fun filterRooms(query: String) {
        val lowerQuery = query.lowercase().trim()
        
        if (lowerQuery.isEmpty()) {
            _filteredRooms.value = _allRooms.value
            return
        }
        
        val filtered = _allRooms.value.filter { room ->
            room.id.lowercase().contains(lowerQuery, ignoreCase = true) ||
            room.name.lowercase().contains(lowerQuery, ignoreCase = true) ||
            room.type.lowercase().contains(lowerQuery, ignoreCase = true)
        }
        
        _filteredRooms.value = filtered
    }
    
    fun onRoomClick(room: Room) {
        viewModelScope.launch {
            
            preferencesManager.addRecentDestination(room.id)
            
            
            _uiState.value = SearchUiState.NavigateToDestination(room.id)
        }
    }
    
    fun clearNavigation() {
        _uiState.value = SearchUiState.Idle
    }
}

