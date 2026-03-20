# Android Photos Implementation Research

## Photo Storage Landscape

### MediaStore API (Recommended Primary Solution)

**What it is:**

- Android's built-in content provider for accessing media files
- Centralized database of all photos/videos on device
- Works across all Android manufacturers (Samsung, Google, OnePlus, etc.)

**Capabilities:**

- Query photos with metadata (location, date, dimensions)
- Access photo thumbnails and full images
- Create and manage albums (API 30+)
- Filter by location coordinates
- Read EXIF data including GPS coordinates
- Works offline

**Permissions:**

- Android 13+ (API 33+): `READ_MEDIA_IMAGES` (granular)
- Android 10-12: `READ_EXTERNAL_STORAGE` (scoped storage)
- For album creation: Write access or Photo Picker

**Pros:**

- ✅ Works on all Android devices
- ✅ No internet required
- ✅ Privacy-friendly (local only)
- ✅ No authentication needed
- ✅ Supports all our use cases
- ✅ Fast querying with proper indexing

**Cons:**

- ❌ Requires runtime permissions
- ❌ Only accesses local photos (not cloud-only Google Photos)
- ⚠️ Album creation APIs limited before Android 11

---

### Photo Picker API (Recommended for Simple Selection)

**What it is:**

- System-provided UI introduced in Android 11 (backported to Android 4.4 via updated Google Play Services)
- Let users select photos without granting broad storage permissions

**Capabilities:**

- Single or multiple photo selection
- Returns content URIs with temporary read access
- No permission manifest needed (system handles it)
- Modern, consistent UI across devices

**Permissions:**

- None required in manifest
- User grants per-photo access via system UI

**Pros:**

- ✅ Best privacy model
- ✅ No permission prompts
- ✅ Google recommended approach
- ✅ Perfect for Feature 1 (select photo for tor)

**Cons:**

- ❌ Cannot query all photos (no bulk scanning)
- ❌ Cannot query by location
- ❌ Cannot create albums
- ❌ Read-only, selection-based only

---

### Google Photos API (NOT Recommended)

**What it is:**

- Cloud service API for accessing photos stored in Google Photos cloud

**Capabilities:**

- Access Google Photos library via REST API
- Search by date, location, content
- Create albums in Google Photos

**Permissions:**

- OAuth 2.0 authentication
- Requires Google account
- User must grant cloud access

**Pros:**

- ✅ Access photos across devices
- ✅ Powerful search capabilities

**Cons:**

- ❌ Requires internet connection
- ❌ Complex OAuth setup
- ❌ Not all users use Google Photos
- ❌ API rate limits and quotas
- ❌ Cannot access local-only photos
- ❌ Overkill for our use cases
- ❌ Additional authentication burden

---

## Recommended Implementation Strategy

### Hybrid Approach: MediaStore + Photo Picker

**For Feature 1: Photos on Tor Detail**

```kotlin
// Use Photo Picker for simple, privacy-friendly selection
val photoPickerLauncher = registerForActivityResult(PickVisualMedia()) { uri ->
    if (uri != null) {
        // Store content URI and associate with tor
        viewModel.addPhotoToTor(torId, uri)
    }
}

// Trigger selection
photoPickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
```

**For Features 2, 3, 5, 6: Use MediaStore**

```kotlin
// Query photos with location data
val projection = arrayOf(
    MediaStore.Images.Media._ID,
    MediaStore.Images.Media.DISPLAY_NAME,
    MediaStore.Images.Media.DATE_TAKEN,
    MediaStore.Images.Media.LATITUDE,
    MediaStore.Images.Media.LONGITUDE
)

val selection = "${MediaStore.Images.Media.LATITUDE} IS NOT NULL"
val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

contentResolver.query(
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    projection,
    selection,
    null,
    sortOrder
)?.use { cursor ->
    // Process photos with GPS coordinates
}
```

---

## MediaStore API Deep Dive

### Use Case Support Matrix

#### ✅ Feature 1: Photos on Tor Detail

**Approach:** Photo Picker for selection, MediaStore for loading

```kotlin
// Selection: Photo Picker (no permission needed)
// Display: Coil/Glide with content URI
// Storage: Store content URI string in Room database
```

