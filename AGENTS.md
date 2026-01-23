# KeepSafe Agent Instructions

## Project Overview
KeepSafe is a Kotlin Android application that provides DNS-based content filtering using VPN technology. It uses OpenDNS Family Shield to block inappropriate content and includes device administration features for parental control.

## Build Commands

### Gradle Tasks
```bash
# Build the project
./gradlew build

# Clean build
./gradlew clean build

# Build specific variant
./gradlew assembleDebug
./gradlew assembleRelease

# Install on device/emulator
./gradlew installDebug
./gradlew installRelease
```

### Testing Commands
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "*TestClassName*"

# Run tests for specific package
./gradlew test --tests "com.macasteglione.keepsafe.*"

# Run single test method
./gradlew test --tests "*TestClassName.testMethodName"

# Run instrumented tests
./gradlew connectedAndroidTest

# Run instrumented tests on specific device
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="com.macasteglione.keepsafe.ExampleInstrumentedTest"
```

### Lint and Code Quality
```bash
# Run Android Lint
./gradlew lint

# Run lint on specific variant
./gradlew lintDebug
./gradlew lintRelease

# Check code quality
./gradlew check

# Generate lint report
./gradlew lint | cat
```

### Code Formatting
```bash
# Format Kotlin code (if ktlint is configured)
./gradlew ktlintFormat

# Check Kotlin formatting
./gradlew ktlintCheck
```

## Code Style Guidelines

### Kotlin/Android Conventions

#### Naming Conventions
- **Classes/Objects**: PascalCase (e.g., `MainActivity`, `PasswordManager`)
- **Functions/Methods**: camelCase (e.g., `validatePassword()`, `startVpnService()`)
- **Variables/Properties**: camelCase (e.g., `vpnRunningState`, `enteredPassword`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `PRIMARY_DNS`, `VPN_ADDRESS`)
- **Package names**: lowercase with dots (e.g., `com.macasteglione.keepsafe`)

#### File Organization
```
app/src/main/java/com/macasteglione/keepsafe/
├── ui/           # UI components and activities
├── service/      # Background services (VPN service)
├── receiver/     # Broadcast receivers
├── data/         # Data management and storage
├── core/         # Core business logic
│   ├── dns/      # DNS configuration
│   └── network/  # Network monitoring
└── admin/        # Device administration
```

#### Imports
- Group imports by package hierarchy
- Use wildcard imports sparingly (only for closely related classes)
- Prefer explicit imports over wildcard imports
- Order: Android SDK, third-party libraries, project imports

```kotlin
import android.content.Context
import androidx.compose.runtime.*
import com.macasteglione.keepsafe.data.PasswordManager
import com.macasteglione.keepsafe.service.DnsVpnService
```

#### Class Structure
- Properties first, then companion objects, then methods
- Group related methods together
- Use meaningful access modifiers (prefer `private` when possible)
- Document public APIs with KDoc

#### Error Handling
- Use try-catch blocks for expected exceptions
- Log errors with appropriate levels (ERROR for failures, WARN for issues, DEBUG for info)
- Provide meaningful error messages to users
- Handle null safety properly with `?.`, `?:`, and `!!` only when necessary

#### Logging
- Use descriptive log tags (class names)
- Include context in log messages
- Use appropriate log levels:
  - `Log.d()` for debug information
  - `Log.i()` for important events
  - `Log.w()` for warnings
  - `Log.e()` for errors

```kotlin
private const val TAG = "MainActivity"

