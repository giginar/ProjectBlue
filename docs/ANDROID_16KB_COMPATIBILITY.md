# Android 16 KB page-size compatibility

Verification date: 2026-09-20. This record applies to the local technical release candidate
built from `de4d39c` plus the uncommitted changes listed in `FINAL_TEST_REPORT.md`.

## Build and packaging

The project uses Android Gradle Plugin 8.13.2, Gradle 8.13, `compileSdk 36`, `targetSdk 36`,
`minSdk 26`, Java 17 and libGDX 1.14.2. `buildToolsVersion` is not pinned; AGP selects its
default Build Tools 35.0.0, which is installed locally. AGP 8.13.2 is newer than the Android
documentation's AGP 8.5.1 minimum for correct 16 KB ZIP alignment of uncompressed shared
libraries.

All freshly built debug APK, minified/resource-shrunk QA APK and unsigned release AAB contain
the same native payload:

| ABI | APK path | AAB path | Provider |
|---|---|---|---|
| `arm64-v8a` | `lib/arm64-v8a/libgdx.so` | `base/lib/arm64-v8a/libgdx.so` | `gdx-platform:1.14.2:natives-arm64-v8a` |
| `armeabi-v7a` | `lib/armeabi-v7a/libgdx.so` | `base/lib/armeabi-v7a/libgdx.so` | `gdx-platform:1.14.2:natives-armeabi-v7a` |
| `x86_64` | `lib/x86_64/libgdx.so` | `base/lib/x86_64/libgdx.so` | `gdx-platform:1.14.2:natives-x86_64` |

No Google Mobile Ads or UMP `.so` file is packaged. Gradle dependency and archive inspection
identify libGDX as the sole native-library provider.

## Static results

- Build Tools 35.0.0 `zipalign -c -P 16 -v 4` passes on the final QA APK.
- Bundletool 1.18.1 `dump config` reports `PAGE_ALIGNMENT_16K` for the release AAB.
- Every `PT_LOAD` program header in each packaged `libgdx.so` reports `p_align = 0x4000`
  (16,384 bytes), including the 32-bit `armeabi-v7a` library.
- The release AAB remains intentionally unsigned because no owner upload key was supplied.

These checks establish compatible package and ELF alignment. They do not replace execution on
a 16 KB kernel or Play-generated APK inspection from the final signed AAB.

## Runtime status

The available Huawei API 29 device and Android 16/API 36 emulator both report
`getconf PAGE_SIZE` as `4096`. The installed SDK has no 16 KB system image or AVD, so no large
download or machine-wide emulator change was made. **MANUAL VERIFICATION REQUIRED:** install an
APK generated from the final signed AAB on an Android 15-or-newer 16 KB environment, verify
`getconf PAGE_SIZE` returns `16384`, then run cold start, menu, Blue Coast, pause/resume, save
reload and process restart.

## Reproduction

```powershell
.\gradlew.bat :android:assembleQa :android:bundleRelease :android:dumpReleaseBundleConfig
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\zipalign.exe" -c -P 16 -v 4 android\build\outputs\apk\qa\android-qa.apk
adb shell getconf PAGE_SIZE
```

See the Android Developers [page-size guidance](https://developer.android.com/guide/practices/page-sizes)
for final signed-bundle and runtime acceptance steps.
