package com.dartmoortors.ui.map

import androidx.compose.runtime.*
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.dartmoortors.data.model.TorWithVisitState

/**
 * Optimized Marker for Tors.
 * Uses remember to avoid expensive BitmapDescriptor and State recalculations.
 */
@Composable
fun TorMarker(
    torWithState: TorWithVisitState,
    onSelected: (String) -> Unit
) {
    val tor = torWithState.tor
    val isVisited = torWithState.isVisited
    val isAccessible = tor.isAccessible
    
    // Cache the marker state and icon to prevent jank during map panning
    val markerState = rememberMarkerState(key = tor.id, position = LatLng(tor.latitude, tor.longitude))
    
    val markerIcon = remember(isVisited, isAccessible) {
        val color = when {
            isVisited -> BitmapDescriptorFactory.HUE_GREEN
            isAccessible -> BitmapDescriptorFactory.HUE_CYAN
            else -> BitmapDescriptorFactory.HUE_ORANGE
        }
        BitmapDescriptorFactory.defaultMarker(color)
    }

    Marker(
        state = markerState,
        icon = markerIcon,
        onClick = {
            onSelected(tor.id)
            true
        }
    )
}
