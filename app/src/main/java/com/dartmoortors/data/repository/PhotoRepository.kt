package com.dartmoortors.data.repository

import android.net.Uri
import com.dartmoortors.data.model.Photo
import com.dartmoortors.data.model.PhotoScanResult
import com.dartmoortors.data.model.Tor
import com.dartmoortors.service.PhotoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for photo-related operations.
 * Combines PhotoService with TorRepository and VisitedTorRepository for
 * features like auto-matching photos to tors.
 */
@Singleton
class PhotoRepository @Inject constructor(
    private val photoService: PhotoService,
    private val torRepository: TorRepository,
    private val visitedTorRepository: VisitedTorRepository
) {
    companion object {
        const val MATCH_RADIUS_METERS = 100.0
        const val FALLBACK_RADIUS_METERS = 50.0
    }
    
    // Scanning progress state
    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanProgress: StateFlow<ScanProgress?> = _scanProgress
    
    /**
     * Get photos within Dartmoor bounds for the map layer.
     */
    suspend fun getPhotosForMap(): List<Photo> {
        return photoService.getPhotosInDartmoor()
    }
    
    /**
     * Find nearby tors for a photo to potentially associate with.
     */
    fun findTorsNearPhoto(photo: Photo): List<TorWithDistance> {
        val lat = photo.latitude ?: return emptyList()
        val lon = photo.longitude ?: return emptyList()
        
        val allTors = torRepository.getAllTors()
        return allTors.mapNotNull { tor ->
            val distance = photoService.calculateDistance(
                lat,
                lon,
                tor.latitude,
                tor.longitude
            )
            if (distance <= MATCH_RADIUS_METERS) {
                TorWithDistance(tor, distance)
            } else null
        }.sortedBy { it.distanceMeters }
    }
    
    /**
     * Scan photo library for matches with tors.
     * Updates scanProgress during scanning.
     */
    suspend fun scanForTorMatches(): List<PhotoScanResult> {
        val results = mutableMapOf<String, MutableList<PhotoWithDistance>>()
        val allTors = torRepository.getAllTors()
        
        // Get tors that don't already have photos
        val torsWithoutPhotos = allTors.filter { tor ->
            val visitedTor = visitedTorRepository.getVisitedTor(tor.id)
            visitedTor?.photoUri == null
        }
        
        val photos = photoService.getPhotosWithLocation()
        val totalPhotos = photos.size
        
        _scanProgress.value = ScanProgress(0, totalPhotos, 0)
        
        photos.forEachIndexed { index, photo ->
            val pLat = photo.latitude
            val pLon = photo.longitude
            
            if (pLat != null && pLon != null) {
                // Find tors near this photo
                torsWithoutPhotos.forEach { tor ->
                    val distance = photoService.calculateDistance(
                        pLat,
                        pLon,
                        tor.latitude,
                        tor.longitude
                    )
                    
                    if (distance <= MATCH_RADIUS_METERS) {
                        results.getOrPut(tor.id) { mutableListOf() }
                            .add(PhotoWithDistance(photo, distance))
                    }
                }
            }
            
            // Update progress every 100 photos
            if ((index + 1) % 100 == 0 || index == totalPhotos - 1) {
                _scanProgress.value = ScanProgress(
                    photosScanned = index + 1,
                    totalPhotos = totalPhotos,
                    matchesFound = results.size
                )
            }
        }
        
        // Convert to results sorted by number of matches
        return results.mapNotNull { (torId, photosWithDistance) ->
            val tor = allTors.find { it.id == torId } ?: return@mapNotNull null
            PhotoScanResult(
                torId = torId,
                torName = tor.name,
                photos = photosWithDistance.sortedBy { it.distanceMeters }.map { it.photo },
                closestDistanceMeters = photosWithDistance.minOf { it.distanceMeters }
            )
        }.sortedByDescending { it.photos.size }
    }
    
    /**
     * Clear scan progress state.
     */
    fun clearScanProgress() {
        _scanProgress.value = null
    }
    
    /**
     * Associate a photo with a tor.
     * If tor is not visited, marks it as visited with the photo's date.
     */
    suspend fun associatePhotoWithTor(torId: String, photo: Photo) {
        val existingVisit = visitedTorRepository.getVisitedTor(torId)
        
        if (existingVisit == null) {
            // Mark as visited with photo's date
            visitedTorRepository.markAsVisited(torId, photo.dateTaken)
        } else if (photo.dateTaken < existingVisit.visitedDate) {
            // Photo is older than visit date - update to photo's date
            visitedTorRepository.updateVisitedDate(torId, photo.dateTaken)
        }
        
        // Set the photo URI
        visitedTorRepository.setPhoto(torId, photo.uri.toString())
    }
    
    /**
     * Remove photo from a tor.
     */
    suspend fun removePhotoFromTor(torId: String) {
        visitedTorRepository.setPhoto(torId, null)
    }
    
    /**
     * Try to find a fallback photo for a tor using location.
     * Used when stored photo reference becomes invalid.
     */
    suspend fun findFallbackPhoto(torLatitude: Double, torLongitude: Double): Photo? {
        return photoService.findClosestPhoto(torLatitude, torLongitude, FALLBACK_RADIUS_METERS)
    }
    
    /**
     * Check if a photo URI is still valid.
     */
    suspend fun isPhotoValid(uriString: String): Boolean {
        return photoService.isPhotoValid(Uri.parse(uriString))
    }
}

/**
 * Progress state for photo library scanning.
 */
data class ScanProgress(
    val photosScanned: Int,
    val totalPhotos: Int,
    val matchesFound: Int
) {
    val progress: Float get() = if (totalPhotos > 0) photosScanned.toFloat() / totalPhotos else 0f
}

/**
 * Tor with distance from a reference point.
 */
data class TorWithDistance(
    val tor: Tor,
    val distanceMeters: Double
)

/**
 * Photo with distance from a reference point.
 */
private data class PhotoWithDistance(
    val photo: Photo,
    val distanceMeters: Double
)
