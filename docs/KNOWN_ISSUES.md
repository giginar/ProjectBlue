# Final release candidate known issues

Audit date: 2026-09-19. Status: **NOT READY FOR RELEASE**.
See [test evidence](FINAL_TEST_REPORT.md) and [device acceptance steps](MANUAL_TEST_MATRIX.md).
These findings describe the inspected working tree, including pre-existing uncommitted work.

## Release blocker

| ID | Finding | Required closure evidence |
|---|---|---|
| RB-01 | The generated release AAB is unsigned. No upload signing environment was provided; Play package ownership and highest uploaded versionCode are unverified. | Provision the owner's existing upload key through secret storage, confirm package/version, build and verify the signed AAB, and retain its matching R8 mapping. Do not create or commit a key during QA. |
| RB-02 | No Android device/emulator was connected. A successful R8 build does not establish runtime compatibility, lifecycle recovery, native loading or mobile usability. | Pass the mandatory device rows in the manual matrix using APKs generated from the final minified AAB, including API 26 and API 36, cutouts, process death, offline startup and a 16 KB environment. |
| RB-03 | No public privacy-policy URL, confirmed developer identity/contact, audience decision, content rating or completed Play Data Safety declaration is available. The in-app disclosure is a draft without a public policy link. | Publish an owner-approved policy, provide its link in the app/store, and reconcile declarations with the final binary and SDK configuration. Complete Play Console requirements. |
| RB-04 | Required approved store exports are absent: Play icon, feature graphic and actual device screenshots. Existing smoke captures are QA evidence only. | Produce accurate original store assets, record their provenance, review the listing and upload accepted exports. See [listing data](STORE_LISTING_DATA.md). |

## High priority

| ID | Finding | Next action |
|---|---|---|
| HP-01 | The 40 mission/difficulty simulations prove timeline execution and deadline termination, not human clearability. Boss gates are tested with assisted damage/health in several tests. | Play all 40 combinations with ordinary input and representative reachable equipment; record completion, stars, rewards and remaining time. Escalate an impossible mandatory route to a release blocker. |
| HP-02 | Sonar and some pause controls use approximately 55-56 logical units. At a 360 dp phone width, 56/540 of the screen is about 37 dp; HUD text is also small. The large-UI option primarily scales Scene2D menus. | Measure touch bounds and text on small phones. Expand undersized targets and verify HUD/pause accessibility without blocking gameplay. The pause button's larger input rectangle must be measured separately from its visual bounds. |
| HP-03 | Production AdMob/UMP operation is not device-tested. The tested release has ads disabled. | For an ad-supported release, explicitly configure owner-approved IDs/audience through the environment and pass consent, Privacy Options, reward, no-fill, background and stale-callback device tests. This becomes a blocker if ads are enabled. An owner-approved ad-free release can retain ads disabled. |
| HP-04 | Blue Coast has no separately authored midpoint encounter: its midpoint uses the generic fallback message/window over normal waves. | Decide whether the requested distinctive midpoint is a release acceptance requirement. If so, author and test it in a separate content task; this QA did not add a new encounter. |

## Medium priority

| ID | Finding | Next action |
|---|---|---|
| MP-01 | Some per-frame allocations remain, including route objects, a creature-rendering direction array and menu status strings. No sustained device allocation/FPS/thermal profile was available. | Profile on the minimum supported device before targeted optimization; existing fixed pools do not imply zero allocation. |
| MP-02 | New invalid combinations of environment kinds and subsystem configuration can pass local field validation and later dereference a missing subsystem. All shipped 40 routes executed successfully. | Strengthen cross-field authoring validation before introducing new mission data. |
| MP-03 | Android pads the game view for window insets and menus also apply inset padding. Physical cutout placement and asymmetric left/right padding need measurement. | Test both cutout sides, gesture navigation, tablets and multi-window; avoid guessing from desktop viewport captures. |
| MP-04 | Ghost Nets links to NEREID indirectly through the campaign, while its local briefing names abandoned fishing grounds. | Review story continuity if every sector must state an explicit NEREID connection. |
| MP-05 | Submarine special-ability descriptions represent passive loadout bonuses. The active sonar action exists only in sectors with the sonar subsystem (6, 8, 10). | Keep store/tutorial claims accurate; a universal active ability would be a separate feature. |
| MP-06 | In-progress dives are not checkpointed across process death. An earned ad reward can be lost before dismissal/persistence; local claim IDs prevent duplicates but are not server verification. | Verify this documented behavior on device and decide whether it meets product expectations. Completed profile data must survive. |

## Cosmetic

| ID | Finding | Next action |
|---|---|---|
| C-01 | Font, procedural art, synthesized audio and platform icons remain original placeholders. | Approve their release quality or replace them with verified assets; retain reproducible sources and update hashes. |
| C-02 | Achievement banners can overlap the current story message/menu entry. Long centered story lines and the bitmap font need a readability pass; desktop captures show the overlap. | Review narrow-screen captures and sequence banners/wrap text in a focused UI task. |
| C-03 | Some recovery labels, such as wildlife returning, are generic even when the run rescued no creatures. | Make result copy reflect actual run metrics. |

## Manual verification required

All rows marked Pending in [MANUAL_TEST_MATRIX.md](MANUAL_TEST_MATRIX.md) remain open.
Especially: real two-finger input, all weapon/target interactions on device, 40 human campaign
clears, minified install/upgrade, save migration on Android, interruption during file writes,
context loss, low-memory process recreation, consent-region changes, ad lifecycle callbacks,
30-minute memory/thermal soak, accessibility and store-console validation.

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

Deprecated Gradle `shrinkResources` assignment syntax was also updated without changing
release behavior. No large gameplay feature, signing key, production ad identifier or commit
was introduced.
