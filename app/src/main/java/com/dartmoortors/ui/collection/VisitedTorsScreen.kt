package com.dartmoortors.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dartmoortors.data.model.Access
import com.dartmoortors.data.model.TorWithVisitState
import com.dartmoortors.ui.theme.Green
import com.dartmoortors.ui.theme.Orange
import com.dartmoortors.ui.theme.Teal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitedTorsScreen(
    onTorClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val visitedTors by viewModel.visitedTorsInFilteredCollection.collectAsState()
    val selectedCollection by viewModel.selectedCollection.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visited Tors") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header showing count and collection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${visitedTors.size} visited",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                selectedCollection?.let { collection ->
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (visitedTors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No visited tors",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tors you visit will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(visitedTors, key = { it.tor.id }) { torWithState ->
                        VisitedTorListItem(
                            torWithState = torWithState,
                            onClick = { onTorClick(torWithState.tor.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitedTorListItem(
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
            Text("${tor.heightMeters}m • ${tor.classification}")
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Visited",
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
