# Project Blue final release candidate test report

## Prompt 13 technical release candidate follow-up

Date: 2026-09-20. Decision: **READY FOR INTERNAL TESTING — SIGNING REQUIRED FOR CLOSED
TESTING**. This decision covers a local technical candidate. The release AAB is unsigned and is
not Play-uploadable; API 26, a 16 KB runtime and the explicitly human-only rows remain open.

The initial `git status --short` was empty at `de4d39c`; no user changes needed reconciliation.
No destructive Git command, commit, production key, secret, production AdMob identifier,
package/application ID change, dependency upgrade or copied game content was introduced.

### Candidate configuration and variant separation

| Item | Value |
|---|---|
| Android Gradle Plugin / Gradle | 8.13.2 / 8.13 |
| Android SDK | compile 36, target 36, min 26 |
| Build Tools | Not pinned; AGP default/minimum 35.0.0, installed with 37.0.0 |
| libGDX / Java | 1.14.2 / source and target 17; host JDK 21 |
| Google SDKs | Mobile Ads 25.5.0; UMP 4.0.0 |

| Variant | Package | Signing | Shrinking | Ads / UMP debug settings |
|---|---|---|---|---|
| debug | `com.projectblue.game` | Android debug key | minify off; resources off | Ads on with official Google sample IDs; optional UMP test values accepted only from the local environment |
| qa | `com.projectblue.game.qa` | Android debug key | R8 on; resources on | Ads off; IDs and UMP debug fields hardcoded empty |
| release | `com.projectblue.game` | Unsigned without owner environment | R8 on; resources on | Ads off and IDs empty without explicit production environment; UMP debug fields hardcoded empty |

The final normal rebuild also has empty debug UMP test fields; the physical test-device hash was
used only through a transient environment variable and is not present in source, documentation
or final artifacts. A source search confirms there is no `consentInformation.reset()` call.

### Native and 16 KB verification

Debug APK, QA APK and release AAB contain only `libgdx.so` for `arm64-v8a`, `armeabi-v7a` and
`x86_64`; Gradle resolves each from `gdx-platform:1.14.2:natives-<abi>`. No Ads/UMP native
library is packaged. Fresh QA passes Build Tools 35.0.0
`zipalign -c -P 16 -v 4`. Bundletool 1.18.1 reports `PAGE_ALIGNMENT_16K`, and all three
`libgdx.so` files have `PT_LOAD p_align=0x4000`. AGP 8.13.2 exceeds the documented AGP 8.5.1
minimum for correct 16 KB packaging.

Both available targets report a 4096-byte kernel page. There is no installed 16 KB AVD/image,
so runtime installation is **MANUAL VERIFICATION REQUIRED**. See
[ANDROID_16KB_COMPATIBILITY.md](ANDROID_16KB_COMPATIBILITY.md).

### Device, consent, advertising and layout results

| Check | Result |
|---|---|
| Huawei SNE-LX1 | Android 10/API 29, 1080x2340, 480 dpi, physical cutout; 4096-byte pages |
| Pixel_8 AVD | Android 16/API 36, 1080x2400, 420 dpi, x86_64; 4096-byte pages |
| API 26 | Not run: no API 26 image/AVD is installed. No large SDK download was made. **MANUAL VERIFICATION REQUIRED.** |
| Forced EEA UMP | First-launch form, reject, Privacy Options visibility/reopen, accept and persisted cold restart passed on the physical device. Offline request error on API 36 reached the game without ads. No ad request was observed before a consent decision. |
| Rewarded | Official Google test creative displayed and reported `Reward granted`; dismissal returned to Pause with hull restored to 100 and 60 seconds added. Callback/duplicate/one-time negative cases pass unit tests. |
| Interstitial | Natural-boundary and failure behavior pass unit tests. A real display was not forced past the three-win/session policy; **MANUAL VERIFICATION REQUIRED**. |
| Compact menu | The reproduced 720x1280 lower status/build collision is fixed through responsive spacing and a compact one-line build label; fonts are unchanged and no empty ad space is reserved. Main actions remain 72 logical units, exactly 48 dp at 360 dp width, and the status row remains 28 units. |
| HUD touch | Pause visual is 64x58 logical with a 96x96 hit rectangle (64x64 dp at 360 dp width). Sonar visual is 108x56 with an independent 132x72 hit rectangle (88x48 dp). The new outside-visual regression passes. |

