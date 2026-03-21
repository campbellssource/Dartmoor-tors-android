# Photos Feature

This specification covers the integration of the device photo library with Dartmoor Tors, allowing users to associate photos with tors they've visited.

## Overview

Users can associate photos from their photo library with tors they've visited. This creates a personal visual record of their Dartmoor adventures and helps identify previously visited tors.

## Technical Approach

**[Platform-specific]** Photo library access:

**[iOS]**

- Uses PhotoKit (`PHPhotoLibrary.shared()`) with `PhotosPicker`
- Stores `PHAsset.localIdentifier` in the VisitedTor model
- Cross-device sync via `PHCloudIdentifier` (requires iCloud Photos)
- Requires `NSPhotoLibraryUsageDescription` in Info.plist

**[Android]**

- Uses Android Photo Picker API for user-initiated photo selection
- Store content URI in VisitedTor model
- **Limitation**: Google Photos API removed library scanning scopes in March 2025
- Features requiring GPS metadata scanning (Photos Layer, Auto-Match) not available
- Requires no permissions for Photo Picker (user-initiated selection only)

**Both platforms:**

- In-memory caching for loaded images
- Lightweight reference storage (no image duplication)

## Feature 1: Photos on Tor Detail

**Status**: Implemented

When viewing a visited tor's place card, users can associate a photo with that tor.

### User Flow

1. User marks a tor as visited (or views an already-visited tor)
2. User sees "Add Photo" prompt in the hero area of the place card
3. User taps to open photo picker
4. System requests photo library access (if not already granted)
5. User selects a photo
6. Photo displays as a hero image at the top of the place card

### UI Elements

- **Hero Photo Area** (top of TorPlaceCard, 200pt height)
  - Shows photo if one exists (with change/remove overlay buttons)
  - Shows "Add Photo" prompt if no photo
  - Shows loading indicator while fetching

- **Map Annotation**
  - Tors with photos show a small photo icon inside the marker
  - Slightly larger marker size for tors with photos

### Data Model

```
VisitedTor:
  - torId: String
  - visitedDate: Date
  - photoAssetIdentifier: String?  // Platform-specific photo reference
  - photoCloudIdentifier: String?  // For cross-device sync
  - checklist: Checklist

  - hasPhoto: Boolean (computed from photoAssetIdentifier != null)
```

### Edge Cases

- Photo deleted from library: Gracefully show "Add Photo" prompt (asset fetch returns nil)
- Cloud photo not downloaded: Allow network access for download
- Permission denied: Photo picker may still work but asset fetch will fail; user sees "Add Photo" prompt

---

## Feature 2: Photos Layer on Map

**Status**: Implemented

Show photos from the user's library as a map layer, allowing them to discover tors they may have visited based on photo locations.

### User Flow

1. User opens map and selects "Show Photos" from map style menu
2. Photos with GPS coordinates within Dartmoor bounds appear as purple pins on the map
3. User taps a photo pin to see a preview of the photo
4. Preview shows nearby tors (within 100m) that the photo can be associated with
5. User taps a tor to associate the photo with it

### UI Elements

- **Map Style Menu**: "Show Photos" toggle added to map style picker
- **Photo Pins**: Purple circles with photo icon, distinct from tor pins
- **Photo Preview Sheet**: Shows photo, date, and list of nearby tors to associate with

### Technical Implementation

- Photos filtered to Dartmoor bounds (lat 50.35-50.75, lon -4.15 to -3.65)
- Limited to 200 photos for map performance
- Photos sorted by creation date (newest first)
- Nearby tors calculated within 100m radius
- Visited tors shown with green checkmark in association list

---

## Feature 3: Auto-Match Photos to Tors

**Status**: Implemented

Scan the user's photo library to find photos taken near tors and suggest associations.

### User Flow

1. User navigates to Photos tab
2. User taps "Scan Photo Library" button
3. App scans library for photos with GPS coordinates near any tor (within 100m)
4. Progress indicator shows scanning progress (X of Y photos scanned)
5. Results show tors with nearby photos in a review flow
6. User reviews each suggestion and can Add Photo or Skip
7. For tors with multiple nearby photos, user can swipe through to pick the best one

### UI Elements

- **Photos Tab - Auto-Match Section**: Explains feature with "Scan Photo Library" button
- **Ready View**: Explains the feature with Start Scan button
- **Progress View**: Shows scanning progress with percentage bar and match count
- **Review Flow**: Carousel of photos for each tor with Add Photo/Skip buttons
- **Complete View**: Summary of photos added

### Technical Implementation

- Distance threshold: 100m from tor location
- Excludes tors that already have photos associated
- Photos sorted by distance (closest first) within each tor match
- Results sorted by number of photos found (most photos first)
- Progress updates every 100 photos scanned

### Visit Date Behavior

When adding a photo to a tor via auto-match:

- **Unvisited tor**: Automatically marks as visited using the photo's creation date
- **Already visited tor**: Adds photo; if photo date is earlier than existing visit date, updates visit date to match
- This allows users to backfill their visit history from old photos

---

## Feature 4: Shared Album Integration ("Bag Tors Together")

**Status**: Implemented (iOS only)

Link an existing shared album to track which tor photos are shared with others. This section only appears after the user has created their Dartmoor Tors album.

**[iOS]** Uses iCloud Shared Albums
**[Android]** Could use Google Photos shared albums (API limitations may apply)

### User Flow

