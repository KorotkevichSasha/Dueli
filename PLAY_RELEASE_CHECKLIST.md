# Google Play release checklist

## Product decisions that must be completed before the first upload

- Confirm final trademark and store-name availability for `DuelRush` before public release.
- Permanent Android application id selected: `com.duelrush.app`. Google Play package names cannot be changed after the first artifact is uploaded.
- Register the production domain, support email, and privacy-policy URL.
- Publish a working web page where a user can request account deletion. The app already provides in-app deletion under Profile settings, and the backend permanently deletes linked data.
- Keep `play-store/ASSET_PROVENANCE.md` with the release records and retain the original source files for the generated artwork.

## Build and signing

- Enrol in Play App Signing when the Play Console application is created.
- A separate upload key and local `keystore.properties` have been created outside Git. The account owner must still make an encrypted off-device backup.
- Increment `VERSION_CODE` for every upload and use a user-facing semantic `VERSION_NAME`.
- Build an Android App Bundle (`bundleRelease`) against the production HTTPS API.
- Upload first to Internal testing, then Closed testing, and only then Production.

## Play Console declarations

- Complete App content, Data safety, content rating, target audience, ads, and app-access declarations truthfully.
- Declare microphone use for pronunciation/listening features and explain it in the privacy policy.
- Store listing text, feature graphic, high-resolution icon, support contact, and release notes are prepared under `play-store/`.
- The screenshots currently under `play-store/screenshots/` are draft captures from an older interface and must not be uploaded. Capture the signed 1.0.0 build after the Play test account is created.
- Test account deletion, data retention, and the public deletion-request link from a signed release build.

## Current technical baseline (verified 2026-08-27)

- `compileSdk` and `targetSdk`: 36 (Android 16).
- Release shrinking/obfuscation: enabled.
- Cleartext traffic: disabled in release; enabled only in the debug manifest for the emulator.
- Backups: disabled; authentication tokens are encrypted with Android Keystore.
- CI runs unit tests, Android lint, and creates a debug APK on every branch/PR.
- Public `/privacy` and `/delete-account` pages are implemented in the web project; configure `VITE_SUPPORT_EMAIL` and `VITE_OPERATOR_NAME` before deployment.
- UGC controls now include upload-policy acceptance, image screening, in-app reporting, blocking, and an admin report queue.
- `GOOGLE_PLAY_DATA_SAFETY.md` inventories the current data flows for the Play Console declaration.

## Remaining external release inputs

- Permanent application id: `com.duelrush.app`.
- Production API: `https://duelrush-api-2026.onrender.com`.
- Public privacy page: `https://duelrush-admin-2026.onrender.com/privacy`.
- Public deletion page: `https://duelrush-admin-2026.onrender.com/delete-account`.
- Truthful legal operator name matching the verified Play developer account. Support email is `duelrush.app@gmail.com`.
- Copy the generated upload key folder to encrypted off-device storage controlled by the account owner.
- Final test of the signed AAB through Play Internal testing and the Play pre-launch report.

Official references: Android target API requirements, Android App Bundle/signing documentation, and the Google Play account-deletion policy should be rechecked immediately before submission because policy dates change.
