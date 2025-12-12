package com.example.campus360.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campus360.util.LocaleHelper
import com.example.campus360.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch 

data class SettingsState(
    val selectedLanguage: String? = null,
    val appVersion: String = "1.0.0"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        val language = preferencesManager.getLanguage()
        _state.value = _state.value.copy(
            selectedLanguage = language
        )
    }
    
    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            preferencesManager.saveLanguage(languageCode)
            
            LocaleHelper.setLocale(getApplication(), languageCode)
            _state.value = _state.value.copy(
                selectedLanguage = languageCode
            )
        }
    }
}

