# Commit-based versioning

The build calculates a version automatically for each commit:

```text
versionName = <major>.<minor>.<commit count>-g<first 12 characters of the commit ID>
versionCode = number of reachable commits in Git history
```

Example: `0.1.5-gabc123def456` with Android `versionCode=5`.
`major` and `minor` are stored in `version.properties` at the project root.
The patch component is the commit count; no manual increment is needed for each commit.
Change only major/minor for a new major or minor product release.
The Android versionCode counter is not reset.

`gradle/versioning.gradle` is the single source of truth. Its values are used in:

- The Android manifest's `versionName` and `versionCode`.
- Gradle module and distribution versions.
- The menu and desktop window title through the generated `BuildInfo` class.
- APK filenames, artifact directories, and GitHub artifact names.
- Android/Windows filenames and `dist/<version>` directories produced by `package.bat`.

## Commands and output

```powershell
.\package.bat All
.\android.bat version
.\android.bat build
.\android.bat install
```

Query the version without an SDK: `gradlew.bat -q :printVersion`.
For a shareable APK, Windows installer with bundled Java, and portable ZIP, see
[local packaging](LOCAL_PACKAGING.md). No push is required.

```text
build/version/version.json
build/artifacts/0.1.5-gabc123def456/
  ProjectBlue-0.1.5-gabc123def456-debug.apk
  ProjectBlue-0.1.5-gabc123def456-debug.apk.sha256
  BUILD.json
```

`BUILD.json` contains the version name/code, full commit ID, dirty state, APK filename,
and SHA-256. Metadata and the APK come from the same Gradle configuration.
Windows and GitHub scripts do not maintain separate counters.
The APK export task is `:android:packageDebugApk`.
The standard AGP output remains `android/build/outputs/apk/debug/android-debug.apk`;
its embedded version is also calculated automatically.

## Reproducibility and Git history

- Local and CI builds of the same clean commit receive **the same version**.
  Rebuilding does not increment it. APK bytes are not guaranteed to match because
  signing keys and SDK environments may differ.
- Uncommitted changes or new files not excluded by Git add a `-dirty` suffix.
  Ignored build/SDK files do not affect it. A dirty build creates neither a new commit
  nor a new versionCode.
- Full Git history is required. Shallow clones fail with an explanatory error instead
  of producing an incorrectly low versionCode. Use `git fetch --unshallow` to fix this.
- The project must be at the root of its own Git checkout and have at least one commit.
  A source ZIP without Git or an unrelated parent repository cannot supply its version.
- Normal history-preserving development on `main` increases versionCode.
  Merges count all reachable commits, so the value may increase by more than one.
- Different branches can have equal commit counts. The short hash distinguishes their
  version names; versionCode is not a global ordering across branches.
  Track Android distribution order through `main`. Rewriting history or force-pushing
  invalidates the counter's monotonicity guarantee.
- Android versionCode is checked against the range 1-2,100,000,000.
  [Android versioning rules](https://developer.android.com/studio/publish/versioning).

There are no Git hooks, automatic version commits, or build-time source changes.
Version increments therefore cannot trigger a self-sustaining commit/push loop.

## CI and validation

GitHub checkout uses `fetch-depth: 0`. Pull requests build their source commit rather
than a temporary merge commit. Each branch push produces an APK for the latest commit.
If a push contains multiple commits, CI builds the last one; earlier commits can be
checked out and built with the same versioning rules.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/test-versioning.ps1
```

Temporary Git repositories test clean/repeated builds, dirty/untracked changes,
counter increments after commits, distinct branches with equal commit counts, and
rejection of unborn, nested, and shallow repositories.
The same tests run on GitHub with PowerShell Core.
Fixtures are created under the Git-ignored `build/versioning-tests/` directory.
