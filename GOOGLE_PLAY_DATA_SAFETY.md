# DuelRush — Data safety working sheet

Use this sheet when completing Play Console. Recheck it against the deployed production build and every enabled third-party service before submitting.

## Data handled by the current product

| Data category | Purpose | Stored on server | User deletion |
|---|---|---:|---:|
| Email address and username | Account management, authentication, friend search | Yes | Deleted with account |
| Password-derived authentication data | Authentication and security | Yes; password itself is not stored | Deleted with account |
| Profile photo | Account customisation and social features | Yes, when uploaded | Replaced or deleted with account |
| Learning progress, vocabulary, test results | Core learning functionality and personalisation | Yes | Deleted with account |
| Friends, blocks and reports | Social functionality, abuse prevention and moderation | Yes | Account-linked data is deleted; confirm moderation retention policy before release |
| Duel answers, score and history | Multiplayer functionality, scoring and mistake review | Yes | Deleted with account |
| Microphone input / speech recognition | User-initiated pronunciation exercises | DuelRush does not upload or store audio; the Android speech-recognition service selected on the device may process it under that provider's terms | DuelRush receives only recognised text and does not retain an audio recording |
| Technical logs and security events | Reliability, fraud prevention and security | Yes, for a limited operational and security period | Not part of the user profile; retained only as needed for security, abuse prevention and legal obligations |

## Current safeguards

- TLS is mandatory in release builds.
- Authentication tokens are excluded from backup and stored using Android Keystore-backed storage.
- Users can delete their account inside the app.
- Public privacy and account-deletion pages are included in the web project.
- User photos require a content-policy confirmation, are validated and screened on the server, and can be reported.
- Users can block other users; moderation reports are available in the admin panel.

## Verified release-build facts (2026-08-27)

- Production support email: `duelrush.app@gmail.com`.
- Privacy policy: `https://duelrush-admin-2026.onrender.com/privacy`.
- Account deletion: `https://duelrush-admin-2026.onrender.com/delete-account`.
- API is hosted on Render; application data uses the production relational database and MongoDB Atlas; verification mail is sent through Gmail.
- Voice input uses Android `SpeechRecognizer`. The DuelRush API does not receive an audio file.
- The 1.0.0 release contains no advertising, analytics or crash-reporting SDK and does not request the advertising ID.

## Values the Play account owner must finalise

- Truthful legal operator name matching the verified Play developer account.
- Production retention periods and hosting regions for the public policy before production rollout.
- Recheck this form if advertising, analytics, crash reporting, payments or any new SDK is added.
