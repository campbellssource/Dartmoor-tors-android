package com.dartmoortors.ui.map

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.dartmoortors.data.location.LocationService
import com.dartmoortors.data.model.*
import com.dartmoortors.data.repository.PreferencesRepository
import com.dartmoortors.data.repository.TorRepository
import com.dartmoortors.data.repository.VisitedTorRepository
import javax.inject.Inject

private const val TAG = "MapViewModel"

/**
 * Tracking mode for the map camera.
 */
enum class TrackingMode {
    NONE,           // No tracking - user controls camera
    FOLLOW,         // Follow user location
    FOLLOW_COMPASS  // Follow user location and rotate with compass
}

/**
 * ViewModel for the Map screen.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val torRepository: TorRepository,
    private val visitedTorRepository: VisitedTorRepository,
    private val preferencesRepository: PreferencesRepository,
    val locationService: LocationService
) : ViewModel() {
    
    // Dartmoor center coordinates
    companion object {
        val DARTMOOR_CENTER = LatLng(50.55, -3.95)
        const val DEFAULT_ZOOM = 11f
        const val DETAIL_ZOOM = 14f
        const val LOCATION_ZOOM = 15f
    }
    
    private val _selectedTorId = MutableStateFlow<String?>(null)
    val selectedTorId: StateFlow<String?> = _selectedTorId.asStateFlow()
    
    private val _showWelcome = MutableStateFlow(true)
    val showWelcome: StateFlow<Boolean> = _showWelcome.asStateFlow()
    
    private val _cameraTarget = MutableStateFlow<LatLng?>(null)
    val cameraTarget: StateFlow<LatLng?> = _cameraTarget.asStateFlow()
    
    private val _cameraZoom = MutableStateFlow(DEFAULT_ZOOM)
    val cameraZoom: StateFlow<Float> = _cameraZoom.asStateFlow()
    
    private val _trackingMode = MutableStateFlow(TrackingMode.NONE)
    val trackingMode: StateFlow<TrackingMode> = _trackingMode.asStateFlow()
    
    // Location state from LocationService
    val currentLocation: StateFlow<Location?> = locationService.currentLocation
    val compassHeading: StateFlow<Float> = locationService.compassHeading
    val isLocationEnabled: StateFlow<Boolean> = locationService.isLocationEnabled
    
    val isLoading: StateFlow<Boolean> = torRepository.isLoading
    val error: StateFlow<String?> = torRepository.error
    
    val enabledClassifications: StateFlow<Set<Classification>> = preferencesRepository
        .enabledClassifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Classification.entries.filter { it.defaultEnabled }.toSet())
    
    val accessibleOnly: StateFlow<Boolean> = preferencesRepository
        .accessibleOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val mapType: StateFlow<Int> = preferencesRepository
        .mapType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    private val visitedTorIds: StateFlow<Set<String>> = visitedTorRepository
        .getVisitedTorIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    
    /**
     * Filtered tors based on current filter settings.
     */
    val filteredTors: StateFlow<List<TorWithVisitState>> = combine(
        torRepository.tors,
        visitedTorIds,
        enabledClassifications,
        accessibleOnly
    ) { tors, visitedIds, classifications, accessibleOnly ->
        Log.d(TAG, "Filtering tors: total=${tors.size}, classifications=${classifications.map { it.name }}, accessibleOnly=$accessibleOnly")
        val filtered = tors.filter { tor ->
            val classification = Classification.fromString(tor.classification)
            val access = Access.fromString(tor.access)
            
            val classMatch = classifications.contains(classification)
            val accessMatch = !accessibleOnly || access.isAccessible
            
            classMatch && accessMatch
        }.map { tor ->
            TorWithVisitState(
                tor = tor,
                visitedTor = if (visitedIds.contains(tor.id)) {
                    VisitedTor(torId = tor.id, visitedDate = 0L) // Simplified - real data loaded separately
                } else null
            )
        }
        Log.d(TAG, "Filtered tors result: ${filtered.size}")
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Currently selected tor with visit state.
     */
    val selectedTor: StateFlow<TorWithVisitState?> = combine(
        _selectedTorId,
        torRepository.tors,
        visitedTorRepository.getVisitedTors()
    ) { selectedId, tors, visitedTors ->
        selectedId?.let { id ->
            tors.find { it.id == id }?.let { tor ->
                TorWithVisitState(
                    tor = tor,
                    visitedTor = visitedTors.find { it.torId == id }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    init {
        viewModelScope.launch {
            torRepository.loadTors()
        }
        
        viewModelScope.launch {
            preferencesRepository.showWelcome.collect { show ->
                _showWelcome.value = show
            }
        }
    }
    
    /**
     * Select a tor to show in the detail sheet.
     */
    fun selectTor(torId: String) {
        _selectedTorId.value = torId
        torRepository.getTorById(torId)?.let { tor ->
            _cameraTarget.value = LatLng(tor.latitude, tor.longitude)
            _cameraZoom.value = DETAIL_ZOOM
        }
    }
    
    /**
     * Deselect the current tor.
     */
    fun deselectTor() {
        _selectedTorId.value = null
    }
    
    /**
     * Dismiss the welcome dialog.
     */
    fun dismissWelcome() {
        viewModelScope.launch {
            preferencesRepository.setShowWelcome(false)
        }
    }
    
    /**
     * Mark a tor as visited.
     */
    fun markTorAsVisited(torId: String, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            visitedTorRepository.markAsVisited(torId, date)
        }
    }
    
    /**
     * Unmark a tor as visited.
     */
    fun unmarkTorAsVisited(torId: String) {
        viewModelScope.launch {
            visitedTorRepository.unmarkAsVisited(torId)
        }
    }
    
    /**
     * Update visited date for a tor.
     */
    fun updateVisitedDate(torId: String, date: Long) {
        viewModelScope.launch {
            visitedTorRepository.updateVisitedDate(torId, date)
        }
    }
    
    /**
     * Set map type.
     */
    fun setMapType(type: Int) {
        viewModelScope.launch {
            preferencesRepository.setMapType(type)
        }
    }
    
    /**
     * Clear camera target after animation completes.
     */
    fun clearCameraTarget() {
        _cameraTarget.value = null
    }
}
