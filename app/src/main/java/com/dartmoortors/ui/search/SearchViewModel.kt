package com.dartmoortors.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.dartmoortors.data.model.*
import com.dartmoortors.data.repository.CollectionRepository
import com.dartmoortors.data.repository.PreferencesRepository
import com.dartmoortors.data.repository.TorRepository
import com.dartmoortors.data.repository.VisitedTorRepository
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val torRepository: TorRepository,
    private val collectionRepository: CollectionRepository,
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
    
    val selectedCollectionId: StateFlow<String> = preferencesRepository
        .selectedCollectionId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TorCollection.DEFAULT_COLLECTION_ID)
    
    private val selectedCollectionHasSubFilters: StateFlow<Boolean> = combine(
        collectionRepository.collections,
        selectedCollectionId
    ) { collections, selectedId ->
        collections.find { it.id == selectedId }?.hasSubFilters ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    private val visitedTorIds: StateFlow<Set<String>> = visitedTorRepository
        .getVisitedTorIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    
    /**
     * Debounced search query to prevent filtering on every keystroke.
     */
    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
    
    /**
     * Combined filter inputs to reduce combine parameters.
     */
    private data class FilterInputs(
        val tors: List<Tor>,
        val visitedIds: Set<String>,
        val query: String,
        val classifications: Set<Classification>,
        val showAccessible: Boolean,
        val showVisited: Boolean,
        val showUnvisited: Boolean,
        val sort: TorSortOption
    )
    
    private val filterInputs = combine(
        torRepository.tors,
        visitedTorIds,
        debouncedSearchQuery,
        enabledClassifications
    ) { tors, visitedIds, query, classifications ->
        Pair(Pair(tors, visitedIds), Pair(query, classifications))
    }.combine(combine(_showAccessible, _showVisited, _showUnvisited, sortOption) { a, b, c, d ->
        listOf(a, b, c, d)
    }) { pair, filterFlags ->
        FilterInputs(
            tors = pair.first.first,
            visitedIds = pair.first.second,
            query = pair.second.first,
            classifications = pair.second.second,
            showAccessible = filterFlags[0] as Boolean,
            showVisited = filterFlags[1] as Boolean,
            showUnvisited = filterFlags[2] as Boolean,
            sort = filterFlags[3] as TorSortOption
        )
    }
    
    /**
     * Filtered and sorted tors.
     */
    val filteredTors: StateFlow<List<TorWithVisitState>> = combine(
        filterInputs,
        selectedCollectionId,
        selectedCollectionHasSubFilters
    ) { inputs, collectionId, hasSubFilters ->
        inputs.tors.filter { tor ->
            // Must be in selected collection
            if (!tor.isInCollection(collectionId)) return@filter false
            
            val matchesQuery = inputs.query.isBlank() || tor.name.contains(inputs.query, ignoreCase = true)
            val isVisited = inputs.visitedIds.contains(tor.id)
            
            // Only apply classification filter if collection has sub-filters
            // Use cached enum properties for performance
            val classificationMatch = !hasSubFilters || inputs.classifications.contains(tor.classificationEnum)
            
            matchesQuery &&
                classificationMatch &&
                (!inputs.showAccessible || tor.isAccessible) &&
                ((inputs.showVisited && isVisited) || (inputs.showUnvisited && !isVisited))
        }.map { tor ->
            TorWithVisitState(
                tor = tor,
                visitedTor = if (inputs.visitedIds.contains(tor.id)) {
                    VisitedTor(torId = tor.id, visitedDate = 0L)
                } else null
            )
        }.let { list ->
            when (inputs.sort) {
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
            collectionRepository.loadCollections()
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
