# Tests

This file describes user journeys for the purpose of defining user flows, prioritising what to work on and creating tests.

---
# Core smoke tests 

These are the key high level user tasks I'd like us to automate.

## Open app understand what it does
- When the app first opens they should see a welcome card centered on screen with space around it to see the map behind and tab bar below.
- The card uses a material background and contains:
    - [Tabbar Tor icon] Choose tors to bag in Collection
    - [Search icon] Search, filter and sort tors
    - [Photo icon] Add photos and create an album
    - Primary CTA button: "Find tors from your photos" → navigates to Photos tab
    - Skip link to dismiss without action
- No heading - keep text minimal to maximize visibility of content behind
    
## Set up photos
Linked photos
- Photos page has a section for linking the app to one of your album so that when you bag a tor you can take a photo have have this added to an album, both in the app and your native photos album
- Once you've added an album you see an additional option to add a shared album (we can't automatically)
- once an album i set up, addinf

Then see scan photos, mark a tor as visited and find tor to visit page to see concicences of this

## Scan photos to see what you've done
Grant permissions for the app to find photos near tors.
For each tor with a photo within 100m of the tor, show a list of photos. Allow the user to skip or select a photo.

When user selects "Add Photo":
- If tor is **not visited**: Marks tor as visited with visit date set to photo's creation date
- If tor is **already visited**: Adds photo; if photo date is earlier than visit date, updates visit date to match
- If user has Dartmoor Tors album: Auto-adds photo to album

This allows users to backfill their visit history from old photos taken at tors.

## Set up Tors
Visit the collection page to select tors the user would like to see the map, check progress against... create a todo list


## Find a tor to visit

## Navigate to a tor

## Mark a tor as visited



--

# Detailed UI tests

these can be immpleneted as required.

## 1. Open app → Navigate to list → Scroll → Select tor → Open tor detail

**Goal:** A user can find a tor in the list and view its details.

### Preconditions
- App is freshly launched
- Tor data is loaded (bundled CSV has been imported into the data model)
- No tors have been marked as visited

### Steps
1. Launch the app
2. The map view is shown by default
3. Tap the **List** tab in the tab bar
4. The tors list is shown, sorted by height descending by default
5. Scroll down through the list
6. Tap on a tor
7. The tor detail view is presented

### Expected outcomes
- The list displays tor name, visited state (icon), and access level for each row
- Tors are sorted highest-first by default
- Tor names never truncate — they wrap to a new line if needed
- The detail view shows:
  - Tor name as a title (never truncated)
  - Height in metres
  - OS grid reference
  - Latitude and longitude
  - Access (public / private)
  - Link to the Tors of Dartmoor page
  - Wikipedia link (if one exists for this tor)
- The detail view shows a back button that returns the user to the list

---

## 2. Open app → Pan and zoom map → Select a tor

**Goal:** A user can interact with the map and navigate to a tor's detail from it.

### Preconditions
- App is freshly launched
- Tor data is loaded

### Steps
1. Launch the app
2. The map view is shown by default
3. Pan the map to a different area of Dartmoor
4. Pinch to zoom in on a cluster of tors
5. Tap a tor icon on the map

### Expected outcomes
- Tor icons are visible on the map for all tors in the dataset
- Each icon reflects the tor's visited state (e.g. different colour or indicator)
- The map responds correctly to pan and pinch-to-zoom gestures
- Tapping a tor icon opens the tor detail view
- The detail view shows a back button that returns the user to the map

---

## 3. Search flow

**Goal:** A user can search for a tor by name and navigate to its detail.

### Preconditions
- Tor data is loaded

### Steps
1. Tap the **Search** tab in the tab bar
2. A search input is focused (or the user taps the search field)
3. Type a partial tor name (e.g. "hay")
4. Results matching the query appear in a list
5. Tap a result
6. The tor detail view is presented

### Expected outcomes
- Search filters tors by name, case-insensitively, matching partial input
- Results update as the user types
- A clear button (✕) appears when there is text in the input, and tapping it clears the field and resets results
- The previous search query is remembered if the user leaves and returns to the Search tab
- Tapping a result navigates to the correct tor detail view
- If no tors match the query, an appropriate empty state is shown

