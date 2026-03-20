package com.dartmoortors.ui.map

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.dartmoortors.data.model.Access
import com.dartmoortors.data.model.Photo
import com.dartmoortors.data.model.TorWithVisitState
import com.dartmoortors.data.repository.TorWithDistance
import com.dartmoortors.ui.components.TorDetailSheet
import com.dartmoortors.ui.components.WelcomeDialog
import com.dartmoortors.ui.map.TrackingMode
import com.dartmoortors.ui.theme.Green
import com.dartmoortors.ui.theme.Orange
import com.dartmoortors.ui.theme.Purple
import com.dartmoortors.ui.theme.Teal
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "MapScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onTorSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val filteredTors by viewModel.filteredTors.collectAsState()
    val selectedTor by viewModel.selectedTor.collectAsState()
    val showWelcome by viewModel.showWelcome.collectAsState()
    val mapType by viewModel.mapType.collectAsState()
    val cameraTarget by viewModel.cameraTarget.collectAsState()
    val cameraZoom by viewModel.cameraZoom.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Photos layer state - disabled on Android (Google Photos doesn't expose GPS metadata)
    // val showPhotosLayer by viewModel.showPhotosLayer.collectAsState()
    // val mapPhotos by viewModel.mapPhotos.collectAsState()
    // val selectedPhoto by viewModel.selectedPhoto.collectAsState()
    // val nearbyTorsForPhoto by viewModel.nearbyTorsForPhoto.collectAsState()
    // val isLoadingPhotos by viewModel.isLoadingPhotos.collectAsState()
    
    // Location state
    val currentLocation by viewModel.currentLocation.collectAsState()
    val compassHeading by viewModel.compassHeading.collectAsState()
    val trackingMode by viewModel.trackingMode.collectAsState()
    val isLocationEnabled by viewModel.isLocationEnabled.collectAsState()
    
    // Permission state
    var hasLocationPermission by remember { mutableStateOf(viewModel.hasLocationPermission()) }
    
    // Permission launcher for location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            viewModel.startLocationTracking()
        }
    }
    
    // Photo permission launcher disabled - photo layer not supported on Android
    
    // Start location tracking if permission already granted
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.startLocationTracking()
        }
    }
    
    // Log tor count for debugging
    LaunchedEffect(filteredTors.size) {
        Log.d(TAG, "MapScreen received ${filteredTors.size} filtered tors")
    }
    
    // Map camera position
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(MapViewModel.DARTMOOR_CENTER, MapViewModel.DEFAULT_ZOOM)
    }
    
    // Track if camera is being moved by user gesture
    var isUserMovingCamera by remember { mutableStateOf(false) }
    
    // Detect when user starts moving the camera to disable tracking
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            // When camera starts moving, if we're in tracking mode,
            // assume user is interacting and will disable tracking after movement
            isUserMovingCamera = true
        } else {
            // Camera stopped moving - if user was dragging, disable tracking
            if (isUserMovingCamera && trackingMode != TrackingMode.NONE) {
                viewModel.onUserCameraMove()
            }
            isUserMovingCamera = false
        }
    }
    
    // Animate to selected tor or location target
    LaunchedEffect(cameraTarget) {
        cameraTarget?.let { target ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(target, cameraZoom),
                durationMs = 500
            )
            viewModel.clearCameraTarget()
        }
    }
    
    // Follow mode - update camera when location changes
    LaunchedEffect(currentLocation, trackingMode) {
        if (trackingMode == TrackingMode.FOLLOW && currentLocation != null && !isUserMovingCamera) {
            val location = currentLocation!!
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)),
                durationMs = 300
            )
        }
    }
    
    // Compass tracking mode - update camera bearing with compass heading
    LaunchedEffect(compassHeading, trackingMode, currentLocation) {
        if (trackingMode == TrackingMode.FOLLOW_COMPASS && currentLocation != null && !isUserMovingCamera) {
            val location = currentLocation!!
            val newPosition = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(cameraPositionState.position.zoom)
                .bearing(compassHeading)
                .tilt(45f) // Add slight tilt for compass mode
                .build()
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(newPosition),
                durationMs = 100
            )
        }
    }
    
    // Map type conversion
    val googleMapType = when (mapType) {
        1 -> MapType.SATELLITE
        2 -> MapType.TERRAIN
        3 -> MapType.HYBRID
        else -> MapType.NORMAL
    }
    
    // Bottom sheet state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = googleMapType,
                isMyLocationEnabled = hasLocationPermission
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true
            ),
            onMapLoaded = {
                Log.d(TAG, "Map loaded")
            }
        ) {
            // Tor markers
            filteredTors.forEach { torWithState ->
                val tor = torWithState.tor
                val markerColor = when {
                    torWithState.isVisited -> BitmapDescriptorFactory.HUE_GREEN
                    Access.fromString(tor.access).isAccessible -> BitmapDescriptorFactory.HUE_CYAN
                    else -> BitmapDescriptorFactory.HUE_ORANGE
                }
                
                Marker(
                    state = MarkerState(position = LatLng(tor.latitude, tor.longitude)),
                    title = tor.name,
                    snippet = "${tor.heightMeters}m",
                    icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                    onClick = {
                        onTorSelected(tor.id)
                        true
                    }
                )
            }
            
            // Photo markers disabled on Android - Google Photos doesn't expose GPS metadata
            // for programmatic scanning. Users can still add photos to tors manually.
        }
        
        // Map controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Map type button
            var showMapTypeMenu by remember { mutableStateOf(false) }
            
            Box {
                FloatingActionButton(
                    onClick = { showMapTypeMenu = true },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Map Type")
                }
                
                DropdownMenu(
                    expanded = showMapTypeMenu,
                    onDismissRequest = { showMapTypeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Normal") },
                        onClick = { viewModel.setMapType(0); showMapTypeMenu = false },
                        leadingIcon = { if (mapType == 0) Icon(Icons.Default.Check, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Satellite") },
                        onClick = { viewModel.setMapType(1); showMapTypeMenu = false },
                        leadingIcon = { if (mapType == 1) Icon(Icons.Default.Check, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Terrain") },
                        onClick = { viewModel.setMapType(2); showMapTypeMenu = false },
                        leadingIcon = { if (mapType == 2) Icon(Icons.Default.Check, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Hybrid") },
                        onClick = { viewModel.setMapType(3); showMapTypeMenu = false },
                        leadingIcon = { if (mapType == 3) Icon(Icons.Default.Check, null) }
                    )
                    // Photo layer toggle disabled on Android - Google Photos doesn't expose
                    // GPS metadata for programmatic library scanning
                }
            }
        }
        
        // My location button
        FloatingActionButton(
            onClick = {
                if (hasLocationPermission) {
                    viewModel.cycleTrackingMode()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = when (trackingMode) {
                TrackingMode.NONE -> MaterialTheme.colorScheme.surface
                TrackingMode.FOLLOW -> MaterialTheme.colorScheme.primaryContainer
                TrackingMode.FOLLOW_COMPASS -> MaterialTheme.colorScheme.primary
            }
        ) {
            Icon(
                imageVector = when (trackingMode) {
                    TrackingMode.NONE -> Icons.Default.MyLocation
                    TrackingMode.FOLLOW -> Icons.Default.MyLocation
                    TrackingMode.FOLLOW_COMPASS -> Icons.Default.Explore
                },
                contentDescription = "My Location",
                tint = when (trackingMode) {
                    TrackingMode.NONE -> MaterialTheme.colorScheme.onSurface
                    TrackingMode.FOLLOW -> MaterialTheme.colorScheme.onPrimaryContainer
                    TrackingMode.FOLLOW_COMPASS -> MaterialTheme.colorScheme.onPrimary
                }
            )
        }
        
        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Error message
        error?.let { errorMessage ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(errorMessage)
            }
        }
        
        // Welcome dialog
        if (showWelcome) {
            WelcomeDialog(
                onDismiss = { viewModel.dismissWelcome() }
            )
        }
        
        // Tor detail bottom sheet
        selectedTor?.let { torWithState ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.deselectTor() },
                sheetState = sheetState
            ) {
                TorDetailSheet(
                    torWithState = torWithState,
                    onMarkVisited = { viewModel.markTorAsVisited(torWithState.tor.id) },
                    onUnmarkVisited = { viewModel.unmarkTorAsVisited(torWithState.tor.id) },
                    onDateChanged = { date -> viewModel.updateVisitedDate(torWithState.tor.id, date) },
                    onPhotoSelected = { uri -> viewModel.setTorPhoto(torWithState.tor.id, uri.toString()) },
                    onPhotoRemoved = { viewModel.removeTorPhoto(torWithState.tor.id) }
                )
            }
        }
        
        // Photo preview bottom sheet - disabled on Android (photo layer not supported)
    }
}

// PhotoPreviewSheet and NearbyTorItem removed - photo layer not supported on Android
// Google Photos does not expose GPS metadata for programmatic library scanning
