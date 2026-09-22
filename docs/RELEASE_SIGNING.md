# Release signing

The repository contains neither release keys nor passwords. A normal
`gradlew.bat :android:bundleRelease` builds an **unsigned** verification AAB when no signing
environment is supplied. It is useful for build/R8 validation but cannot be uploaded as a signed
release. Debug uses Android development signing. The final application ID is
`com.game.diver.oceanguard`; the historical unreleased ID was `com.projectblue.game`.
The migration changes Android's app sandbox, so old development installs do not upgrade in place.
The internal Java namespace and desktop profile path remain unchanged.

Supply these only through a local/CI secret environment, with the keystore outside the checkout:

| Variable | Required value |
| --- | --- |
| `OCEANGUARD_UPLOAD_KEYSTORE` | Absolute path to the Ocean Guard upload keystore |
| `OCEANGUARD_UPLOAD_KEY_ALIAS` | Ocean Guard upload key alias |
| `OCEANGUARD_UPLOAD_STORE_PASSWORD` | Keystore password |
| `OCEANGUARD_UPLOAD_KEY_PASSWORD` | Key password |

All four must be supplied together; partial signing fails configuration. On the publisher's
Windows workstation, run the external DPAPI loader before Gradle:

```powershell
. C:\Users\yigit\blueborn-games\ocean-guard\signing\load-signing-env.ps1
.\gradlew.bat :android:packageInternalGooglePlayBundle
```

The loader decrypts credentials only for the current Windows user and places them in the current
PowerShell process. The internal packaging task requires signing, rejects production advertising,
uses the release build with Google's sample App ID and disabled ad requests, validates the AAB,
and exports it under `build/store/google-play/`. It does not print credentials. Do not pass
passwords in CLI arguments, commit them in Gradle properties, attach
them to build logs, or share private signing files. `.gitignore` excludes local properties,
signing/ad/secret property files, `.env` files, private keys and keystores. Run
`powershell -File tools/verify-release-inputs.ps1` before staging a release.

Google's [app signing guide](https://developer.android.com/studio/publish/app-signing) distinguishes
the **upload key** from the **app signing key** managed by Play App Signing. Ocean Guard uses its
own dedicated upload key. Keep an independent, secure backup of the Ocean Guard key and recovery
information before production publishing.
An AdMob ID is not a private credential, but this project still requires production identifiers
to stay out of source/Git. Generated build outputs contain them when production ads are enabled.

`version.properties` and `gradle/versioning.gradle` remain the central version source.
`versionCode` is the full-history Git commit count; `versionName` includes major/minor, count,
commit and dirty status. Do not reuse a versionCode already uploaded to Play. Parallel branches
can have the same count, so release management must check the highest uploaded code; this task
does not create a commit to bump it. See [versioning](VERSIONING.md).

Build the upload artifact with `gradlew.bat :android:packageInternalGooglePlayBundle` after loading
the environment. The versioned AAB, SHA-256 file, `mapping.txt`, and other available R8 reports are
exported to `build/store/google-play/`. The original mapping remains at
`android/build/outputs/mapping/release/mapping.txt`. Keep the mapping with its exact AAB for crash
deobfuscation. Use `jarsigner -verify -strict -certs` to inspect bundle signing, and Play's internal test track
to validate generated device APKs. See the official [command-line build guide](https://developer.android.com/build/building-cmdline).

Owner inputs still needed before later uploads: the highest uploaded versionCode and the authorized
Play App Signing/account setup. Do not send
passwords or keystores through repository content or chat; provision them through secret storage.
