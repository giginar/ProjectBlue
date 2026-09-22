# Final release candidate known issues

## Prompt 13 follow-up reclassification (2026-09-20)

No previously recorded finding was deleted. The current technical candidate is **READY FOR
INTERNAL TESTING — SIGNING REQUIRED FOR CLOSED TESTING**; this is not production or Play upload
approval.

| Existing ID | Current classification and evidence |
|---|---|
| RB-01 | Still a release blocker. The release AAB builds and validates but is unsigned; no owner upload key, Play package ownership check or Play version-code check was supplied. |
| RB-02 | Narrowed, not closed. Static 16 KB packaging and all ELF LOAD alignments pass, and API 29/API 36 runtime checks pass. API 26 and an actual 16 KB kernel remain **MANUAL VERIFICATION REQUIRED**, as do tablet/foldable and human campaign coverage. |
| RB-03 | Unchanged owner/Play Console blocker: privacy URL, audience decision, declarations, rating and account inputs remain absent. |
| RB-04 | Resolved: Play icon, feature graphic and seven final screenshots were exported under `store/assets/google-play/ocean-guard/`. |
| HP-01 | Unchanged. Automated routes do not prove ordinary-input clears, boss completion or progression unlocks. |
| HP-02 | Technical layout portion fixed. The compact menu now preserves its status/build rows at 720x1280 without shrinking fonts. The sonar visual remains 108x56 logical units, while its independent hit rectangle is now 132x72 logical units (88x48 dp at 360 dp width); the pause hit rectangle remains 96x96 logical units (64x64 dp). Human touch feel and simultaneous two-finger input remain manual. |
| HP-03 | Narrowed. Forced-EEA first form, reject, Privacy Options reopen, accept, restart persistence, error fallback and a real rewarded test ad passed with Google's test mechanisms. A real interstitial at the production policy boundary, early ad dismissal and background/process death during forms/ads remain manual. Production ads remain disabled and unconfigured. |
| MP-01 | A controlled 30-minute API 29/API 36 sample was added; no ANR or monotonic heap/PSS growth was observed. It is a bounded sample, not a minimum-device profiler campaign; trustworthy physical FPS/throttling and explicit texture/entity counters remain open. |
| C-04 | Fixed for the observed 720x1280 lower-menu collision and covered by the desktop GL smoke. Real narrow-phone visual approval remains manual. |

Detailed 16 KB evidence is in [ANDROID_16KB_COMPATIBILITY.md](ANDROID_16KB_COMPATIBILITY.md).
The historical audit below is retained to show why each item existed.

Audit date: 2026-09-19. Status: **NOT READY FOR RELEASE**.
See [test evidence](FINAL_TEST_REPORT.md) and [device acceptance steps](MANUAL_TEST_MATRIX.md).
These findings describe the inspected working tree, including pre-existing uncommitted work.

## Release blocker

| ID | Finding | Required closure evidence |
|---|---|---|
| RB-01 | The generated release AAB is unsigned. No upload signing environment was provided; Play package ownership and highest uploaded versionCode are unverified. | Provision the owner's existing upload key through secret storage, confirm package/version, build and verify the signed AAB, and retain its matching R8 mapping. Do not create or commit a key during QA. |
| RB-02 | Debug and local minified QA APKs ran on API 29 hardware and an API 36 emulator, but the minimum API 26, 16 KB page-size environment, tablet/foldable and full human campaign/boss routes remain unverified. | Pass the remaining mandatory rows using APKs generated from the final signed AAB, including API 26, 16 KB, full campaign/boss, context-loss and extended-soak coverage. |
| RB-03 | Privacy/support Pages are prepared with Blueborn Games and ykucukcinar@gmail.com, but the URLs are not verified live. Legal developer identity, audience decision, content rating and the Play Data Safety declaration remain open. | Activate GitHub Pages, verify the expected URLs, resolve `LEGAL NAME INPUT REQUIRED`, reconcile declarations with the final binary and SDK configuration, and complete Play Console requirements. |
| RB-04 | Resolved: required Play icon, feature graphic and seven accurate game screenshots are present. | Review the listing and upload the accepted exports. See [listing data](STORE_LISTING_DATA.md). |

## High priority

| ID | Finding | Next action |
|---|---|---|
| HP-01 | The 40 mission/difficulty simulations prove timeline execution and deadline termination, not human clearability. Boss gates are tested with assisted damage/health in several tests. | Play all 40 combinations with ordinary input and representative reachable equipment; record completion, stars, rewards and remaining time. Escalate an impossible mandatory route to a release blocker. |
| HP-02 | Sonar and some pause controls use approximately 55-56 logical units. At a 360 dp phone width, 56/540 of the screen is about 37 dp; HUD text is also small. The large-UI option primarily scales Scene2D menus. | Measure touch bounds and text on small phones. Expand undersized targets and verify HUD/pause accessibility without blocking gameplay. The pause button's larger input rectangle must be measured separately from its visual bounds. |
| HP-03 | The official rewarded test ad and offline no-fill fallback passed on the API 36 emulator, and UMP returned a non-EEA result on both devices. Forced EEA consent choices, Privacy Options and a real interstitial placement were not exercised. Production ads remain disabled/unconfigured. | For an ad-supported release, use owner-approved identifiers/audience settings and pass forced-region consent, Privacy Options, interstitial, background and stale-callback device tests. An owner-approved ad-free release can retain ads disabled. |
| HP-04 | Blue Coast has no separately authored midpoint encounter: its midpoint uses the generic fallback message/window over normal waves. | Decide whether the requested distinctive midpoint is a release acceptance requirement. If so, author and test it in a separate content task; this QA did not add a new encounter. |