### Controlled 30-minute sample

The same debug processes were sampled at 0, 10 and 20 minutes while exercising Blue Coast,
result screens, Pause, Home/return, forced-EEA choices, an offline UMP failure and an official
rewarded success. Both processes were force-stopped and cold-started after the 20-minute sample;
the 30-minute row therefore also verifies save reload and shows the expected post-restart reset.

| Target / minute | Java heap KiB | Native heap KiB | Total PSS KiB | Graphics KiB | Battery temperature |
|---|---:|---:|---:|---:|---:|
| Huawei / 0 | 27,932 | 22,632 | Not captured by the Android 10 parser | 4,588 | 32.0 C |
| Huawei / 10 | 27,136 | 22,184 | Not captured by the Android 10 parser | 4,916 | 32.0 C |
| Huawei / 20 | 27,160 | 22,188 | Not captured by the Android 10 parser | 4,916 | 32.0 C |
| Huawei / 30, after restart | 19,116 | 20,680 | 130,286 manual process sample | 4,584 | 32.0 C |
| API 36 emulator / 0 | 18,200 | 29,140 | 157,401 | 0 | 25.0 C, emulated |
| API 36 emulator / 10 | 16,572 | 23,028 | 162,729 | 0 | 25.0 C, emulated |
| API 36 emulator / 20 | 17,540 | 19,344 | 162,556 | 0 | 25.0 C, emulated |
| API 36 emulator / 30, after restart | 22,452 | 30,340 | 160,501 | 0 | 25.0 C, emulated |

An independent Huawei process sample between the 10- and 20-minute marks reported 138,781 KiB
total PSS. Pre-restart Java/native heap and emulator PSS do not show monotonic growth. Crash buffers are
empty and no ANR was observed. The Huawei battery sensor briefly reported 33.0 C after restart
and returned to 32.0 C. SurfaceView `gfxinfo` counted too few physical frames, and the emulator
was sharing the host with Gradle/R8 work; neither stream is presented as trustworthy FPS or
thermal-throttling evidence. No device-side texture/entity counters were available. The desktop
managed-texture recreation/count smoke passed, but physical profiling remains a separate manual
quality check.

### Regression result

The final normal configuration completed **255 tests in 34 suites**, zero failures/errors/skips;
debug and QA lint (zero errors, four existing warnings each); debug APK; R8/resource-shrunk QA
APK; R8/resource-shrunk unsigned release AAB and bundletool validation; desktop distributions;
desktop OpenGL smoke; separate-process save reload; asset inventory and negative license gate;
release-input scan; Android structure/signature/alignment scan; and `git diff --check`.

Final artifacts:

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| `android-debug.apk` | 9,697,750 | `F01B65A3281169A2BB51DA4AF09F8AF3957C6914304138002E35CEBC1DA2F6C6` |
| `android-qa.apk` | 3,588,641 | `9E3EAC6F72F05D3320D937F9943C1545B9E2C8AD609905365E2100CC16078D59` |
| `android-release.aab` | 5,622,905 | `68D1A6C8F8E2D1F3D61E04B99737CD85857F4F68D15E2E1836718ED2D3587886` |

The QA APK is debug-signed and the release AAB has zero signature blocks. Neither is a
production artifact. The historical Prompt 13 report follows and remains useful context; its
older hashes, test count and decision do not describe this follow-up candidate.

Date: 2026-09-19. Decision: **NOT READY FOR RELEASE**.

The automated candidate checks and the device checks described below pass after nine focused
defect fixes. Publication remains blocked by unsigned release output, incomplete manual campaign
and device coverage, privacy/store owner inputs and approved store exports. **The code candidate
is technically suitable for continued closed-testing QA, but no Play-uploadable closed-test
artifact exists until the owner supplies signing and Play Console inputs.**

