package com.example.campus360.ui.settings

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campus360.R
import com.example.campus360.util.LocaleHelper
import com.example.campus360.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
    
    fun updateLanguage(languageCode: String, onLanguageChanged: () -> Unit) {
        viewModelScope.launch {
            preferencesManager.saveLanguage(languageCode)
            
            LocaleHelper.setLocale(getApplication(), languageCode)
            _state.value = _state.value.copy(
                selectedLanguage = languageCode
            )
            
            val context = getApplication<Application>()
            val message = if (languageCode == "sv") {
                context.getString(R.string.language_changed_swedish)
            } else {
                context.getString(R.string.language_changed_english)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            
            onLanguageChanged()
        }
    }
    
    fun downloadMap() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                withContext(Dispatchers.IO) {
                    val assets = context.assets
                    val inputStream: InputStream = assets.open("Floor new.png")
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, "map.png")
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        
                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        
                        uri?.let {
                            resolver.openOutputStream(it)?.use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        } ?: throw Exception("Failed to create file in Downloads")
                    } else {
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs()
                        }
                        
                        val outputFile = File(downloadsDir, "map.png")
                        val outputStream = FileOutputStream(outputFile)
                        
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                        outputStream.close()
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.map_download_success),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.map_download_error, e.message ?: "Unknown error"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

