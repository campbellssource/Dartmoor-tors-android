# User Data

## Overview

The main user data is a checklist tracking which tors have been visited. Data is stored locally and synced to cloud storage.

## Data Model

### VisitedTor
- `torId`: String - references the tor
- `visitedDate`: Date - when marked as visited
- `photoAssetIdentifier`: String? - photo library reference for associated photo (device-specific)
- `photoCloudIdentifier`: String? - cloud photo identifier for cross-device sync
- `checklist`: Checklist - the parent checklist this visit belongs to

### Checklist
A checklist is a collection of visited tors that can be synced and shared:
- `id`: UUID
- `name`: String - display name for the checklist
- `isOwned`: Bool - whether this is the user's own checklist
- `visitedTors`: [VisitedTor] - the tors marked as visited

## Storage Strategy

1. **Local First**: All data stored locally via platform database
2. **Cloud Sync**: Automatically syncs to cloud when available
3. **Offline Support**: Works fully offline with local data, syncs when connectivity is restored
4. **Photo Sync**: Photos are referenced via cloud identifiers where available

**[Platform-specific]** Storage implementation:

**[iOS]**
- Local: SwiftData with `@Model` classes
- Cloud: CloudKit private database
- Photo sync: PHCloudIdentifier (requires iCloud Photos)

**[Android]**
- Local: Room database or DataStore
- Cloud: Google Drive API, Firebase, or custom backend (to be determined)
- Photo sync: Google Photos API or photo hash matching

## Sharing

### Current Implementation
- **Share Progress**: Users can share a text summary of their visited tors via system share sheet
- **Database Mode**: Supports both private and shared databases

**[iOS]** Uses ShareLink and CKShare infrastructure (real-time sharing not yet in UI)
**[Android]** Use Android Sharesheet for text sharing

### Future: Real-time Collaboration
- Owner can invite others by account
- Shared users can view progress in real-time
- "Shared With Me" section shows checklists shared by others

## Requirements

- Store visited tors locally (implemented)
- Sync to cloud when available (implemented on iOS)
- Share progress summary via text (implemented)
- Share checklist with other users in real-time (infrastructure in place, UI not implemented)
- View shared checklists (not implemented)
- Future: Export list

## User Auth / Recognition

**[iOS]** Authentication handled via CloudKit - no separate login required. User identity determined by iCloud account.

**[Android]** Options:
- Google Sign-In for Google Drive sync
- Firebase Authentication
- No auth required for local-only usage