## Prompt 13 Android device verification

The remaining runtime work was performed locally from IntelliJ's workspace, not in a cloud
environment. The initial working tree was clean. ADB 37.0.1 found both targets:

| Target | Android | Display evidence |
|---|---|---|
| Huawei SNE-LX1 (`HVYDU19124010569`) | Android 10 / API 29 | 1080x2340, 480 dpi, physical top cutout 90 px |
| `sdk_gphone64_x86_64` (`emulator-5554`) | Android 16 / API 36 | 1080x2400, 420 dpi, top cutout 132 px; also exercised at 720x1280 (16:9) |

Installed variants were `android-debug.apk` (`com.projectblue.game`) and the local
`android-qa.apk` (`com.projectblue.game.qa`). The QA variant is non-debuggable, R8/minified,
resource-shrunk, signed with the Android debug certificate and has ads disabled. It is not a
production or Play-uploadable artifact. Debug advertising used only Google's official sample
App ID and rewarded/interstitial units.

| Installed artifact | Size | SHA-256 |
|---|---:|---|
| `android-debug.apk` | 9,697,746 bytes | `603892567C52285F1269C4995FDC1C807796A474F08FBB0B30811C53AF8BFC4F` |
| `android-qa.apk` | 3,588,637 bytes | `E93D0324DA0E1B282FEFA86EB993641D184FE20D313C049D74F21037667BCF07` |

Both APKs use APK Signature Scheme v2 and the same local Android Debug certificate
(`SHA-256 EEF614D0F01E3F2FB8A269331AE44DCE1DC55058ACA6A9963D8CAB26416C9531`).

Passed on both targets: clean install, cold launch, update-over-existing install, main menu,
level select, Blue Coast startup/gameplay, automatic fire, ADB drag movement, pause hit target,
portrait layout, real cutout safe area, Home/background/foreground, Android Back key, screen
off/on, forced process death/relaunch, profile creation/reload, airplane-mode cold launch,
no-network gameplay, debug native/OpenGL startup and minified QA startup. The emulator also
passed recent-task dismissal, a real failed-run result screen, the official Google rewarded
test ad, earned reward dismissal and one-time continue return. Offline ad loads failed without
blocking navigation or gameplay.

No final product failure remains from the executed checks. Two device findings were fixed.
Rewarded continue restored the world correctly but Pause drew cached pre-continue hull/time
labels; `Hud.refresh`, the continue transition and `HudTest` now refresh and cover those labels.
The first final QA launch on the locked API 29 phone also exposed an early-lifecycle crash:
Android called `resume()` before `create()` initialized `ScreenRouter`. Null-safe lifecycle
guards and `ProjectBlueGameLifecycleTest` cover that ordering. The rebuilt minified QA APK then
passed a locked-screen start, three repeated cold starts and a visible launch on the phone, plus
a cold start on the emulator, with an empty crash buffer.

The complete captured crash stack is retained at
`build/device-qa/physical/qa-r8-crash-stacktrace.txt` and was retraced with the matching R8 map:

```text
FATAL EXCEPTION: GLThread 7743
Process: com.projectblue.game.qa, PID: 11274
java.lang.NullPointerException: Attempt to write to field 'boolean i4.l.d' on a null object reference
    at a1.i.onDrawFrame(r8-map-id-4ccb77c6178b42c18a3e9ebe35e1f2198e0a5617a476d1bea9085890e9690119:119)
    at a1.h.run(r8-map-id-4ccb77c6178b42c18a3e9ebe35e1f2198e0a5617a476d1bea9085890e9690119:30)
    at android.opengl.GLSurfaceView$GLThread.guardedRun(GLSurfaceView.java:1521)
    at android.opengl.GLSurfaceView$GLThread.run(GLSurfaceView.java:1281)
```

**MANUAL VERIFICATION REQUIRED:** real simultaneous two-finger input; exhaustive HUD hit-box
measurement and gesture navigation; a successful Blue Coast clear and next-sector unlock;
an on-device boss clear; forced UMP EEA/consent/privacy-options choices; interstitial display
after its three-win/session cooldown; physical-device recent-card dismissal; audible audio
pause/resume; forced Android EGL context loss; API 26 and 16 KB page-size runtimes; tablet/
foldable/multi-window; and the 30-minute memory/thermal soak. ADB cannot honestly certify touch
feel, simultaneous fingers or audible output.

