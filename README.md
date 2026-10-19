# Photosense

An Android app for searching your photos with natural-language queries.
Photo embeddings are generated on-device, so the app works offline entirely.

## Requirements

- Android Studio
- Android SDK 34
- A device or emulator running Android 8.0 (API 26) or newer

## Model setup

The model binaries are intentionally not stored in GitHub. 

```text
app/src/main/assets/models/clip-image-int8.ort
app/src/main/assets/models/clip-text-int8.ort
```

The tokenizer files are already included in `app/src/main/assets/tokenizer/`.

## Run

1. Open the project in Android Studio.
2. Allow Gradle to sync and download dependencies.
3. Build and run the app on a device or emulator.
4. Grant photo access.
5. Choose **Index Album**, then search from the home screen.

Indexing and search run locally on the device. The app does not upload photos
or search queries.

## Release a signed APK

Create a private release keystore once:

```powershell
keytool -genkeypair -v -keystore clipgallery-release.jks -alias clipgallery -keyalg RSA -keysize 2048 -validity 10000
```

Set these environment variables before building:

```text
RELEASE_KEYSTORE_FILE
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

Then run:

```powershell
.\gradlew.bat assembleRelease
```

The signed APK is generated at `app/build/outputs/apk/release/app-release.apk`.
Never commit the keystore or passwords. Upload the APK through a GitHub Release.

## Project structure

- `app/src/main/java/.../MainActivity.kt`: photo search screen
- `app/src/main/java/.../IndexAlbumsActivity.kt`: album indexing
- `app/src/main/java/.../ml/`: tokenization, image processing, and embeddings
- `app/src/main/java/.../data/`: Room database models and access
