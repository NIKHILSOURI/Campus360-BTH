package com.example.campus360.ui.category
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campus360.data.MapRepository
import com.example.campus360.data.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CategoryResultsUiState {
    object Loading : CategoryResultsUiState()
    data class Success(val rooms: List<Room>) : CategoryResultsUiState()
    data class Error(val message: String) : CategoryResultsUiState()
}

class CategoryResultsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MapRepository(application)
    
    private val _uiState = MutableStateFlow<CategoryResultsUiState>(CategoryResultsUiState.Loading)
    val uiState: StateFlow<CategoryResultsUiState> = _uiState.asStateFlow()
    
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()
    
    fun loadCategoryRooms(category: String) {
        viewModelScope.launch {
            _uiState.value = CategoryResultsUiState.Loading
            
            if (!repository.isDataLoaded()) {
                repository.loadAllAssets()
            }
            val categoryRooms = repository.getRoomsByCategory(category)
            _rooms.value = categoryRooms
            _uiState.value = CategoryResultsUiState.Success(categoryRooms)
        }
    }
    
    fun getCategoryName(category: String): String {
        return when (category.lowercase()) {
            "clothing" -> "Clothing"
            "coffee" -> "Coffee"
            "electronics" -> "Electronics"
            "convenience" -> "Convenience"
            "toilet" -> "Toilets"
            "sports" -> "Sports"
            "beauty" -> "Beauty"
            "lifestyle" -> "Lifestyle"
            "lecture" -> "Lecture Halls"
            "classroom" -> "Classrooms"
            "lab" -> "Labs"
            else -> category.replaceFirstChar { it.uppercase() }
        }
    }
}

