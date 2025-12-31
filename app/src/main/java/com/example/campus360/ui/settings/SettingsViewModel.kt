package com.example.campus360.ui.settings

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campus360.R
import com.example.campus360.util.LocaleHelper
import com.example.campus360.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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
    
    fun downloadMap() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream = context.assets.open("Floor new.png")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (bitmap != null) {
                    val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveImageToDownloadsQ(bitmap, context)
                    } else {
                        saveImageToDownloadsLegacy(bitmap, context)
                    }
                    
                    val message = if (saved) {
                        context.getString(R.string.map_download_started)
                    } else {
                        context.getString(R.string.map_download_error, "Failed to save")
                    }
                    
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.map_download_error, "Failed to load image"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                val context = getApplication<Application>()
                Toast.makeText(
                    context,
                    context.getString(R.string.map_download_error, e.message ?: "Unknown error"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun saveImageToDownloadsQ(bitmap: Bitmap, context: Context): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "map.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    private fun saveImageToDownloadsLegacy(bitmap: Bitmap, context: Context): Boolean {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, "map.png")
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

