# Tor Detail

## Overview

This page shows all the details we have on the selected Tor, including whether the user has visited it.

## Requirements

- Shows Tor name as a title
- Shows height in meters
- Shows OS Grid reference
- Shows Longitude and Latitude
- Shows access (public or private - private shown in orange text for consistency with map/list)
- Shows link to Tors of Dartmoor page
- Shows link to Wikipedia page (if there is one)
- Shows back link to either map or list view (depending on where you've come from)
- Shows "Show on Map" button that navigates to the Map tab and centres on this tor
- Shows visited state with toggle to mark/unmark as visited
- When visited, shows editable date picker to change the visited date
- Shows photo if one has been associated with this tor (see Photos section below)
- Shows "Open in Maps" section with:
    - Apple Maps - opens in native Maps app **[iOS]**
    - Google Maps - opens in Google Maps app if installed, otherwise opens in browser
    - OS Maps - opens OS Maps app or web **[Future]**

## Photos

When a tor has been visited, users can associate a photo:

### Hero Photo Area (top of detail view, 200pt height)
- Shows photo if one exists (with change/remove overlay buttons)
- Shows "Add Photo" prompt if no photo
- Shows loading indicator while fetching

### Photo Badges
When a photo is displayed, badges indicate album status:
- **Bottom-left**: Dartmoor Tors album status (green check if in album, blue plus if not)
- **Bottom-right**: Shared album status (green person icon if shared, orange slash if not)

### Photo Viewer
Tapping the photo opens a full-screen viewer with:
- Pinch-to-zoom and pan gestures
- Double-tap to reset zoom
- Album status badges with action buttons
- Photo date taken
- Remove button (removes from tor AND from Dartmoor Tors album)

**[Platform-specific]** Photo library access:
- **[iOS]** Uses PhotoKit (PHPhotoLibrary, PHAsset)
- **[Android]** Use MediaStore or Photo Picker API

## UI / Layout

- Tor name as Title (never truncated), should use native font styles of a title on a page
- Hero photo area at top (when visited)
- Details section with all tor information
- Actions section with visit controls and navigation options

## Notes

- Tor details should be available when offline
- Photos are stored as references to the device's photo library, not duplicated

Link to OS Map: https://explore.osmaps.com/pin?pinCoordinates=50.4304919847431,-3.876843456348439
