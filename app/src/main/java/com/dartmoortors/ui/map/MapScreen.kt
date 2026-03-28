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
    val clusterItems by viewModel.clusterItems.collectAsState()
    val selectedTor by viewModel.selectedTor.collectAsState()
    val showWelcome by viewModel.showWelcome.collectAsState()
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
    var hasPhotoPermission by remember { mutableStateOf(viewModel.hasPhotoPermission()) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            viewModel.startLocationTracking()
        }
    }
    
    val photoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPhotoPermission = granted
        if (granted) {
            viewModel.setShowPhotosLayer(true)
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
                        onTorSelected(item.id)
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
                        // Custom individual marker appearance
                        val color = when {
                            item.isVisited -> Green
                            item.isAccessible -> Teal
                            else -> Orange
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(color, CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                )
            } else {
                // Individual markers for smaller collections (no clustering)
                clusterItems.forEach { item ->
                    val color = when {
                        item.isVisited -> Green
                        item.isAccessible -> Teal
                        else -> Orange
                    }
                    MarkerComposable(
                        keys = arrayOf(item.id, item.isVisited),
                        state = rememberMarkerState(position = item.position),
                        title = item.title,
                        onClick = {
                            onTorSelected(item.id)
                            true
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(color, CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
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
            
            // Photo markers (purple pins)
            if (showPhotosLayer) {
                mapPhotos.forEach { photo ->
                    if (photo.latitude != null && photo.longitude != null) {
                        Marker(
                            state = rememberMarkerState(key = photo.uri.toString(), position = LatLng(photo.latitude, photo.longitude)),
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
            FloatingActionButton(
                onClick = { viewModel.resetToDefaultView() },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.ZoomOutMap, contentDescription = "Reset to Dartmoor")
            }

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
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
        
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        
        error?.let { errorMessage ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(errorMessage)
            }
        }
        
        if (showWelcome) {
            WelcomeDialog(onDismiss = { viewModel.dismissWelcome() })
        }
        
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
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.uri)
                .size(600, 400)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = "Photo preview",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = dateFormatter.format(Date(photo.dateTaken)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

@Composable
private fun NearbyTorItem(
    torWithDistance: TorWithDistance,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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
            Icon(Icons.Default.Add, contentDescription = "Associate", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
