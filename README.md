# Dartmoor Tors Android

An Android app for exploring and tracking visits to Dartmoor tors. Displays 900+ tors on a map, allows users to mark visits, associate photos, and track progress.

## Quick Start

### Prerequisites

- Android SDK installed (typically at `~/Library/Android/sdk` on macOS)
- Java Development Kit (JDK) 17 or higher
- An Android emulator or physical device

### Starting the Emulator

1. **List available emulators:**

   ```bash
   ~/Library/Android/sdk/emulator/emulator -list-avds
   ```

2. **Start an emulator:**

   ```bash
   ~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1
   ```

   Or use the workspace task: `Start Emulator` from VS Code

3. **Verify device is connected:**

   ```bash
   ~/Library/Android/sdk/platform-tools/adb devices
   ```

   You should see output like:

   ```
   List of devices attached
   emulator-5554   device
   ```

### Building and Installing

1. **Build the debug APK:**

   ```bash
   ./gradlew assembleDebug
   ```

2. **Install on connected device/emulator:**

   ```bash
   ./gradlew installDebug
   ```

3. **Or do both in one step:**
   ```bash
   ./gradlew installDebug
   ```
   Note: Make sure an emulator is running or a device is connected first!

### Useful Commands

- **Clean build:** `./gradlew clean`
- **Run tests:** `./gradlew test`
- **Check connected devices:** `~/Library/Android/sdk/platform-tools/adb devices`
- **List emulators:** `~/Library/Android/sdk/emulator/emulator -list-avds`

## Project Structure

See [CLAUDE.md](CLAUDE.md) for detailed project documentation, specifications, and architecture.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Maps:** Google Maps SDK for Android
- **Database:** Room (local) + Google Drive/Firebase (cloud sync)
- **Architecture:** MVVM with Repository pattern
- **DI:** Hilt (Dagger)
