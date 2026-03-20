package com.dartmoortors.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.dartmoortors.data.model.TorCollection
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CollectionRepository"

/**
 * Repository for managing collection data loaded from the bundled JSON file.
 */
@Singleton
class CollectionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val _collections = MutableStateFlow<List<TorCollection>>(emptyList())
    val collections: StateFlow<List<TorCollection>> = _collections.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var isLoaded = false
    
    /**
     * Load collections from the bundled JSON file.
     */
    suspend fun loadCollections() {
        if (isLoaded) {
            Log.d(TAG, "Collections already loaded, skipping")
            return
        }
        
        Log.d(TAG, "Starting to load collections from assets")
        _isLoading.value = true
        _error.value = null
        
        withContext(Dispatchers.IO) {
            try {
                val json = context.assets.open("collections.json").bufferedReader().use { it.readText() }
                Log.d(TAG, "Read JSON file, size: ${json.length} characters")
                val type = object : TypeToken<List<TorCollection>>() {}.type
                val collectionList: List<TorCollection> = gson.fromJson(json, type)
                Log.d(TAG, "Parsed ${collectionList.size} collections from JSON")
                // Sort by sortOrder
                _collections.value = collectionList.sortedBy { it.sortOrder }
                isLoaded = true
                Log.d(TAG, "Collections loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load collections", e)
                _error.value = "Failed to load collections: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Get a collection by ID.
     */
    fun getCollectionById(id: String): TorCollection? {
        return _collections.value.find { it.id == id }
    }
    
    /**
     * Get all collections as a list.
     */
    fun getAllCollections(): List<TorCollection> {
        return _collections.value
    }
}
