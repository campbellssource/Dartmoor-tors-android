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
        uri?.let {
            // Take persistent permission so the URI remains valid after app restart
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Some URIs don't support persistent permissions - continue anyway
                // The photo may become inaccessible after restart
            }
            onPhotoSelected(it)
        }
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
        // Heading: Tor name
        Text(
            text = tor.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sub heading: [height] [tor category with link]
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${tor.heightMeters}m height.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            tor.classification?.let { classification ->
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Type:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = classification,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.torsofdartmoor.co.uk/about.php#classification"))
                        context.startActivity(intent)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Further sub heading: [Access status] [not in collection]
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = access.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (access.isAccessible) MaterialTheme.colorScheme.onSurfaceVariant else Orange
            )
            if (!torWithState.isInActiveCollection) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Not in selected collection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bagged status area
        if (torWithState.isVisited) {
            // Visited state: date with edit button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bagged on",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = torWithState.visitedTor?.let {
                                dateFormatter.format(Date(it.visitedDate))
                            } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Edit")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onUnmarkVisited,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Remove from Bagged")
            }
        } else {
            // Not visited: full width button "Bag this Tor"
            Button(
                onClick = onMarkVisited,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bag this Tor")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        // Detail section
        // Grid reference with OS Maps link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OS Grid: ${tor.osGridRef}",
                style = MaterialTheme.typography.bodyLarge
            )
            TextButton(
                onClick = {
                    val uri = Uri.parse("https://osmaps.com/map?lat=${tor.latitude}&lon=${tor.longitude}&zoom=16")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                }
            ) {
                Text("OS Maps")
            }
        }

        // Coordinates with Google Maps link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lat: ${"%.4f".format(tor.latitude)}, Long: ${"%.4f".format(tor.longitude)}",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(
                onClick = {
                    val uri = Uri.parse("geo:${tor.latitude},${tor.longitude}?q=${tor.latitude},${tor.longitude}(${Uri.encode(tor.name)})")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                }
            ) {
                Text("Google Maps")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tors of Dartmoor link with UTM
        tor.torsOfDartmoorURL?.let { url ->
            OutlinedButton(
                onClick = {
                    val urlWithUtm = "$url?utm_source=dartmoortorsapp-tordetail&medium=android"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlWithUtm))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Language, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View on torsofdartmoor.co.uk")
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
                Text("Read about on Wikipedia")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        // Metadata section
        Text(
            text = "Details",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        tor.parish?.let { parish ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Parish",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = parish,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        tor.rockType?.let { rockType ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rock Type",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = rockType,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Collections section
        tor.collections?.takeIf { it.isNotEmpty() }?.let { collections ->
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Collections",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = collections.map { collectionDisplayName(it) }.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
 * Convert collection ID to display name.
 */
private fun collectionDisplayName(id: String): String {
    return when (id) {
        "os-map" -> "OS Map Tors"
        "tors-of-dartmoor" -> "Compendium"
        "rock-idols" -> "Rock Idols"
        else -> id.replace("-", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
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