**Fallback:** If Photo Picker unavailable (rare), use MediaStore with `READ_MEDIA_IMAGES`

---

#### ✅ Feature 2: Photos Layer on Map

**Approach:** MediaStore query with location filter

```kotlin
// Query photos within Dartmoor bounds
val selection = """
    ${MediaStore.Images.Media.LATITUDE} BETWEEN ? AND ?
    AND ${MediaStore.Images.Media.LONGITUDE} BETWEEN ? AND ?
""".trimIndent()

val selectionArgs = arrayOf(
    "50.35", "50.75",  // Dartmoor latitude range
    "-4.15", "-3.65"   // Dartmoor longitude range
)

// Limit to 200 photos for performance
// Display as purple pins on Google Maps
```

**Permission:** `READ_MEDIA_IMAGES` required

---

#### ✅ Feature 3: Auto-Match Photos to Tors

**Approach:** MediaStore bulk scan with location filtering

```kotlin
// 1. Load all photos with GPS coordinates
val photosWithLocation = queryPhotosWithLocation()

// 2. For each photo, calculate distance to nearest tor
photosWithLocation.forEach { photo ->
    val nearbyTors = findTorsWithinDistance(
        photo.latitude,
        photo.longitude,
        radiusMeters = 100.0
    )

    if (nearbyTors.isNotEmpty()) {
        // Add to suggestion list
    }
}

// 3. Present suggestions to user
```

**Challenge:** Performance with large libraries (10,000+ photos)
**Solution:**

- Process in background coroutine
- Update UI every 100 photos
- Use spatial indexing for tor lookups

**Permission:** `READ_MEDIA_IMAGES` required

---

#### ⚠️ Feature 4: Shared Album Integration

**Challenge:** Android has no equivalent to iCloud Shared Albums
**Options:**

1. **Skip this feature on Android** (simplest)
2. **Use Google Photos shared albums** (complex, requires Google Photos API)
3. **Build custom sharing** (e.g., share album via link/QR code)

**Recommendation:** Skip for MVP, consider custom solution later

---

#### ✅ Feature 5: Dartmoor Tors Album

**Approach:** MediaStore album creation (Android 11+)

```kotlin
// Create album
val albumValues = ContentValues().apply {
    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Dartmoor Tors")
    put(MediaStore.Images.Media.IS_PENDING, 1)
}

// Add photos to album by copying/moving to album directory
// Or use MediaStore.createWriteRequest() on Android 11+
```

**Challenge:** Album APIs limited before Android 11
**Workaround:** Create dedicated folder in Pictures directory

**Permission:**

- Android 11+: User approval via `MediaStore.createWriteRequest()`
- Android 10: `WRITE_EXTERNAL_STORAGE` (deprecated)

---

#### ✅ Feature 6: Location-Based Photo Fallback

**Approach:** MediaStore query by proximity

```kotlin
fun findPhotoNearTor(torLat: Double, torLon: Double, radiusMeters: Double): Uri? {
    val photos = queryPhotosWithLocation()

    return photos
        .filter { photo ->
            calculateDistance(torLat, torLon, photo.latitude, photo.longitude) <= radiusMeters
        }
        .minByOrNull { photo ->
            calculateDistance(torLat, torLon, photo.latitude, photo.longitude)
        }
        ?.uri
}
```

**Permission:** `READ_MEDIA_IMAGES` required

---

## Implementation Checklist

### Phase 1: Basic Photo Selection (Feature 1)

- [ ] Add Photo Picker dependency to `build.gradle.kts`
- [ ] Implement photo selection in TorDetailBottomSheet
- [ ] Store content URI in `VisitedTor` entity
- [ ] Display photo using Coil image loader
- [ ] Handle permission fallback to MediaStore if needed

### Phase 2: MediaStore Integration (Features 2, 3, 6)

- [ ] Add `READ_MEDIA_IMAGES` permission to manifest
- [ ] Create `PhotoService` with MediaStore queries
- [ ] Implement location-based photo queries
- [ ] Add photos layer toggle to map
- [ ] Build auto-match scanning flow
- [ ] Implement location-based fallback

### Phase 3: Album Management (Feature 5)

