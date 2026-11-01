# Kalaban

Single-module Android app (`com.kalaban.farshidb2011`) built with Jetpack Compose and Material 3.

## Prerequisites

- **Java 17** on `PATH`, with `JAVA_HOME` set  
  This machine uses Eclipse Temurin at `~/.jdks/current` (wired in `~/.bashrc`).
- **Android SDK** with platform **34** and build-tools  
  Project SDK path is set in `local.properties`:

  ```properties
  sdk.dir=/home/farshid/Apps/Android/Sdk
  ```

  Adjust `sdk.dir` if your SDK lives elsewhere. Do not commit machine-specific secrets; `local.properties` is gitignored.
- Device or emulator with `adb` available (`$ANDROID_HOME/platform-tools`).

## Build and run

From the project root:

```bash
# Debug APK
make build
# or
./gradlew assembleDebug

# Install on a connected device
make install

# Build, install, and launch MainActivity
make run

# Release APK (minified; signed with debug-keystore placeholder)
make release

# Play Store bundle
make bundle

# Unit tests
./gradlew test

# Clean
make clean
```

Other helpers: `make devices`, `make logs`, `make uninstall`, `make info`.

## Project layout

- App module: `app/`
- Package: `com.kalaban.farshidb2011`
- Version catalog: `gradle/libs.versions.toml` (AGP 8.2.2, Kotlin 1.9.22, Compose BOM 2024.01.00, Material3 1.2.1)
- Gradle wrapper: 8.5

## Notes

- Release signing currently reuses the local debug keystore as a placeholder. Replace `signingConfigs.release` in `app/build.gradle.kts` before publishing.
- `new-project.sh` can clone/rename this template for a new package name.
