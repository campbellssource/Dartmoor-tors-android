package com.dartmoortors.service

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.dartmoortors.data.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PhotoService"

/**
 * Service for accessing photos from the device's media library using MediaStore.
 * 
 * This service provides functionality for:
 * - Querying photos with location data
 * - Finding photos within geographic bounds (e.g., Dartmoor)
 * - Finding photos near specific coordinates
 * - Loading photo metadata
 */
@Singleton
class PhotoService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver get() = context.contentResolver
    
    companion object {
        // Dartmoor bounds
        const val DARTMOOR_MIN_LAT = 50.35
        const val DARTMOOR_MAX_LAT = 50.75
        const val DARTMOOR_MIN_LON = -4.15
        const val DARTMOOR_MAX_LON = -3.65
        
        // Default limits
        const val DEFAULT_PHOTO_LIMIT = 200
        const val DEFAULT_NEARBY_RADIUS_METERS = 100.0
    }
    
    /**
     * Query all photos with GPS location data within Dartmoor bounds.
     * Limited to [limit] photos for performance.
     */
    suspend fun getPhotosInDartmoor(limit: Int = DEFAULT_PHOTO_LIMIT): List<Photo> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getPhotosInDartmoor: hasPermission=${hasPhotoPermission()}")
        val allWithLocation = getPhotosWithLocation()
        Log.d(TAG, "getPhotosInDartmoor: found ${allWithLocation.size} photos with location")
        
        val inBounds = allWithLocation.filter { photo ->
            photo.latitude != null && photo.longitude != null &&
            photo.latitude >= DARTMOOR_MIN_LAT && photo.latitude <= DARTMOOR_MAX_LAT &&
            photo.longitude >= DARTMOOR_MIN_LON && photo.longitude <= DARTMOOR_MAX_LON
        }
        Log.d(TAG, "getPhotosInDartmoor: ${inBounds.size} photos within Dartmoor bounds")
        
        // Log some sample locations if we have photos but none in bounds
        if (allWithLocation.isNotEmpty() && inBounds.isEmpty()) {
            allWithLocation.take(5).forEach { photo ->
                Log.d(TAG, "Sample photo location: lat=${photo.latitude}, lon=${photo.longitude}")
            }
        }
        
        inBounds.take(limit)
    }
    
    /**
     * Query all photos with GPS location data.
     * For features that need to scan the full library.
     */
    suspend fun getPhotosWithLocation(): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()
        var totalPhotos = 0
        var photosWithLocation = 0
        
        // Debug: Check total count of ALL images first
        val countProjection = arrayOf(MediaStore.Images.Media._ID)
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            countProjection,
            null,
            null,
            null
        )?.use { cursor ->
            Log.d(TAG, "DEBUG: Total images in EXTERNAL_CONTENT_URI: ${cursor.count}")
        }
        
        // Also check INTERNAL
        contentResolver.query(
            MediaStore.Images.Media.INTERNAL_CONTENT_URI,
            countProjection,
            null,
            null,
            null
        )?.use { cursor ->
            Log.d(TAG, "DEBUG: Total images in INTERNAL_CONTENT_URI: ${cursor.count}")
        }
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN
        )
        
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        
        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                Log.d(TAG, "getPhotosWithLocation: cursor has ${cursor.count} photos")
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                
                while (cursor.moveToNext()) {
                    totalPhotos++
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val dateTaken = cursor.getLong(dateColumn)
                    
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    
                    // Try to get location from EXIF
                    val location = getPhotoLocation(uri)
                    
                    if (location != null) {
                        photosWithLocation++
                        photos.add(
                            Photo(
                                id = id,
                                uri = uri,
                                dateTaken = dateTaken,
                                latitude = location.first,
                                longitude = location.second,
                                displayName = name
                            )
                        )
                    }
                }
            } ?: Log.e(TAG, "getPhotosWithLocation: cursor is null!")
            
            Log.d(TAG, "getPhotosWithLocation: processed $totalPhotos photos, $photosWithLocation have location")
        } catch (e: Exception) {
            Log.e(TAG, "getPhotosWithLocation: error querying photos", e)
        }
        
        photos
    }
    
    /**
     * Find photos near a specific location.
     */
    suspend fun getPhotosNearLocation(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = DEFAULT_NEARBY_RADIUS_METERS
    ): List<Photo> = withContext(Dispatchers.IO) {
        getPhotosWithLocation().filter { photo ->
            photo.latitude != null && photo.longitude != null &&
            calculateDistance(latitude, longitude, photo.latitude, photo.longitude) <= radiusMeters
        }.sortedBy { photo ->
            calculateDistance(latitude, longitude, photo.latitude!!, photo.longitude!!)
        }
    }
    
    /**
     * Find the closest photo to a location within a radius.
     */
    suspend fun findClosestPhoto(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = DEFAULT_NEARBY_RADIUS_METERS
    ): Photo? {
        return getPhotosNearLocation(latitude, longitude, radiusMeters).firstOrNull()
    }
    
    /**
     * Get photo information by URI.
     */
    suspend fun getPhotoByUri(uri: Uri): Photo? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN
        )
        
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val dateTaken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))
                val location = getPhotoLocation(uri)
                
                Photo(
                    id = id,
                    uri = uri,
                    dateTaken = dateTaken,
                    latitude = location?.first,
                    longitude = location?.second,
                    displayName = name
                )
            } else null
        }
    }
    
    /**
     * Check if a photo URI is still valid (not deleted).
     */
    suspend fun isPhotoValid(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media._ID), null, null, null)?.use {
                it.count > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Extract GPS location from photo EXIF data.
     * Requires ACCESS_MEDIA_LOCATION permission on Android 10+.
     */
    private var exifErrorCount = 0
    private fun getPhotoLocation(uri: Uri): Pair<Double, Double>? {
        return try {
            val photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.setRequireOriginal(uri)
            } else {
                uri
            }
            
            contentResolver.openInputStream(photoUri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val latLong = FloatArray(2)
                if (exif.getLatLong(latLong)) {
                    Pair(latLong[0].toDouble(), latLong[1].toDouble())
                } else null
            }
        } catch (e: Exception) {
            // Photo might not have EXIF data or location permission denied
            exifErrorCount++
            if (exifErrorCount <= 3) {
                Log.w(TAG, "getPhotoLocation failed for $uri: ${e.message}")
            } else if (exifErrorCount == 4) {
                Log.w(TAG, "getPhotoLocation: suppressing further error logs...")
            }
            null
        }
    }
    
    /**
     * Calculate distance between two GPS coordinates in meters.
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }
    
    /**
     * Check if the app has permission to read photos.
     */
    fun hasPhotoPermission(): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        // Check for partial access on Android 14+
        val hasPartialAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        } else false
        
        Log.d(TAG, "hasPhotoPermission: full=$hasPermission, partial=$hasPartialAccess, SDK=${Build.VERSION.SDK_INT}")
        
        return hasPermission || hasPartialAccess
    }
    
    /**
     * Check if we have FULL photo access (not just partial/selected).
     */
    fun hasFullPhotoAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get the permission string needed for this device's API level.
     */
    fun getRequiredPhotoPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}
