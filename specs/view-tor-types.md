# Collections View

## Overview

The Collections view allows users to select which collection of tors to display throughout the app. Collections are curated subsets of the full tor database, each with different scopes and purposes. This helps users focus on specific sets of tors and improves map performance by reducing the number of rendered annotations.

## Tab Bar

- **Tab Name**: "Collection"
- **Icon**: Custom tor silhouette icon (`tabbar-tor` asset, rendered as template image)

## Collections

The app supports multiple collections, each representing a different curated set of tors:

| Collection ID         | Display Name        | Description                                                                 | Approx Count |
| --------------------- | ------------------- | --------------------------------------------------------------------------- | ------------ |
| `tors-of-dartmoor`    | Tors of Dartmoor    | Complete database from torsofdartmoor.co.uk with classification sub-filters | ~900         |
| `os-map`              | OS Map Tors         | Tors marked on Ordnance Survey maps                                         | TBD          |
| `public`              | Public Collection   | Community-curated collection                                                | TBD          |
| `dartmoor-compendium` | Dartmoor Compendium | Tors from the Dartmoor Compendium reference                                 | TBD          |
| `rock-idols`          | Rock Idols          | Featured prominent tors                                                     | TBD          |

### Default Collection

The default collection is **Public** — a curated set of the most notable and accessible tors, suitable for most users.

## Sections

### Collection Selector Section

A list of available collections. Each row shows:

- Collection name
- Brief description
- Number of tors in collection
- Checkmark for currently selected collection

Tapping a collection selects it and updates the map/list views throughout the app.

### Sub-filters Section (Tors of Dartmoor only)

This section only appears when the "Tors of Dartmoor" collection is selected. It is completely hidden for all other collections.

When visible, it allows users to toggle visibility of different tor classifications:

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

### Access Filter

- **Accessible Only**: On by default — shows only tors that can be legally accessed
- Accessible includes: Public, Public (part private), Private (but accessible), Private (fee required)
- Not accessible includes: Private (visible only), Private (seek permission), Private (no access)
- Users can disable this filter to see all tors including those on restricted/private land
- This filter applies across all collections

### About Section

Brief explanation of the collections feature and current selection stats:

- Currently selected collection name
- Total tors in collection (based on current filters)
- Tors visited count
- Remaining count

### Collection Introduction Section

Displays the `introduction` text from the selected collection's metadata. If the collection has a `url`, shows a link to learn more.

If the collection has an `imageAsset`, displays the image prominently (e.g., as a header image or alongside the introduction).

### Tors of Dartmoor Credits Section

Additional credits shown only when the "Tors of Dartmoor" collection is selected:

- Additional credit: "Paul is the instigator, designer and webmaster for Tors of Dartmoor. Max has explored extensively and published a specialist book on East Dartmoor's lesser-known tors."
- Profit sharing note: "Any profit from this app will be shared annually with the Tors of Dartmoor website."

### Storage Section

- Shows where tors visited data is stored
- **[iOS]** Shows iCloud sync status
- **[Android]** Show Google Drive/Firebase sync status (if implemented)

### App Section

- Version info
- Link to Tors of Dartmoor website

## Data Structure

### Collections Field in Tor Data

Each tor in `tors.json` includes a `collections` array listing which collections it belongs to:

```json
{
  "id": "haytor",
  "name": "Haytor Rocks",
  "collections": ["tors-of-dartmoor", "os-map", "rock-idols"],
  ...
}
```

A tor can belong to multiple collections. Every tor must explicitly include `tors-of-dartmoor` in its collections array to appear in that collection.

### Collection Metadata

Collection metadata is stored in a separate `collections.json` file in the Resources folder:

```json
[
  {
    "id": "public",
    "name": "Public",
    "description": "A curated collection of the most notable and accessible tors on Dartmoor.",
    "introduction": "This collection features the best-known tors that are easy to reach and offer stunning views. Perfect for casual walkers and first-time visitors to Dartmoor.",
    "imageAsset": "collection-public",
    "url": null,
    "hasSubFilters": false,
    "sortOrder": 0
  },
  {
    "id": "tors-of-dartmoor",
    "name": "Tors of Dartmoor",
    "description": "The complete database of every tor on Dartmoor.",
    "introduction": "A comprehensive database of every tor on Dartmoor, meticulously researched by Tim Jenkinson since the mid-1990s. In 2017, Tim teamed up with Paul Buck and Max Piper to get all the tors logged and photographed.",
    "imageAsset": "collection-tors-of-dartmoor",
    "url": "https://www.torsofdartmoor.co.uk",
    "hasSubFilters": true,
    "sortOrder": 1
  },
  {
    "id": "os-map",
    "name": "OS Map Tors",
    "description": "Tors marked on Ordnance Survey maps.",
    "introduction": "These are the tors you'll find labelled on Ordnance Survey Explorer and Landranger maps — the classic landmarks of Dartmoor.",
    "imageAsset": "collection-os-map",
    "url": null,
    "hasSubFilters": false,
    "sortOrder": 2
  },
  {
    "id": "dartmoor-compendium",
    "name": "Dartmoor Compendium",
    "description": "Tors from the Dartmoor Compendium reference.",
    "introduction": "Tors documented in the Dartmoor Compendium, a comprehensive online encyclopedia of Dartmoor.",
    "imageAsset": "collection-dartmoor-compendium",
    "url": "https://www.dartmoor-compendium.co.uk",
    "hasSubFilters": false,
    "sortOrder": 3
  },
  {
    "id": "rock-idols",
    "name": "Rock Idols",
    "description": "Featured prominent tors.",
    "introduction": "The iconic rock formations that define Dartmoor's skyline.",
    "imageAsset": "collection-rock-idols",
    "url": null,
    "hasSubFilters": false,
    "sortOrder": 4
  }
]
```

**Metadata Fields:**

| Field           | Type    | Description                                                   |
| --------------- | ------- | ------------------------------------------------------------- |
| `id`            | String  | Unique identifier matching values in tor `collections` arrays |
| `name`          | String  | Display name shown in the collection selector                 |
| `description`   | String  | Brief one-line description shown in the selector row          |
| `introduction`  | String  | Longer introductory text shown when collection is selected    |
| `imageAsset`    | String? | Optional asset name for collection artwork/icon               |
| `url`           | String? | Optional URL for more information about the collection        |
| `hasSubFilters` | Bool    | Whether this collection shows classification sub-filters      |
| `sortOrder`     | Int     | Display order in the collection list                          |

## Filtering Logic

1. **Collection filter**: Include only tors where `collections` array contains selected collection ID
2. **Sub-filter** (Tors of Dartmoor only): Further filter by enabled classifications
3. **Access filter**: Further filter by access status if "Accessible Only" is enabled

The filtered tor list is used throughout the app (map, search, statistics).

## Performance Considerations

Collections improve map performance by reducing the number of annotations rendered. When a smaller collection is selected (e.g., "OS Map Tors" with ~150 tors), the map renders significantly faster than with the full ~900 tor database.

## Data Sync

- Collection selection is stored locally and synced across devices
- Visit data stored locally using platform database
  - **[iOS]** SwiftData
  - **[Android]** Room database or similar
- Cloud sync for cross-device access
  - **[iOS]** CloudKit
  - **[Android]** Google Drive API or Firebase (to be determined)
- Works fully offline with local data

## Future Considerations

- User-created custom collections
- Sharing collections with other users
- Collection-specific statistics and badges
