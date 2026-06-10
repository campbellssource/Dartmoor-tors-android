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

    // Compass/magnetometer accuracy, so the UI can prompt for calibration when the
    // heading can't be trusted (T4-11).
    private val _compassAccuracy = MutableStateFlow(CompassAccuracy.UNKNOWN)
    val compassAccuracy: StateFlow<CompassAccuracy> = _compassAccuracy.asStateFlow()

    private var locationCallback: LocationCallback? = null
    private var compassListener: SensorEventListener? = null

    // Hysteresis state for the flat<->upright handoff so it can't flip-flop at the boundary.
    private var holdingUpright = false
    
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val remappedRotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Above this pitch (radians from flat) the phone is treated as held upright/aiming,
    // and we remap axes so heading tracks where it points rather than a gimbal-locked
    // azimuth that drifts with tilt (T4-11). ~45 degrees.
    private val uprightPitchThreshold = Math.PI / 4.0
    private val POSTURE_HYSTERESIS = Math.toRadians(8.0)

    // Circular exponential-moving-average state for damping compass jitter (T4-11).
    // We smooth the heading's sin/cos components so the 0/360 wraparound is handled
    // correctly. Lower alpha = smoother but laggier.
    private val headingSmoothingAlpha = 0.15f
    private var smoothedHeadingSin = 0f
    private var smoothedHeadingCos = 0f
    private var headingInitialised = false

    // Throttle sensor-driven heading updates (~20 Hz); smoothing handles the rest.
    private val compassUpdateIntervalMs = 50L
    
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
        // Reset transient state so the line doesn't swing from a stale heading on (re)start.
        headingInitialised = false
        holdingUpright = false
        _compassAccuracy.value = CompassAccuracy.UNKNOWN

        // Prefer the fused rotation-vector sensor (gyroscope + accelerometer + magnetometer).
        // It is far more stable than the raw accelerometer + magnetometer combination, which
        // is the main cause of the jittery bearing line (T4-11).
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVector != null) {
            compassListener = object : SensorEventListener {
                private var lastUpdateTime = 0L
                override fun onSensorChanged(event: SensorEvent) {
                    applyAccuracy(event.accuracy)

                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTime < compassUpdateIntervalMs) return
                    lastUpdateTime = now

                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    publishHeading(headingFromRotationMatrix())
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = applyAccuracy(accuracy)
            }
            sensorManager.registerListener(compassListener, rotationVector, SensorManager.SENSOR_DELAY_UI)
            return
        }

        // Fallback: accelerometer + magnetometer for devices without a rotation-vector sensor.
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (accelerometer == null || magnetometer == null) {
            // No way to determine north on this device.
            _compassAccuracy.value = CompassAccuracy.UNKNOWN
            return
        }

        compassListener = object : SensorEventListener {
            private var lastUpdateTime = 0L

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER ->
                        System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                        applyAccuracy(event.accuracy) // magnetometer accuracy is the calibration signal
                    }
                }

                val now = System.currentTimeMillis()
                if (now - lastUpdateTime < compassUpdateIntervalMs) return
                lastUpdateTime = now

                if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
                    publishHeading(headingFromRotationMatrix())
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) applyAccuracy(accuracy)
            }
        }

        sensorManager.registerListener(compassListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(compassListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    /** True if this device has the sensors needed to determine a compass heading. */
    fun hasCompass(): Boolean =
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null ||
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null

    /** Map a raw SensorManager accuracy constant to our enum and publish it. */
    private fun applyAccuracy(accuracy: Int) {
        _compassAccuracy.value = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
            else -> CompassAccuracy.UNRELIABLE // SENSOR_STATUS_UNRELIABLE / NO_CONTACT
        }
    }

    /**
     * Derive a compass heading (degrees) from [rotationMatrix], choosing the axis
     * convention based on how the phone is held (T4-11):
     *  - Flat (screen up): the standard azimuth = where the top edge points.
     *  - Upright (aiming at a tor): remap so heading tracks where the device points,
     *    instead of a gimbal-locked azimuth that swings with tilt.
     *
     * The flat<->upright switch uses hysteresis so it can't flip-flop at the boundary.
     */
    private fun headingFromRotationMatrix(): Float {
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val pitchAbs = Math.abs(orientationAngles[1].toDouble()) // radians; ~0 flat, ~PI/2 upright

        holdingUpright = if (holdingUpright) {
            pitchAbs > uprightPitchThreshold - POSTURE_HYSTERESIS
        } else {
            pitchAbs > uprightPitchThreshold + POSTURE_HYSTERESIS
        }

        val azimuthRadians = if (!holdingUpright) {
            orientationAngles[0]
        } else {
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remappedRotationMatrix
            )
            SensorManager.getOrientation(remappedRotationMatrix, orientationAngles)
            orientationAngles[0]
        }
        return Math.toDegrees(azimuthRadians.toDouble()).toFloat()
    }

    /**
     * Apply circular EMA smoothing to a raw azimuth (degrees) and publish the damped heading.
     */
    private fun publishHeading(rawAzimuthDegrees: Float) {
        val radians = Math.toRadians(rawAzimuthDegrees.toDouble())
        val sin = Math.sin(radians).toFloat()
        val cos = Math.cos(radians).toFloat()

        if (!headingInitialised) {
            smoothedHeadingSin = sin
            smoothedHeadingCos = cos
            headingInitialised = true
        } else {
            smoothedHeadingSin += headingSmoothingAlpha * (sin - smoothedHeadingSin)
            smoothedHeadingCos += headingSmoothingAlpha * (cos - smoothedHeadingCos)
        }

        val smoothed = Math.toDegrees(
            Math.atan2(smoothedHeadingSin.toDouble(), smoothedHeadingCos.toDouble())
        ).toFloat()
        _compassHeading.value = (smoothed + 360) % 360
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

/**
 * Reported trustworthiness of the device compass (T4-11). [UNRELIABLE] and [LOW]
 * mean the heading should not be trusted until the magnetometer is calibrated
 * (wave the phone in a figure-8).
 */
enum class CompassAccuracy {
    UNKNOWN,
    UNRELIABLE,
    LOW,
    MEDIUM,
    HIGH;

    /** Whether the UI should prompt the user to calibrate the compass. */
    val needsCalibration: Boolean
        get() = this == UNRELIABLE || this == LOW
}
