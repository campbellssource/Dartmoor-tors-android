package com.dartmoortors.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.dartmoortors.data.model.Classification
import com.dartmoortors.data.model.TorCollection
import com.dartmoortors.data.model.Tor
import com.dartmoortors.data.repository.CollectionRepository
import com.dartmoortors.data.repository.PreferencesRepository
import com.dartmoortors.data.repository.TorRepository
import com.dartmoortors.data.repository.VisitedTorRepository
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val torRepository: TorRepository,
    private val collectionRepository: CollectionRepository,
    private val visitedTorRepository: VisitedTorRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    /**
     * All available collections.
     */
    val collections: StateFlow<List<TorCollection>> = collectionRepository.collections
    
    /**
     * The currently selected collection ID.
     */
    val selectedCollectionId: StateFlow<String> = preferencesRepository
        .selectedCollectionId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TorCollection.DEFAULT_COLLECTION_ID)
    
    /**
     * The currently selected collection.
     */
    val selectedCollection: StateFlow<TorCollection?> = combine(
        collections,
        selectedCollectionId
    ) { collections, selectedId ->
        collections.find { it.id == selectedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    /**
     * Whether the current collection has sub-filters (classification toggles).
     * Computed directly from collections and selectedCollectionId to avoid race conditions.
     */
    val hasSubFilters: StateFlow<Boolean> = combine(
        collections,
        selectedCollectionId
    ) { collections, selectedId ->
        collections.find { it.id == selectedId }?.hasSubFilters ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val enabledClassifications: StateFlow<Set<Classification>> = preferencesRepository
        .enabledClassifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Classification.entries.filter { it.defaultEnabled }.toSet())
    
    val accessibleOnly: StateFlow<Boolean> = preferencesRepository
        .accessibleOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val tors: StateFlow<List<Tor>> = torRepository.tors
    
    private val visitedTorIds: StateFlow<Set<String>> = visitedTorRepository
        .getVisitedTorIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    
    /**
     * Count of tors per classification (within the selected collection).
     */
    val classificationCounts: StateFlow<Map<Classification, Int>> = combine(
        torRepository.tors,
        selectedCollectionId
    ) { tors, collectionId ->
        tors.filter { it.isInCollection(collectionId) }
            .groupBy { Classification.fromString(it.classification) }
            .mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    /**
     * Count of tors per collection.
     */
    val collectionTorCounts: StateFlow<Map<String, Int>> = torRepository.tors.map { tors ->
        val counts = mutableMapOf<String, Int>()
        tors.forEach { tor ->
            tor.collections.forEach { collectionId ->
                counts[collectionId] = (counts[collectionId] ?: 0) + 1
            }
        }
        counts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    /**
     * Filtered tors based on collection, classification, and access filters.
     * Uses collections directly to compute hasSubFilters inline, avoiding race conditions.
     */
    val filteredTors: StateFlow<List<Tor>> = combine(
        torRepository.tors,
        collections,
        selectedCollectionId,
        enabledClassifications,
        accessibleOnly
    ) { tors, collectionsList, collectionId, classifications, accessibleOnly ->
        // Compute hasSubFilters directly from collections to ensure synchronization
        val hasSubFilters = collectionsList.find { it.id == collectionId }?.hasSubFilters ?: false
        
        tors.filter { tor ->
            // Must be in selected collection
            if (!tor.isInCollection(collectionId)) return@filter false
            
            // Apply classification filter only if collection has sub-filters
            if (hasSubFilters) {
                val classification = Classification.fromString(tor.classification)
                if (!classifications.contains(classification)) return@filter false
            }
            
            // Apply access filter
            if (accessibleOnly && !tor.isAccessible) return@filter false
            
            true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Total filtered tors count.
     */
    val totalFilteredCount: StateFlow<Int> = filteredTors.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    /**
     * Count of visited tors within the current filtered set.
     */
    val visitedCount: StateFlow<Int> = combine(
        filteredTors,
        visitedTorIds
    ) { filtered, visitedIds ->
        filtered.count { visitedIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    /**
     * Progress data.
     */
    data class Progress(
        val total: Int,
        val visited: Int,
        val remaining: Int
    )
    
    val progress: StateFlow<Progress> = combine(
        totalFilteredCount,
        visitedCount
    ) { total, visited ->
        Progress(
            total = total,
            visited = visited,
            remaining = (total - visited).coerceAtLeast(0)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Progress(0, 0, 0))
    
    init {
        viewModelScope.launch {
            torRepository.loadTors()
            collectionRepository.loadCollections()
        }
    }
    
    /**
     * Select a collection.
     */
    fun selectCollection(collectionId: String) {
        viewModelScope.launch {
            preferencesRepository.setSelectedCollectionId(collectionId)
        }
    }
    
    fun toggleClassification(classification: Classification) {
        viewModelScope.launch {
            val current = enabledClassifications.value
            val enabled = !current.contains(classification)
            preferencesRepository.toggleClassification(classification, enabled)
        }
    }
    
    fun setAccessibleOnly(accessibleOnly: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAccessibleOnly(accessibleOnly)
        }
    }
}
