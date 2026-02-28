# Contributing to Dartmoor Tors Android App

Thank you for your interest in contributing to the Dartmoor Tors Android app! This guide will help you get started.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [How to Contribute](#how-to-contribute)
- [Code Guidelines](#code-guidelines)
- [Testing](#testing)
- [Pull Request Process](#pull-request-process)

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Dartmoor-tors-android.git
   cd Dartmoor-tors-android
   ```
3. **Create a branch** for your feature:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Development Setup

### Prerequisites

- JDK 17 or later
- Android Studio (latest stable version)
- Android SDK API Level 34
- Git

### Initial Setup

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Run the app to ensure everything works:
   ```bash
   ./gradlew assembleDebug
   ```

For detailed setup instructions, see [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md).

## Project Structure

```
Dartmoor-tors-android/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/dartmoor/tors/
│   │       │   ├── MainActivity.kt
│   │       │   ├── TorDetailActivity.kt
│   │       │   ├── TorAdapter.kt
│   │       │   └── Tor.kt
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   ├── values/
│   │       │   └── drawable/
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

For more details, see [ARCHITECTURE.md](ARCHITECTURE.md).

## How to Contribute

### Types of Contributions

We welcome the following contributions:

1. **Bug fixes**
2. **New features**
3. **Documentation improvements**
4. **Code refactoring**
5. **Performance improvements**
6. **UI/UX enhancements**
7. **Tests**

### Finding Something to Work On

- Check the [Issues](https://github.com/campbellssource/Dartmoor-tors-android/issues) page
- Look for issues labeled `good first issue` or `help wanted`
- Propose new features by opening an issue first

### Before You Start

1. **Check existing issues** to avoid duplicate work
2. **Discuss major changes** by opening an issue first
3. **Keep changes focused** - one feature per PR
4. **Follow coding standards** (see below)

## Code Guidelines

### Kotlin Style

Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

```kotlin
// Good
class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupRecyclerView()
    }
    
    private fun setupRecyclerView() {
        // Implementation
    }
}

// Bad
class MainActivity:AppCompatActivity(){
    lateinit var recyclerView:RecyclerView
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        SetupRecyclerView()
    }
}
```

### Naming Conventions

- **Classes**: PascalCase (`MainActivity`, `TorAdapter`)
- **Functions**: camelCase (`getAllTors()`, `setupRecyclerView()`)
- **Constants**: UPPER_SNAKE_CASE (`EXTRA_TOR_ID`, `MAX_HEIGHT`)
- **Variables**: camelCase (`torList`, `heightMeters`)
- **Resources**: snake_case (`activity_main`, `tor_name`)

### XML Layout Guidelines

```xml
<!-- Good: Properly formatted -->
<TextView
    android:id="@+id/tor_name"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:textSize="18sp"
    android:textStyle="bold" />

<!-- Bad: Inconsistent formatting -->
<TextView android:id="@+id/tor_name"
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:textSize="18sp" android:textStyle="bold"/>
```

### Code Organization

1. **Group related code** together
2. **Use meaningful names** for variables and functions
3. **Keep functions small** (ideally < 20 lines)
4. **Add comments** for complex logic
5. **Remove unused code** and imports

### Dependencies

When adding new dependencies:

1. **Check for existing alternatives** in the project
2. **Use the latest stable version**
3. **Update `build.gradle.kts`**
4. **Document why** the dependency is needed

Example:
```kotlin
dependencies {
    // Image loading library for tor photos
    implementation("io.coil-kt:coil:2.4.0")
}
```

## Testing

### Manual Testing

Before submitting a PR:

1. **Build the app**: `./gradlew assembleDebug`
2. **Install on device**: `./gradlew installDebug`
3. **Test your changes** thoroughly
4. **Test edge cases**:
   - Empty states
   - Orientation changes
   - Different screen sizes
   - Dark mode

### Automated Testing

When adding tests:

```kotlin
// Unit test example
class TorRepositoryTest {
    @Test
    fun `getAllTors should return 10 tors`() {
        val tors = TorRepository.getAllTors()
        assertEquals(10, tors.size)
    }
}

// UI test example
@Test
fun clickTorOpenDetailScreen() {
    onView(withId(R.id.tors_recycler_view))
        .perform(RecyclerViewActions.actionOnItemAtPosition<TorAdapter.TorViewHolder>(0, click()))
    onView(withId(R.id.detail_name))
        .check(matches(isDisplayed()))
}
```

Run tests:
```bash
./gradlew test              # Unit tests
./gradlew connectedAndroidTest  # UI tests
```

### Code Quality

Run linting before submitting:
```bash
./gradlew lint
```

Fix any warnings or errors reported.

## Pull Request Process

### 1. Update Your Branch

Keep your branch up to date with main:

```bash
git fetch origin
git rebase origin/main
```

### 2. Commit Your Changes

Write clear, descriptive commit messages:

```bash
# Good
git commit -m "Add search functionality to tor list"
git commit -m "Fix crash when tor description is null"

# Bad
git commit -m "update"
git commit -m "fixes"
```

Follow [Conventional Commits](https://www.conventionalcommits.org/):
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation only
- `style:` Code style changes
- `refactor:` Code refactoring
- `test:` Adding tests
- `chore:` Maintenance tasks

### 3. Push Your Changes

```bash
git push origin feature/your-feature-name
```

### 4. Create Pull Request

1. Go to the repository on GitHub
2. Click "New Pull Request"
3. Select your branch
4. Fill in the PR template:

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
How did you test this?

## Screenshots (if applicable)
Add screenshots for UI changes

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No new warnings
- [ ] Tests added/updated
- [ ] All tests passing
```

### 5. Code Review

- Address reviewer feedback
- Update your branch as needed
- Be patient and respectful

### 6. Merge

Once approved, your PR will be merged!

## Feature Ideas

Here are some features you could contribute:

### Easy (Good First Issues)

- [ ] Add more tors to the dataset
- [ ] Improve tor descriptions
- [ ] Add string resources for localization
- [ ] Create app icon variations
- [ ] Add about screen
- [ ] Sort tors by name/height

### Medium

- [ ] Add search functionality
- [ ] Implement favorites feature
- [ ] Add filtering options
- [ ] Create settings screen
- [ ] Add map view
- [ ] Implement dark mode refinements
- [ ] Add tor images

### Advanced

- [ ] Integrate with real API
- [ ] Add Room database
- [ ] Implement offline mode
- [ ] Add location-based features
- [ ] AR tor identification
- [ ] Route planning
- [ ] Social features (reviews, ratings)
- [ ] Weather integration

## Resources

### Android Development

- [Android Developer Guides](https://developer.android.com/guide)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Material Design Guidelines](https://material.io/design)

### Dartmoor Information

- [Dartmoor National Park](https://www.dartmoor.gov.uk/)
- [Wikipedia: List of Dartmoor Tors](https://en.wikipedia.org/wiki/List_of_Dartmoor_tors_and_hills)
- [Ordnance Survey Maps](https://www.ordnancesurvey.co.uk/)

## Code of Conduct

### Our Standards

- Be respectful and inclusive
- Welcome newcomers
- Accept constructive criticism
- Focus on what's best for the project
- Show empathy towards others

### Unacceptable Behavior

- Harassment or discrimination
- Trolling or insulting comments
- Personal or political attacks
- Publishing others' private information
- Unprofessional conduct

## Getting Help

If you need help:

1. **Check documentation**: README, BUILD_INSTRUCTIONS, ARCHITECTURE
2. **Search issues**: Someone may have had the same question
3. **Ask questions**: Open an issue with the `question` label
4. **Join discussions**: Participate in GitHub Discussions

## Recognition

Contributors will be:
- Listed in CONTRIBUTORS.md
- Mentioned in release notes
- Thanked in the README

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (see LICENSE file).

## Thank You!

Thank you for contributing to the Dartmoor Tors Android app. Every contribution, no matter how small, makes a difference!

Happy coding! 🏔️📱
