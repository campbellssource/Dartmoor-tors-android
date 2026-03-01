# User Location

## Overview

The app uses the device's location to provide proximity-based features. Location access is optional and requested only when the user initiates a location-dependent action.

## Requirements

### Location Permission

- Location access uses the standard platform location permission dialog
- Permission is requested on-demand when the user first triggers a location feature:
  - Tapping "My Location" button on the map
  - Selecting "Distance from me" sort option in the list
- Use "When In Use" / "While Using" authorization (not "Always")
- If permission is denied, show an appropriate message explaining how to enable it in Settings

**[iOS]** Uses Core Location with `CLLocationManager`
**[Android]** Uses Fused Location Provider with `ACCESS_FINE_LOCATION` permission

### Map: My Location Button

- A button in the map UI that pans/zooms the map to center on the user's current location
- Button should use a standard location arrow icon
  - **[iOS]** `location.fill` SF Symbol
  - **[Android]** `my_location` Material icon
- Tapping the button:
  1. Requests location permission if not already granted
  2. Once permission granted, animates the map to the user's location
  3. Shows the standard blue dot for user location on the map
- Button placement: Consistent with platform map conventions
  - **[iOS]** Bottom-left or top-right corner following Apple HIG
  - **[Android]** Bottom-right following Google Maps conventions

### List: Sort by Distance

- Add "Distance from me" as a sort option
- When selected:
  1. Requests location permission if not already granted
  2. Once permission granted, sorts tors by distance from user (nearest first)
  3. Shows distance value in the list row (e.g., "2.3 km")
- If location unavailable, fall back to default sort or show message

## UI / Layout

Map button:
- Use system styling consistent with other map controls
- Consider grouping with other map controls if present

List sort:
- Integrate with existing sort picker/menu

## Notes

- Location should work offline (GPS doesn't require network)
- Consider caching last known location for quick initial display
- **[iOS]** Follow Apple best practices: https://developer.apple.com/design/human-interface-guidelines/accessing-private-data
- **[Android]** Follow Android location best practices: https://developer.android.com/develop/sensors-and-location/location
