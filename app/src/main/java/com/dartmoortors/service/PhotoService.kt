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
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PhotoService"

/**
 * Service for accessing photos from the device's media library.
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
        
        const val DEFAULT_PHOTO_LIMIT = 500
        const val DEFAULT_NEARBY_RADIUS_METERS = 100.0
    }
    
    /**
     * Query photos with GPS location data within Dartmoor bounds.
     */
    suspend fun getPhotosInDartmoor(limit: Int = DEFAULT_PHOTO_LIMIT): List<Photo> = withContext(Dispatchers.IO) {
        if (!hasPhotoPermission()) return@withContext emptyList()

        val photos = mutableListOf<Photo>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN
        )

        val selection = "${MediaStore.Images.Media.MIME_TYPE} LIKE ?"
        val selectionArgs = arrayOf("image/%")
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val location = getPhotoLocation(uri)
                    
                    if (location != null) {
                        val (lat, lon) = location
                        if (lat in DARTMOOR_MIN_LAT..DARTMOOR_MAX_LAT && 
                            lon in DARTMOOR_MIN_LON..DARTMOOR_MAX_LON) {
                            photos.add(
                                Photo(
                                    id = id,
                                    uri = uri,
                                    dateTaken = cursor.getLong(dateColumn),
                                    latitude = lat,
                                    longitude = lon,
                                    displayName = cursor.getString(nameColumn)
                                )
                            )
                            count++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying Dartmoor photos", e)
        }
        
        photos
    }

    /**
     * Query all photos with GPS location data.
     */
    suspend fun getPhotosWithLocation(): List<Photo> = withContext(Dispatchers.IO) {
        if (!hasPhotoPermission()) return@withContext emptyList()
        
        val photos = mutableListOf<Photo>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN
        )
        
        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null, null,
                "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val location = getPhotoLocation(uri)
                    
                    if (location != null) {
                        photos.add(
                            Photo(
                                id = id,
                                uri = uri,
                                dateTaken = cursor.getLong(dateColumn),
                                latitude = location.first,
                                longitude = location.second,
                                displayName = cursor.getString(nameColumn)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying photos with location", e)
        }
        photos
    }

    /**
     * Extracts location using ExifInterface.
     */
    private fun getPhotoLocation(uri: Uri): Pair<Double, Double>? {
        return try {
            val photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.setRequireOriginal(uri)
            } else uri
            
            context.contentResolver.openInputStream(photoUri)?.use { stream ->
                val exif = ExifInterface(stream)
                val latLong = FloatArray(2)
                if (exif.getLatLong(latLong)) {
                    Pair(latLong[0].toDouble(), latLong[1].toDouble())
                } else null
            }
        } catch (e: Exception) {
            null
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
        val photos = getPhotosWithLocation()
        return photos.filter { photo ->
            photo.latitude != null && photo.longitude != null &&
            calculateDistance(latitude, longitude, photo.latitude, photo.longitude) <= radiusMeters
        }.minByOrNull { photo ->
            calculateDistance(latitude, longitude, photo.latitude!!, photo.longitude!!)
        }
    }

    /**
     * Check if a photo URI is still valid.
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

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    fun hasPhotoPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        val hasFull = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        
        val hasPartial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else false
        
        return hasFull || hasPartial
    }

    fun getRequiredPhotoPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}
