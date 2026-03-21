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
import kotlin.math.max

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
    
    // Photos layer state (works with locally-stored photos only)
    val showPhotosLayer by viewModel.showPhotosLayer.collectAsState()
    val mapPhotos by viewModel.mapPhotos.collectAsState()
    val selectedPhoto by viewModel.selectedPhoto.collectAsState()
    val nearbyTorsForPhoto by viewModel.nearbyTorsForPhoto.collectAsState()
    val isLoadingPhotos by viewModel.isLoadingPhotos.collectAsState()
    
    // Location state
    val currentLocation by viewModel.currentLocation.collectAsState()
    val compassHeading by viewModel.compassHeading.collectAsState()
    val trackingMode by viewModel.trackingMode.collectAsState()
    val isLocationEnabled by viewModel.isLocationEnabled.collectAsState()
    
    // Compass line of sight state
    val showCompassLine by viewModel.showCompassLine.collectAsState()
    
    // Permission state
    var hasLocationPermission by remember { mutableStateOf(viewModel.hasLocationPermission()) }
    var hasPhotoPermission by remember { mutableStateOf(viewModel.hasPhotoPermission()) }
    
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
    
    // Permission launcher for photos
    val photoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPhotoPermission = granted
        if (granted) {
            viewModel.setShowPhotosLayer(true)
        }
    }
    
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
            
            // Compass line of sight
            if (showCompassLine && currentLocation != null && hasLocationPermission) {
                val userLatLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
                val endPoint = viewModel.calculateCompassLineEndpoint(cameraPositionState.position.zoom)
                endPoint?.let { end ->
                    Polyline(
                        points = listOf(userLatLng, end),
                        color = Color.Red,
                        width = 8f
                    )
                }
            }
            
            // Photo markers (purple pins) - only shows locally stored photos
            if (showPhotosLayer) {
                mapPhotos.forEach { photo ->
                    if (photo.latitude != null && photo.longitude != null) {
                        Marker(
                            state = MarkerState(position = LatLng(photo.latitude, photo.longitude)),
                            title = photo.displayName ?: "Photo",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                            onClick = {
                                viewModel.selectPhoto(photo)
                                true
                            }
                        )
                    }
                }
            }
        }
        
        // Map controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Reset to Dartmoor overview button
            FloatingActionButton(
                onClick = { viewModel.resetToDefaultView() },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.ZoomOutMap, contentDescription = "Reset to Dartmoor")
            }

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
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Show Photos") },
                        onClick = { 
                            if (showPhotosLayer) {
                                viewModel.setShowPhotosLayer(false)
                            } else {
                                if (hasPhotoPermission) {
                                    viewModel.setShowPhotosLayer(true)
                                } else {
                                    photoPermissionLauncher.launch(viewModel.getRequiredPhotoPermission())
                                }
                            }
                            showMapTypeMenu = false
                        },
                        leadingIcon = { 
                            if (showPhotosLayer) Icon(Icons.Default.Check, null, tint = Purple)
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.PhotoLibrary, 
                                contentDescription = null,
                                tint = if (showPhotosLayer) Purple else LocalContentColor.current
                            )
                        }
                    )
                }
            }
        }
        
        // Bottom-right control buttons column
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compass line of sight button (only visible when location permission granted)
            if (hasLocationPermission) {
                FloatingActionButton(
                    onClick = { viewModel.toggleCompassLine() },
                    containerColor = if (showCompassLine) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Compass Line of Sight",
                        tint = if (showCompassLine) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
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
        
        // Photo preview bottom sheet
        selectedPhoto?.let { photo ->
            val photoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { viewModel.deselectPhoto() },
                sheetState = photoSheetState
            ) {
                PhotoPreviewSheet(
                    photo = photo,
                    nearbyTors = nearbyTorsForPhoto,
                    onTorSelected = { torId -> viewModel.associatePhotoWithTor(photo, torId) },
                    onDismiss = { viewModel.deselectPhoto() }
                )
            }
        }
    }
}

/**
 * Bottom sheet showing a photo preview with nearby tors to associate.
 */
@Composable
private fun PhotoPreviewSheet(
    photo: Photo,
    nearbyTors: List<TorWithDistance>,
    onTorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        // Photo preview
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.uri)
                .crossfade(true)
                .build(),
            contentDescription = "Photo preview",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Photo date
        Text(
            text = dateFormatter.format(Date(photo.dateTaken)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Nearby tors section
        if (nearbyTors.isEmpty()) {
            Text(
                text = "No tors within 100m of this photo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Nearby tors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(nearbyTors) { torWithDistance ->
                    NearbyTorItem(
                        torWithDistance = torWithDistance,
                        onClick = { onTorSelected(torWithDistance.tor.id) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Cancel button
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}

/**
 * List item for a nearby tor in the photo preview sheet.
 */
@Composable
private fun NearbyTorItem(
    torWithDistance: TorWithDistance,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = torWithDistance.tor.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${torWithDistance.distanceMeters.toInt()}m away • ${torWithDistance.tor.heightMeters}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.Add,
                contentDescription = "Associate with this tor",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