Release blockers remain: an owner-signed production AAB and package/version confirmation, the
public privacy-policy URL and Play Console declarations, approved store exports, and closure of
the high-priority manual campaign/device rows. High findings are the missing human boss/clear
evidence and missing API 26/16 KB runtime. Medium findings are the open touch/accessibility,
context-loss and extended-soak coverage. Cosmetic review remains open for narrow 16:9 layout
crowding and placeholder-art approval.

Principal commands executed for this device pass were:

```powershell
where.exe adb
adb version
adb devices -l
git status --short
.\gradlew.bat :check :lwjgl3:build :android:testDebugUnitTest :android:lintDebug :android:lintQa :android:verifyDebugAdConfiguration :android:assembleDebug :android:assembleQa --rerun-tasks --warning-mode all
.\gradlew.bat :lwjgl3:run --args=--smoke
.\gradlew.bat :lwjgl3:run --args=--smoke-reload
.\gradlew.bat :core:test :android:assembleQa --rerun-tasks --warning-mode all
adb -d install -r android\build\outputs\apk\debug\android-debug.apk
adb -e install -r android\build\outputs\apk\debug\android-debug.apk
adb -d install -r android\build\outputs\apk\qa\android-qa.apk
adb -e install -r android\build\outputs\apk\qa\android-qa.apk
```

All device actions used `adb -d` or `adb -e`; package starts, input, screen capture, lifecycle,
network, package metadata, process and logcat commands were also target-qualified. The final
decision is **NOT READY FOR RELEASE**. The remaining limitations are listed above and in the
manual matrix; no production key, production AdMob ID, AAB device install or Play upload was used.

## Previous automated audit baseline (historical)

The following baseline records the earlier Prompt 13 automated audit. Its HEAD, dirty-tree and
artifact values are historical; the device addendum above records this task's initially clean
`016d001ad0f4a73c2fd7452d15520b03c9bc9bdd` checkout and final rebuilt APKs.

| Item | Inspected candidate |
|---|---|
| Repository / branch | ProjectBlue / `main`; pre-commit QA snapshot |
| Baseline HEAD | `07f65ef45ace3bd9ad52860befce5078e6af260b` |
| Audited artifact version | `0.1.16-g07f65ef45ace-dirty`, versionCode `16` |
| Initial working tree | 34 modified tracked files and 20 untracked status entries; preserved |
| Host | Windows x64; Microsoft OpenJDK 21.0.12; Java source/bytecode 17 |
| Framework/build | libGDX 1.14.2, Gradle 8.13, AGP 8.13.2 |
| Android | minSdk 26, compile/target 36, build-tools 35.0.0 |
| Android SDKs | AdMob 25.5.0, UMP 4.0.0; Android-only dependencies |
| Release configuration | `DEBUG=false`, R8 and resource shrinking enabled, ads disabled, no upload signing supplied |
| Device availability | Huawei SNE-LX1 API 29 and x86_64 emulator API 36; both online |

Read the repository instructions and inventoried source, configuration, tests, documentation,
assets, scripts and Android release setup before editing. Reviewed the shared simulation,
all ten authored missions and bosses, input/render/resource ownership, progression/save
paths, ads/consent boundaries, build rules and asset provenance. No architecture replacement,
Kotlin, new major feature, dependency upgrade, production ad ID or signing secret was added.
Initial status, binary diff and file hashes were preserved under ignored `build/final-qa/`.
No destructive Git command was used. The audit and artifact verification were completed
against the pre-commit snapshot above; the later source commit/push was explicitly requested
after the report was produced and does not change the recorded artifact hashes.

## Executed verification

