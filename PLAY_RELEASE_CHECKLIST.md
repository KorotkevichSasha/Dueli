# Google Play release checklist

## Product decisions that must be completed before the first upload

- Confirm final trademark and store-name availability for `DuelRush` before public release.
- Select the permanent Android application id. Google Play package names cannot be changed after the first artifact is uploaded; do not publish `com.example.duelingo`.
- Register the production domain, support email, and privacy-policy URL.
- Publish a working web page where a user can request account deletion. The app already provides in-app deletion under Profile settings, and the backend permanently deletes linked data.
- Replace or document the provenance and license of every remaining raster illustration and Lottie animation. The launcher icon and achievement icons no longer rely on remote third-party artwork, but repository history is not proof of rights for the other assets.

## Build and signing

- Enrol in Play App Signing and create a separate upload key.
- Store `upload-key.jks` and `keystore.properties` outside Git with encrypted backups.
- Increment `VERSION_CODE` for every upload and use a user-facing semantic `VERSION_NAME`.
- Build an Android App Bundle (`bundleRelease`) against the production HTTPS API.
- Upload first to Internal testing, then Closed testing, and only then Production.

## Play Console declarations

- Complete App content, Data safety, content rating, target audience, ads, and app-access declarations truthfully.
- Declare microphone use for pronunciation/listening features and explain it in the privacy policy.
- Add store listing text, screenshots for required device sizes, feature graphic, high-resolution icon, support contact, and release notes.
- Test account deletion, data retention, and the public deletion-request link from a signed release build.

## Current technical baseline (verified 2026-08-07)

- `compileSdk` and `targetSdk`: 36 (Android 16).
- Release shrinking/obfuscation: enabled.
- Cleartext traffic: disabled in release; enabled only in the debug manifest for the emulator.
- Backups: disabled; authentication tokens are encrypted with Android Keystore.
- CI runs unit tests, Android lint, and creates a debug APK on every branch/PR.
- Public `/privacy` and `/delete-account` pages are implemented in the web project; configure `VITE_SUPPORT_EMAIL` and `VITE_OPERATOR_NAME` before deployment.
- UGC controls now include upload-policy acceptance, image screening, in-app reporting, blocking, and an admin report queue.
- `GOOGLE_PLAY_DATA_SAFETY.md` inventories the current data flows for the Play Console declaration.

## Remaining external release inputs

- Permanent application id chosen by the account owner.
- Production HTTPS API and public policy URLs.
- Legal operator name and monitored support email.
- Upload keystore created and backed up by the account owner.
- Final test of the signed AAB through Play Internal testing and the Play pre-launch report.

Official references: Android target API requirements, Android App Bundle/signing documentation, and the Google Play account-deletion policy should be rechecked immediately before submission because policy dates change.
