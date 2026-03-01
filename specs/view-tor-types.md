# Collection View

## Overview

The Collection view allows users to filter which types of tors are displayed throughout the app. It shows tor classifications with toggle controls and displays progress statistics for visited tors.

## Tab Bar

- **Tab Name**: "Collection"
- **Icon**: Custom tor silhouette icon (`tabbar-tor` asset, rendered as template image)

## Sections

### About Section
- Brief explanation: "This app uses the tors and categories from Tors of Dartmoor. There are over 900 tors in total but the default filters show around 310 of the most prominent accessible ones."

### Tors of Dartmoor Collection Section
Credits the data source:
- Description: "The Tors of Dartmoor Collection is a comprehensive database of every tor on Dartmoor, meticulously researched by Tim Jenkinson since the mid-1990s. In 2017, Tim teamed up with Paul Buck and Max Piper to get all the tors logged and photographed."
- Additional credit: "Paul is the instigator, designer and webmaster for Tors of Dartmoor. Max has explored extensively and published a specialist book on East Dartmoor's lesser-known tors."
- Profit sharing note: "Any profit from this app will be shared annually with the Tors of Dartmoor website."
- Link to https://www.torsofdartmoor.co.uk

### Tor Types Section

Users can toggle visibility of different tor classifications (from torsofdartmoor.co.uk):

**Default ON:**
- Summit
- Summit Avenue
- Valley Side

**Default OFF:**
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

Each classification shows the count of tors in that category.
Shows the total selected out of the full total (~900 tors).

### Access Filter

- **Accessible Only**: On by default - shows only tors that can be legally accessed
- Accessible includes: Public, Public (part private), Private (but accessible), Private (fee required)
- Not accessible includes: Private (visible only), Private (seek permission), Private (no access)
- Users can disable this filter to see all tors including those on restricted/private land

### Progress Section

- Total Tors count (based on current filters)
- Tors Visited count
- Remaining count

### Storage Section

- Shows where tors visited data is stored
- **[iOS]** Shows iCloud sync status
- **[Android]** Show Google Drive/Firebase sync status (if implemented)

### App Section

- Version info
- Link to Tors of Dartmoor website

## Data Sync

- Visit data stored locally using platform database
  - **[iOS]** SwiftData
  - **[Android]** Room database or similar
- Cloud sync for cross-device access
  - **[iOS]** CloudKit
  - **[Android]** Google Drive API or Firebase (to be determined)
- Works fully offline with local data
