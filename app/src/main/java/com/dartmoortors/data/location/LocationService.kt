package com.dartmoortors.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing device location and compass heading.
 */
@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    
    private val _compassHeading = MutableStateFlow(0f)
    val compassHeading: StateFlow<Float> = _compassHeading.asStateFlow()
    
    private val _isLocationEnabled = MutableStateFlow(false)
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled.asStateFlow()
    
    private var locationCallback: LocationCallback? = null
    private var compassListener: SensorEventListener? = null
    
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    
    /**
     * Check if location permission is granted.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start location updates.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (!hasLocationPermission()) return
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Update every 5 seconds
        ).apply {
            setMinUpdateIntervalMillis(2000L)
            setWaitForAccurateLocation(false)
        }.build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _currentLocation.value = location
                    _isLocationEnabled.value = true
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
        
        // Also get last known location immediately
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                _currentLocation.value = it
                _isLocationEnabled.value = true
            }
        }
    }
    
    /**
     * Stop location updates.
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        _isLocationEnabled.value = false
    }
    
    /**
     * Start compass updates for device heading.
     * Updates are throttled to reduce battery usage and main thread load.
     */
    fun startCompassUpdates() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        if (accelerometer == null || magnetometer == null) return
        
        compassListener = object : SensorEventListener {
            private var lastUpdateTime = 0L
            private val updateIntervalMs = 100L // Throttle to 10 updates per second (was 60)
            
            override fun onSensorChanged(event: SensorEvent) {
                val currentTime = System.currentTimeMillis()
                
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                    }
                }
                
                // Throttle orientation calculation
                if (currentTime - lastUpdateTime < updateIntervalMs) return
                lastUpdateTime = currentTime
                
                // Calculate orientation
                if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    // Convert from radians to degrees
                    val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    // Normalize to 0-360
                    _compassHeading.value = (azimuth + 360) % 360
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        sensorManager.registerListener(
            compassListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI // Reduced from SENSOR_DELAY_GAME for better battery/performance
        )
        sensorManager.registerListener(
            compassListener,
            magnetometer,
            SensorManager.SENSOR_DELAY_UI
        )
    }
    
    /**
     * Stop compass updates.
     */
    fun stopCompassUpdates() {
        compassListener?.let {
            sensorManager.unregisterListener(it)
        }
        compassListener = null
    }
    
    /**
     * Get distance from current location to a point.
     */
    fun distanceTo(latitude: Double, longitude: Double): Float? {
        val current = _currentLocation.value ?: return null
        val results = FloatArray(1)
        Location.distanceBetween(
            current.latitude, current.longitude,
            latitude, longitude,
            results
        )
        return results[0]
    }
}
