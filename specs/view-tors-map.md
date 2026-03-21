# Tors Map

## Overview

Native map view following platform map guidelines. This is the primary view of the app.

**[iOS]** Follows Apple's [Maps HIG pattern](https://developer.apple.com/design/human-interface-guidelines/maps).
**[Android]** Should follow [Google Maps SDK patterns](https://developers.google.com/maps/documentation/android-sdk/overview).

## Requirements

### Map Markers
Icons showing the locations of tors on a map. 
Each icon:
- Is selectable to open place card
- Shows visited and access state via colour:
  - **Green**: Visited
  - **Teal/Blue**: Accessible (unvisited) - includes Public, Public part-private, Private but accessible, Private with fee
  - **Orange**: Not accessible (unvisited) - Private land requiring permission or with no access

Future: We may have two types of icon, one zoomed out with less info so we can show multiple tors without overlapping, one when zoomed in that shows more information (e.g. user photo, altitude).

### Map Base Layers

**[iOS]** Apple MapKit:
- Standard (road map)
- Satellite
- Hybrid

**[Android]** Google Maps SDK:
- Normal (road map)
- Satellite
- Hybrid
- Terrain

Future: OS Maps (https://osdatahub.os.uk/plans), OpenStreetMap

### Photos Layer
Toggle to show photos from user's library as purple pins on the map:
- Photos with GPS coordinates within Dartmoor bounds appear as markers
- Tapping a photo pin shows preview with nearby tors (within 100m)
- User can associate photo with a tor from the preview

## UI / Layout

Full screen map.
Tab bar navigation along bottom (always visible).
Toolbar buttons for location and map style in navigation bar.

### Place Card Sheet

When a tor is selected (from map marker or from search tab):
- Appears as a sheet with detents: compact (220pt), medium, large
- Contains: tor details, mark visited button, directions, links, photo
- Background interaction enabled through medium detent (map remains interactive)
- Dismissing returns to map
- **Map Positioning**: When navigating to a tor from search, the tor is positioned 1/4 down from the top of the screen, centering it in the visible map area above the card

**[Platform-specific]** Sheet behaviour:
- **[iOS]** Uses native sheet presentation with detents
- **[Android]** Use BottomSheetBehavior with similar peek heights

### My Location Button
- Button to pan/zoom map to user's current location
- Uses standard location arrow icon
  - **[iOS]** `location.fill` SF Symbol
  - **[Android]** `my_location` Material icon
- Provides visual feedback when tapped
- Requests location permission on first tap (see [user-location.md](user-location.md))
- Shows standard blue dot for user location when enabled
- User is free to pan the map after centering

### Compass Tracking Button
- Only visible when location permission is granted
- Positioned below the My Location button
- Uses compass icon
  - **[iOS]** `compass.drawing` SF Symbol
  - **[Android]** `explore` Material icon
- Icon turns blue/highlighted when tracking is active
- Behaviour:
  - First tap: Enables heading tracking - map orientation follows phone compass
  - Second tap: Disables tracking and resets map to north orientation
  - If user pans or zooms while tracking, tracking is automatically disabled

### Compass Line of Sight Button
- Only visible when location permission is granted
- Positioned above the My Location button in the bottom-right controls
- Uses directional icon
  - **[Android]** `near_me` Material icon
- Button turns red/highlighted when line is active
- Behaviour:
  - Tap: Toggles visibility of a compass line on the map
  - When active, draws a red line from the user's location in the direction the device's compass is pointing
  - Line length scales with zoom level:
    - Closer zoom (15): ~500m line
    - Default zoom (11): ~5km line
    - Farther zoom (8): ~20km line
  - Line updates in real-time as the user rotates their device
- Use case: Helps users identify a distant tor by aligning their phone toward it and seeing what tors the line passes through

### Map Compass
- Built-in map compass control
- Automatically appears when map is rotated away from north
- Tapping it resets map to north orientation

**[iOS]** Built-in MapKit compass control.
**[Android]** Google Maps SDK provides this automatically.

## Notes

**[iOS]** Follows best practice: https://developer.apple.com/design/human-interface-guidelines/maps
**[Android]** Follow Material Design guidelines for maps.