## Medium priority

| ID | Finding | Next action |
|---|---|---|
| MP-01 | Some per-frame allocations remain, including route objects, a creature-rendering direction array and menu status strings. No sustained device allocation/FPS/thermal profile was available. | Profile on the minimum supported device before targeted optimization; existing fixed pools do not imply zero allocation. |
| MP-02 | New invalid combinations of environment kinds and subsystem configuration can pass local field validation and later dereference a missing subsystem. All shipped 40 routes executed successfully. | Strengthen cross-field authoring validation before introducing new mission data. |
| MP-03 | The API 29 phone (90 px cutout) and API 36 emulator (132 px cutout) kept HUD/menu content inside the top safe area. Cutout rotation/sides, gesture navigation, tablets and multi-window are still unmeasured. | Test both cutout sides, gesture navigation, tablets and multi-window with physical interaction. |
| MP-04 | Ghost Nets links to NEREID indirectly through the campaign, while its local briefing names abandoned fishing grounds. | Review story continuity if every sector must state an explicit NEREID connection. |
| MP-05 | Submarine special-ability descriptions represent passive loadout bonuses. The active sonar action exists only in sectors with the sonar subsystem (6, 8, 10). | Keep store/tutorial claims accurate; a universal active ability would be a separate feature. |
| MP-06 | In-progress dives are not checkpointed across process death. An earned ad reward can be lost before dismissal/persistence; local claim IDs prevent duplicates but are not server verification. | Verify this documented behavior on device and decide whether it meets product expectations. Completed profile data must survive. |

## Cosmetic

| ID | Finding | Next action |
|---|---|---|
| C-01 | Font, procedural art, synthesized audio and platform icons remain original placeholders. | Approve their release quality or replace them with verified assets; retain reproducible sources and update hashes. |
| C-02 | Achievement banners can overlap the current story message/menu entry. Long centered story lines and the bitmap font need a readability pass; desktop captures show the overlap. | Review narrow-screen captures and sequence banners/wrap text in a focused UI task. |
| C-03 | Some recovery labels, such as wildlife returning, are generic even when the run rescued no creatures. | Make result copy reflect actual run metrics. |
| C-04 | The emulated 720x1280 (16:9) portrait layout remained usable but showed crowded lower-menu/build text. | Review and tune the narrow-phone layout on a real small-screen device before approving store captures. |

## Manual verification required

Rows explicitly marked `MANUAL VERIFICATION REQUIRED` in
[MANUAL_TEST_MATRIX.md](MANUAL_TEST_MATRIX.md) remain open. Especially: real two-finger input,
all weapon/target interactions, 40 human campaign clears, an on-device boss clear, Android save
migration/interrupted writes, forced EGL context loss, API 26 and 16 KB runtime, forced EEA UMP,
interstitial display, physical recent-card dismissal, audible audio behavior, the 30-minute
memory/thermal soak, accessibility and store-console validation.

## Fixed during this audit

| ID | Defect and minimal correction | Regression evidence |
|---|---|---|
| F-01 | Required missions invoked `InputStream.readAllBytes()`, unavailable below Android API 33. Reused a bounded buffered reader compatible with minSdk 26. | `MissionConfigTest`: old-API stream simulation and 128 KiB limit. Baseline debug DEX contained the incompatible call. |
| F-02 | Continuing after the final escape timed out immediately failed again because the boss stayed in `ESCAPE_FAILED`. Restart the escape warning when continuing. | `NereidCoreWorldTest`: successful clear and single-use continue on all four difficulties. |
| F-03 | A second finger could not activate sonar/pause while the movement finger was held. Check action targets before rejecting a second movement pointer. | `PointerInputTest`: movement ownership, second-finger action and input boundaries. |
| F-04 | Generated menu textures were unmanaged and could not survive GL context loss. Retain source pixmaps and use managed texture data; dispose retained sources at shutdown. | Desktop smoke deletes/reloads GPU textures and validates restored handles and stable managed-texture count. |
| F-05 | Freed wildlife could remain clamped at the vortex boundary indefinitely and occupy a pool slot. Include the exact boundary and the upper escape bound in cleanup. | `ReleaseCandidateWorldTest`: despawn and reuse the same slot. |
| F-06 | Laser fire ignored protected coral and damaged enemies behind it without the environmental penalty. Include coral in nearest-hit selection without kill/salvage rewards. | `ReleaseCandidateWorldTest`: coral damage, blocked enemy, no habitat reward, later enemy hit. |
| F-07 | The asset gate exempted every file under `licenses/`, allowing unregistered packaged assets. Exempt only the two inventory documents and cross-check inventory keys/paths. | `tools/test-asset-licenses.ps1`: an isolated unlisted probe under `licenses/` must fail the gate and is then removed. |
| F-08 | Rewarded continue restored the world to full hull and added time, but Pause rendered cached pre-continue hull/time labels. Refresh cached HUD labels immediately after the world mutation. | `HudTest` reproduces failure, continues, refreshes and verifies `HULL 100/100`. |
| F-09 | Android could call `ProjectBlueGame.resume()` before `create()` initialized the screen router; the API 29 phone exposed the resulting null dereference in the minified QA build. Guard lifecycle dependencies until creation completes. | `ProjectBlueGameLifecycleTest` calls pause/resume before creation; rebuilt QA passed locked-screen startup, three cold starts on API 29 and a cold start on API 36. The full suite contains 254 passing tests. |

Deprecated Gradle `shrinkResources` assignment syntax was also updated without changing
release behavior. No large gameplay feature, signing key, production ad identifier or commit
was introduced.
