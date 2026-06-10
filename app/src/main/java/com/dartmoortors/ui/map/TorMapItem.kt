package com.dartmoortors.ui.map

import com.google.android.gms.maps.model.LatLng
import com.dartmoortors.data.model.TorWithVisitState

/**
 * Lightweight model for a single tor pin on the map.
 *
 * Holds only the fields the marker layer needs, with a value-based equals/hashCode so
 * StateFlow can skip emissions when nothing visible changed. (Previously implemented
 * ClusterItem for marker clustering, which was removed in T2-04.)
 */
class TorMapItem(
    val torWithState: TorWithVisitState
) {
    val id: String = torWithState.tor.id
    val position: LatLng = LatLng(torWithState.tor.latitude, torWithState.tor.longitude)
    val title: String = torWithState.tor.name
    val isVisited: Boolean = torWithState.isVisited
    val isAccessible: Boolean = torWithState.tor.isAccessible
    val isInActiveCollection: Boolean = torWithState.isInActiveCollection

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TorMapItem) return false
        return id == other.id && isVisited == other.isVisited &&
            isAccessible == other.isAccessible && isInActiveCollection == other.isInActiveCollection
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + isVisited.hashCode()
        result = 31 * result + isAccessible.hashCode()
        result = 31 * result + isInActiveCollection.hashCode()
        return result
    }
}
