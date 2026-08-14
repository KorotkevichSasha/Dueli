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
| Microphone input / speech recognition | User-initiated pronunciation exercises | Verify final speech provider and whether audio leaves the device | Must match final provider behaviour |
| Technical logs and security events | Reliability, fraud prevention and security | Yes | Define production retention period before release |

## Current safeguards

- TLS is mandatory in release builds.
- Authentication tokens are excluded from backup and stored using Android Keystore-backed storage.
- Users can delete their account inside the app.
- Public privacy and account-deletion pages are included in the web project.
- User photos require a content-policy confirmation, are validated and screened on the server, and can be reported.
- Users can block other users; moderation reports are available in the admin panel.

## Values that must be final before Play Console submission

- Production support email and operator/legal name.
- Public HTTPS privacy-policy and account-deletion URLs.
- Production hosting regions, processors and log/backup retention periods.
- Exact speech-recognition provider behaviour and audio retention.
- Whether analytics, crash reporting, advertising or any additional SDK is enabled in the release build.
