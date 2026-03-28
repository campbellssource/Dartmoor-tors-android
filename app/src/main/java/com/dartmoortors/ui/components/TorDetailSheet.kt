package com.dartmoortors.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.dartmoortors.data.model.Access
import com.dartmoortors.data.model.TorWithVisitState
import com.dartmoortors.ui.theme.Orange
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorDetailSheet(
    torWithState: TorWithVisitState,
    onMarkVisited: () -> Unit,
    onUnmarkVisited: () -> Unit,
    onDateChanged: (Long) -> Unit,
    onPhotoSelected: (Uri) -> Unit,
    onPhotoRemoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tor = torWithState.tor
    val context = LocalContext.current
    val access = Access.fromString(tor.access)
    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }
    
    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = torWithState.visitedTor?.visitedDate ?: System.currentTimeMillis()
    )
    
    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onPhotoSelected(it) }
    }
    
    // Track image loading state
    var imageLoadState by remember { mutableStateOf<AsyncImagePainter.State?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Photo Section (only visible for visited tors)
        if (torWithState.isVisited) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val photoUri = torWithState.visitedTor?.photoUri
                
                if (photoUri != null) {
                    // Show photo
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUri)
                            .size(800, 400) // Target size for memory efficiency
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "Photo of ${tor.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onState = { state -> imageLoadState = state }
                    )
                    
                    // Loading indicator
                    if (imageLoadState is AsyncImagePainter.State.Loading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    // Error state - show broken photo indicator
                    if (imageLoadState is AsyncImagePainter.State.Error) {
                        BrokenPhotoIndicator(
                            onReplaceClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRemoveClick = onPhotoRemoved
                        )
                    } else {
                        // Overlay buttons for change/remove
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)),
                                        startY = 150f
                                    )
                                )
                        )
                        
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Change photo")
                            }
                            FilledTonalIconButton(onClick = onPhotoRemoved) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove photo")
                            }
                        }
                    }
                } else {
                    // No photo - show add prompt
                    AddPhotoPrompt(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            }
        }
        
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = if (torWithState.isVisited) 16.dp else 0.dp, bottom = 24.dp)
        ) {
        // Tor name
        Text(
            text = tor.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Height
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Terrain,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${tor.heightMeters}m",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Grid reference
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.GridOn,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tor.osGridRef,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Coordinates
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Place,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "%.6f, %.6f".format(tor.latitude, tor.longitude),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Access status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (access.isAccessible) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (access.isAccessible) MaterialTheme.colorScheme.primary else Orange
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = access.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (access.isAccessible) MaterialTheme.colorScheme.onSurface else Orange
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Visited toggle
        if (torWithState.isVisited) {
            // Visited state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Visited",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = onUnmarkVisited) {
                            Text("Unmark")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Date display/picker
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = torWithState.visitedTor?.let {
                                dateFormatter.format(Date(it.visitedDate))
                            } ?: "Select date"
                        )
                    }
                }
            }
        } else {
            // Not visited - show mark as visited button
            Button(
                onClick = onMarkVisited,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark as Visited")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // External links
        Text(
            text = "Open in...",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Google Maps link
        OutlinedButton(
            onClick = {
                val uri = Uri.parse("geo:${tor.latitude},${tor.longitude}?q=${tor.latitude},${tor.longitude}(${Uri.encode(tor.name)})")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Map, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Google Maps")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Tors of Dartmoor link
        tor.torsOfDartmoorURL?.let { url ->
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Language, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tors of Dartmoor")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Wikipedia link
        tor.wikipediaURL?.let { url ->
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wikipedia")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Share button
        Button(
            onClick = {
                val shareText = buildString {
                    appendLine(tor.name)
                    append("${tor.heightMeters}m")
                    tor.parish?.let { append(" | $it") }
                    appendLine()
                    appendLine("OS Grid: ${tor.osGridRef}")
                    appendLine("Lat: ${"%.6f".format(tor.latitude)}, Long: ${"%.6f".format(tor.longitude)}")
                    appendLine()
                    appendLine("Open in Dartmoor Tors: https://dartmoortors.com/tor/${tor.id}")
                    tor.torsOfDartmoorURL?.let { appendLine("Tors of Dartmoor: $it") }
                    append("Google Maps: https://maps.google.com/?q=${tor.latitude},${tor.longitude}")
                }
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share ${tor.name}")
                context.startActivity(shareIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share")
        }
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateChanged(it) }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Prompt to add a photo when none exists.
 */
@Composable
private fun AddPhotoPrompt(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AddAPhoto,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add Photo",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Indicator shown when a photo reference is broken (file deleted or inaccessible).
 */
@Composable
private fun BrokenPhotoIndicator(
    onReplaceClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.BrokenImage,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Photo unavailable",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "The original file may have been deleted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = onReplaceClick) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Replace")
                }
                OutlinedButton(onClick = onRemoveClick) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
        }
    }
}
