# App Screenshots & UI Description

This document describes the visual appearance and user interface of the Dartmoor Tors Android app.

## Main Screen (List View)

### Layout Description

The main screen displays a scrollable list of all Dartmoor tors.

**Header:**
- App bar with title "Dartmoor Tors"
- Dartmoor green color theme (#4A7C59)

**List Items:**
Each tor is displayed in a Material Design card with:
- **Tor Name** (Bold, 18sp) - e.g., "High Willhays"
- **Height** (14sp, secondary text color) - e.g., "621m (2037ft)"
- **Grid Reference** (14sp, secondary text color) - e.g., "SX 580 892"
- Card has rounded corners (8dp radius) and subtle elevation shadow
- Cards are separated by 8dp margins

**Visual Representation:**

```
┌─────────────────────────────────────┐
│  Dartmoor Tors                   ≡  │ ← Green App Bar
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐  │
│  │  High Willhays              │  │ ← Card 1
│  │  621m (2037ft)                │  │
│  │  SX 580 892                   │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │  Yes Tor                      │  │ ← Card 2
│  │  619m (2030ft)                │  │
│  │  SX 581 901                   │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │  Hay Tor                      │  │ ← Card 3
│  │  457m (1499ft)                │  │
│  │  SX 757 770                   │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │  Belstone Tor                 │  │ ← Card 4
│  │  529m (1736ft)                │  │
│  │  SX 619 915                   │  │
│  └───────────────────────────────┘  │
│                                     │
│           (scroll for more)         │
└─────────────────────────────────────┘
```

### Interaction

- **Tap on any card**: Opens the detail view for that tor
- **Scroll**: Smoothly scrolls through the list
- **Visual feedback**: Card slightly elevates on press (ripple effect)

## Detail Screen

### Layout Description

The detail screen shows comprehensive information about the selected tor.

**Header:**
- App bar with back arrow (←)
- Title shows the tor name (e.g., "High Willhays")
- Same green color theme

**Content (Scrollable):**
- **Tor Name** (Large, bold, 24sp)
- **Height** - "Height: 621m (2037ft)"
- **Grid Reference** - "Grid Reference: SX 580 892"
- **Coordinates** - "Coordinates: 50.6917, -4.0167"
- **Description** (16sp, regular) - Multiple paragraphs of descriptive text

**Visual Representation:**

```
┌─────────────────────────────────────┐
│  ← High Willhays                 ≡  │ ← Green App Bar
├─────────────────────────────────────┤
│                                     │
│  High Willhays                      │ ← Name (Large)
│                                     │
│  Height: 621m (2037ft)              │
│                                     │
│  Grid Reference: SX 580 892         │
│                                     │
│  Coordinates: 50.6917, -4.0167      │
│                                     │
│  High Willhays is the highest       │
│  point on Dartmoor and in southern  │
│  England. Located on the northern   │ ← Description
│  part of the moor, it offers        │
│  stunning views across Devon.       │
│                                     │
│                                     │
│           (scroll for more)         │
└─────────────────────────────────────┘
```

### Interaction

- **Back arrow**: Returns to the main list
- **Scroll**: View full description if it's long
- **System back button**: Also returns to main list

## Color Scheme

The app uses a nature-inspired color palette:

### Primary Colors
- **Dartmoor Green**: `#4A7C59` - Main app bar, primary elements
- **Dartmoor Green Dark**: `#2F5238` - Darker variant for status bar

### Secondary Colors
- **Teal**: `#FF03DAC5` - Accent color for highlights

### Text Colors
- **Primary Text**: Dark gray/black (system default)
- **Secondary Text**: Medium gray (for metadata like height, grid ref)

### Background
- **White/Light gray**: Card backgrounds
- **System background**: App background (supports dark mode)

## Typography

The app uses the default Material Design font (Roboto):

- **App Bar Title**: 20sp, medium weight
- **Tor Name (List)**: 18sp, bold
- **Tor Name (Detail)**: 24sp, bold
- **Body Text**: 16sp, regular
- **Secondary Info**: 14sp, regular

## Icon & Branding

### App Icon

The launcher icon features:
- **Background**: Dartmoor green (#4A7C59)
- **Foreground**: Simple white geometric representation of a tor (rocky outcrop)
- Adaptive icon for Android 8.0+
- Round and square variants

### Empty States

(Currently not needed as data is hardcoded, but for future reference)

```
┌─────────────────────────────────────┐
│  Dartmoor Tors                   ≡  │
├─────────────────────────────────────┤
│                                     │
│            🏔️                       │
│                                     │
│      No tors available              │
│      Check your connection          │
│                                     │
│      [Retry Button]                 │
│                                     │
└─────────────────────────────────────┘
```

## Responsive Design

### Phone (Portrait)

- Single column list
- Full-width cards
- Comfortable padding (16dp)

### Phone (Landscape)

- Wider cards with more horizontal space
- Content wraps appropriately
- Detail view optimized for horizontal reading

### Tablet

- Two-column layout for list (future enhancement)
- Master-detail pattern (list + detail side by side)
- Larger text sizes

## Dark Mode Support

The app supports Android's dark mode:

### Dark Theme Colors

- **Background**: Dark gray (#121212)
- **Cards**: Elevated dark gray (#1E1E1E)
- **Text**: White/light gray
- **Primary**: Lighter green (#6FA77B)
- **Shadows**: More pronounced

### Visual Representation (Dark Mode)

```
┌─────────────────────────────────────┐
│  Dartmoor Tors              ☰       │ ← Dark Green Bar
├─────────────────────────────────────┤
│  ╔═══════════════════════════════╗  │
│  ║  High Willhays                ║  │ ← Dark Card
│  ║  621m (2037ft)                ║  │   (Light text)
│  ║  SX 580 892                   ║  │
│  ╚═══════════════════════════════╝  │
│  ╔═══════════════════════════════╗  │
│  ║  Yes Tor                      ║  │
│  ║  619m (2030ft)                ║  │
│  ║  SX 581 901                   ║  │
│  ╚═══════════════════════════════╝  │
└─────────────────────────────────────┘
```

## Animations & Transitions

### List Screen

- **Item appearance**: Cards fade in slightly when scrolling
- **Tap feedback**: Material ripple effect (circular reveal)
- **Elevation**: Cards lift slightly on press (2dp → 8dp)

### Navigation

- **Activity transition**: Smooth slide animation
  - List → Detail: Slide left
  - Detail → List: Slide right
- **Shared element transition**: (Future enhancement)
  - Tor name animates from list to detail position

### Loading States

(Future enhancement for API data)
- **Skeleton screens**: Placeholder cards with shimmer effect
- **Pull to refresh**: Material Design refresh indicator

## Accessibility

### Visual Accessibility

- **Text contrast**: WCAG AA compliant (4.5:1 minimum)
- **Touch targets**: Minimum 48dp × 48dp
- **Font scaling**: Supports Android font size settings

### Screen Reader Support

- **Content descriptions**: All interactive elements labeled
- **Heading hierarchy**: Proper semantic structure
- **Navigation hints**: Clear instructions for interactions

### Keyboard Navigation

- **Focus indicators**: Visible when using external keyboard
- **Tab order**: Logical top-to-bottom, left-to-right
- **Enter key**: Activates focused items

## UI Polish

### Micro-interactions

- **Scroll bounce**: Overscroll effect at list ends
- **Card shadows**: Subtle depth perception
- **Status bar**: Colored to match app bar
- **Navigation bar**: Tinted appropriately

### Edge Cases

- **Very long names**: Text truncates with ellipsis (...)
- **No description**: "No description available" placeholder
- **Orientation change**: State preserved

## Comparison with Similar Apps

This POC takes inspiration from:

1. **Google Maps**: Clean list items with essential info
2. **AllTrails**: Card-based design for outdoor locations
3. **Wikipedia**: Comprehensive detail views
4. **Material Design Guidelines**: Standard Android patterns

## Future UI Enhancements

Ideas for production version:

1. **Photos**: Hero images for each tor
2. **Maps**: Inline map showing tor location
3. **Weather**: Current conditions at tor location
4. **Difficulty**: Visual indicator (easy/moderate/hard)
5. **Favorites**: Star icon to save favorites
6. **Search**: Search bar in toolbar
7. **Filters**: Bottom sheet for filtering tors
8. **Share**: Share button in detail view
9. **3D view**: AR view of tors (future tech)
10. **Route planning**: Navigate to tor feature

## Mockup Tools

To create actual mockups, use:

- **Figma**: Professional UI design tool
- **Adobe XD**: Adobe's design platform
- **Sketch**: Mac-only design app
- **Android Studio Layout Editor**: Direct XML editing

## Testing the UI

To verify the UI:

1. **Visual inspection**: Run app on emulator/device
2. **Different screen sizes**: Test on various devices
3. **Accessibility Scanner**: Use Android's accessibility tools
4. **Dark mode**: Toggle system dark mode
5. **Font sizes**: Change system font size
6. **Rotation**: Test portrait and landscape
7. **User testing**: Get feedback from real users

## Conclusion

The Dartmoor Tors app features a clean, modern Material Design interface that's intuitive and accessible. The simple two-screen design makes it easy to browse tors and view detailed information.

For actual screenshots, build and run the app, then use:
- Android Studio: Screenshot tool in Device File Explorer
- Device: Power + Volume Down buttons
- ADB: `adb shell screencap -p /sdcard/screenshot.png`
