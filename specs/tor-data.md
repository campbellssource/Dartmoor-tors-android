# Tor Data

## Overview

The app comes bundled with a comprehensive database of Dartmoor tors sourced from Tors of Dartmoor (https://www.torsofdartmoor.co.uk).

## Data Source

- App format: JSON (`tors.json` )
- Total tors: 900+
- Data includes coordinates, height, classification, access status, and links

## Tor Model

Each tor has the following properties:

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Unique identifier |
| `name` | String | Display name of the tor |
| `latitude` | Double | GPS latitude coordinate |
| `longitude` | Double | GPS longitude coordinate |
| `height` | Int | Height in meters |
| `gridRef` | String | OS Grid Reference |
| `classification` | Enum | Type of tor (Summit, Valley Side, etc.) |
| `access` | Enum | Access status (Public, Private, etc.) |
| `torOfDartmoorURL` | String? | Link to Tors of Dartmoor page |
| `wikipediaURL` | String? | Link to Wikipedia article (if exists) |

## Classifications

From torsofdartmoor.co.uk:
- Summit
- Summit Avenue
- Valley Side
- Spur
- Emergent
- Small
- Ruined
- Clitter
- Gorge
- Gully
- Artificial
- Boulder
- Glacial Remains

## Access Levels

**Accessible (shown in teal/blue):**
- Public
- Public (part private)
- Private (but accessible)
- Private (fee required)

**Not Accessible (shown in orange):**
- Private (visible only)
- Private (seek permission)
- Private (no access)

## Data Loading

**[iOS]** 
- `TorDataService` loads JSON at app launch
- Data is bundled in app (no network required)
- Uses `ObservableObject` for SwiftUI integration

**[Android]**
- Load JSON from assets folder
- Parse with Gson or Moshi
- Use Repository pattern with ViewModel

## Offline Support

All tor data is bundled with the app and available offline. No network connection is required to browse tors, view details, or mark visits.
