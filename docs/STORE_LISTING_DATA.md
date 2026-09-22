# Store listing data

Status: **DRAFT / NOT READY TO PUBLISH**, 2026-09-19. Copy reflects implemented systems;
actual mobile acceptance remains pending. No store entry was submitted.

## Known build metadata

| Field | Value / status |
|---|---|
| App title | Project Blue: Ocean Guard |
| Application ID | `com.game.diver.oceanguard`; final release identity |
| Audited artifact version | `0.1.16-g07f65ef45ace-dirty` / code `16`; not confirmed available in Play |
| Platform | Android, minimum API 26, target API 36 |
| Orientation | Portrait gameplay; large-screen behavior requires device verification |
| Proposed category | Game / Action; owner review pending |
| In-game UI languages | English and Turkish; explicit first-launch choice and in-game switching |
| Player mode | Single player; local progression, no account/sign-in |
| Connectivity | Core game designed for offline play; optional ads/consent need connectivity when enabled |
| Purchases | No in-app billing integration; upgrades use earned salvage |
| Ads declaration | Final owner decision pending; audited release has ads disabled, debug uses test ads |
| Public publisher/studio | Blueborn Games |
| Support email | ykucukcinar@gmail.com |
| Legal developer/controller name | LEGAL NAME INPUT REQUIRED |
| Website | Expected: `https://giginar.github.io/ProjectBlue/`; activation/verification required |
| Public privacy-policy URL | Expected: `https://giginar.github.io/ProjectBlue/privacy.html`; not verified live |
| Public support URL | Expected: `https://giginar.github.io/ProjectBlue/support.html`; not verified live |
| Audience / content rating / regions / price | Owner/Play Console decisions pending; no rating inferred from the ocean theme |
| Data Safety | Pending final SDK/configuration review; see [notes](DATA_SAFETY_NOTES.md) |

## Short description

Restore ocean habitats, rescue wildlife and challenge NEREID across ten sectors

This draft is 79 characters, within the 80-character limit.
See [Play preview-asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151).

## Full description draft

Pilot a submarine through polluted coastal waters, damaged reefs, frozen depths and the
NEREID headquarters. Clear waste, free trapped wildlife and divers, and confront the
machines harming the ocean.

Project Blue: Ocean Guard combines automatic fire with drag movement and close-range cleanup and rescue.
Protect coral as you fight: damage to the habitat affects your dive report. Each sector
introduces different environmental challenges, from drifting fishing nets and oil leaks
to sonar interference, thermal hazards and deep-water pressure.

- Explore ten ocean sectors with distinct environments and multi-stage bosses.
- Unlock Normal, Hard, Expert and Abyss challenges through campaign progress.
- Choose from three submarines, four pilots and five weapons with different strengths.
- Spend earned salvage on six permanent upgrade types.
- Improve stars, scores, Cleanup and Rescue records, and earn sixteen local achievements.
- Restore habitats, complete the final escape and return to earlier sectors.
- Adjust sound, music, motion, background effects, particles and menu size.

Campaign progress is stored on your device. Core gameplay is designed to work offline.
An interrupted dive restarts from the menus after the operating system closes the app;
completed progression is saved locally. No online account is required.

Advertising disclosure must be added or finalized after the owner selects the release
configuration. Do not publish this drafting instruction as store copy.

## Required visual exports

| Export | Specification / plan | Current status |
|---|---|---|
| Play icon | 512 x 512 PNG, at most 1024 KB | Ready at `store/assets/google-play/ocean-guard/app-icon-512.png` |
| Feature graphic | 1024 x 500, JPEG or 24-bit PNG without alpha | Ready at `store/assets/google-play/ocean-guard/feature-graphic-1024x500.png` |
| Phone screenshots | Seven accurate 1080 x 1920 game captures | Ready under `store/assets/google-play/ocean-guard/screenshots/` |
| Master art source | Reproducible/layered source with provenance and export settings | Pending |

Icon/feature specifications above follow the current
[Play requirements](https://support.google.com/googleplay/android-developer/answer/9866151).
The six-screen sequence is this project's editorial plan, not a claim about Google's
minimum screenshot count. Use final device captures and check the Console's current
requirements for each device type offered. See [store asset plan](STORE_ASSET_PLAN.md).

## Review constraints

Do not claim cloud saves, multiplayer, Play Games synchronization, a universal active
special ability, controller/accessibility certification, zero data collection or verified
performance on all devices. Do not describe the placeholders as finished production art.
Do not compare artwork, characters or distinctive designs to another game.
Keep all published claims aligned with the final enabled SDKs and verified behavior.

**GITHUB PAGES ACTIVATION/URL VERIFICATION REQUIRED.** Approval is still needed for
brand/trademark availability, legal developer identity, audience/rating, ads, pricing/regions,
screenshots and final copy. These inputs cannot be
derived safely from source code. Their absence is tracked in [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

**GITHUB PAGES MANUAL ACTIVATION REQUIRED:** select GitHub Actions as the Pages source in the
repository settings if it is not already enabled.

## Remaining product decisions

- **TARGET AUDIENCE DECISION REQUIRED**
- **PUBLISHING COUNTRIES DECISION REQUIRED**
- **STORE LANGUAGE DECISION REQUIRED**
- **REWARDED BENEFIT DECISION REQUIRED**

Pricing, Data Safety, Content Rating and other Play Console forms also remain unresolved.
