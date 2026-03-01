package com.dartmoortors.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.dartmoortors.data.model.Classification
import com.dartmoortors.data.model.Tor
import com.dartmoortors.data.repository.PreferencesRepository
import com.dartmoortors.data.repository.TorRepository
import com.dartmoortors.data.repository.VisitedTorRepository
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val torRepository: TorRepository,
    private val visitedTorRepository: VisitedTorRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    val enabledClassifications: StateFlow<Set<Classification>> = preferencesRepository
        .enabledClassifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Classification.entries.filter { it.defaultEnabled }.toSet())
    
    val accessibleOnly: StateFlow<Boolean> = preferencesRepository
        .accessibleOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val tors: StateFlow<List<Tor>> = torRepository.tors
    
    private val visitedCount: StateFlow<Int> = visitedTorRepository
        .getVisitedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    /**
     * Count of tors per classification.
     */
    val classificationCounts: StateFlow<Map<Classification, Int>> = torRepository.tors.map { tors ->
        tors.groupBy { Classification.fromString(it.classification) }
            .mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    /**
     * Total filtered tors count.
     */
    val totalFilteredCount: StateFlow<Int> = combine(
        torRepository.tors,
        enabledClassifications,
        accessibleOnly
    ) { tors, classifications, accessibleOnly ->
        tors.count { tor ->
            val classification = Classification.fromString(tor.classification)
            val isAccessible = com.dartmoortors.data.model.Access.fromString(tor.access).isAccessible
            classifications.contains(classification) && (!accessibleOnly || isAccessible)
        }
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
