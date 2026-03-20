package com.dartmoortors.data.model

import android.net.Uri

/**
 * Represents a photo from the device's media library with location data.
 */
data class Photo(
    val id: Long,
    val uri: Uri,
    val dateTaken: Long,
    val latitude: Double?,
    val longitude: Double?,
    val displayName: String?
) {
    /**
     * Check if this photo has valid GPS coordinates.
     */
    val hasLocation: Boolean get() = latitude != null && longitude != null
}

/**
 * Represents a match between a photo and a tor during auto-matching.
 */
data class PhotoTorMatch(
    val photo: Photo,
    val tor: Tor,
    val distanceMeters: Double
)

/**
 * Result of scanning the photo library for tor matches.
 */
data class PhotoScanResult(
    val torId: String,
    val torName: String,
    val photos: List<Photo>,
    val closestDistanceMeters: Double
)
