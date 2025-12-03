package com.example.campus360.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "campus360_prefs"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_RECENT_DESTINATIONS = "recent_destinations"
        private const val MAX_RECENT_DESTINATIONS = 5
    }
    
    fun getLanguage(): String? {
        return prefs.getString(KEY_LANGUAGE, null)
    }
    
    fun saveLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
    
    fun hasLanguage(): Boolean {
        return getLanguage() != null
    }
    
    fun getRecentDestinations(): List<String> {
        val json = prefs.getString(KEY_RECENT_DESTINATIONS, null)
        if (json == null) return emptyList()
        
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun addRecentDestination(roomId: String) {
        val current = getRecentDestinations().toMutableList()
        
        
        current.remove(roomId)
        
        
        current.add(0, roomId)
        
        
        if (current.size > MAX_RECENT_DESTINATIONS) {
            current.removeAt(current.size - 1)
        }
        
        val json = gson.toJson(current)
        prefs.edit().putString(KEY_RECENT_DESTINATIONS, json).apply()
    }
    
    fun clearRecentDestinations() {
        prefs.edit().remove(KEY_RECENT_DESTINATIONS).apply()
    }
}

