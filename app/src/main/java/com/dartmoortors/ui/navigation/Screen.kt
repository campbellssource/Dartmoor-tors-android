package com.dartmoortors.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for the app.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Map : Screen(
        route = "map",
        title = "Map",
        selectedIcon = Icons.Filled.Map,
        unselectedIcon = Icons.Outlined.Map
    )
    
    data object Collection : Screen(
        route = "collection",
        title = "Collection",
        selectedIcon = Icons.Filled.Map, // Will use custom tor icon
        unselectedIcon = Icons.Outlined.Map
    )
    
    data object Photos : Screen(
        route = "photos",
        title = "Photos",
        selectedIcon = Icons.Filled.PhotoLibrary,
        unselectedIcon = Icons.Outlined.PhotoLibrary
    )
    
    data object Search : Screen(
        route = "search",
        title = "Search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )
    
    companion object {
        val bottomNavItems = listOf(Map, Collection, Photos, Search)
    }
}

/**
 * Routes for non-bottom-nav destinations.
 */
object Routes {
    const val TOR_DETAIL = "tor/{torId}"
    
    fun torDetail(torId: String) = "tor/$torId"
}
