package com.dartmoortors.ui.photos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.dartmoortors.data.model.Photo
import com.dartmoortors.data.model.PhotoScanResult
import com.dartmoortors.data.repository.ScanProgress

@Composable
fun PhotosScreen(
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val scanState by viewModel.scanState.collectAsState()
    val hasPermission by viewModel.hasPhotoPermission.collectAsState()
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.checkPhotoPermission()
        if (granted) {
            viewModel.startScan()
        }
    }
    
    when (val state = scanState) {
        is ScanState.Ready -> ReadyView(
            hasPermission = hasPermission,
            onStartScan = {
                if (hasPermission) {
                    viewModel.startScan()
                } else {
                    permissionLauncher.launch(viewModel.getRequiredPhotoPermission())
                }
            }
        )
        is ScanState.Scanning -> ScanningView(
            progress = state.progress,
            onCancel = { viewModel.cancel() }
        )
        is ScanState.Reviewing -> ReviewingView(
            state = state,
            onAddPhoto = { viewModel.addCurrentPhoto() },
            onSkip = { viewModel.skipCurrentTor() },
            onNextPhoto = { viewModel.nextPhoto() },
            onPreviousPhoto = { viewModel.previousPhoto() },
            onCancel = { viewModel.cancel() }
        )
        is ScanState.Complete -> CompleteView(
            photosAdded = state.photosAdded,
            onDone = { viewModel.resetToReady() }
        )
    }
}

@Composable
private fun ReadyView(
    hasPermission: Boolean,
    onStartScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Auto-scan section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Find Photos Near Tors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scan your photo library to find photos taken near tors. Photos with GPS coordinates within 100m of a tor will be suggested for association.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onStartScan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Photo Library")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Manual add explanation
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Manual Photo Add",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You can also add photos manually:\n\n1. Tap a tor marker on the map\n2. Mark the tor as visited\n3. Tap \"Add Photo\" to select from your library",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Photos on Map section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Photos on Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Use the layers button on the Map tab to show photos on the map. Tap a photo marker to see nearby tors.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Local photos note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Local Photos Only",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Only photos stored locally on your device are scanned. Cloud-only photos (Google Photos) won't appear until downloaded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ScanningView(
    progress: ScanProgress,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Scanning photos...",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (progress.totalPhotos > 0) {
            LinearProgressIndicator(
                progress = { progress.progress },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${progress.photosScanned} of ${progress.totalPhotos} photos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${progress.matchesFound} tors found with nearby photos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        TextButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
private fun ReviewingView(
    state: ScanState.Reviewing,
    onAddPhoto: () -> Unit,
    onSkip: () -> Unit,
    onNextPhoto: () -> Unit,
    onPreviousPhoto: () -> Unit,
    onCancel: () -> Unit
) {
    val result = state.currentResult
    val photo = state.currentPhoto
    
    if (result == null || photo == null) {
        // Should not happen, but handle gracefully
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Text(
                text = "${state.currentIndex + 1} of ${state.totalResults}",
                style = MaterialTheme.typography.titleMedium
            )
            // Placeholder for alignment
            Spacer(modifier = Modifier.width(64.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tor name
        Text(
            text = result.torName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "${result.closestDistanceMeters.toInt()}m away",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Photo with navigation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo.uri)
                    .size(1200, 1200) // Larger size for full-screen viewing
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Photo near ${result.torName}",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Fit
            )
            
            // Photo navigation arrows
            if (result.photos.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Previous photo
                    FilledIconButton(
                        onClick = onPreviousPhoto,
                        enabled = state.currentPhotoIndex > 0,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous photo")
                    }
                    
                    // Next photo
                    FilledIconButton(
                        onClick = onNextPhoto,
                        enabled = state.currentPhotoIndex < result.photos.size - 1,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next photo")
                    }
                }
                
                // Photo counter
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "${state.currentPhotoIndex + 1} / ${result.photos.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
            
            Button(
                onClick = onAddPhoto,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Photo")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Photos added counter
        if (state.photosAdded > 0) {
            Text(
                text = "${state.photosAdded} photo${if (state.photosAdded > 1) "s" else ""} added",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompleteView(
    photosAdded: Int,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (photosAdded > 0) Icons.Default.CheckCircle else Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (photosAdded > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (photosAdded > 0) "Scan Complete!" else "No Local Photos Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (photosAdded > 0) {
            Text(
                text = "$photosAdded photo${if (photosAdded > 1) "s" else ""} added to tor${if (photosAdded > 1) "s" else ""}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "No photos with GPS coordinates were found on your device.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Using Google Photos?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Photos backed up to Google Photos but not downloaded locally won't appear here.\n\nTo include them:\n\n1. Open the Google Photos app\n2. Find photos taken on Dartmoor\n3. Tap ⋮ → Download\n4. Return here and scan again",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Alternatively, take new photos with your device camera on your next Dartmoor visit — these are stored locally by default.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onDone) {
            Text("Done")
        }
    }
}
