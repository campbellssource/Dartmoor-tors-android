# Build Instructions for Dartmoor Tors Android App

This document provides detailed instructions for building the Dartmoor Tors Android application.

## Environment Requirements

### Required Software

1. **Java Development Kit (JDK)**
   - Version: JDK 17 or later
   - Download: [Adoptium Temurin JDK 17](https://adoptium.net/)
   - Set `JAVA_HOME` environment variable

2. **Android Studio** (Recommended)
   - Version: Arctic Fox (2020.3.1) or later
   - Download: [Android Studio](https://developer.android.com/studio)
   - Includes Android SDK and Gradle

3. **Android SDK**
   - API Level 34 (Android 14.0)
   - Build Tools 34.0.0 or later
   - Platform tools

4. **Gradle**
   - Version: 8.2 (included via wrapper)
   - Wrapper scripts: `gradlew` (Unix/Mac) and `gradlew.bat` (Windows)

### Network Requirements

**Important**: The build process requires internet access to download dependencies from:
- Google Maven Repository (`dl.google.com`)
- Maven Central (`repo.maven.apache.org`)
- Gradle Plugin Portal

If you're behind a corporate firewall or proxy, configure Gradle accordingly.

## Building with Android Studio

### Step 1: Clone the Repository

```bash
git clone https://github.com/campbellssource/Dartmoor-tors-android.git
cd Dartmoor-tors-android
```

### Step 2: Open in Android Studio

1. Launch Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned repository directory
4. Click "Open"

### Step 3: Sync Project

Android Studio will automatically:
- Download Gradle dependencies
- Download Android SDK components (if missing)
- Index the project

Wait for the sync to complete (check the progress bar at the bottom).

### Step 4: Build the Project

Choose one of the following methods:

**Method A: Build Menu**
1. Click `Build` → `Make Project`
2. Or use keyboard shortcut: `Ctrl+F9` (Windows/Linux) or `Cmd+F9` (Mac)

**Method B: Build Variants**
1. Click `Build` → `Select Build Variant`
2. Choose `debug` or `release`
3. Click `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`

### Step 5: Run on Device/Emulator

1. Connect an Android device via USB (with Developer Mode enabled)
   - OR -
2. Create/start an Android Virtual Device (AVD) in Android Studio

3. Click the green "Run" button (or press `Shift+F10`)
4. Select your target device
5. The app will be installed and launched automatically

## Building from Command Line

### Unix/Linux/Mac

```bash
# Make gradlew executable (if needed)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug APK on connected device
./gradlew installDebug

# Run all checks and tests
./gradlew check

# Clean build
./gradlew clean assembleDebug
```

### Windows

```cmd
# Build debug APK
gradlew.bat assembleDebug

# Build release APK
gradlew.bat assembleRelease

# Install debug APK on connected device
gradlew.bat installDebug

# Run all checks and tests
gradlew.bat check

# Clean build
gradlew.bat clean assembleDebug
```

## Output Locations

After a successful build, you'll find the APK files at:

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Installing the APK Manually

### On a Physical Device

1. Enable Developer Options on your Android device:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Go back to Settings → System → Developer Options
   - Enable "USB Debugging"

2. Connect device via USB

3. Install using ADB:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### On an Emulator

1. Start your Android emulator

2. Drag and drop the APK file onto the emulator window
   - OR -
   Use ADB:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Troubleshooting

### Issue: "Could not resolve com.android.tools.build:gradle"

**Solution**: 
- Check internet connectivity
- Verify access to Google Maven repository
- Try running with `--refresh-dependencies` flag:
  ```bash
  ./gradlew assembleDebug --refresh-dependencies
  ```

### Issue: "SDK location not found"

**Solution**:
Create a `local.properties` file in the project root:
```properties
sdk.dir=/path/to/your/android/sdk
```

For example:
- Mac: `sdk.dir=/Users/username/Library/Android/sdk`
- Linux: `sdk.dir=/home/username/Android/Sdk`
- Windows: `sdk.dir=C\:\\Users\\username\\AppData\\Local\\Android\\Sdk`

### Issue: "Unsupported Java version"

**Solution**:
- Ensure you're using JDK 17
- Set `JAVA_HOME` environment variable
- In Android Studio: File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK

### Issue: Build takes too long

**Solution**:
- Add these lines to `gradle.properties`:
  ```properties
  org.gradle.daemon=true
  org.gradle.parallel=true
  org.gradle.configureondemand=true
  org.gradle.caching=true
  ```

### Issue: Out of memory during build

**Solution**:
- Increase heap size in `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError
  ```

## Proxy Configuration

If you're behind a proxy, create/edit `gradle.properties` in your Gradle home directory (`~/.gradle/` on Unix, `%USERPROFILE%\.gradle\` on Windows):

```properties
systemProp.http.proxyHost=your.proxy.host
systemProp.http.proxyPort=8080
systemProp.http.proxyUser=username
systemProp.http.proxyPassword=password

systemProp.https.proxyHost=your.proxy.host
systemProp.https.proxyPort=8080
systemProp.https.proxyUser=username
systemProp.https.proxyPassword=password
```

## Gradle Commands Reference

| Command | Description |
|---------|-------------|
| `./gradlew tasks` | List all available tasks |
| `./gradlew assembleDebug` | Build debug APK |
| `./gradlew assembleRelease` | Build release APK |
| `./gradlew installDebug` | Install debug APK |
| `./gradlew uninstallAll` | Uninstall all variants |
| `./gradlew clean` | Delete build directory |
| `./gradlew build` | Full build with tests |
| `./gradlew check` | Run all checks |
| `./gradlew lint` | Run lint checks |
| `./gradlew test` | Run unit tests |
| `./gradlew connectedAndroidTest` | Run instrumented tests |
| `./gradlew dependencies` | Show dependency tree |

## Build Variants

The app supports two build types:

1. **Debug**
   - Debuggable
   - Not optimized
   - Includes debug symbols

2. **Release**
   - Optimized with ProGuard
   - Not debuggable
   - Smaller APK size
   - **Note**: Requires signing configuration for distribution

## Next Steps

After successfully building the app, you can:

1. **Test the app**: Explore the list of Dartmoor tors and tap on each to see details
2. **Modify the data**: Edit `app/src/main/java/com/dartmoor/tors/Tor.kt` to add more tors
3. **Customize the UI**: Modify layout files in `app/src/main/res/layout/`
4. **Add features**: Implement map view, search, favorites, etc.

## Support

For issues or questions:
- Check the main [README.md](README.md)
- Review Android Studio documentation
- Consult [Android Developer Documentation](https://developer.android.com/docs)

## Build Status

To ensure the project builds correctly in your environment:

1. Clone the repository
2. Run `./gradlew clean build`
3. Check for any errors in the output
4. If successful, you should see "BUILD SUCCESSFUL"
