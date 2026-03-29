package com.dartmoortors.ui.map

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import com.dartmoortors.data.model.Access
import com.dartmoortors.data.model.Photo
import com.dartmoortors.data.model.TorWithVisitState
import com.dartmoortors.data.repository.TorWithDistance
import com.dartmoortors.ui.components.TorDetailSheet
import com.dartmoortors.ui.map.TrackingMode
import com.dartmoortors.ui.theme.Green
import com.dartmoortors.ui.theme.Orange
import com.dartmoortors.ui.theme.Purple
import com.dartmoortors.ui.theme.Teal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

private const val TAG = "MapScreen"

@Composable
private fun AsyncPhotoThumbnail(
    photoUri: String,
    fallbackColor: Color,
    size: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(fallbackColor),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photoUri)
                .size((size * 2).toInt()) // Request 2x for pixel density
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = "Photo thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onTorSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val clusterItems by viewModel.clusterItems.collectAsState()
    val selectedTor by viewModel.selectedTor.collectAsState()
    val mapType by viewModel.mapType.collectAsState()
    val cameraTarget by viewModel.cameraTarget.collectAsState()
    val cameraZoom by viewModel.cameraZoom.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Photos layer state
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            viewModel.startLocationTracking()
        }
    }
    
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.startLocationTracking()
        }
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(MapViewModel.DARTMOOR_CENTER, MapViewModel.DEFAULT_ZOOM)
    }
    
    var isUserMovingCamera by remember { mutableStateOf(false) }
    
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            isUserMovingCamera = true
        } else {
            if (isUserMovingCamera && trackingMode != TrackingMode.NONE) {
                viewModel.onUserCameraMove()
            }
            isUserMovingCamera = false
        }
    }
    
    LaunchedEffect(cameraTarget) {
        cameraTarget?.let { target ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(target, cameraZoom),
                durationMs = 500
            )
            viewModel.clearCameraTarget()
        }
    }
    
    LaunchedEffect(currentLocation, trackingMode) {
        if (trackingMode == TrackingMode.FOLLOW && currentLocation != null && !isUserMovingCamera) {
            val location = currentLocation!!
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)),
                durationMs = 300
            )
        }
    }
    
    LaunchedEffect(compassHeading, trackingMode, currentLocation) {
        if (trackingMode == TrackingMode.FOLLOW_COMPASS && currentLocation != null && !isUserMovingCamera) {
            val location = currentLocation!!
            val newPosition = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(cameraPositionState.position.zoom)
                .bearing(compassHeading)
                .tilt(45f)
                .build()
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(newPosition),
                durationMs = 100
            )
        }
    }
    
    val googleMapType = when (mapType) {
        1 -> MapType.SATELLITE
        2 -> MapType.TERRAIN
        3 -> MapType.HYBRID
        else -> MapType.NORMAL
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    
    Box(modifier = Modifier.fillMaxSize()) {
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
            )
        ) {
            // Conditional clustering: only cluster when there are more than 270 tors
            if (clusterItems.size > 270) {
                // OPTIMIZED: Marker Clustering for large collections
                // This groups markers together when zoomed out, significantly improving panning performance.
                Clustering(
                    items = clusterItems,
                    onClusterItemClick = { item ->
                        viewModel.selectTor(item.id)
                        true
                    },
                    clusterContent = { cluster ->
                        // Custom cluster appearance (Circle with count)
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            tonalElevation = 4.dp,
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = cluster.size.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    },
                    clusterItemContent = { item ->
                        // Show photo thumbnail if visited and has photo
                        if (item.isVisited && item.hasPhoto && item.photoUri != null) {
                            AsyncPhotoThumbnail(
                                photoUri = item.photoUri,
                                fallbackColor = Green,
                                size = 16f
                            )
                        } else {
                            // Custom individual marker appearance - simple circle
                            val color = when {
                                !item.isInActiveCollection -> Color.Gray
                                item.isVisited -> Green
                                item.isAccessible -> Teal
                                else -> Orange
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(color, CircleShape)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                    }
                )

                // Show selected tor with grey marker if it's not in the active collection
                selectedTor?.let { torWithState ->
                    if (!torWithState.isInActiveCollection) {
                        MarkerComposable(
                            keys = arrayOf(torWithState.tor.id, "selected-out-of-collection"),
                            state = rememberMarkerState(
                                position = LatLng(torWithState.tor.latitude, torWithState.tor.longitude)
                            ),
                            title = torWithState.tor.name
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color.Gray, CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }
            } else {
                // NO CLUSTERING: Show individual markers for smaller collections
                // This allows for more granular control when the number of markers is manageable.
                clusterItems.forEach { item ->
                    MarkerComposable(
                        keys = arrayOf(item.id, item.isVisited, item.isInActiveCollection),
                        state = rememberMarkerState(position = item.position),
                        title = item.title,
                        onClick = {
                            viewModel.selectTor(item.id)
                            true
                        }
                    ) {
                        if (item.isVisited && item.hasPhoto && item.photoUri != null) {
                            AsyncPhotoThumbnail(
                                photoUri = item.photoUri,
                                fallbackColor = Green,
                                size = 20f
                            )
                        } else {
                            val color = when {
                                !item.isInActiveCollection -> Color.Gray
                                item.isVisited -> Green
                                item.isAccessible -> Teal
                                else -> Orange
                            }
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(color, CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }
            }

            // Photos Layer
            if (showPhotosLayer) {
                mapPhotos.forEach { photo ->
                    if (photo.latitude != null && photo.longitude != null) {
                        MarkerComposable(
                            keys = arrayOf(photo.id, photo.uri),
                            state = rememberMarkerState(position = LatLng(photo.latitude, photo.longitude)),
                            onClick = {
                                viewModel.selectPhoto(photo)
                                true
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .border(2.dp, Purple, CircleShape)
                                    .padding(2.dp)
                            ) {
                                AsyncPhotoThumbnail(
                                    photoUri = photo.uri.toString(),
                                    fallbackColor = Purple,
                                    size = 28f
                                )
                            }
                        }
                    }
                }
            }
            
            // Compass Line of Sight
            if (showCompassLine && currentLocation != null && trackingMode == TrackingMode.FOLLOW_COMPASS) {
                val userLatLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
                val endPoint = viewModel.calculateCompassLineEndpoint(cameraPositionState.position.zoom)
                endPoint?.let { end ->
                    Polyline(
                        points = listOf(userLatLng, end),
                        color = Purple.copy(alpha = 0.6f),
                        width = 5f,
                        geodesic = true
                    )
                }
            }
        }

        // Overlay Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Map Type Selector
            var showMapTypeMenu by remember { mutableStateOf(false) }
            Box {
                FloatingActionButton(
                    onClick = { showMapTypeMenu = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = when(mapType) {
                            1 -> Icons.Default.Terrain
                            2 -> Icons.Default.Map
                            else -> Icons.Default.Satellite
                        },
                        contentDescription = "Change Map Type"
                    )
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
                }
            }

            // Compass Line Toggle
            if (hasLocationPermission) {
                FloatingActionButton(
                    onClick = { viewModel.toggleCompassLine() },
                    containerColor = if (showCompassLine) Purple else MaterialTheme.colorScheme.surface,
                    contentColor = if (showCompassLine) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Toggle Compass Line"
                    )
                }
            }
        }

        // Bottom Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 80.dp) // Avoid overlap with bottom nav if present
        ) {
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
                    TrackingMode.FOLLOW -> MaterialTheme.colorScheme.primary
                    TrackingMode.FOLLOW_COMPASS -> Purple
                },
                contentColor = when (trackingMode) {
                    TrackingMode.NONE -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onPrimary
                }
            ) {
                Icon(
                    imageVector = when (trackingMode) {
                        TrackingMode.NONE -> Icons.Default.MyLocation
                        TrackingMode.FOLLOW -> Icons.Default.LocationSearching
                        TrackingMode.FOLLOW_COMPASS -> Icons.Default.Explore
                    },
                    contentDescription = "Location Tracking"
                )
            }
        }
        
        // Photo Detail Sheet
        selectedPhoto?.let { photo ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.deselectPhoto() },
                sheetState = sheetState
            ) {
                PhotoDetailContent(
                    photo = photo,
                    nearbyTors = nearbyTorsForPhoto,
                    onTorClick = { torId ->
                        viewModel.associatePhotoWithTor(photo, torId)
                        viewModel.deselectPhoto()
                        onTorSelected(torId)
                    }
                )
            }
        }

        // Tor Detail Sheet
        selectedTor?.let { torWithState ->
            TorDetailSheet(
                torWithState = torWithState,
                onMarkVisited = { viewModel.markTorAsVisited(torWithState.tor.id) },
                onUnmarkVisited = { viewModel.unmarkTorAsVisited(torWithState.tor.id) },
                onDateChanged = { date -> viewModel.updateVisitedDate(torWithState.tor.id, date) },
                onPhotoSelected = { uri -> viewModel.setTorPhoto(torWithState.tor.id, uri.toString()) },
                onPhotoRemoved = { viewModel.removeTorPhoto(torWithState.tor.id) }
            )
        }
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun PhotoDetailContent(
    photo: Photo,
    nearbyTors: List<TorWithDistance>,
    onTorClick: (String) -> Unit
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault())
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Photo Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.uri)
                .crossfade(true)
                .build(),
            contentDescription = "Photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sdf.format(Date(photo.dateTaken)),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (nearbyTors.isNotEmpty()) {
            Text(
                text = "Nearby Tors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(nearbyTors) { torWithDistance ->
                    ListItem(
                        headlineContent = { Text(torWithDistance.tor.name) },
                        supportingContent = { 
                            Text("${String.format(Locale.getDefault(), "%.2f", torWithDistance.distanceMeters / 1000.0)} km away") 
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Orange, CircleShape)
                            )
                        },
                        modifier = Modifier.clickable { onTorClick(torWithDistance.tor.id) }
                    )
                }
            }
        } else {
            Text(
                text = "No tors found nearby this photo location.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