---

## 4. Mark tor as visited

**Goal:** A user can mark a tor as visited, and this state is reflected consistently across views.

### Preconditions
- Tor data is loaded
- The tor is not yet marked as visited

### Steps
1. Navigate to any tor's detail view (via List, Map, or Search)
2. Tap the control to mark the tor as visited
3. Navigate back to the list view
4. Navigate to the map view

### Expected outcomes
- The detail view reflects the updated visited state immediately after tapping
- The tor's row in the list view shows the visited indicator
- The tor's icon on the map shows the visited indicator
- The visited state persists if the app is closed and relaunched (stored in SwiftData / CloudKit)

---

## 5. Filter and sort in list view

**Goal:** A user can sort and filter the tors list to surface the content most relevant to them.

### Preconditions
- Tor data is loaded
- At least one tor has been marked as visited

### 5a. Sort: Alphabetical A–Z
1. Open the **List** tab
2. Select sort order: **A–Z**
3. The list re-orders alphabetically ascending

### 5b. Sort: Alphabetical Z–A
1. Select sort order: **Z–A**
2. The list re-orders alphabetically descending

### 5c. Sort: Height (default)
1. Select sort order: **Height**
2. The list re-orders from highest to lowest

### 5d. Filter: Hide visited tors
1. Enable the **Hide visited** filter
2. The list no longer shows tors that have been marked as visited
3. Disable the filter — the visited tors reappear

### Expected outcomes
- Sort and filter options are accessible from the list view (e.g. a toolbar button or menu)
- Sort and filter can be used in combination (e.g. hide visited + sort A–Z)
- The row count changes correctly when the hide-visited filter is active
- The selected sort/filter state is clear to the user (e.g. a selected/active indicator)

---

## 6. Error states

**Goal:** The app handles missing permissions and connectivity gracefully.

### 6a. No network connectivity

#### Preconditions
- The tor data has been loaded and cached at least once

#### Steps
1. Put the device into airplane mode
2. Launch the app (or the app is already open)
3. Browse the list and map
4. Open a tor detail view

#### Expected outcomes
- The tors list loads from the local SwiftData store — no error is shown
- The map loads tor icons from the local store — no error is shown
- Tor detail information is shown from the local store — no error is shown
- Visited state changes made offline are stored locally and sync to CloudKit when connectivity is restored
- If data has never been loaded, an appropriate message is shown explaining that connectivity is required on first launch

### 6b. Location permission denied

#### Preconditions
- The user has denied location access when prompted (or has not yet been prompted)

#### Steps
1. Launch the app with location permission denied
2. Navigate to the **List** tab
3. Attempt to select **Sort by proximity**

#### Expected outcomes
- The app does not crash
- Sort by proximity is either hidden or disabled when location is unavailable
- If the user attempts to use proximity sort, a prompt or message explains that location access is required and offers a way to open Settings
- All other sort and filter options remain fully functional
---

## 7. Unmark a tor as visited

**Goal:** A user can remove the visited state from a tor, and this is reflected consistently across views.

### Preconditions
- Tor data is loaded
- At least one tor has already been marked as visited

### Steps
1. Navigate to the detail view of a tor that has been marked as visited (via List, Map, or Search)
2. Tap the control to unmark the tor as visited
3. Navigate back to the list view
4. Navigate to the map view

### Expected outcomes
- The detail view reflects the updated (unvisited) state immediately after tapping
- The tor's row in the list view no longer shows the visited indicator
- The tor's icon on the map no longer shows the visited indicator
- The unvisited state persists if the app is closed and relaunched
- If the **Hide visited** filter is active in the list, the tor reappears in the list after being unmarked

---

## 8. Show tor on map from detail view

**Goal:** A user can navigate from a tor's detail view directly to the map, centred on that tor.

### Preconditions
- Tor data is loaded

### Steps
1. Navigate to a tor's detail view (via List or Search)
2. Tap the "Show on Map" button

### Expected outcomes
- The app switches to the Map tab
- The map centres on the selected tor's location
- The map zooms to a level where the tor marker is clearly visible
- The tor's marker is visible on the map

