# To Do

List of features and views defined in specifications and their current status.

```
Status values:
  Spec Incomplete  — spec exists but is not fully defined
  Not Implemented  — spec is complete, not yet built
  Implemented      — built but not tested
  Tested           — built and tested
  Disabled         — was implemented but currently disabled
```

---

## Views

- [Tors Map](view-tors-map.md) Status: `Implemented`
    - Map layer switching (satellite / road / hybrid) Status: `Implemented`
    - Tor icons showing visited state Status: `Implemented`
    - Tor icons coloured by access (teal=accessible, orange=not accessible, green=visited) Status: `Implemented`
    - My location button Status: `Implemented`
    - Compass/heading button Status: `Implemented`
    - Filters apply to map markers Status: `Implemented`
    - Photos layer (show photos as purple pins) Status: `Implemented`
    - Map positioning (tor 1/4 down when selected from search) Status: `Implemented`
    - Zoomed in/out icon variants Status: `Spec Incomplete`
    - Future: Google Maps layer
    - Future: OS Maps layer
    - Future: OpenStreetMap layer

- Search List (SearchListView) Status: `Implemented`
    - Integrated search bar Status: `Implemented`
    - Show tor name, visited state, access Status: `Implemented`
    - Not accessible tors shown in orange Status: `Implemented`
    - Sort by height (highest/lowest) Status: `Implemented`
    - Sort alphabetically (A-Z, Z-A) Status: `Implemented`
    - Sort by proximity (requires location) Status: `Implemented`
    - Sort directionally (N-S, S-N, E-W, W-E) Status: `Implemented`
    - Show distance when location available Status: `Implemented`
    - Filters from Collection apply to list Status: `Implemented`
    - Future: Filter by categorisation (has "tor", set)

- [Tor Detail](view-tor-detail.md) / Place Card Status: `Implemented`
    - Name, height, OS grid ref, lat/long, access, external links Status: `Implemented`
    - Not accessible shown in orange Status: `Implemented`
    - Back navigation to map or list Status: `Implemented`
    - Show on Map button Status: `Implemented`
    - Offline availability Status: `Implemented`
    - Editable visited date Status: `Implemented`
    - Open in Apple Maps Status: `Implemented`
    - Open in Google Maps (app or web fallback) Status: `Implemented`
    - Hero photo with album badges Status: `Implemented`
    - Full-screen photo viewer with zoom/pan Status: `Implemented`
    - Future: Open in OS Maps, What3Words, OSM

- [Collection](view-tor-types.md) Status: `Implemented`
    - Classification filters (Summit, Summit Avenue, Valley Side, etc.) Status: `Implemented`
    - Accessible Only filter Status: `Implemented`
    - Progress section (visited count) Status: `Implemented`
    - Tors of Dartmoor Collection credits (Tim, Paul, Max) Status: `Implemented`
    - iCloud sync status display Status: `Implemented`
    - Share progress via ShareLink Status: `Implemented`
    - Link to Tors of Dartmoor website Status: `Implemented`
    - Share checklist via CKShare (real-time) Status: `Not Implemented`
    - View shared checklists Status: `Not Implemented`
    - App info and credits Status: `Implemented`
    - Future: Export data

---

## Features

- [Navigation](navigation.md) Status: `Implemented`
    - Tab bar with Map, Collection, Photos, and Search Status: `Implemented`
    - **[iOS]** Uses iOS 26+ Tab API with search role
    - **[Android]** Needs Material Design bottom navigation

- [Tor Data](tor-data.md) Status: `Implemented`
    - Load tor list from JSON into app data model Status: `Implemented`
    - 900+ tors with full metadata Status: `Implemented`
    - Wikipedia URLs for 39 tors Status: `Implemented`

- [User Data](user-data.md) Status: `Implemented`
    - Track visited tors Status: `Implemented`
    - **[iOS]** SwiftData + CloudKit sync Status: `Implemented`
    - **[Android]** Room + cloud sync Status: `Not Implemented`
    - Share progress via ShareLink Status: `Implemented`
    - Share checklist via CKShare (real-time) Status: `Not Implemented`
    - View shared checklists Status: `Not Implemented`
    - Future: Export visited list

- [User Location](user-location.md) Status: `Implemented`
    - Request location permission on-demand Status: `Implemented`
    - My location button on map Status: `Implemented`
    - Sort by distance in list Status: `Implemented`
    - Show distance in list rows Status: `Implemented`

- [Photos](photos.md) Status: `Implemented`
    - Feature 1: Photos on Tor Detail Status: `Implemented`
    - Feature 2: Photos Layer on Map Status: `Implemented`
    - Feature 3: Auto-Match Photos to Tors Status: `Implemented`
    - Feature 4: Shared Album Integration (read-only) Status: `Implemented`
    - Feature 5: Dartmoor Tors Album Status: `Implemented`
    - Feature 6: Location-Based Photo Fallback Status: `Implemented`
    - Feature 7: In-app Album Viewer Status: `Implemented`
    - Feature 8: Missing Photos from Shared Album View Status: `Implemented`

---

## Platform Status

| Feature | iOS | Android |
|---------|-----|---------|
| Map View | ✅ | Not started |
| Search/List | ✅ | Not started |
| Tor Detail | ✅ | Not started |
| Collection/Filters | ✅ | Not started |
| Navigation | ✅ | Not started |
| Tor Data Loading | ✅ | Not started |
| Local Storage | ✅ SwiftData | Not started |
| Cloud Sync | ✅ CloudKit | Not started |
| Photos Integration | ✅ PhotoKit | Not started |
| Location Services | ✅ Core Location | Not started |

---

## Future Features (not yet specced)

- Future: Tor sets (e.g. OS maps, Tors+, Compendium)
- Future: OS Maps layer integration

---

## Backlog

Priority items to address in future development cycles:

### High Priority
- OS Maps layer integration
- Android app development

### Tests
- Add UI tests for user journeys 1, 5, 6, 8 (from tests.md)
- Add unit tests for ChecklistService
- Add unit tests for TorFilterState

### Performance
- PhotoService.scanForPhotosNearTors: Move to background thread (O(photos × tors) complexity)
- TorDataService: Use dictionary lookup for tor(withId:) - currently O(n)
- TorDataService: Consider async loading to avoid blocking app launch
- PhotoService: Add cache eviction policy (currently unbounded)
- TorsMapView: Consider annotation clustering for 900+ markers

### Platform Support
- macOS support (PhotoService returns nil, platform-specific UI branches exist)
- Android app (separate codebase, shared spec)

### Code Quality
- Migrate TorDataService from ObservableObject to @Observable
- Remove debug print statements from PhotoService
