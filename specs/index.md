# Specifications

This directory contains feature and design specifications for the Dartmoor Tors app.

## Platform Support

This specification is written to support both iOS and Android implementations. Platform-specific notes are marked with:
- **[iOS]** - iOS/Apple-specific implementation details
- **[Android]** - Android-specific implementation details
- **[Platform-specific]** - Requires different implementation per platform

## How to Use

- Create one Markdown file per feature or area of the app.
- Reference these files in `CLAUDE.md` or mention them in prompts so Claude can read them.
- Suggested naming: `feature-name.md` (e.g., `tor-list.md`, `offline-sync.md`).

## Views

| File | View |
|---|---|
| [view-tors-map.md](view-tors-map.md) | Tors Map (with integrated search sheet) |
| [view-tor-detail.md](view-tor-detail.md) | Tor Detail / Place Card |
| [view-tor-types.md](view-tor-types.md) | Collection |

## Features

- [Navigation](navigation.md)
- [Photos](photos.md)
- [User Data](user-data.md)
- [User Location](user-location.md)
- [Tor Data](tor-data.md)

## To Do

See [todo.md](todo.md) for keeping track of what has been specified and implemented.

## End to End Tests

See [tests.md](tests.md) for user journey tests.