| Check | Result | Evidence / limits |
|---|---|---|
| Initial baseline | PASS: 198 JUnit tests; desktop and Android builds | `build/final-qa/baseline-build.log`; passing baseline did not cover the defects below |
| Final full unit/headless suite | PASS: **254 tests, 34 suites, 0 failures, 0 errors, 0 skipped** | `core/build/reports/tests/test/index.html`, XML results; all tests rerun after the device fixes |
| Headless world integration | PASS: all 10 sectors x 4 difficulties; boss/environment/save tests | Included in the 254 count, not an additional test count; no separate headless module |
| Desktop build/distributions | PASS | `:lwjgl3:build`; Java-runtime distribution built; Windows installer packaging was not rerun |
| Desktop GL smoke | PASS | Real OpenGL process, all ten sector render paths, menus, drag, pause/lifecycle, results, replay, equipment, settings and aspect ratios |
| Texture recovery regression | PASS | Smoke deletes generated UI/font GPU textures, invalidates/reloads managed textures and checks valid handles/count; not actual Android OS context loss |
| Separate-process save reload | PASS | `--smoke-reload` restores campaign, equipment, achievements and settings from isolated smoke profile |
| Android debug APK | PASS | `:android:assembleDebug`; debug signature, ZIP alignment and 64-bit native alignment verified |
| Android release AAB | PASS as **unsigned verification artifact** | `:android:bundleRelease`, R8/resource shrinking and `:android:verifyReleaseBundle` passed |
| Android debug unit tests | NO-SOURCE | Task executed; no Android unit/instrumentation tests exist. No device test pass is claimed |
| Desktop test source set | NO-SOURCE | Integration coverage comes from the separate GL smoke run |
| Android lint debug/release | PASS with warnings: **0 errors, 4 warnings per variant** | `android/build/reports/lint-results-debug.*`, `lint-results-release.*` |
| IntelliJ static inspections | No errors in requested changed Java files; 44 warnings returned | `build/final-qa/final-ide-inspections.json`; reviewed below; not whole-program proof |
| Asset inventory/hash gate | PASS | Exactly five custom media files plus two exempt inventory documents; isolated regeneration matches all five |
| Asset-gate negative regression | PASS | Unlisted file under `assets/licenses/` rejected; temporary probe removed |
| Release-input scan | PASS | Only official demo ad identifiers; ignored sensitive paths; no literal signing credentials found |
| Packaged content inspection | PASS | Debug APK and AAB contain all ten mission JSONs, catalog and expected custom assets/inventories |
| Minimum-API regression | PASS in tests and DEX inspection | Incompatible `InputStream.readAllBytes()` invocation removed from MissionConfig debug DEX |
| Script syntax / diff whitespace | PASS | PowerShell parser checks and `git diff --check`; Git line-ending notices are not whitespace errors |
| Versioning fixture script | Not run | `tools/test-versioning.ps1` creates temporary Git commits; omitted to honor the no-commit instruction |
| Android runtime / R8 device execution | PASS with manual gaps | Debug ran on API 29 and 36; debug-signed minified QA APK ran on both. API 26, 16 KB and full manual campaign coverage remain open |

The complete final Gradle command was:

```powershell
.\gradlew.bat :check :lwjgl3:build :android:testDebugUnitTest :android:lintDebug :android:lintRelease :android:verifyDebugAdConfiguration :android:assembleDebug :android:bundleRelease :android:verifyReleaseBundle --rerun-tasks --warning-mode all
```

It completed successfully in 2m 39s with 118 tasks executed. Additional commands and
publication steps are in [RELEASE_STEPS.md](RELEASE_STEPS.md). The final GL smoke and reload
logs are `build/final-qa/final-smoke.log` and `final-smoke-reload.log`; captures are under
`build/smoke/`. Build evidence is local/ignored and should be archived with the candidate
before those output directories are replaced by another run.

## Defects fixed and regression evidence