Log.d(TAG, "VPN state changed: $isActive")
```

### Jetpack Compose Guidelines

#### State Management
- Use `remember` for composable-local state
- Use `mutableStateOf()` for mutable state
- Prefer `LaunchedEffect` for side effects
- Use meaningful state variable names

```kotlin
var enteredPassword by remember { mutableStateOf("") }
var isLoading by remember { mutableStateOf(false) }
```

#### Composables
- Use descriptive names ending with component type (e.g., `PasswordValidationScreen`)
- Accept parameters for data, not for behavior when possible
- Use `Modifier` parameter for customization
- Provide default values for optional parameters

#### Layout
- Use appropriate layout components (`Column`, `Row`, `Box`)
- Apply padding and spacing consistently (16.dp, 8.dp, 24.dp)
- Use `fillMaxWidth()` and `fillMaxHeight()` appropriately
- Center content when appropriate

### Security Best Practices

#### Password Handling
- Never log passwords or sensitive data
- Use encrypted storage (Tink library as implemented)
- Validate passwords on device side
- Clear sensitive data from memory when not needed

#### Permissions
- Request minimal necessary permissions
- Explain permission usage to users
- Handle permission denials gracefully

### Testing Guidelines

#### Unit Tests
- Test business logic in data and core classes
- Mock dependencies using Mockito or similar
- Test edge cases and error conditions
- Use descriptive test method names

#### Instrumented Tests
- Test UI interactions with Espresso
- Test service behavior
- Use test-specific manifests when needed

### Performance Considerations

#### Memory Management
- Avoid memory leaks in services and receivers
- Clean up resources in `onDestroy()` methods
- Use appropriate data structures for collections

#### Network Operations
- Perform network operations off main thread
- Use appropriate timeouts
- Handle network failures gracefully

### Code Comments and Documentation

#### KDoc Comments
- Document all public classes, functions, and properties
- Use `@param` for parameters, `@return` for return values
- Provide usage examples when helpful

```kotlin
/**
 * Validates the user password against stored encrypted password.
 *
 * @param context Android context for data store access
 * @param password Plain text password to validate
 * @return true if password is correct, false otherwise
 */
fun validatePassword(context: Context, password: String): Boolean
```

#### Inline Comments
- Explain complex logic or algorithms
- Comment workarounds for known issues
- Avoid obvious comments

### Commit Message Guidelines

Follow conventional commit format:
```
type(scope): description

[optional body]

[optional footer]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Test additions/modifications
- `chore`: Maintenance tasks

Examples:
```
feat(ui): add password validation dialog
fix(vpn): handle network reconnection properly
refactor(data): improve password encryption logic
```

### Architecture Patterns

#### MVVM Pattern
- **Model**: Data classes and business logic (`PasswordManager`, `VpnStateManager`)
- **View**: Composables and Activities (`MainActivity`, `PasswordValidationActivity`)
- **ViewModel**: State management (not currently implemented, consider adding)

#### Service Layer
- Use Android Services for background tasks (VPN service)
- Implement proper lifecycle management
- Handle service restarts appropriately

### Dependency Injection
- Consider using Hilt for dependency injection in larger projects
- Currently using manual dependency injection
- Keep dependencies loosely coupled

### Constants and Configuration
- Define constants in companion objects or separate files
- Use build config for environment-specific values
- Centralize DNS and VPN configuration in `DnsConfiguration`

### Error Messages
- Use string resources for user-facing messages
- Provide clear, actionable error messages
- Include appropriate icons and colors for different message types

### Accessibility
- Provide content descriptions for images
- Use appropriate text sizes and contrast ratios
- Support screen readers with proper labeling

## Common Issues and Solutions

### Java Version Compatibility
- Project requires Java 11 (specified in `build.gradle.kts`)
- Ensure JAVA_HOME points to Java 11 for builds
- Use `./gradlew --version` to verify Gradle setup

### VPN Permission Handling
- Handle `VpnService.prepare()` result properly
- Request VPN permission before starting service
- Check permission status before operations

### Device Admin Management
- Request admin privileges when needed
- Handle admin deactivation gracefully
- Explain permission requirements to users

### Network State Monitoring
- Register network callbacks properly
- Handle network changes without causing loops
- Clean up callbacks on service destruction

## Development Workflow

1. Create feature branch from `main`
2. Implement changes with tests
3. Run `./gradlew check` to verify code quality
4. Test on device/emulator
5. Submit pull request with detailed description
6. Code review and merge

## Project-Specific Notes

- **VPN Technology**: Uses Android VpnService for DNS filtering
- **Security**: Implements device admin restrictions and encrypted password storage
- **UI Framework**: Jetpack Compose for modern Android UI
- **Data Storage**: DataStore for preferences, SharedPreferences for state
- **Encryption**: Google Tink library for secure password storage
- **Network Monitoring**: Automatic VPN reconnection on network changes
- **Boot Persistence**: VPN auto-starts after device boot and app updates

## Cursor Rules Integration
No Cursor rules (.cursor/rules/) or Copilot rules (.github/copilot-instructions.md) found in this repository.