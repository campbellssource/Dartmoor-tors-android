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
import com.dartmoortors.data.repository.CollectionRepository
import com.dartmoortors.data.repository.PhotoRepository
import com.dartmoortors.data.repository.PreferencesRepository
import com.dartmoortors.data.repository.TorRepository
import com.dartmoortors.data.repository.TorWithDistance
import com.dartmoortors.data.repository.VisitedTorRepository
import com.dartmoortors.service.PhotoService
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
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val torRepository: TorRepository,
    private val collectionRepository: CollectionRepository,
    private val visitedTorRepository: VisitedTorRepository,
    private val preferencesRepository: PreferencesRepository,
    private val photoService: PhotoService,
    private val photoRepository: PhotoRepository,
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
    
    private val _selectedPhoto = MutableStateFlow<Photo?>(null)
    val selectedPhoto: StateFlow<Photo?> = _selectedPhoto.asStateFlow()
    
    private val _nearbyTorsForPhoto = MutableStateFlow<List<TorWithDistance>>(emptyList())
    val nearbyTorsForPhoto: StateFlow<List<TorWithDistance>> = _nearbyTorsForPhoto.asStateFlow()
    
    private val _showWelcome = MutableStateFlow(true)
    val showWelcome: StateFlow<Boolean> = _showWelcome.asStateFlow()
    
    private val _cameraTarget = MutableStateFlow<LatLng?>(null)
    val cameraTarget: StateFlow<LatLng?> = _cameraTarget.asStateFlow()
    
    private val _cameraZoom = MutableStateFlow(DEFAULT_ZOOM)
    val cameraZoom: StateFlow<Float> = _cameraZoom.asStateFlow()
    
    private val _trackingMode = MutableStateFlow(TrackingMode.NONE)
    val trackingMode: StateFlow<TrackingMode> = _trackingMode.asStateFlow()
    
    // Compass line of sight
    private val _showCompassLine = MutableStateFlow(false)
    val showCompassLine: StateFlow<Boolean> = _showCompassLine.asStateFlow()
    
    // Photos for map layer
    private val _mapPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val mapPhotos: StateFlow<List<Photo>> = _mapPhotos.asStateFlow()
    
    private val _isLoadingPhotos = MutableStateFlow(false)
    val isLoadingPhotos: StateFlow<Boolean> = _isLoadingPhotos.asStateFlow()
    
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
    
    val selectedCollectionId: StateFlow<String> = preferencesRepository
        .selectedCollectionId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TorCollection.DEFAULT_COLLECTION_ID)
    
    private val selectedCollectionHasSubFilters: StateFlow<Boolean> = combine(
        collectionRepository.collections,
        selectedCollectionId
    ) { collections, selectedId ->
        collections.find { it.id == selectedId }?.hasSubFilters ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val mapType: StateFlow<Int> = preferencesRepository
        .mapType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)  // Default to Hybrid
    
    val showPhotosLayer: StateFlow<Boolean> = preferencesRepository
        .showPhotosLayer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    private val visitedTorIds: StateFlow<Set<String>> = visitedTorRepository
        .getVisitedTorIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    
    /**
     * Filtered tors based on current filter settings.
     */
    private val filterInputs = combine(
        torRepository.tors,
        visitedTorIds,
        enabledClassifications,
        accessibleOnly
    ) { tors, visitedIds, classifications, accessibleOnly ->
        FilterInputs(tors, visitedIds, classifications, accessibleOnly)
    }
    
    private data class FilterInputs(
        val tors: List<Tor>,
        val visitedIds: Set<String>,
        val classifications: Set<Classification>,
        val accessibleOnly: Boolean
    )
    
    val filteredTors: StateFlow<List<TorWithVisitState>> = combine(
        filterInputs,
        selectedCollectionId,
        selectedCollectionHasSubFilters
    ) { inputs, collectionId, hasSubFilters ->
        Log.d(TAG, "Filtering tors: total=${inputs.tors.size}, collection=$collectionId, hasSubFilters=$hasSubFilters, classifications=${inputs.classifications.map { it.name }}, accessibleOnly=${inputs.accessibleOnly}")
        val filtered = inputs.tors.filter { tor ->
            // Must be in selected collection
            if (!tor.isInCollection(collectionId)) return@filter false
            
            // Only apply classification filter if collection has sub-filters
            // Use cached enum properties for performance
            if (hasSubFilters) {
                if (!inputs.classifications.contains(tor.classificationEnum)) return@filter false
            }
            
            // Apply access filter using cached property
            if (inputs.accessibleOnly && !tor.isAccessible) return@filter false
            
            true
        }.map { tor ->
            TorWithVisitState(
                tor = tor,
                visitedTor = if (inputs.visitedIds.contains(tor.id)) {
                    VisitedTor(torId = tor.id, visitedDate = 0L) // Simplified - real data loaded separately
                } else null
            )
        }
        Log.d(TAG, "Filtered tors result: ${filtered.size}")
        filtered
    }
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Map markers as ClusterItems for efficient rendering.
     */
    val clusterItems: StateFlow<List<TorClusterItem>> = filteredTors
        .map { tors -> tors.map { TorClusterItem(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Currently selected tor with visit state.
     * Uses O(1) map lookups and direct observation for fast response.
     */
    val selectedTor: StateFlow<TorWithVisitState?> = _selectedTorId
        .flatMapLatest { selectedId ->
            if (selectedId == null) {
                flowOf(null)
            } else {
                val tor = torRepository.getTorById(selectedId)
                if (tor == null) {
                    flowOf(null)
                } else {
                    visitedTorRepository.observeVisitedTor(selectedId).map { visitedTor ->
                        TorWithVisitState(tor = tor, visitedTor = visitedTor)
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    init {
        viewModelScope.launch {
            torRepository.loadTors()
            collectionRepository.loadCollections()
        }
        
        viewModelScope.launch {
            preferencesRepository.showWelcome.collect { show ->
                _showWelcome.value = show
            }
        }
        
        // Load photos when photos layer is enabled
        viewModelScope.launch {
            showPhotosLayer.collect { enabled ->
                if (enabled) {
                    loadPhotosForMap()
                } else {
                    _mapPhotos.value = emptyList()
                }
            }
        }
    }
    
    /**
     * Load photos with location data within Dartmoor bounds.
     */
    private fun loadPhotosForMap() {
        viewModelScope.launch {
            _isLoadingPhotos.value = true
            try {
                val photos = photoRepository.getPhotosForMap()
                _mapPhotos.value = photos
                Log.d(TAG, "Loaded ${photos.size} photos for map")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load photos for map", e)
            } finally {
                _isLoadingPhotos.value = false
            }
        }
    }
    
    /**
     * Refresh photos on the map layer.
     */
    fun refreshMapPhotos() {
        if (showPhotosLayer.value) {
            loadPhotosForMap()
        }
    }
    
    /**
     * Select a photo to show in the preview sheet.
     */
    fun selectPhoto(photo: Photo) {
        _selectedPhoto.value = photo
        viewModelScope.launch {
            _nearbyTorsForPhoto.value = photoRepository.findTorsNearPhoto(photo)
        }
        photo.latitude?.let { lat ->
            photo.longitude?.let { lon ->
                _cameraTarget.value = LatLng(lat, lon)
                _cameraZoom.value = DETAIL_ZOOM
            }
        }
    }
    
    /**
     * Deselect the current photo.
     */
    fun deselectPhoto() {
        _selectedPhoto.value = null
        _nearbyTorsForPhoto.value = emptyList()
    }
    
    /**
     * Associate a photo with a tor from the photo preview sheet.
     */
    fun associatePhotoWithTor(photo: Photo, torId: String) {
        viewModelScope.launch {
            photoRepository.associatePhotoWithTor(torId, photo)
            _selectedPhoto.value = null
            _nearbyTorsForPhoto.value = emptyList()
            // Refresh to remove the associated photo from the map layer
            refreshMapPhotos()
        }
    }
    
    /**
     * Toggle photos layer visibility.
     */
    fun setShowPhotosLayer(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowPhotosLayer(show)
        }
    }
    
    /**
     * Select a tor to show in the detail sheet.
     */
    fun selectTor(torId: String) {
        Log.d(TAG, "selectTor called: $torId")
        _selectedTorId.value = torId
        torRepository.getTorById(torId)?.let { tor ->
            _cameraTarget.value = LatLng(tor.latitude, tor.longitude)
            _cameraZoom.value = DETAIL_ZOOM
        }
        Log.d(TAG, "selectTor completed: $torId")
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
    
    /**
     * Check if location permission is available.
     */
    fun hasLocationPermission(): Boolean = locationService.hasLocationPermission()
    
    /**
     * Check if photo permission is available.
     */
    fun hasPhotoPermission(): Boolean = photoService.hasPhotoPermission()
    
    /**
     * Get the permission string needed for reading photos.
     */
    fun getRequiredPhotoPermission(): String = photoService.getRequiredPhotoPermission()
    
    /**
     * Start location tracking.
     */
    fun startLocationTracking() {
        locationService.startLocationUpdates()
        locationService.startCompassUpdates()
    }
    
    /**
     * Stop location tracking.
     */
    fun stopLocationTracking() {
        locationService.stopLocationUpdates()
        locationService.stopCompassUpdates()
        _trackingMode.value = TrackingMode.NONE
    }
    
    /**
     * Cycle through tracking modes: NONE -> FOLLOW -> FOLLOW_COMPASS -> NONE
     */
    fun cycleTrackingMode() {
        _trackingMode.value = when (_trackingMode.value) {
            TrackingMode.NONE -> TrackingMode.FOLLOW
            TrackingMode.FOLLOW -> TrackingMode.FOLLOW_COMPASS
            TrackingMode.FOLLOW_COMPASS -> TrackingMode.NONE
        }
        
        // If entering a tracking mode, center on current location
        if (_trackingMode.value != TrackingMode.NONE) {
            currentLocation.value?.let { location ->
                _cameraTarget.value = LatLng(location.latitude, location.longitude)
                _cameraZoom.value = LOCATION_ZOOM
            }
        }
    }
    
    /**
     * Set tracking mode directly.
     */
    fun setTrackingMode(mode: TrackingMode) {
        _trackingMode.value = mode
    }
    
    /**
     * Called when user manually moves the camera.
     */
    fun onUserCameraMove() {
        // Disable tracking when user manually moves the map
        if (_trackingMode.value != TrackingMode.NONE) {
            _trackingMode.value = TrackingMode.NONE
        }
    }
    
    /**
     * Reset camera to default Dartmoor overview and disable location tracking.
     */
    fun resetToDefaultView() {
        _trackingMode.value = TrackingMode.NONE
        _cameraTarget.value = DARTMOOR_CENTER
        _cameraZoom.value = DEFAULT_ZOOM
    }
    
    /**
     * Toggle the compass line of sight visibility.
     */
    fun toggleCompassLine() {
        _showCompassLine.value = !_showCompassLine.value
    }
    
    /**
     * Calculate the endpoint of the compass line based on current location, heading, and zoom.
     * Line length scales with zoom level - longer at lower zoom (farther view).
     * 
     * @param zoomLevel Current camera zoom level
     * @return The destination LatLng or null if location is not available
     */
    fun calculateCompassLineEndpoint(zoomLevel: Float): LatLng? {
        val location = currentLocation.value ?: return null
        val heading = compassHeading.value
        
        // Calculate line length based on zoom level
        // At zoom 15 (close): ~500m, zoom 11 (default): ~5km, zoom 8 (far): ~20km
        val baseDistance = 500.0 // meters at zoom 15
        val zoomFactor = Math.pow(2.0, (15 - zoomLevel).toDouble())
        val distanceMeters = baseDistance * zoomFactor
        
        // Convert heading to radians
        val bearingRadians = Math.toRadians(heading.toDouble())
        
        // Earth's radius in meters
        val earthRadius = 6371000.0
        
        // Convert start lat/lon to radians
        val lat1 = Math.toRadians(location.latitude)
        val lon1 = Math.toRadians(location.longitude)
        
        // Calculate destination point using haversine formula
        val angularDistance = distanceMeters / earthRadius
        
        val lat2 = Math.asin(
            Math.sin(lat1) * Math.cos(angularDistance) +
            Math.cos(lat1) * Math.sin(angularDistance) * Math.cos(bearingRadians)
        )
        
        val lon2 = lon1 + Math.atan2(
            Math.sin(bearingRadians) * Math.sin(angularDistance) * Math.cos(lat1),
            Math.cos(angularDistance) - Math.sin(lat1) * Math.sin(lat2)
        )
        
        // Convert back to degrees
        return LatLng(
            Math.toDegrees(lat2),
            Math.toDegrees(lon2)
        )
    }

    /**
     * Move camera to current location.
     */
    fun goToMyLocation() {
        currentLocation.value?.let { location ->
            _cameraTarget.value = LatLng(location.latitude, location.longitude)
            _cameraZoom.value = LOCATION_ZOOM
        }
    }
    
    /**
     * Get distance from current location to a tor in meters.
     */
    fun distanceToTor(tor: Tor): Float? {
        return locationService.distanceTo(tor.latitude, tor.longitude)
    }
    
    /**
     * Set photo for a visited tor.
     */
    fun setTorPhoto(torId: String, photoUri: String?) {
        viewModelScope.launch {
            visitedTorRepository.setPhoto(torId, photoUri)
        }
    }
    
    /**
     * Remove photo from a visited tor.
     */
    fun removeTorPhoto(torId: String) {
        viewModelScope.launch {
            visitedTorRepository.setPhoto(torId, null)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopLocationTracking()
    }
}