| Fix | Before / root cause | Minimal correction and validation |
|---|---|---|
| F-01 Android API compatibility | Required mission loading directly invoked `InputStream.readAllBytes()` in the generated DEX, although that method requires API 33 and minSdk is 26. Android lint did not catch this core-module use. | Shared buffered reader with a 128 KiB limit; tests simulate an unavailable API and oversized input; final DEX has no such invocation. [Android API reference](https://developer.android.com/reference/java/io/InputStream#readAllBytes()) |
| F-02 Final-escape continue | Debugger observed `finished=true`, `ESCAPE_FAILED`, full hull after continue and one update. The failed boss state survived the continue. | Restart escape warning for active/failed escape; single-use continue remains enforced. Eight parameterized final completion/continue cases pass across four difficulties. |
| F-03 Two-finger actions | Debugger observed movement pointer 0 causing pointer 1 to return before sonar handling. | Check pause/sonar hit targets before rejecting an additional movement pointer; three input regressions pass. |
| F-04 Texture recreation | Debugger reported a generated menu texture `managed=false`; the new GL smoke assertion failed. Pixmaps were disposed immediately after uploading unmanaged textures. | Retain the tiny CPU sources for managed reload and dispose them with the theme. Real GL delete/reload smoke passes. |
| F-05 Wildlife pool cleanup | Debugger observed a freed creature still active at x=600 and y=1757 after 600 frames: vortex clamping prevented the strict greater-than despawn condition. | Include the boundary and upper escape bound; regression confirms despawn and reuse of the same pool slot. |
| F-06 Laser/environment collision | Debugger observed coral health 30, enemy health 40 and coral damage 0 after a laser shot through coral. | Nearest-target laser selection includes protected coral, records habitat damage and never grants coral kill/salvage rewards; regression passes. |
| F-07 Unregistered asset bypass | The entire `licenses/` subtree was excluded from the packageable-file inventory. | Only two exact metadata files are exempt; hash keys and documented paths must match the allowlist. Negative probe test passes. |
| F-08 Rewarded-continue HUD cache | On the API 36 emulator, rewarded continue restored the world but Pause displayed cached pre-continue hull/time text while the live health bar was full. | Refresh cached HUD strings immediately after `continueAfterFailure`; `HudTest` verifies the label changes from `HULL 0/100` to `HULL 100/100`. |
| F-09 Early Android lifecycle crash | A debug-signed minified QA cold start behind the API 29 lock screen delivered `resume()` before `create()` initialized `ScreenRouter`, causing a GL-thread null dereference. | Guard pre-create `pause()`/`resume()` dependencies; `ProjectBlueGameLifecycleTest` reproduces both callbacks before creation. The rebuilt QA APK passed locked-screen startup, three physical cold starts and an emulator cold start with no crash. |

Failing-before and passing-after logs are retained in `build/final-qa/` as
`regressions-before/after.log`, `campaign-before/after.log`, `texture-before.log`,
`texture-after-smoke.log`, and `android-api-before/after.log`. Runtime investigations used
IntelliJ debugger values/breakpoints after short-lived logpoint sessions were insufficient.
Agent-created breakpoints and sessions were removed; existing disabled user exception
breakpoints were preserved. No speculative Android runtime pass was inferred from JVM evidence.

Local source anchors for the runtime fixes: [continue state](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/logic/GameWorld.java:60),
[escape reset](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/logic/LeviathanCore.java:49),
[pointer dispatch](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/input/PointerInput.java:22),
[managed texture ownership](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/ui/MenuTheme.java:64),
[wildlife cleanup](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/logic/GameWorld.java:1343),
[laser target selection](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/logic/GameWorld.java:1429)
[compatible mission reader](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/config/MissionConfig.java:300),
[rewarded HUD refresh](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/ui/Hud.java:25) and
[pre-create lifecycle guards](D:/Workspace/ProjectBlue/core/src/main/java/com/projectblue/game/ProjectBlueGame.java:50).

## Gameplay, progression and content assessment

Movement, relative drag, automatic fire, all five weapons, bullets/collisions, hull/shield,
cleanup beam, waste collection, rescue, environmental penalties, pauses, results, bosses,
finale and replay have source/test coverage. The second-finger, laser/coral, freed-creature
and final-escape defects above are now covered explicitly. Active sonar is available in
sonar-enabled sectors; vessel special-ability descriptions otherwise represent passive
bonuses. Physical controls and readability remain open device checks.

All ten sector records, sequential sector/difficulty locks, independent best stars/scores/
Cleanup/Rescue, salvage, six upgrade types, four pilots, three vessels, five weapons,
sixteen local achievements and final completion were reviewed against config and tests.
Schema v5 preserves earlier profile formats through explicit/additive migrations; frozen
v2 data, checksums, backup recovery, temporary/atomic writes and write-failure rollback are
covered. Corrupt primary and backup fall back visibly to defaults. In-progress dives are
not persisted across process death; completed progression is.

[MANUAL_TEST_MATRIX.md](MANUAL_TEST_MATRIX.md) contains a row for every sector's theme,
environment problem, mechanic, enemy/rescue roster, midpoint, boss/start/deadline, rewards
and NEREID/story connection, plus all 40 human-clear slots. Recovery rendering/result views
and difficulty multipliers are present for all ten. Blue Coast's midpoint is a generic
fallback, not a distinct authored encounter; Ghost Nets' NEREID link is indirect.

The 40 route tests restore hull, execute the full timeline and expect deadline failure
without bypassing boss gates. They verify finite player coordinates, bullet limits and
authored enemy counts. Boss tests/desktop smoke also use assisted combat in places.
These are **not evidence of ordinary-input clearability, fair difficulty or first-clear
economy balance**. The four-difficulty final gate/escape tests do confirm that the tested
restoration/energy/sonar sequence can reach completion with the standard loadout under
their assisted conditions.

## Mobile, advertising and technical assessment

Portrait FitViewport and menu ExtendViewport paths, back navigation, input cancellation,
pause/resume and save-on-background are implemented. Android window insets are handled in
the launcher, but physical cutouts/asymmetric padding need testing. Narrow/wide desktop
captures pass layout navigation; small sonar/pause targets, HUD text and overlapping
achievement/story banners remain findings. Large UI does not comprehensively scale the
game HUD. Reduced particles, background detail and motion are available.

Debug uses official Google test IDs. Reward settlement requires earned+dismissed callbacks
and a successful durable claim, with duplicate/stale/failure paths tested. Continue is
limited to one per run. Interstitials are restricted to successful-result navigation, skip
the first session, and use three-clear/three-minute caps. Consent relies on UMP permission;
Privacy Options invalidates stale ads. Offline/no-fill paths preserve gameplay/navigation
in shared tests/source review. Android SDK callbacks, forms, region changes and process
interruption remain unverified. No production identifiers were found in repository inputs;
the tested release disables ads and uses empty reward/interstitial unit IDs.

Entities use fixed pools; offscreen cleanup and listener/resource ownership were reviewed.
AssetManager owns audio/font assets, theme owns generated textures/source pixels, and
screen transitions release stages/listeners. The vortex pool retention and unmanaged
texture defects were corrected. This source/short-smoke review does not establish absence
of leaks: device heap/native-memory and thermal soak remain required. Some route/render/
menu allocations remain. Config field/range/ID validation exists; malformed cross-subsystem
combinations still need stronger validation before future content additions.

Release BuildConfig is non-debuggable; reset code is listed as removed in R8 `usage.txt`.
Desktop smoke code is outside Android dependencies. The release contains all mission data;
bundletool validates structure and native alignment checks pass. Runtime after obfuscation
has not been exercised on Android. Secret scans/log review found no credential or profile
payload logging in the inspected paths; that is scoped evidence, not a universal guarantee.

## Warnings reviewed

- Android lint: `UnusedAttribute` for predictive-back attribute on older APIs;
  `AndroidGradlePluginVersion` recommends a newer wrapper; `LockedOrientationActivity`
  and `DiscouragedApi` flag portrait restrictions/Android 16 large-screen behavior.
  The verified pinned build pair was retained; orientation requires device acceptance.
- Javac notes legacy inset API use in AndroidLauncher. The API 26-29 compatibility path
  needs those calls; newer versions use their guarded inset APIs.
- The final build reports SDK XML version 4 being read by tooling supporting up to 3.
  Build/lint/artifact checks still pass. Align SDK/tooling versions in a separately verified
  environment before publication; no opportunistic toolchain upgrade was made here.
- IntelliJ warnings include unused members/constants, redundant escapes/casts, nullable
  mission/subsystem paths and a stream-copy efficiency suggestion. Shipped mission paths
  pass the 40-route suite; future invalid config combinations are MP-02. The bounded UTF-8
  byte conversion was retained rather than adopting a newer Android-incompatible overload.
- Deprecated Gradle `shrinkResources` assignment syntax was corrected. Git's LF/CRLF notices
  remain host configuration notices; `git diff --check` reports no whitespace errors.

## Generated artifacts

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| `android/build/outputs/apk/debug/android-debug.apk` | 9697750 | `bc0e79716ba0fe1c5c0000f124e0897267256ddbb06e5ec2d3f10ae9134bfec8` |
| `android/build/outputs/bundle/release/android-release.aab` | 5621725 | `5f677333c5a77b878d04b04b15cdaf64e895e8d2fcbaec13e096dbaba7466ceb` |

APK is debug-signed; AAB has **zero signature blocks**. These hashes identify local QA
outputs, not publishable signed releases. Both include arm64-v8a, armeabi-v7a and x86_64;
64-bit ELF LOAD alignment is at least 16384 bytes, and debug APK ZIP alignment passes 16 KB
verification. Actual 16 KB device execution is still required. Preserve the matching
`android/build/outputs/mapping/release/mapping.txt` with a later signed artifact.

## Files changed by this audit

Pre-existing user edits remain in these and other files. The list below describes this
audit's additional changes, not ownership of the entire working-tree diff.

- Production Java: `core/.../config/MissionConfig.java`, `input/PointerInput.java`,
  `logic/GameWorld.java`, `logic/LeviathanCore.java`, `ui/MenuTheme.java`.
- Regression/integration: `core/src/test/java/com/projectblue/game/config/MissionConfigTest.java`,
  `input/PointerInputTest.java` (new), `logic/NereidCoreWorldTest.java`,
  `logic/ReleaseCandidateWorldTest.java` (new), and
  `lwjgl3/src/main/java/com/projectblue/game/lwjgl3/DesktopSmokeGame.java`.
- Build/tooling: root `build.gradle`, `android/build.gradle`, new
  `tools/test-asset-licenses.ps1`.
- Inventory/current guidance: `assets/licenses/ASSET_LICENSES.md`, `README.md`,
  `docs/SECTORS_2_3.md`, `docs/EQUIPMENT_SYSTEMS.md`, `docs/META_PROGRESSION.md`,
  `docs/STORE_ASSET_PLAN.md`.
- Requested reports: this file, [KNOWN_ISSUES.md](KNOWN_ISSUES.md),
  [RELEASE_STEPS.md](RELEASE_STEPS.md), [ASSET_AUDIT.md](ASSET_AUDIT.md),
  [MANUAL_TEST_MATRIX.md](MANUAL_TEST_MATRIX.md), [STORE_LISTING_DATA.md](STORE_LISTING_DATA.md).

## Final classification

| Classification | Outstanding findings |
|---|---|
| Release blocker | RB-01 unsigned AAB/owner package-version confirmation; RB-02 Android runtime acceptance; RB-03 public privacy/developer/Play declarations; RB-04 approved store exports |
| High priority | HP-01 human campaign/balance validation; HP-02 physical touch/HUD usability; HP-03 production ads if enabled; HP-04 Blue Coast authored midpoint gap |
| Medium priority | MP-01 allocation/soak profiling; MP-02 config cross-validation; MP-03 cutout/inset behavior; MP-04 story link; MP-05 ability expectation; MP-06 process-death limitations |
| Cosmetic | C-01 placeholders; C-02 banner/text overlap; C-03 generic recovery wording |
| Manual verification required | Device/lifecycle/R8/ads/accessibility and 40 human clears listed in the manual matrix |

Detailed closure criteria are in [KNOWN_ISSUES.md](KNOWN_ISSUES.md). Until release blockers
have evidence of closure, the decision remains **NOT READY FOR RELEASE**. No signing-key
creation, store upload or publication was performed by this audit.
