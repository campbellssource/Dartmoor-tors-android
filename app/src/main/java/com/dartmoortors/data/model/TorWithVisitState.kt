package com.dartmoortors.data.model

/**
 * UI-friendly representation of a tor with its visited state.
 */
data class TorWithVisitState(
    val tor: Tor,
    val visitedTor: VisitedTor?,
    val distanceMeters: Float? = null,
    val isInActiveCollection: Boolean = true
) {
    val isVisited: Boolean get() = visitedTor != null
    val hasPhoto: Boolean get() = visitedTor?.photoUri != null
}

/**
 * Filter state for searching/browsing tors.
 */
data class TorFilter(
    val searchQuery: String = "",
    val enabledClassifications: Set<Classification> = Classification.entries.filter { it.defaultEnabled }.toSet(),
    val accessibleOnly: Boolean = true,
    val showVisited: Boolean = true,
    val showUnvisited: Boolean = true
)

/**
 * Sort options for tor lists.
 */
enum class TorSortOption(val displayName: String) {
    HEIGHT_DESC("Height (highest)"),
    HEIGHT_ASC("Height (lowest)"),
    NAME_ASC("Name A–Z"),
    NAME_DESC("Name Z–A"),
    DISTANCE("Distance"),
    NORTH_TO_SOUTH("North to South"),
    SOUTH_TO_NORTH("South to North"),
    EAST_TO_WEST("East to West"),
    WEST_TO_EAST("West to East")
}
