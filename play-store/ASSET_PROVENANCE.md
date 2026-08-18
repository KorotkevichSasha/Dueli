# Asset provenance

## Google Play feature graphic

- Files: `feature-graphic-source.png`, `feature-graphic-1024x500.png`
- Created: 2026-08-04
- Tool: OpenAI image generation (`image_gen`)
- Purpose: original Google Play feature graphic for this application
- External logos, brands, characters, screenshots, flags, and text: none requested or used
- Prompt summary: two original abstract speech-bubble characters racing around an open book; premium 3D editorial style; navy, cyan, blue, and gold palette; no text, trademarks, copyrighted characters, owls, green bird mascots, swords, violence, or watermark.

## Google Play app icon

- File: `app-icon-512.png`
- Created: 2026-08-04
- Method: original geometric artwork drawn specifically for this project from simple shapes
- External source assets: none

## In-app icons and animations

- Navigation, microphone, playback, status, and launcher graphics under `app/src/main/res/drawable` are project-authored vector XML files.
- Navigation pulse animations under `app/src/main/assets` are project-authored Lottie JSON files.
- The previous raster icon set and legacy launcher WebP files were removed because their source and license could not be verified.

## In-app avatars and duel illustration

- Files: `app/src/main/res/drawable-nodpi/avatar_01.png` through `avatar_10.png`, and `duel_hero.png`.
- Added to the project: 2026-08-13 in commit `a81effa82ef8a55622698f096d22528c545acf16`.
- Method: original AI-generated artwork commissioned specifically for DuelRush during product development; the duel illustration is the same original source artwork used for the Play feature graphic.
- External source assets, logos, brands, copyrighted characters, screenshots, and watermarks: none requested or incorporated.
- Purpose: bundled default profile choices and the duel landing-page hero. The files are stored locally and do not depend on a third-party asset service at runtime.

Before publishing, keep this file with the release records and run a final trademark review after the permanent product name is selected.
