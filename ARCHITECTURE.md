# App Architecture

This document describes the architecture and design patterns used in the Dartmoor Tors Android POC app.

## Overview

The app follows a simple layered architecture suitable for a POC application:

```
┌─────────────────────────────────────┐
│         Presentation Layer          │
│  (Activities, Adapters, Layouts)    │
├─────────────────────────────────────┤
│         Data Layer                  │
│  (Repository, Data Models)          │
└─────────────────────────────────────┘
```

## Layers

### Presentation Layer

Handles the UI and user interactions.

#### Components:

1. **MainActivity.kt**
   - Entry point of the app
   - Displays the list of tors using RecyclerView
   - Handles navigation to detail screen
   - Location: `app/src/main/java/com/dartmoor/tors/MainActivity.kt`

2. **TorDetailActivity.kt**
   - Shows detailed information about a selected tor
   - Receives tor ID via Intent extras
   - Displays name, height, coordinates, and description
   - Location: `app/src/main/java/com/dartmoor/tors/TorDetailActivity.kt`

3. **TorAdapter.kt**
   - RecyclerView adapter for the tors list
   - Binds tor data to list item views
   - Handles click events to open detail view
   - Location: `app/src/main/java/com/dartmoor/tors/TorAdapter.kt`

4. **Layouts**
   - `activity_main.xml`: Main screen with RecyclerView
   - `activity_tor_detail.xml`: Detail screen with ScrollView
   - `item_tor.xml`: Individual list item with Material CardView
   - Location: `app/src/main/res/layout/`

### Data Layer

Manages data and business logic.

#### Components:

1. **Tor.kt** (Data Model)
   ```kotlin
   data class Tor(
       val id: Int,
       val name: String,
       val heightMeters: Int,
       val heightFeet: Int,
       val gridReference: String,
       val latitude: Double,
       val longitude: Double,
       val description: String
   )
   ```
   - Immutable data class representing a tor
   - Contains all information about a single tor
   - Location: `app/src/main/java/com/dartmoor/tors/Tor.kt`

2. **TorRepository** (Repository Pattern)
   ```kotlin
   object TorRepository {
       fun getAllTors(): List<Tor>
       fun getTorById(id: Int): Tor?
   }
   ```
   - Singleton object for data access
   - Provides hardcoded data for POC
   - Could be extended to fetch from API/Database
   - Location: `app/src/main/java/com/dartmoor/tors/Tor.kt`

## Design Patterns

### Repository Pattern

The Repository pattern abstracts the data source:

- **Current**: Hardcoded in-memory data
- **Future**: Could be swapped with API calls, local database (Room), or file storage
- **Benefits**: Separation of concerns, easier testing, flexible data sources

### ViewHolder Pattern

Used in RecyclerView for efficient list rendering:

```kotlin
inner class TorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(tor: Tor) { /* ... */ }
}
```

- Reduces findViewById() calls
- Improves scrolling performance
- Reuses views for better memory management

### Activity Navigation

Simple Intent-based navigation:

```kotlin
val intent = Intent(this, TorDetailActivity::class.java).apply {
    putExtra(EXTRA_TOR_ID, tor.id)
}
startActivity(intent)
```

## UI Components

### Material Design Components

The app uses Material Design 3 components:

1. **MaterialCardView**
   - Used for list items
   - Provides elevation and corner radius
   - Consistent card design

2. **RecyclerView**
   - Efficient list display
   - Smooth scrolling
   - Memory efficient

3. **ConstraintLayout**
   - Flexible layout system
   - Flat view hierarchy
   - Better performance

4. **Theme**
   - Material 3 theme
   - Custom Dartmoor green color scheme
   - Day/Night mode support

## Data Flow

### Viewing the List

```
User Opens App
    ↓
MainActivity.onCreate()
    ↓
TorRepository.getAllTors()
    ↓
TorAdapter initialized with data
    ↓
RecyclerView displays list
```

### Viewing Details

```
User taps on a tor
    ↓
TorAdapter.onTorClick(tor)
    ↓
MainActivity.openTorDetail(tor)
    ↓
Intent created with tor.id
    ↓
TorDetailActivity.onCreate()
    ↓
TorRepository.getTorById(id)
    ↓
Detail view populated with data
```

## File Structure