- [ ] Implement album creation for Android 11+
- [ ] Track album ID in SharedPreferences
- [ ] Auto-add photos to album when associated with tors
- [ ] Handle Android 10 compatibility

### Phase 4: Polish

- [ ] Add loading states and error handling
- [ ] Optimize large library scanning
- [ ] Cache photo thumbnails
- [ ] Add analytics events

---

## Code Architecture

```
app/src/main/java/com/dartmoortors/
├── data/
│   ├── model/
│   │   ├── Photo.kt              # Photo data class
│   │   └── PhotoMatch.kt         # Auto-match result
│   ├── local/
│   │   └── PhotoDao.kt           # If caching photo metadata
│   └── repository/
│       └── PhotoRepository.kt    # Photo data operations
├── service/
│   └── PhotoService.kt           # MediaStore operations
├── ui/
│   ├── photos/
│   │   ├── PhotosScreen.kt       # Photos tab composable
│   │   ├── PhotosViewModel.kt    # Photos logic
│   │   └── AutoMatchScreen.kt    # Auto-match flow
│   └── detail/
│       └── TorDetailSheet.kt     # Photo hero section
└── util/
    └── PermissionHelper.kt       # Permission handling
```

---

## Dependencies to Add

```kotlin
// build.gradle.kts (app level)
dependencies {
    // Photo Picker (backport support)
    implementation("androidx.activity:activity-compose:1.9.0")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Permissions handling
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Location utilities
    implementation("com.google.android.gms:play-services-location:21.2.0")
}
```

---

## Permission Handling

### Manifest Declaration

```xml
<!-- AndroidManifest.xml -->
<manifest>
    <!-- Modern photo access (Android 13+) -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <!-- Legacy photo access (Android 10-12) -->
    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <!-- For reading photo location (optional) -->
    <uses-permission android:name="android.permission.ACCESS_MEDIA_LOCATION" />
</manifest>
```

### Runtime Request

```kotlin
@Composable
fun RequestPhotoPermission(
    onPermissionGranted: () -> Unit
) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionState = rememberPermissionState(permission) { granted ->
        if (granted) onPermissionGranted()
    }

    LaunchedEffect(Unit) {
        if (!permissionState.hasPermission) {
            permissionState.launchPermissionRequest()
        }
    }
}
```

---

## Performance Considerations

### Large Library Scanning

- Use coroutines with `Dispatchers.IO`
- Process photos in batches (100-500 at a time)
- Update UI periodically, not per-photo
- Consider caching photo metadata in Room

### Map Performance

- Limit to 200 photos on map
- Use clustering for dense areas
- Load thumbnails, not full images
- Debounce map region changes

### Memory Management

- Use Coil's built-in caching
- Load thumbnails via `MediaStore.Images.Thumbnails`
- Don't hold full-res images in memory
- Clear caches when leaving photos features

---

## Testing Strategy

### Unit Tests

- Photo distance calculation
- Dartmoor bounds filtering
- URI persistence and retrieval

### Integration Tests

- MediaStore querying (use test images)
- Photo Picker integration
- Permission handling flows

### Manual Testing

- Test on various Android versions (10, 11, 13, 14)
- Test with large photo libraries (1000+, 10000+ photos)
- Test with photos missing GPS data
- Test with denied permissions

---

## References

- [MediaStore API Documentation](https://developer.android.com/reference/android/provider/MediaStore)
- [Photo Picker Guide](https://developer.android.com/training/data-storage/shared/photopicker)
- [Photo Picker Jetpack Compose](https://developer.android.com/training/data-storage/shared/photopicker#jetpack-compose)
- [Scoped Storage Guide](https://developer.android.com/training/data-storage)
- [ACCESS_MEDIA_LOCATION Permission](https://developer.android.com/training/data-storage/shared/media#media-location-permission)

---

## Decision: MediaStore + Photo Picker

**Primary API:** MediaStore for all querying, scanning, and album features
**Secondary API:** Photo Picker for simple photo selection (Feature 1)
**Google Photos API:** Not needed for our use cases

This approach:

- ✅ Supports all features in the spec
- ✅ Works on all Android devices
- ✅ Maintains user privacy
- ✅ Works offline
- ✅ Requires minimal external dependencies
- ✅ Follows modern Android best practices
