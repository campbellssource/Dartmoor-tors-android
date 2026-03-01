package com.dartmoortors.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun WelcomeDialog(
    onDismiss: () -> Unit,
    onFindPhotos: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Feature list
                WelcomeFeatureItem(
                    icon = Icons.Default.Terrain,
                    text = "Choose tors to bag in Collection"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                WelcomeFeatureItem(
                    icon = Icons.Default.Search,
                    text = "Search, filter and sort tors"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                WelcomeFeatureItem(
                    icon = Icons.Default.PhotoLibrary,
                    text = "Add photos and create an album"
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Primary CTA
                Button(
                    onClick = onFindPhotos,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Find tors from your photos")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Skip link
                TextButton(onClick = onDismiss) {
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
private fun WelcomeFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