```
com.dartmoor.tors/
├── MainActivity.kt              # List screen
├── TorDetailActivity.kt         # Detail screen
├── TorAdapter.kt                # RecyclerView adapter
└── Tor.kt                       # Data model & repository

res/
├── layout/
│   ├── activity_main.xml        # Main screen layout
│   ├── activity_tor_detail.xml  # Detail screen layout
│   └── item_tor.xml             # List item layout
├── values/
│   ├── strings.xml              # String resources
│   ├── colors.xml               # Color definitions
│   └── themes.xml               # App theme
├── drawable/
│   └── ic_launcher_foreground.xml  # App icon
└── mipmap-*/
    └── ic_launcher.xml          # Adaptive icons
```

## Dependencies

### Core Libraries

```kotlin
// AndroidX Core
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")

// Material Design
implementation("com.google.android.material:material:1.11.0")

// Layouts
implementation("androidx.constraintlayout:constraintlayout:2.1.4")
implementation("androidx.recyclerview:recyclerview:1.3.2")
implementation("androidx.cardview:cardview:1.0.0")
```

## Future Architecture Improvements

For a production app, consider:

### MVVM Pattern

```
View (Activity/Fragment)
    ↓
ViewModel (LiveData/StateFlow)
    ↓
Repository
    ↓
Data Source (API/Database)
```

### Dependency Injection

- Hilt or Koin for DI
- Better testability
- Cleaner dependencies

### Navigation Component

```kotlin
// Replace Intent-based navigation with:
findNavController().navigate(
    R.id.action_list_to_detail,
    bundleOf("torId" to tor.id)
)
```

### Room Database

```kotlin
@Entity(tableName = "tors")
data class Tor(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name") val name: String,
    // ...
)

@Dao
interface TorDao {
    @Query("SELECT * FROM tors")
    fun getAllTors(): Flow<List<Tor>>
}
```

### Retrofit for API

```kotlin
interface TorApi {
    @GET("tors")
    suspend fun getAllTors(): List<Tor>
    
    @GET("tors/{id}")
    suspend fun getTorById(@Path("id") id: Int): Tor
}
```

### Coroutines for Async

```kotlin
viewModelScope.launch {
    val tors = withContext(Dispatchers.IO) {
        repository.getAllTors()
    }
    _torsLiveData.value = tors
}
```

### Testing

```kotlin
// Unit Tests
class TorRepositoryTest {
    @Test
    fun `getAllTors returns all tors`() {
        val tors = TorRepository.getAllTors()
        assertEquals(10, tors.size)
    }
}

// UI Tests
@Test
fun clickingTorOpensDetailScreen() {
    onView(withText("High Willhays"))
        .perform(click())
    onView(withId(R.id.detail_name))
        .check(matches(withText("High Willhays")))
}
```

## Performance Considerations

### Current Implementation

✅ RecyclerView for efficient list rendering
✅ ViewHolder pattern for view reuse
✅ Minimal dependencies
✅ No network calls (instant data loading)

### Future Optimizations

- Image loading with Coil or Glide
- Pagination for large datasets
- Caching strategies
- Background data sync
- Memory optimization

## Security Considerations

### Current State

- No sensitive data stored
- No network communication
- No user authentication
- Read-only data

### Future Requirements

- API authentication tokens
- Secure local storage (EncryptedSharedPreferences)
- Network security config
- ProGuard obfuscation
- Certificate pinning for API calls

## Accessibility

The app includes basic accessibility features:

- Content descriptions for images
- Proper heading hierarchy
- Touch target sizes (48dp minimum)
- Material Design's accessibility guidelines

Future improvements:
- TalkBack optimization
- Screen reader support
- Voice commands
- High contrast mode
- Font scaling support

## Localization

Current state:
- All strings in English
- String resources in `strings.xml`

Future support:
- Multiple language translations
- Right-to-left (RTL) layout support
- Locale-specific formatting (dates, numbers)
- Region-specific content

## Error Handling

Current implementation:
- Basic null checks
- Activity finish on missing data

Future improvements:
- Try-catch blocks for API calls
- User-friendly error messages
- Retry mechanisms
- Offline mode handling
- Analytics for error tracking

## Conclusion

This POC demonstrates a clean, simple architecture suitable for a basic Android app. It can be extended with modern Android development practices (MVVM, Coroutines, Room, Retrofit) for a production-ready application.
