package com.dartmoortors.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dartmoortors.data.model.Access
import com.dartmoortors.data.model.TorSortOption
import com.dartmoortors.data.model.TorWithVisitState
import com.dartmoortors.ui.theme.Green
import com.dartmoortors.ui.theme.Orange
import com.dartmoortors.ui.theme.Teal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onTorClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredTors by viewModel.filteredTors.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val showAccessible by viewModel.showAccessible.collectAsState()
    val showVisited by viewModel.showVisited.collectAsState()
    val showUnvisited by viewModel.showUnvisited.collectAsState()
    
    var showSortMenu by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            onSearch = { },
            active = false,
            onActiveChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search tors…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            }
        ) { }
        
        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = showAccessible,
                onClick = { viewModel.toggleAccessible() },
                label = { Text("Accessible") }
            )
            FilterChip(
                selected = showVisited,
                onClick = { viewModel.toggleVisited() },
                label = { Text("Visited") }
            )
            FilterChip(
                selected = showUnvisited,
                onClick = { viewModel.toggleUnvisited() },
                label = { Text("Unvisited") }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Sort button
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                }
                
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    TorSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                viewModel.setSortOption(option)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (sortOption == option) {
                                    Icon(Icons.Default.Check, null)
                                }
                            }
                        )
                    }
                }
            }
        }
        
        // Results count
        Text(
            text = "${filteredTors.size} tors",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        // Tor list
        if (filteredTors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tors found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredTors, key = { it.tor.id }) { torWithState ->
                    TorListItem(
                        torWithState = torWithState,
                        onClick = { onTorClick(torWithState.tor.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TorListItem(
    torWithState: TorWithVisitState,
    onClick: () -> Unit
) {
    val tor = torWithState.tor
    val access = Access.fromString(tor.access)

    val color = when {
        torWithState.isVisited -> Green
        access.isAccessible -> Teal
        else -> Orange
    }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = tor.name,
                color = if (!access.isAccessible) Orange else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Column {
                Text("${tor.heightMeters}m • ${tor.classification}")
                if (!torWithState.isInActiveCollection) {
                    Text(
                        text = "Not in active Collection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (torWithState.isVisited) Icons.Default.CheckCircle else Icons.Default.Place,
                contentDescription = null,
                tint = color
            )
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
    HorizontalDivider()
}
