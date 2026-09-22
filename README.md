# Project Blue: Ocean Guard

An original, Android-first 2D underwater shooter by **Blueborn Games**, written in Java/libGDX
and played in portrait orientation. Pilot a submarine through **Blue Coast**, **Coral Gardens**,
**Ghost Nets**, **Sunken City**, **Black Tide**, **Silent Reef**, **Frozen Depths**, **Abyss Mine**, **Plastic Vortex**, and **NEREID Core** to stop NEREID extraction, protect habitats,
clear industrial pollution, and rescue wildlife and divers. Cleanup and rescue visibly improve each habitat.

The campaign has **10 authored, playable sectors** and a persistent final recovery scene.
It includes four difficulties, persistent progression, a Hangar,
crew/vessel selection, permanent upgrades,
achievements, settings, and dive reports. Data-driven TIDE/MANTA/LEVIATHAN vessels, four
pilots, six upgrades, five weapons and sixteen local achievements extend that progression.
The new default is TIDE / Kaia / Pulse Cannon; Kaia adds a cleanup bonus. The shared
fixed-step simulation remains covered by regression tests. See
[equipment systems and balance](docs/EQUIPMENT_SYSTEMS.md) and
[meta-progression](docs/META_PROGRESSION.md), [Blue Coast](docs/BLUE_COAST.md), and
[Sectors 2-3](docs/SECTORS_2_3.md), [Sectors 4-5](docs/SECTORS_4_5.md), and
[Sectors 6-7](docs/SECTORS_6_7.md), [Sectors 8-9](docs/SECTORS_8_9.md), and [NEREID Core](docs/NEREID_CORE.md) for
architecture, provisional balance, and manual checks.

Sky Force is a genre reference only. No names, assets, UI, levels, enemies, story,
or source code have been copied. No game assets have been downloaded from the internet.

## Build packages to share

**Double-click `package.bat` in the project root.** It builds an Android APK, a Windows x64
installer EXE, and a portable ZIP, then opens the output folder.
The Windows packages include Java; players do not need to install a JDK or SDK.

To run from a terminal without opening a folder or waiting for a keypress:

```powershell
.\package.bat All
```

Shareable files are written to **`dist/<version>/`**. Send `*-android.apk` to phone users
and `*-windows-x64-setup.exe` to Windows users. `*-portable.zip` runs without installation.
Each new commit increments the version; rebuilding does not. Uncommitted code is marked
`-dirty`. No GitHub account, push, or Actions step is required.

For individual platforms, optional tests, and first-time setup, see the
[local packaging guide](docs/LOCAL_PACKAGING.md).

## Quick start - Windows / IntelliJ IDEA Community

1. Open the project root **as a Gradle project**. Select **Wrapper** as the Gradle distribution.
2. Select **JDK 17 or 21** for the Project SDK and Gradle JVM.
3. Run in the terminal:

```powershell
.\gradlew.bat :lwjgl3:run
```

You can also run `lwjgl3 > application > run` from the Gradle tool window. Java gameplay
and the desktop version can be developed without an Android IDE plugin.
Android packaging requires the Android SDK. Machine-specific SDK paths are not committed.

Linux/macOS: `sh ./gradlew :lwjgl3:run`. On macOS, the Gradle run task adds the JVM
first-thread option. The platform verified in this development session is Windows x64.

## Controls

- Hold the left mouse button or one finger inside the play area and **drag**. Relative
  movement prevents the submarine from jumping to the initial touch position.
- Firing is automatic. Cyan projectiles belong to the player; red projectiles belong to drones.
- Move within 112 units of a bottle: the base cleanup time is 0.42 seconds, reduced by
  vessel, pilot and Cleanup Beam bonuses.
- Stay within 96 units of a turtle to remove its net. Base rescue takes **1.5 uninterrupted
  seconds**, reduced by pilot and Rescue System bonuses.
- Gold salvage pieces are pulled toward the submarine when it gets close.
- Use the top-right pause button, **Esc**, **P**, or Android's back button to pause.
- Continue from the pause screen. Returning to the menu ends the current dive.
- Toggle sound and music from Settings or the pause screen; settings are saved.
- Scroll menu panels with a finger drag or mouse wheel. Back/Esc returns to the parent screen.

## Versions and compatibility

Checked against official sources on 2026-09-16:

