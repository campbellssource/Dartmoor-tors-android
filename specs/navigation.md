# Navigation

Uses standard platform navigation patterns.

**[iOS]** Follows [tab bar HIG](https://developer.apple.com/design/human-interface-guidelines/tab-bars).
**[Android]** Should follow [Material Design bottom navigation](https://m3.material.io/components/navigation-bar/overview).

## Tab Bar

Four tabs:
- **Map** - [Map view](view-tors-map.md) (default tab)
- **Collection** - [Collection](view-tor-types.md) - filter tor classifications and view progress
- **Photos** - Manage photo features: link shared albums, scan for photos near tors, view album
- **Search** - Search tab for finding tors

### Tab Icons
- **Map**: Map icon
  - **[iOS]** SF Symbol `map`
  - **[Android]** Material icon `map`
- **Collection**: Custom tor silhouette icon (asset: `tabbar-tor`, rendered as template)
- **Photos**: Photo stack icon
  - **[iOS]** SF Symbol `photo.stack`
  - **[Android]** Material icon `photo_library`
- **Search**: System search icon
  - **[iOS]** Uses `.search` role which automatically pins to trailing edge with system styling
  - **[Android]** Material icon `search`

## Search Tab

The search tab displays a searchable list of tors with:
- Search bar
  - **[iOS]** At bottom with `.search` role
  - **[Android]** At top following Material patterns
- Filter chips at top: Accessible, Visited, Unvisited
- Sort menu in toolbar
- Tapping a tor navigates to the Map tab and opens its place card

## Welcome Screen

On first launch, a welcome modal sheet appears over the app explaining features. The map is visible behind the modal, giving users context. Users must tap "Get Started" to dismiss.

## Other Views
- [Tor detail](view-tor-detail.md) - Users reach this via the place card that appears when selecting a tor on the map or from the search list.
