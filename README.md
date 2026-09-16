# Project Blue

An original, Android-first 2D underwater shooter prototype written in Java/libGDX
and played in portrait orientation. Pilot a submarine, disable drones, collect plastic,
rescue turtles, and complete **The Quiet Reef**, a **180-second** level. Cleanup and
rescue visibly improve the water color, coral, and fish density.

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
- Move within 112 units of a bottle: the cleanup beam collects it in 0.42 seconds.
- Stay within 96 units of a turtle for **1.5 uninterrupted seconds** to remove its net.
- Gold salvage pieces are pulled toward the submarine when it gets close.
- Use the top-right pause button, **Esc**, **P**, or Android's back button to pause.
- Continue from the pause screen. Returning to the menu ends the current dive.
- Toggle sound and music from the main menu or pause screen; settings are saved.

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
| Android application ID | `com.projectblue.game` |
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
  config/GameConfig          Game balance, timing, scoring, and limits
  logic/                     Pure Java GameWorld, Rules, LevelResult, seeded random
  events/GameEvents          Synchronous game event dispatch without allocations
  input/                     PlayerInput, PointerInput, MenuInput
  render/OceanRenderer       Programmatic underwater visuals
  ui/                        HUD, palette, shared drawing resources
  screens/                   Boot, MainMenu, Game, Pause, Result, ScreenRouter
  assets/GameAssets          Central AssetManager
  audio/AudioService         Sound/music settings and lifecycle handling
  save/                      Versioned profile, checksum, safe defaults
  platform/                  Service interfaces and shared no-op behavior
lwjgl3/                      Desktop launcher, platform adapter, GL smoke check
android/                     Android launcher, safe window insets, no-op adapter
assets/                      Original font/audio and license inventory
tools/GenerateAssets.java     Offline asset regeneration
```

- The simulation runs at **60 fixed steps/second**; long frame/resume intervals are capped
  at 0.1 seconds. `GameWorld` imports neither Android nor libGDX. UI code does not own game rules.
- Bullets, drones, plastic, turtles, salvage, and particles use fixed-capacity,
  preallocated pools. Visual effects have a separate random stream from gameplay.
- The HUD updates 10 times per second using reusable StringBuilders.
  Screens do not dispose shared GPU resources; their owners dispose them at shutdown.
- ScreenRouter applies transitions at the end of a frame. The pause screen retains the
  current GameScreen without advancing its simulation. Results/menu transitions release
  subscriptions from the previous run.
- Android `onPause/onResume` is handled through libGDX. Backgrounding resets input,
  pauses gameplay/audio, and saves the profile. Returning requires selecting **Resume Dive**.
- Android system bar and cutout insets are applied to the game View. FitViewport preserves
  the full play area with letterboxing on wide screens, tablets, and window resizing.
- `AdsService`, `ConsentService`, `AchievementService`, `AnalyticsService`, and `PlatformService`
  define the platform boundary. The no-op ads service reports unavailable and never grants
  rewards. No network permission, ad SDK, or account connection is included.
- Profile schema **v1** includes explicit v0 migration and CRC32 corruption detection.
  Writes use temporary files and backups; missing, corrupt, or unknown schemas fall back
  to defaults. Storage errors are shown in the menu/results without crashing the game.
  CRC32 is not a security or anti-cheat mechanism.
- Desktop saves: `.projectblue/profile.properties` in the user's home directory.
  Android saves: the app's private files directory. Smoke mode uses `build/smoke/profile`.
- After the operating system kills the process, a new launch opens the main menu.
  In-progress dives are not restored from disk. Settings and completed run results persist.

## Level and scoring rules

The level contains 40 drones, 36 plastic items, and 5 turtles. A fixed seed, a deterministic
spawn schedule, and a quiet final stretch make the level learnable.

- Combat = destroyed / 40; Cleanup = collected / 36; Rescue = rescued / 5.
- Integrity = remaining health / 100. Percentages are clamped to 0-100.
- Drones award 100 points, plastic 40, rescues 300, and each salvage unit 20.
  Each drone drops 5 salvage. Completing the level adds 500 + remaining health x 5.
- A failed dive earns **0 stars**; a completed dive earns at least **1 star**.
  An average of at least 45 across the four categories earns **2 stars**.
  An average of at least 75 with every category at least 50 earns **3 stars**.
- Visual recovery: Cleanup x 55% + Rescue x 45%.

## Verification and limitations

Verified in the Windows x64 development session on 2026-09-16:

- **32 JUnit 5 tests passed:** the seven requested rule groups, plus collisions, pooling,
  uninterrupted rescue, salvage, seeded reproducibility, 180-second survival,
  save round trips, corruption, schema migration, and write failures.
- A real LWJGL3/OpenGL window passed boot, menu, drag, pause/resume, lifecycle pause/resume,
  wide viewport, 180-second completion, results, saving, and replay checks.
- Desktop distributions were built. Test report: `core/build/reports/tests/test/index.html`;
  screenshots: `build/smoke/`. See [local packaging](docs/LOCAL_PACKAGING.md) for installer validation status.
- Android SDK 36 was installed and a **debug APK was built**. Application ID, minimum/target
  SDK, three ABIs, APK signature, and **16 KB ZIP alignment** were verified.
  The arm64-v8a/x86_64 ELF LOAD segments are also aligned to **16384 bytes**.
  Android lint completed without errors; warnings about portrait orientation, version
  recommendations, and manifest compatibility are available in the report.
- **No Android device/emulator was connected, so actual Android launch, touch input, and
  GPU context loss were not tested.** Building an APK does not replace device testing
  or establish readiness for Play Store publication.
- Visuals/audio are original placeholders. Professional artwork, music production,
  localization, cross-device performance profiling, and comprehensive balancing are pending.
- Level/pilot selection, upgrades, real ads, a consent SDK, Google Play Games,
  online analytics, and additional levels are outside this phase.

Asset policy and source inventory: [ASSET_LICENSES.md](assets/licenses/ASSET_LICENSES.md).
Unverified sources/licenses or changes to verified asset hashes stop packaging.

## Repository language

All repository content, filenames, code comments, generated instructions, and commit
messages use English. See [AGENTS.md](AGENTS.md) for the repository conventions.
