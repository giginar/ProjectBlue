# Release signing

The repository creates neither release keys nor passwords. A normal
`gradlew.bat :android:bundleRelease` builds an **unsigned** verification AAB when no signing
environment is supplied. It is useful for build/R8 validation but cannot be uploaded as a signed
release. Debug uses Android development signing. The application ID stays unchanged to preserve
existing development profiles; different signing certificates prevent overwriting production installs.

Supply these only through a local/CI secret environment, with the keystore outside the checkout:

| Variable | Required value |
| --- | --- |
| `PB_UPLOAD_KEYSTORE` | Absolute path to the existing upload keystore |
| `PB_UPLOAD_KEY_ALIAS` | Upload key alias |
| `PB_UPLOAD_STORE_PASSWORD` | Keystore password |
| `PB_UPLOAD_KEY_PASSWORD` | Key password |

All four must be supplied together; partial signing fails configuration. The script does not
print them. Do not pass passwords in CLI arguments, commit them in Gradle properties, attach
them to build logs, or share private signing files. `.gitignore` excludes local properties,
signing/ad/secret property files, `.env` files, private keys and keystores. Run
`powershell -File tools/verify-release-inputs.ps1` before staging a release.

Google's [app signing guide](https://developer.android.com/studio/publish/app-signing) distinguishes
the **upload key** from the **app signing key** managed by Play App Signing. Use the owner's
existing Play configuration; rotating keys or changing ownership is outside this task.
An AdMob ID is not a private credential, but this project still requires production identifiers
to stay out of source/Git. Generated build outputs contain them when production ads are enabled.

`version.properties` and `gradle/versioning.gradle` remain the central version source.
`versionCode` is the full-history Git commit count; `versionName` includes major/minor, count,
commit and dirty status. Do not reuse a versionCode already uploaded to Play. Parallel branches
can have the same count, so release management must check the highest uploaded code; this task
does not create a commit to bump it. See [versioning](VERSIONING.md).

Build with `gradlew.bat :core:test :lwjgl3:build :android:assembleDebug :android:bundleRelease`.
The AAB is `android/build/outputs/bundle/release/android-release.aab`; preserve the matching
`android/build/outputs/mapping/release/mapping.txt` in restricted release artifacts for crash
deobfuscation. Use `jarsigner -verify` to inspect bundle signing, and Play's internal test track
to validate generated device APKs. See the official [command-line build guide](https://developer.android.com/build/building-cmdline).

Owner inputs still needed: Play package identity confirmation, upload signing configuration,
highest uploaded versionCode, and the authorized Play App Signing/account setup. Do not send
passwords or keystores through repository content or chat; provision them through secret storage.