1. User creates Dartmoor Tors album (Feature 5)
2. "Bag Tors Together" section appears in Photos tab
3. User taps "Link a Shared Album"
4. User selects an existing shared album from their library
5. On tor photos, user sees shared album status badges
6. User can view which photos are missing from the shared album

### UI Elements

- **Photos Tab - "Bag Tors Together" Section** (only visible when Dartmoor Tors album exists):
  - Intro text explaining the limitation (app can't add to shared albums)
  - "Link a Shared Album" button (when no album linked)
  - Album name with "Change"/"Unlink" buttons (when album linked)
  - "Show Missing Photos" button with count

- **Missing Photos View**:
  - Grid of photos in Dartmoor Tors album but not in shared album
  - Shows photo date to help user find it in Photos app
  - Loading indicator while fetching

- **Shared Album Picker Sheet**:
  - List of user's shared albums with photo counts
  - Shows checkmark for currently linked album

- **TorPlaceCard - Hero Photo Thumbnail**:
  - Bottom-left badge: Dartmoor Tors album status (green check or blue plus)
  - Bottom-right badge: Shared album status (green person.2 or orange slash)
  - Tapping opens full-screen photo viewer

### Limitations

- User must create the shared album in Photos app first
- Cannot create shared albums programmatically
- Cannot programmatically add photos to shared albums (platform restriction)
- Album participants managed in Photos app, not this app
- User must manually add photos to shared album via Photos app

---

## Feature 5: Dartmoor Tors Album

**Status**: Implemented

Create a dedicated "Dartmoor Tors" album in the device's photo library that automatically collects all photos associated with tors.

### User Flow

1. User navigates to Photos tab
2. User taps "Create Album" in the Dartmoor Tors Album section
3. App creates a "Dartmoor Tors" album in the user's photo library
4. When associating a photo with a tor (via any method), the photo is automatically added to this album
5. Album collects all tor photos in one place for easy sharing
6. When removing a photo from a tor, it is also removed from this album
7. User can view all photos in the album via "View Album" button

### UI Elements

- **Photos Tab - Dartmoor Tors Album Section**:
  - "Create Album" button (when album not yet created)
  - Album status indicator with checkmark (when album exists)
  - Photo count
  - "View Album" button to see all photos in-app
  - Creating this album unlocks the "Bag Tors Together" section

- **Album Photos View** (in-app viewer):
  - Grid layout of all photos in the album
  - Sorted by date (newest first)
  - Loading indicator while fetching

### Technical Implementation

**[iOS]**

- Album created using `PHAssetCollectionChangeRequest.creationRequestForAssetCollection`
- Photos added via `PHAssetCollectionChangeRequest.addAssets()`
- Album identifier stored in UserDefaults for persistence

**[Android]**

- Create album via MediaStore
- Track album ID in SharedPreferences
- Add photos to album via MediaStore insert

### Advantages Over Shared Albums

- Can add photos programmatically (shared albums cannot be modified via API)
- User can manually share the album with others after creation
- Photos collected automatically without user action

---

## Feature 6: Location-Based Photo Fallback

**Status**: Implemented

If a stored photo reference fails to load (e.g., due to identifier changes between app restarts), the app attempts to find a matching photo by location.

### Technical Implementation

- When loading a photo fails via identifier, falls back to location search
- Searches for photos within 50m of the tor's coordinates
- Returns the closest matching photo if found
- Provides resilience against photo identifier changes

---

## Files (iOS Implementation)

| File                                     | Purpose                                                                           |
| ---------------------------------------- | --------------------------------------------------------------------------------- |
| `Services/PhotoService.swift`            | Image loading, caching, authorization, album management                           |
| `Models/VisitedTor.swift`                | Contains `photoAssetIdentifier` property                                          |
| `Views/PhotosView.swift`                 | Photos tab - album setup, shared album linking, auto-match scanning, album viewer |
| `Views/Components/TorPlaceCard.swift`    | Hero photo thumbnail with album badges                                            |
| `Views/Components/PhotoViewerView.swift` | Full-screen photo viewer with zoom/pan and album actions                          |
| `Views/Components/FindPhotosView.swift`  | Auto-match scanning flow                                                          |
| `Views/TorsMapView.swift`                | Photo indicators on map annotations, photos layer                                 |
| `Info.plist`                             | `NSPhotoLibraryUsageDescription`                                                  |

---

## Implementation Status

| Feature                         | iOS            | Android                                  |
| ------------------------------- | -------------- | ---------------------------------------- |
| Feature 1: Photos on Tor Detail | ✅ Implemented | ✅ Implemented (Photo Picker)            |
| Feature 2: Photos Layer on Map  | ✅ Implemented | ✅ Implemented (local photos only)       |
| Feature 3: Auto-Match Photos    | ✅ Implemented | ✅ Implemented (local photos only)       |
| Feature 4: Shared Album         | ✅ Implemented | ❌ Not possible (API removed March 2025) |
| Feature 5: Dartmoor Tors Album  | ✅ Implemented | ❌ Not implemented                       |
| Feature 6: Location Fallback    | ✅ Implemented | ✅ Implemented (local photos only)       |

### Android Notes

The photo features on Android work with **locally-stored photos only**. Photos that are backed up to Google Photos but not downloaded to the device will not appear on the map layer.

This is because Android's MediaStore API only has access to photos physically stored on the device. Cloud-only photos (synced via Google Photos) are not accessible programmatically.

To use the photo map layer effectively, users should:

- Take photos directly with their device camera (stored locally by default)
- Or download specific photos from Google Photos to local storage