| Component | Version / decision |
|---|---|
| Java source and bytecode | **17** (`--release 17`); local build/test JVM: Microsoft OpenJDK **21.0.12** |
| libGDX | **1.14.2**, current stable release at the time of verification |
| LWJGL | **3.3.3**, the dependency published with the libGDX backend |
| Gradle Wrapper | **8.13**, official distribution SHA-256 verification enabled |
| Android Gradle Plugin | **8.13.2**, pinned for Java 17 and API 36 compatibility |
| Android compile / target SDK | **36 / 36** |
| Android minimum SDK | **26** (Android 8.0), a simple baseline for the standard Java APIs in use |
| Android application ID | `com.game.diver.oceanguard` |
| Android ABI | `arm64-v8a`, `armeabi-v7a`, `x86_64` |
| JUnit | **5.13.4**, Jupiter / JUnit Platform |
| Logical play area | **540 x 960**, FitViewport, portrait |

As of August 31, 2026, Google Play requires at least API 36 for new apps and updates.
AGP 8.13 supports up to API 36.1 and is compatible with Gradle 8.13/JDK 17.
Gradle and AGP are a verified version pair, not a claim to use their latest releases.

Sources: [libGDX releases](https://libgdx.com/dev/versions/),
[Google Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en),
[AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes),
[JUnit 5.13.4](https://docs.junit.org/5.13.4/release-notes/),
[Android 16 KB page support](https://developer.android.com/guide/practices/page-sizes).

## Tests, distributions, and Android builds

See the [Android testing guide](docs/ANDROID_TESTING.md) for APK builds and phone installation.
On Windows, `android.bat build` installs a missing SDK, runs tests/lint, and produces
`build/artifacts/<version>/ProjectBlue-<version>-debug.apk`. For a phone connected over USB,
`android.bat install` installs the APK and launches the game.
GitHub Actions creates a downloadable, versioned APK artifact on pushes to any branch.
The version is derived from the Git commit count and ID; use `android.bat version` to
display it. The same version appears in the menu and desktop window title.
See [versioning](docs/VERSIONING.md) for branch and history rules.

```powershell
# Pure Java tests and asset license checks without an Android SDK
.\gradlew.bat :check

# Automated desktop check in a real OpenGL window; exits when complete
.\gradlew.bat :lwjgl3:run --args=--smoke

# Launch a second process to verify the smoke profile survived application exit
.\gradlew.bat :lwjgl3:run --args=--smoke-reload

# Explicit desktop development build; enables the guarded reset control in Settings
.\gradlew.bat :lwjgl3:run -PdevelopmentBuild=true

# Desktop distribution that requires an installed Java runtime
.\gradlew.bat :lwjgl3:installDist
.\lwjgl3\build\install\lwjgl3\bin\lwjgl3.bat

# Prepare the three Android ABI libraries without an SDK
.\gradlew.bat :android:extractNatives

# Build with an installed Android SDK
.\gradlew.bat :android:assembleDebug

# Export the APK, checksum, and BUILD.json using the commit version
.\gradlew.bat :android:packageDebugApk
```

**The leading `:` matters:** `:check` runs only the root verification task;
`check` may also select Android lint tasks in subprojects and require an SDK.

The Android SDK must contain `platforms;android-36`, `build-tools;35.0.0`, and `platform-tools`,
with the package licenses accepted through SDK Manager.
AGP 8.13 defaults to build-tools 35.0.0; compile/target SDK remain 36.
If SDK Manager is available:

```text
sdkmanager "platforms;android-36" "build-tools;35.0.0" "platform-tools"
```

Set the SDK location through `ANDROID_HOME` or create a Git-ignored
`local.properties` file in the project root:

```properties
sdk.dir=C\:/Users/YOUR_USER/AppData/Local/Android/Sdk
```

APK: `android/build/outputs/apk/debug/android-debug.apk`.
On a connected device: `adb install -r android/build/outputs/apk/debug/android-debug.apk`.

## Architecture

```text
core/       com.projectblue.game
  ProjectBlueGame             Application and resource ownership
  config/                    GameConfig, CampaignConfig, ContentCatalog, Difficulty, RunSpec, Loadout
  logic/                     Pure Java GameWorld, Rules, LevelResult, seeded random, weapon strategies
  events/GameEvents          Synchronous game event dispatch without allocations
  input/                     PlayerInput, PointerInput, MenuInput
  render/OceanRenderer       Programmatic underwater visuals
  ui/                        HUD, palette, shared drawing resources, original Scene2D theme
  screens/                   Scene2D menus, campaign, hangar, settings, Game/Pause, ScreenRouter
  assets/GameAssets          Central AssetManager
  audio/AudioService         Sound/music settings and lifecycle handling
  save/                      Versioned profile, per-level records, migrations, backup recovery
  platform/                  Service interfaces and shared no-op behavior
lwjgl3/                      Desktop launcher, platform adapter, GL smoke check
android/                     Android launcher, safe window insets, AdMob/UMP adapters
assets/                      Original font/audio and license inventory
core/src/main/resources/     Campaign, mission, equipment, and achievement configuration
tools/GenerateAssets.java     Offline asset regeneration
```

- The simulation runs at **60 fixed steps/second**; long frame/resume intervals are capped
  at 0.1 seconds. `GameWorld` imports neither Android nor libGDX. UI code does not own game rules.
- Bullets, drones, plastic, turtles, salvage, and particles use fixed-capacity,
  preallocated pools. Visual effects have a separate random stream from gameplay.
- Settings can reduce background geometry, motion, and cosmetic particles for slower devices.
- The HUD updates 10 times per second using reusable StringBuilders.
  Screens do not dispose shared GPU resources; their owners dispose them at shutdown.
- ScreenRouter applies transitions at the end of a frame. The pause screen retains the
  current GameScreen without advancing its simulation. Results retain the frozen run for an
  optional rewarded continue; leaving results releases its subscriptions.
- Android `onPause/onResume` is handled through libGDX. Backgrounding resets input,
  pauses gameplay/audio, and saves the profile. Returning requires selecting **Resume Dive**.
- Android system bar and cutout insets are applied to the game View. FitViewport preserves
  the full play area with letterboxing on wide screens, tablets, and window resizing.
- New Scene2D menus use a separate ExtendViewport, scrollable content, 84-unit touch
  targets, and a persistent Back control. They do not change the gameplay viewport.
- `AdsService`, `ConsentService`, `AchievementService`, `AnalyticsService`, and `PlatformService`
  define the platform boundary. The no-op ads service reports unavailable and never grants
  rewards. Android alone includes AdMob and UMP; debug uses official demo IDs and release
  ads are disabled until explicitly configured. No account connection is enabled.
  See [ads integration](docs/ADS_INTEGRATION.md), [privacy](docs/PRIVACY_CHECKLIST.md),
  [data safety](docs/DATA_SAFETY_NOTES.md), [signing](docs/RELEASE_SIGNING.md), and
  [Play release checks](docs/PLAY_RELEASE_CHECKLIST.md).
- Profile schema **v5** includes explicit v0/v1/v2 migrations, additive v3/v4 handling, and CRC32 corruption detection.
  Equipment selections, upgrade purchases and achievement notifications persist locally.
  Purchases commit to disk before updating the live profile; write failures spend no salvage.
  Completed results remain retryable in memory after a storage error. Writes use verified temporary files,
  atomic replacement where supported, and verified backups; corrupt profiles try the backup before
  falling back to defaults. Storage errors appear throughout the menus; Settings provides
  a save retry. An old v1 completion becomes Level 1 / Normal progress without inventing
  historical cleanup/rescue percentages.
  CRC32 is not a security or anti-cheat mechanism.
- Desktop saves: `.projectblue/profile.properties` in the user's home directory.
  Android saves: the app's private files directory. Smoke mode uses `build/smoke/profile`.
- After the operating system kills the process, a new launch opens the main menu.
  In-progress dives are not restored from disk. Settings and completed run results persist.

## Level and scoring rules

Levels 1-10 are authored JSON missions in the shared `GameScreen` and `GameWorld`. Blue Coast's
five-minute target route uses a deterministic
JSON timeline with 36 scheduled drones, 52 cleanup targets, 3 turtles, 4 coral areas, and the
three-stage Shoreline Compactor. Six component-based enemy definitions share movement, weapon,
stats, and reward systems.

- Combat uses enemies actually encountered. Cleanup uses authored waste and applies coral damage;
  Rescue uses the three turtles; Integrity uses remaining hull and coral protection.
- Enemy and waste rewards come from the mission config. Completing the level adds the existing
  completion and remaining-hull score bonuses.
- A failed dive earns **0 stars**; a completed dive earns at least **1 star**.
  An average of at least 45 across the four categories earns **2 stars**.
  An average of at least 75 with every category at least 50 earns **3 stars**.
- Visual recovery combines cleanup, rescue, and coral integrity, then completes its transition
  during the six-second post-boss recovery sequence.

Only Level 1 / Normal is open in a new profile. One-star clears open Coral Gardens, Ghost Nets,
Sunken City, Black Tide, Silent Reef, Frozen Depths, Abyss Mine, Plastic Vortex, and NEREID Core.
Every playable sector opens **Normal > Hard > Expert > Abyss** in order.
Best stars, score, cleanup, rescue, and completed difficulties are saved independently.

Higher difficulties increase enemy density, health, bullet speed, firing frequency, and boss cadence.
Each boss has telegraphed state changes and gated core damage. Mission timelines are in the ten
JSON files under [`config`](core/src/main/resources/config); difficulty tuning remains in
[`campaign.properties`](core/src/main/resources/config/campaign.properties).
The first-clear economy assumptions, upgrade curves, and equipment tradeoffs are documented in
[`docs/BALANCING.md`](docs/BALANCING.md).

## Verification and limitations

Verified in the Windows x64 development session on 2026-09-19:

- **252 JUnit 5 tests passed:** the original rule groups, plus collisions, pooling,
  uninterrupted rescue, salvage, seeded reproducibility, Blue Coast completion,
  save round trips, corruption, schema migration, and write failures. Added coverage includes
  all difficulty multipliers, live spawn/shot/boss behavior, independent locks, replay records,
  purchases, loadouts, achievements, real file persistence, backup recovery and reset gating.
  Equipment coverage includes config validation/fallback, concurrent purchase and save-failure
  rollback, all five weapon behaviors, shield/cleanup/rescue effects, v2 profile migration,
  all ten authored timelines, reusable net cutting, sonar, thermal, pressure, deterministic current,
  cleanup combo, and environment systems,
  v3-to-v4 and v4-to-v5 profile migration, every authored boss state machine, the final escape,
  assisted base-loadout boss-gate completion, final unlock/achievement, and post-finale save reload.
  Final QA adds all 40 mission/difficulty timelines, four-difficulty final-escape continue,
  second-finger actions, laser/coral damage, freed-wildlife cleanup and minSdk-safe config loading.
  These tests do not establish human clearability or balance across all 40 combinations.
- A real LWJGL3/OpenGL window passed boot, menu, drag, pause/resume, lifecycle pause/resume,
  wide viewport, Blue Coast and Coral Gardens completion, Ghost Nets entry, results, saving, and replay checks. All requested
  screens, purchases, settings, narrow/wide menu layouts, and Shoreline Compactor rendering were exercised.
  Weapon selection, locks and managed GPU texture deletion/reload were also exercised.
  A separate application launch verified
  profile persistence, including vessel, pilot and weapon choices.
- Desktop distributions were built. Test report: `core/build/reports/tests/test/index.html`;
  screenshots: `build/smoke/`. See [local packaging](docs/LOCAL_PACKAGING.md) for installer validation status.
- Android SDK 36 was installed; a **debug APK and unsigned R8 release AAB were built**.
  Bundletool validation passed. Application ID, minimum/target
  SDK, three ABIs, APK signature, and **16 KB ZIP alignment** were verified.
  The arm64-v8a/x86_64 ELF LOAD segments are also aligned to **16384 bytes**.
  Android lint completed without errors; warnings about portrait orientation, version
  recommendations, and manifest compatibility are available in the report.
- **No Android device/emulator was connected, so actual Android launch, touch input, and
  GPU context loss were not tested.** Building an APK does not replace device testing
  or establish readiness for Play Store publication.
- Visuals/audio are original placeholders. The in-game UI supports English and Turkish with
  an explicit first-launch choice and Settings switching. Professional artwork, music production,
  additional languages, cross-device performance profiling, and comprehensive balancing are pending.
- NEREID Core is authored and playable. Signed publication, real Android acceptance tests,
  GitHub Pages activation, Play Console inputs and approved store exports remain outstanding.

**NOT READY FOR RELEASE.** See the [final test report](docs/FINAL_TEST_REPORT.md),
[known issues](docs/KNOWN_ISSUES.md), [release steps](docs/RELEASE_STEPS.md),
[asset audit](docs/ASSET_AUDIT.md), [manual matrix](docs/MANUAL_TEST_MATRIX.md) and
[store listing draft](docs/STORE_LISTING_DATA.md) for the current release decision.

Asset policy and source inventory: [ASSET_LICENSES.md](assets/licenses/ASSET_LICENSES.md).
Unverified sources/licenses or changes to verified asset hashes stop packaging.

## Release identity and support

- Public game name: **Project Blue: Ocean Guard**
- Publisher/studio brand: **Blueborn Games**
- Support: **ykucukcinar@gmail.com**
- Android application ID: `com.game.diver.oceanguard`
- Expected GitHub Pages base URL: `https://giginar.github.io/ProjectBlue/`
- Expected privacy policy: `https://giginar.github.io/ProjectBlue/privacy.html`
- Expected support page: `https://giginar.github.io/ProjectBlue/support.html`

**GITHUB PAGES ACTIVATION/URL VERIFICATION REQUIRED.** These URLs are expected from the
repository remote and must not be treated as live until the Pages deployment succeeds.
**GITHUB PAGES MANUAL ACTIVATION REQUIRED:** select GitHub Actions as the Pages source in the
repository settings if it is not already enabled.

## Repository language

All repository content, filenames, code comments, generated instructions, and commit
messages use English. See [AGENTS.md](AGENTS.md) for the repository conventions.
