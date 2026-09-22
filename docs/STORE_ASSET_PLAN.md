# Project Blue: Ocean Guard store asset plan

## Release positioning

Store art should show ocean restoration, readable submarine silhouettes and the shift from polluted to living water. It must use Project Blue: Ocean Guard's own recovery-console identity and original world designs. Do not reference or visually imitate Sky Force or another game.

## Required deliverables

| Deliverable | Specification | Status |
|---|---|---|
| Master app icon | 1024 by 1024 PNG, no transparency, simple original submarine/sonar mark, readable at 48 px | Placeholder specification only |
| Google Play icon export | 512 by 512 PNG, maximum 1024 KB; separate export from the master | Ready at `store/assets/google-play/ocean-guard/app-icon-512.png` |
| Android adaptive icon | 432 by 432 foreground safe within the central 264 px; separate solid/gradient background; monochrome vector | Missing; legacy `ic_blue.xml` is a prototype |
| Android splash | Android 12 SplashScreen-compatible centered mark, deep-ink background, no small text | Missing; use the icon mark until approved |
| Feature graphic | 1024 by 500 JPEG or 24-bit PNG without alpha, no store badges, minimal copy | Ready at `store/assets/google-play/ocean-guard/feature-graphic-1024x500.png` |
| Phone screenshots | At least six portrait captures, recommended 1080 by 1920 or higher | Seven 1080 by 1920 exports ready under `store/assets/google-play/ocean-guard/screenshots/` |
| Short description | One sentence about restoring ten ocean sectors | Copy review required |
| Full description | Original feature list, accessibility settings, offline/privacy behavior and supported devices | Copy review required |
| Privacy policy location | Public policy page matching the in-app Privacy screen before any public release | Missing external page |

## Screenshot sequence

Current copy, owner inputs and source links are in [STORE_LISTING_DATA.md](STORE_LISTING_DATA.md).
See [FINAL_TEST_REPORT.md](FINAL_TEST_REPORT.md) for actual verification status.

1. Active recovery gameplay with cleanup beam and color returning.
2. A boss telegraph showing geometric warning cues.
3. Ten-sector Level Select with stars and difficulty state.
4. Hangar submarine comparison.
5. Pilot passive and upgrade states.
6. Result screen with stars and before/after restoration.
7. Optional accessibility settings capture.

Capture without debug overlays at representative tall and standard phone aspect ratios. Keep store text outside gameplay-critical areas. Localize captions as separate store artwork rather than baking text into reusable game art.

## Acceptance and licensing

Every visual must have a layered or reproducible source, export settings, creator, source, license, project path and usage note. Add package-bound files to `assets/licenses/ASSET_LICENSES.md` and the allowlist only after rights review. Store-only exports should be inventoried there as store material even when they are not packaged in the APK.

The current procedural game captures, Blue Grid font, three synthesized audio files, loading treatment and launcher vector remain placeholders. No generated store artwork was added in this pass because the established game identity is code-native and production character/environment designs have not yet been approved.
