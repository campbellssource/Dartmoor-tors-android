package com.dartmoortors.ui.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.dartmoortors.data.model.TorWithVisitState

/**
 * Optimized ClusterItem for Tors to enable marker clustering.
 */
class TorClusterItem(
    val torWithState: TorWithVisitState
) : ClusterItem {
    override fun getPosition(): LatLng = LatLng(torWithState.tor.latitude, torWithState.tor.longitude)
    override fun getTitle(): String = torWithState.tor.name
    override fun getSnippet(): String? = null
    override fun getZIndex(): Float? = null
    
    // Stable ID for the item to prevent flashing
    val id: String = torWithState.tor.id
    
    // Visited state for icon selection
    val isVisited: Boolean = torWithState.isVisited
    val isAccessible: Boolean = torWithState.tor.isAccessible

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TorClusterItem) return false
        return id == other.id && isVisited == other.isVisited && isAccessible == other.isAccessible
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + isVisited.hashCode()
        result = 31 * result + isAccessible.hashCode()
        return result
    }
}
