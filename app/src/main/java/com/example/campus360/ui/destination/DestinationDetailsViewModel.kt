package com.example.campus360.ui.destination

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
 
sealed class DestinationDetailsUiState {
    object Loading : DestinationDetailsUiState()
    data class Success(val room: Room) : DestinationDetailsUiState()
    data class Error(val message: String) : DestinationDetailsUiState()
    data class NavigateToMap(val roomId: String) : DestinationDetailsUiState()
    data class NavigateToChooseStart(val roomId: String) : DestinationDetailsUiState()
    object Idle : DestinationDetailsUiState()
}

class DestinationDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MapRepository(application)
    private val preferencesManager = PreferencesManager(application)
    
    private val _uiState = MutableStateFlow<DestinationDetailsUiState>(DestinationDetailsUiState.Idle)
    val uiState: StateFlow<DestinationDetailsUiState> = _uiState.asStateFlow()
    
    init {
        android.util.Log.d("DestinationDetailsViewModel", "ViewModel initialized")
    }
    
    fun loadRoom(roomId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DestinationDetailsViewModel", "loadRoom called with ID: $roomId")
                
                if (roomId.isEmpty()) {
                    android.util.Log.e("DestinationDetailsViewModel", "Room ID is empty")
                    _uiState.value = DestinationDetailsUiState.Error("Room ID is empty")
                    return@launch
                }
                
                _uiState.value = DestinationDetailsUiState.Loading
                
                
                if (!repository.isDataLoaded()) {
                    android.util.Log.d("DestinationDetailsViewModel", "Data not loaded, loading assets...")
                    val success = repository.loadAllAssets()
                    if (!success) {
                        android.util.Log.e("DestinationDetailsViewModel", "Failed to load assets")
                        _uiState.value = DestinationDetailsUiState.Error("Failed to load map data")
                        return@launch
                    }
                }
                
                android.util.Log.d("DestinationDetailsViewModel", "Getting room by ID: $roomId")
                val room = repository.getRoomById(roomId)
                if (room != null) {
                    android.util.Log.d("DestinationDetailsViewModel", "Room found: ${room.name}")
                    
                    preferencesManager.addRecentDestination(roomId)
                    _uiState.value = DestinationDetailsUiState.Success(room)
                } else {
                    android.util.Log.e("DestinationDetailsViewModel", "Room not found: $roomId")
                    
                    val allRooms = repository.rooms ?: emptyList()
                    android.util.Log.d("DestinationDetailsViewModel", "Available rooms: ${allRooms.map { it.id }.take(10)}")
                    _uiState.value = DestinationDetailsUiState.Error("Room not found: $roomId")
                }
            } catch (e: Exception) {
                android.util.Log.e("DestinationDetailsViewModel", "Exception loading room", e)
                e.printStackTrace()
                _uiState.value = DestinationDetailsUiState.Error("Error loading room: ${e.message ?: "Unknown error"}")
            }
        }
    }
    
    fun showOnMap(roomId: String) {
        _uiState.value = DestinationDetailsUiState.NavigateToMap(roomId)
    }
    
    fun navigateFromMyLocation(roomId: String) {
        _uiState.value = DestinationDetailsUiState.NavigateToChooseStart(roomId)
    }
    
    fun clearNavigation() {
        _uiState.value = DestinationDetailsUiState.Idle
    }
}

