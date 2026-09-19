# Local APK and Windows installer builds

**Double-click `package.bat`.** After running checks, it prepares shareable files,
opens the output folder, and waits for a keypress before closing the terminal.
Errors remain visible. No GitHub account, push, or Actions download step is required.

From the IntelliJ terminal:

```powershell
# Both platforms; no folder window or keypress prompt
.\package.bat All

# A single platform
.\package.bat Android
.\package.bat Windows

# Also test the packaged EXE in a real OpenGL session
.\package.bat All -SmokeTest

# Open the output folder when complete
.\package.bat All -OpenOutput
```

The script also works when called by absolute path from another working directory.
`-SmokeTest` requires a graphics session. It checks the menu, dragging, pause/resume,
lifecycle, aspect ratio, a full Blue Coast run, results, and replay, then exits.
System `JAVA_HOME` and Java PATH entries are removed during that test to verify the
bundled runtime. Normal packaging always runs JUnit and asset checks; Android builds
also require lint, signature, and alignment checks to pass.

## Which files should I send?

Example output:

```text
dist/
  LATEST.txt
  0.1.7-gabc123def456/
    ProjectBlue-0.1.7-gabc123def456-android.apk
    ProjectBlue-0.1.7-gabc123def456-windows-x64-setup.exe
    ProjectBlue-0.1.7-gabc123def456-windows-x64-portable.zip
    BUILD.json
    SHA256SUMS.txt
    README.txt
```

| Recipient's device | File to send | How to run |
|---|---|---|
| Android 8.0+ phone/tablet | `*-android.apk` | Save it to the phone and open it. If prompted, allow the app opening the APK to install from that source. |
| Windows x64 | `*-windows-x64-setup.exe` | Run the installation wizard, then launch from the desktop or Start menu. |
| Windows x64, without installation | `*-windows-x64-portable.zip` | Extract the entire ZIP and open `ProjectBlue.exe`. The EXE alone is not sufficient. |

Both EXE and ZIP distributions **include Java**. Players do not need Gradle, Git,
a JDK, or the Android SDK. Installation targets the current Windows user and does not
require an administrator installation. Uninstall through Windows Settings.
The game profile is stored in `%USERPROFILE%\.projectblue` and is preserved on uninstall.
macOS/Linux installers are outside the scope of this Windows script; Gradle desktop run
remains available for development.

The APK is a debug-signed test package. The Windows EXE is not code-signed, so Windows
may show a publisher verification warning for a downloaded package.
APKs built on the same computer keep the same update signature as long as the local
debug key is retained. APKs signed by another computer or CI may not install over an
existing app. Uninstalling the old app deletes its profile.
Store publication and release signing are not included.

## How do versions advance?

The existing [commit-based versioning](VERSIONING.md) is used:

```text
<major>.<minor>.<commit count>-g<12-character commit ID>[-dirty]
```

- New commit: new version. A local commit is enough; no push is required.
- Repackaging the same commit does not increment the version.
- Uncommitted changes add `-dirty`. Different dirty builds can share a version;
  commit your changes before sending a distinctly versioned update.
- The menu, window title, APK manifest, and filenames contain the same full version.
- Windows Installer uses the numeric version `major.minor.commitCount`. The Git ID and
  `-dirty` remain in filenames and in the game. Windows field limits are `255.255.65535`;
  the script stops if they are exceeded rather than generating an invalid version.
- A new commit's Windows installer uses a stable upgrade ID to update the previous release.
  Installing a different dirty/branch build with the same numeric version may require
  uninstalling the old package first. Use the portable ZIP for quick experiments.
- A version folder is replaced only after the newly selected packages are complete.
  The previous output for that version is preserved under `build/packaging/previous-*`;
  other version folders are retained. Selecting `Windows` or `Android` publishes only that
  platform in the version folder. `dist/LATEST.txt` names the last successful output folder.

`BUILD.json` records the full commit, version, target, bundled Java version, and each
package's size/SHA-256. Installer bytes are not guaranteed to be reproducible.
Do not edit source files during a build; both platforms use one Gradle invocation and
the same version snapshot. A lock prevents two packaging scripts from running concurrently.

## Developer machine requirements

- Windows x64, a Git checkout with full history, and an **x64 JDK 17 or 21**
  available through `JAVA_HOME` or PATH. Locally verified JDK: Microsoft OpenJDK
  **21.0.12**; source compatibility remains Java 17.
- First-time setup needs internet access for Gradle/Maven dependencies, a missing Android
  SDK, and WiX tools. Tool preparation is automatic; no GitHub account is created.
- Android SDK preparation uses `android.bat prepare`: SDK 36 / build-tools 35.0.0.
  See the [Android guide](ANDROID_TESTING.md) for SDK location rules.
- Windows installers use the portable **WiX 3.14.1** archive from the official WiX release.
  Its SHA-256 is pinned:
  `6ac824e1642d6f7277d0ed7ea09411a508f6116ba6fae0aa5f2c7daa2ff43d31`.
  This hash was measured from the downloaded official archive; it is not a claim of
  an upstream signature. Cache: `%LOCALAPPDATA%\ProjectBlue\build-tools`.
  The archive's `LICENSE.TXT` contains the Microsoft Reciprocal License.
  System PATH and installed programs are not changed.
- The JDK's `jpackage` tool bundles the game and required Java modules.
  Java's `runtime/legal` notices and dependency licenses inside JARs are preserved.
  The package icon is generated locally from the original Android geometry and is
  recorded in the asset inventory.

Build outputs, tools, and signing keys are not committed. On failure, `dist/LATEST.txt`
is unchanged and temporary working files remain in the reported `build/packaging/`
directory for diagnosis. A successful build ends with `READY: ...`.

## Validation status

The initial packaging validation covered 32 JUnit tests, Android lint/signature/16 KB alignment, combined platform builds,
Android-only builds from a different working directory, and real OpenGL gameplay with
the packaged Java runtime. Concurrent packaging and invalid target/test combinations
were rejected.
The subsequent [meta-progression update](META_PROGRESSION.md) passes 68 JUnit tests,
Android debug/lint, desktop distribution, and expanded OpenGL/relaunch checks. Installer
creation, signature and alignment checks were not repeated for that update.
The silent Windows installation attempt did not complete in this development environment.
Installer creation and embedded version metadata were checked, but installation,
upgrade, and uninstall have not been verified end to end.
Physical Android testing also requires a connected device.

Official technical references: [JDK 21 jpackage](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html),
[WiX 3.14.1](https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm),
[Windows Installer version fields](https://learn.microsoft.com/en-us/windows/win32/msi/productversion).
