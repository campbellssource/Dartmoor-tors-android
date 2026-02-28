# Dartmoor Tors Android App

A POC (Proof of Concept) Android application for browsing and exploring Dartmoor Tors.

## Overview

This Android app provides information about the famous tors (rocky outcrops) of Dartmoor, Devon, England. The app features:

- **List of Tors**: Browse through 10 prominent Dartmoor tors
- **Detailed Information**: View details including height, grid reference, coordinates, and descriptions
- **Material Design UI**: Modern, user-friendly interface

## Features

### Included Tors

The POC includes information about 10 notable Dartmoor tors:

1. **High Willhays** (621m) - Highest point on Dartmoor
2. **Yes Tor** (619m) - Second highest point
3. **Hay Tor** (457m) - Most visited and iconic
4. **Belstone Tor** (529m) - Northern edge views
5. **Vixen Tor** (320m) - Distinctive formation
6. **Hound Tor** (448m) - Medieval village ruins
7. **Bowerman's Nose** (404m) - Legendary rock stack
8. **Great Mis Tor** (539m) - Western moorland
9. **Great Staple Tor** (450m) - Near Merrivale
10. **Rippon Tor** (473m) - Eastern Dartmoor views

### Technical Stack

- **Language**: Kotlin
- **UI**: Material Design 3 Components
- **Architecture**: Simple MVVM pattern with Repository
- **Views**: RecyclerView for list, ScrollView for details
- **Navigation**: Activity-based navigation

## Project Structure

```
app/
├── src/main/
│   ├── java/com/dartmoor/tors/
│   │   ├── MainActivity.kt          # Main list screen
│   │   ├── TorDetailActivity.kt     # Detail screen
│   │   ├── Tor.kt                   # Data model & repository
│   │   └── TorAdapter.kt            # RecyclerView adapter
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml         # Main screen layout
│   │   │   ├── activity_tor_detail.xml   # Detail screen layout
│   │   │   └── item_tor.xml              # List item layout
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   ├── colors.xml
│   │   │   └── themes.xml
│   │   └── drawable/
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## Building the App

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 17 or later
- Android SDK with API Level 34
- Gradle 8.2

### Build Instructions

**Note**: Due to environment constraints, you may need to build this project with proper internet access to Google's Maven repository.

1. Clone the repository:
   ```bash
   git clone https://github.com/campbellssource/Dartmoor-tors-android.git
   cd Dartmoor-tors-android
   ```

2. Open the project in Android Studio, or build from command line:
   ```bash
   ./gradlew assembleDebug
   ```

3. Run on an emulator or device:
   ```bash
   ./gradlew installDebug
   ```

### Troubleshooting

If you encounter build issues:

1. Ensure you have internet access to download dependencies
2. Check that your Android SDK is properly configured
3. Verify that ANDROID_HOME environment variable is set
4. Try running `./gradlew clean build`

## Data Model

The `Tor` data class includes:

- `id`: Unique identifier
- `name`: Tor name
- `heightMeters`: Height in meters
- `heightFeet`: Height in feet
- `gridReference`: OS Grid Reference
- `latitude`: GPS latitude
- `longitude`: GPS longitude
- `description`: Detailed description

## Future Enhancements

Potential improvements for a production version:

1. **Map Integration**: Display tors on an interactive map
2. **Navigation**: Provide walking directions
3. **Photos**: Add images of each tor
4. **Weather**: Show current weather conditions
5. **Favorites**: Allow users to save favorite tors
6. **Offline Support**: Full offline functionality
7. **Search**: Search tors by name or features
8. **Filters**: Filter by height, location, difficulty
9. **User Reviews**: Community ratings and comments
10. **AR Features**: Augmented reality tor identification

## Data Source

The tor information in this POC is based on publicly available data about Dartmoor tors. For a production app, consider integrating with official APIs or databases such as:

- Ordnance Survey API
- Dartmoor National Park Authority
- OpenStreetMap

## License

This is a POC project. Please ensure proper licensing for any production use.

## Contributing

This is a proof of concept. For contributions or suggestions, please open an issue or pull request.

