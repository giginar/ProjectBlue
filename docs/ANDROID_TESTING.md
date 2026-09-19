# Testing Project Blue on Android

Build an APK with IntelliJ IDEA Community and JDK 17/21 without installing Android Studio.
Use `android.bat` in the project root on Windows. The phone requires **Android 8.0 or newer**.
Debug retains `com.projectblue.game` so existing development installs keep their profiles.
Debug and production signing certificates differ; do not install debug over a production install.
Debug ads use Google's demo IDs only. See [ads integration](ADS_INTEGRATION.md) and
[Play release checks](PLAY_RELEASE_CHECKLIST.md) before distributing a production build.

**To share with a friend:** double-click `package.bat` or run `package.bat Android`.
The shareable APK is placed in `dist/<version>/`. The same script can build a Windows
installer: see [local packaging](LOCAL_PACKAGING.md).

## Build on this computer

```powershell
# Install a missing SDK from Google, run tests, and build the APK
.\android.bat build

# Install or prepare Android SDK packages only
.\android.bat setup

# Display the current commit version without installing the SDK
.\android.bat version
```

The default SDK location is `%LOCALAPPDATA%\Android\Sdk`. An existing `local.properties`,
`ANDROID_HOME`, or `ANDROID_SDK_ROOT` takes precedence.
To choose another location: `android.bat setup -SdkPath D:\Android\Sdk`.

Setup verifies Google's **15859902** Windows command-line tools archive against its
published SHA-256, then installs SDK 36, build-tools 35.0.0, and platform-tools.
Required package licenses are accepted through `sdkmanager`.
[Official tools and license](https://developer.android.com/studio),
[sdkmanager documentation](https://developer.android.com/tools/sdkmanager).

The system-wide PATH is not changed. The SDK location is written to Git-ignored
`local.properties`. Downloaded SDK files, signing keys, and APKs are not committed.

Each build runs JUnit tests, asset verification, and Android lint.
After verifying the APK signature and 16 KB ZIP alignment, it produces:

- `build/artifacts/<version>/ProjectBlue-<version>-debug.apk`
- A matching `.sha256` file and `BUILD.json`

Example: `ProjectBlue-0.1.5-gabc123def456-debug.apk`. The version is the same in Android
package metadata and the filename. Each version has its own folder; previous APKs are
preserved. See [VERSIONING.md](VERSIONING.md).

Copy the APK to the phone and open it with a file manager. If Android prompts you, allow
the file manager or browser opening the APK to install apps from that source.

## Install and launch over USB

1. Enable developer options and **USB debugging** on the phone.
2. Connect a USB data cable and accept the computer authorization prompt on the phone.
3. Run:

```powershell
.\android.bat devices
.\android.bat install
```

`install` rebuilds the APK, runs checks, installs it with `adb install -r`, and launches
the `AndroidLauncher` activity. Normal updates preserve the profile.
When multiple devices or emulators are connected, select one explicitly:

```powershell
.\android.bat install -Serial DEVICE_ID
.\android.bat logs -Serial DEVICE_ID
```

`logs` follows AndroidRuntime and libGDX logs; press Ctrl+C to stop.
If no device is available or authorized, the script reports an error. It does not select
a random device or uninstall the app automatically.
Some phones require the manufacturer's ADB USB driver on Windows.
[Official device connection instructions](https://developer.android.com/studio/run/device).

## Download an APK from GitHub

The [Android Debug APK workflow](https://github.com/giginar/ProjectBlue/actions/workflows/android-debug.yml)
runs on pushes to all branches, pull requests, and manual **Run workflow** requests.

1. Sign in to GitHub and open a successful workflow run.
2. Download `ProjectBlue-<version>-debug` from **Artifacts**.
3. Extract the ZIP, copy `ProjectBlue-<version>-debug.apk` to your phone, and install it.

The archive contains the APK, SHA-256 checksum, and `BUILD.json` identifying the version
and source commit. APK artifacts are retained for 30 days; test/lint reports for 14 days.
[GitHub artifact download documentation](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts).

These are **debug-signed test packages**. Local and CI signing keys differ, and CI debug
keys may also change between runs. If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`,
uninstall the old app manually before installing the new APK; uninstalling deletes the
local profile. The script does not do this automatically.
Store publication, release signing keys, and persistent CI signing are outside this workflow.

## Quick device check

- The menu opens, sound/music settings work, and touches map to the correct positions.
- Dragging moves the submarine; automatic fire and collisions work.
- Approaching a bottle triggers cleanup; staying beside a turtle for 1.5 seconds rescues it.
- The pause button and Android back button work.
- Returning from the home screen leaves the game paused; resuming does not jump the clock.
- Results and stars appear after the Shoreline Compactor and recovery sequence; replay starts a fresh run.
- Fully stopping and relaunching the process restores the menu and saved settings.

No physical device/emulator was connected in this session, so actual Android launch,
touch input, and GPU context behavior remain unverified. APK builds, signature/alignment
verification, and static lint checks do not replace device testing.
