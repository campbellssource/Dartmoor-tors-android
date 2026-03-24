package com.dartmoortors.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.dartmoortors.data.model.Tor
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TorRepository"

/**
 * Repository for managing tor data loaded from the bundled JSON file.
 */
@Singleton
class TorRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val _tors = MutableStateFlow<List<Tor>>(emptyList())
    val tors: StateFlow<List<Tor>> = _tors.asStateFlow()
    
    // Cached map for O(1) lookup by ID
    private var _torsById: Map<String, Tor> = emptyMap()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var isLoaded = false
    
    /**
     * Load tors from the bundled JSON file.
     */
    suspend fun loadTors() {
        if (isLoaded) {
            Log.d(TAG, "Tors already loaded, skipping")
            return
        }
        
        Log.d(TAG, "Starting to load tors from assets")
        _isLoading.value = true
        _error.value = null
        
        withContext(Dispatchers.IO) {
            try {
                val json = context.assets.open("tors.json").bufferedReader().use { it.readText() }
                Log.d(TAG, "Read JSON file, size: ${json.length} characters")
                val type = object : TypeToken<List<Tor>>() {}.type
                val torList: List<Tor> = gson.fromJson(json, type)
                Log.d(TAG, "Parsed ${torList.size} tors from JSON")
                if (torList.isNotEmpty()) {
                    Log.d(TAG, "First tor: ${torList.first()}")
                }
                _tors.value = torList
                _torsById = torList.associateBy { it.id }
                isLoaded = true
                Log.d(TAG, "Tors loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load tors", e)
                _error.value = "Failed to load tors: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Get a tor by ID. O(1) lookup using cached map.
     */
    fun getTorById(id: String): Tor? {
        return _torsById[id]
    }
    
    /**
     * Get all tors as a list.
     */
    fun getAllTors(): List<Tor> {
        return _tors.value
    }
    
    /**
     * Search tors by name.
     */
    fun searchTors(query: String): List<Tor> {
        if (query.isBlank()) return _tors.value
        return _tors.value.filter { 
            it.name.contains(query, ignoreCase = true) 
        }
    }
}
