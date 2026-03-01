package com.dartmoortors.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.dartmoortors.data.model.*
import com.dartmoortors.data.repository.PreferencesRepository
import com.dartmoortors.data.repository.TorRepository
import com.dartmoortors.data.repository.VisitedTorRepository
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val torRepository: TorRepository,
    private val visitedTorRepository: VisitedTorRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _showAccessible = MutableStateFlow(true)
    val showAccessible: StateFlow<Boolean> = _showAccessible.asStateFlow()
    
    private val _showVisited = MutableStateFlow(true)
    val showVisited: StateFlow<Boolean> = _showVisited.asStateFlow()
    
    private val _showUnvisited = MutableStateFlow(true)
    val showUnvisited: StateFlow<Boolean> = _showUnvisited.asStateFlow()
    
    val sortOption: StateFlow<TorSortOption> = preferencesRepository.sortOption
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TorSortOption.HEIGHT_DESC)
    
    val enabledClassifications: StateFlow<Set<Classification>> = preferencesRepository
        .enabledClassifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Classification.entries.filter { it.defaultEnabled }.toSet())
    
    private val visitedTorIds: StateFlow<Set<String>> = visitedTorRepository
        .getVisitedTorIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    
    /**
     * Filtered and sorted tors.
     */
    val filteredTors: StateFlow<List<TorWithVisitState>> = combine(
        torRepository.tors,
        visitedTorIds,
        _searchQuery,
        enabledClassifications,
        _showAccessible,
        _showVisited,
        _showUnvisited,
        sortOption
    ) { values ->
        val tors = values[0] as List<Tor>
        val visitedIds = values[1] as Set<String>
        val query = values[2] as String
        val classifications = values[3] as Set<Classification>
        val showAccessible = values[4] as Boolean
        val showVisited = values[5] as Boolean
        val showUnvisited = values[6] as Boolean
        val sort = values[7] as TorSortOption
        
        tors.filter { tor ->
            val matchesQuery = query.isBlank() || tor.name.contains(query, ignoreCase = true)
            val classification = Classification.fromString(tor.classification)
            val access = Access.fromString(tor.access)
            val isVisited = visitedIds.contains(tor.id)
            
            matchesQuery &&
                classifications.contains(classification) &&
                (!showAccessible || access.isAccessible) &&
                ((showVisited && isVisited) || (showUnvisited && !isVisited))
        }.map { tor ->
            TorWithVisitState(
                tor = tor,
                visitedTor = if (visitedIds.contains(tor.id)) {
                    VisitedTor(torId = tor.id, visitedDate = 0L)
                } else null
            )
        }.let { list ->
            when (sort) {
                TorSortOption.HEIGHT_DESC -> list.sortedByDescending { it.tor.heightMeters }
                TorSortOption.HEIGHT_ASC -> list.sortedBy { it.tor.heightMeters }
                TorSortOption.NAME_ASC -> list.sortedBy { it.tor.name }
                TorSortOption.NAME_DESC -> list.sortedByDescending { it.tor.name }
                TorSortOption.NORTH_TO_SOUTH -> list.sortedByDescending { it.tor.latitude }
                TorSortOption.SOUTH_TO_NORTH -> list.sortedBy { it.tor.latitude }
                TorSortOption.EAST_TO_WEST -> list.sortedByDescending { it.tor.longitude }
                TorSortOption.WEST_TO_EAST -> list.sortedBy { it.tor.longitude }
                TorSortOption.DISTANCE -> list // TODO: Implement distance sorting
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    init {
        viewModelScope.launch {
            torRepository.loadTors()
        }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun toggleAccessible() {
        _showAccessible.value = !_showAccessible.value
    }
    
    fun toggleVisited() {
        _showVisited.value = !_showVisited.value
    }
    
    fun toggleUnvisited() {
        _showUnvisited.value = !_showUnvisited.value
    }
    
    fun setSortOption(option: TorSortOption) {
        viewModelScope.launch {
            preferencesRepository.setSortOption(option)
        }
    }
}
