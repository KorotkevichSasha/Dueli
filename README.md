# Android application

## Development

The debug build connects to `http://10.0.2.2:8082`, which is the host machine from the Android emulator. Run:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Release bundle

Choose the permanent product name and package id before the first Play upload. Then copy `keystore.properties.example` to `keystore.properties`, point it at the upload key, and run:

```text
./gradlew bundleRelease -PAPI_BASE_URL=https://api.your-domain.example -PAPPLICATION_ID=com.yourcompany.product -PVERSION_CODE=1 -PVERSION_NAME=1.0.0
```

The build intentionally refuses a release that uses HTTP, `api.example.com`, `com.example.*`, or has no signing configuration. Keep the upload key and its passwords outside Git and in an encrypted backup.

See `PLAY_RELEASE_CHECKLIST.md` before publishing.
