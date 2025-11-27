package com.example.campus360.ui.choosestart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campus360.data.Landmark
import com.example.campus360.data.MapRepository
import com.example.campus360.data.Room
import com.example.campus360.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ChooseStartLocationUiState {
    object Idle : ChooseStartLocationUiState()
    data class NavigateToMap(val roomId: String, val startNodeId: String) : ChooseStartLocationUiState()
    data class ShowMapPicker(val roomId: String) : ChooseStartLocationUiState()
    object IdleMapPicker : ChooseStartLocationUiState()
}

class ChooseStartLocationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MapRepository(application)
    
    private val _uiState = MutableStateFlow<ChooseStartLocationUiState>(ChooseStartLocationUiState.Idle)
    val uiState: StateFlow<ChooseStartLocationUiState> = _uiState.asStateFlow()
    
    private val _destinationRoom = MutableStateFlow<Room?>(null)
    val destinationRoom: StateFlow<Room?> = _destinationRoom.asStateFlow()
    
    private val _allPlaces = MutableStateFlow<List<Room>>(emptyList())
    val allPlaces: StateFlow<List<Room>> = _allPlaces.asStateFlow()
    
    val landmarks = listOf(
        Landmark("main_entrance", "Main Entrance", "node_013"), 
        Landmark("central_area", "Central Area", "node_015") 
    )
    
    init {
        android.util.Log.d("ChooseStartLocationViewModel", "ViewModel initialized")
    }
    
    fun loadDestinationRoom(roomId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ChooseStartLocationViewModel", "Loading destination room: $roomId")
                
                
                if (!repository.isDataLoaded()) {
                    android.util.Log.d("ChooseStartLocationViewModel", "Data not loaded, loading assets...")
                    val success = repository.loadAllAssets()
                    if (!success) {
                        android.util.Log.e("ChooseStartLocationViewModel", "Failed to load assets")
                        return@launch
                    }
                }
                
                val room = repository.getRoomById(roomId)
                if (room != null) {
                    android.util.Log.d("ChooseStartLocationViewModel", "Destination room loaded: ${room.name} (${room.nodeId})")
                    _destinationRoom.value = room
                } else {
                    android.util.Log.e("ChooseStartLocationViewModel", "Room not found: $roomId")
                }
                
                
                val allRooms = repository.rooms ?: emptyList()
                _allPlaces.value = allRooms
            } catch (e: Exception) {
                android.util.Log.e("ChooseStartLocationViewModel", "Error loading destination room", e)
                e.printStackTrace()
            }
        }
    }
    
    fun selectLandmark(landmark: Landmark) {
        android.util.Log.d("ChooseStartLocationViewModel", "Landmark selected: ${landmark.label} (${landmark.nodeId})")
        val destinationRoom = _destinationRoom.value
        if (destinationRoom != null) {
            android.util.Log.d("ChooseStartLocationViewModel", "Navigating to map with route: ${landmark.nodeId} -> ${destinationRoom.nodeId}")
            
            _uiState.value = ChooseStartLocationUiState.NavigateToMap(
                roomId = destinationRoom.id,
                startNodeId = landmark.nodeId
            )
        } else {
            android.util.Log.e("ChooseStartLocationViewModel", "Destination room is null, cannot navigate")
        }
    }
    
    fun selectPlace(room: Room) {
        android.util.Log.d("ChooseStartLocationViewModel", "Place selected: ${room.name} (${room.nodeId})")
        val destinationRoom = _destinationRoom.value
        if (destinationRoom != null) {
            android.util.Log.d("ChooseStartLocationViewModel", "Navigating to map with route: ${room.nodeId} -> ${destinationRoom.nodeId}")
            
            _uiState.value = ChooseStartLocationUiState.NavigateToMap(
                roomId = destinationRoom.id,
                startNodeId = room.nodeId
            )
        } else {
            android.util.Log.e("ChooseStartLocationViewModel", "Destination room is null, cannot navigate")
        }
    }
    
    fun showMapPicker() {
        android.util.Log.d("ChooseStartLocationViewModel", "Show map picker requested")
        val destinationRoom = _destinationRoom.value
        if (destinationRoom != null) {
            android.util.Log.d("ChooseStartLocationViewModel", "Navigating to map picker for room: ${destinationRoom.id}")
            _uiState.value = ChooseStartLocationUiState.ShowMapPicker(destinationRoom.id)
        } else {
            android.util.Log.e("ChooseStartLocationViewModel", "Destination room is null, cannot show map picker")
        }
    }
    
    fun selectNodeFromMap(nodeId: String) {
        val destinationRoom = _destinationRoom.value
        if (destinationRoom != null) {
            _uiState.value = ChooseStartLocationUiState.NavigateToMap(
                roomId = destinationRoom.id,
                startNodeId = nodeId
            )
        }
    }
    
    fun clearNavigation() {
        _uiState.value = ChooseStartLocationUiState.Idle
    }
    
    fun clearMapPicker() {
        _uiState.value = ChooseStartLocationUiState.IdleMapPicker
    }
}

