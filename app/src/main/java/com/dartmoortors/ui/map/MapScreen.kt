package com.dartmoortors.ui.map

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.dartmoortors.data.model.Access
import com.dartmoortors.data.model.TorWithVisitState
import com.dartmoortors.ui.components.TorDetailSheet
import com.dartmoortors.ui.components.WelcomeDialog
import com.dartmoortors.ui.theme.Green
import com.dartmoortors.ui.theme.Orange
import com.dartmoortors.ui.theme.Teal

private const val TAG = "MapScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onTorSelected: (String) -> Unit
) {
    val filteredTors by viewModel.filteredTors.collectAsState()
    val selectedTor by viewModel.selectedTor.collectAsState()
    val showWelcome by viewModel.showWelcome.collectAsState()
    val mapType by viewModel.mapType.collectAsState()
    val cameraTarget by viewModel.cameraTarget.collectAsState()
    val cameraZoom by viewModel.cameraZoom.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Log tor count for debugging
    LaunchedEffect(filteredTors.size) {
        Log.d(TAG, "MapScreen received ${filteredTors.size} filtered tors")
    }
    
    // Map camera position
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(MapViewModel.DARTMOOR_CENTER, MapViewModel.DEFAULT_ZOOM)
    }
    
    // Animate to selected tor
    LaunchedEffect(cameraTarget) {
        cameraTarget?.let { target ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(target, cameraZoom),
                durationMs = 500
            )
            viewModel.clearCameraTarget()
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
                isMyLocationEnabled = false // Handle separately with permission
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true
            )
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
                }
            }
        }
        
        // My location button
        FloatingActionButton(
            onClick = { /* TODO: Handle location */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "My Location")
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
                onDismiss = { viewModel.dismissWelcome() },
                onFindPhotos = {
                    viewModel.dismissWelcome()
                    // TODO: Navigate to photos tab
                }
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
                    onDateChanged = { date -> viewModel.updateVisitedDate(torWithState.tor.id, date) }
                )
            }
        }
    }
}
