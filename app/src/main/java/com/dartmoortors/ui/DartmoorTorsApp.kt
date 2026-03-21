package com.dartmoortors.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dartmoortors.ui.collection.CollectionScreen
import com.dartmoortors.ui.map.MapScreen
import com.dartmoortors.ui.map.MapViewModel
import com.dartmoortors.ui.navigation.Screen
import com.dartmoortors.ui.photos.PhotosScreen
import com.dartmoortors.ui.search.SearchScreen

@Composable
fun DartmoorTorsApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Shared map ViewModel to maintain state across navigation
    val mapViewModel: MapViewModel = hiltViewModel()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            if (screen.route == Screen.Map.route) {
                                mapViewModel.resetToDefaultView()
                            }
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Map.route) {
                MapScreen(
                    viewModel = mapViewModel,
                    onTorSelected = { torId ->
                        mapViewModel.selectTor(torId)
                    }
                )
            }
            
            composable(Screen.Collection.route) {
                CollectionScreen()
            }
            
            composable(Screen.Photos.route) {
                PhotosScreen()
            }
            
            composable(Screen.Search.route) {
                SearchScreen(
                    onTorClick = { torId ->
                        mapViewModel.selectTor(torId)
                        navController.navigate(Screen.Map.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
