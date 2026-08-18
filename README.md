# Android application

## Development

The debug build connects to `http://127.0.0.1:8082`. For a USB device or emulator, expose the local backend first with `adb reverse tcp:8082 tcp:8082`. Run:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Release bundle

Choose the permanent product name and package id before the first Play upload. Then copy `keystore.properties.example` to `keystore.properties`, point it at the upload key, and run:

```text
./gradlew bundleRelease -PAPI_BASE_URL=https://api.your-domain.example -PAPPLICATION_ID=com.yourcompany.product -PPRIVACY_POLICY_URL=https://your-domain.example/privacy -PVERSION_CODE=1 -PVERSION_NAME=1.0.0
```

The build intentionally refuses a release that uses HTTP, `api.example.com`, `com.example.*`, or has no signing configuration. Keep the upload key and its passwords outside Git and in an encrypted backup.

See `PLAY_RELEASE_CHECKLIST.md` before publishing.
